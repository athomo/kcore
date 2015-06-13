import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeSet;
import java.util.logging.Logger;

import edu.cmu.graphchi.ChiFilenames;
import edu.cmu.graphchi.ChiLogger;
import edu.cmu.graphchi.ChiVertex;
import edu.cmu.graphchi.GraphChiContext;
import edu.cmu.graphchi.GraphChiProgram;
import edu.cmu.graphchi.datablocks.IntConverter;
import edu.cmu.graphchi.engine.GraphChiEngine;
import edu.cmu.graphchi.engine.VertexInterval;
import edu.cmu.graphchi.io.CompressedIO;
import edu.cmu.graphchi.preprocessing.EdgeProcessor;
import edu.cmu.graphchi.preprocessing.FastSharder;
import edu.cmu.graphchi.preprocessing.VertexIdTranslate;
import edu.cmu.graphchi.preprocessing.VertexProcessor;
import edu.cmu.graphchi.util.IdInt;
import edu.cmu.graphchi.util.Toplist;

/**
 * K-core decomposition algorithm
 *
 * Outputs: a file containing key-value pairs: vertexId, coreness
 *
 * How does it work ?
 * 1 - Initializes vertex values to their degrees then those values are communicated to neighbors. 
 * 2 - for each vertex v, an upper-bound is computed on its coreness based on the values received from neighbors.
 * 3 - if the upper-bound is better than its current value, v updates its value with the upper-bound.
 * 4 - Steps 2 and 3 are repeated until no more value updates are occurring.
 *
 * KCoreDecomposer is inspired from the algorithm presented in the following paper:
 * 
 * A. Montresor, F. De Pellegrini, and D. Miorandi. Distributed k-core decomposition. 
 * Parallel and Distributed Systems, IEEE Trans., 24(2), 2013.
 *
 *
 * @author Wissam Khaouid, wissamk@uvic.ca, Alex Thomo, thomo@uvic.ca
 */

public class KCoreGC_M implements GraphChiProgram<Integer, Integer> {

    public static final int INFINITY = Integer.MAX_VALUE;

    protected int vertexValuesUpdated;
    protected int nVertexesScheduled;
    protected int nVertexes = 0;

    private int nIterations = 0;

    private static Logger logger = ChiLogger.getLogger("kCoreDecomposition");

    
    public void update(ChiVertex<Integer, Integer> v, GraphChiContext context) {

        int iteration = context.getIteration();
        int d_v = v.numOutEdges();

        if (iteration == 0) {
            v.setValue(d_v);
            broadcastValue(v, d_v);
            nVertexes++;
            vertexValuesUpdated++;
            context.getScheduler().addTask(v.getId());
            nVertexesScheduled++;
        } else {
        	int localEstimate = computeUpperBound(v);
        	if(localEstimate < v.getValue()) {
        		v.setValue(localEstimate);
                broadcastValue(v, localEstimate);
                vertexValuesUpdated++;
                
                //Only schedule vertices that have some chance of getting their upper bound tightened.  
                for(int i=0; i<d_v; i++) {
					int u = v.inEdge(i).getVertexId(); //N_v[i];
					int core_u = v.inEdge(i).getValue();
					if(localEstimate<=core_u) {
						context.getScheduler().addTask(u);
						nVertexesScheduled++;
					}
				}               
        	}
        }
    }

    
	int computeUpperBound(ChiVertex<Integer, Integer> v) {
		
		int d_v = v.numOutEdges();
		int core_v = v.getValue();
		
		int[] c = new int[core_v+1];
		for(int i=0; i<=core_v; i++)
			c[i]=0;
		
		for(int i=0; i<d_v; i++) {
			int core_u = v.inEdge(i).getValue();
			int j = Math.min(core_v, core_u);
			c[j]++;
		}
		
		int cumul = 0;
		for(int i=core_v; i>=2; i--) {
			cumul = cumul + c[i];
			if (cumul >= i)
				return i;
		}
		
		return d_v;
	}

    /**
     * Broadcasts a value to the neighbors by writing it to the out-edges
     */

    public void broadcastValue(ChiVertex<Integer, Integer> vertex, int value) {
        for(int i = 0; i < vertex.numOutEdges(); i++) {
            vertex.outEdge(i).setValue(value);
        }
    }

    /**
     * Invoked with the start of a new iteration
     */
    public void beginIteration(GraphChiContext ctx) {
        vertexValuesUpdated = 0;
        nVertexesScheduled = 0;
    }

    /**
     * Invoked at the end of every iteration
     */
    public void endIteration(GraphChiContext ctx) {
        System.out.println(vertexValuesUpdated + " updates.");
        System.out.println(nVertexesScheduled + " vertices scheduled for the next iteration.");
        System.out.println("iteration " + ctx.getIteration() + " ends.");

        nIterations ++;
        if( vertexValuesUpdated == 0 ) {
            System.out.println("no updates in this round. No more rounds .. KCore-montresor terminates!");
            ctx.getScheduler().removeAllTasks();
        }
    }

    public void beginInterval(GraphChiContext ctx, VertexInterval interval) {}

    public void endInterval(GraphChiContext ctx, VertexInterval interval) {}

    public void beginSubInterval(GraphChiContext ctx, VertexInterval interval) {}

    public void endSubInterval(GraphChiContext ctx, VertexInterval interval) {}

    @SuppressWarnings("rawtypes")
	protected static FastSharder createSharder(String graphName, int numShards) throws IOException {
        return new FastSharder<Integer, Integer>(graphName, numShards, new VertexProcessor<Integer>() {
            public Integer receiveVertexValue(int vertexId, String token) {
                return 0;
            }
        }, new EdgeProcessor<Integer>() {
            public Integer receiveEdge(int from, int to, String token) {
                return 0;
            }
        }, new IntConverter(), new IntConverter());
    }

    
    
    public static void main(String[] args) throws Exception {
    	long startTime = System.currentTimeMillis();
    	
        /** Run from command line (Example)
         *	java -Xmx4g -cp "bin:lib/*" -Dnum_threads=4 KCoreGraphChi filename nbrOfShards filetype
         */
    	//args = new String[] {"./graphchidata/simplegraph.txt", "1", "edgelist"};
    	
    	if(args.length != 3) {
    		System.out.println("Usage: java -Xmx4g -cp \"bin:lib/*\" -Dnum_threads=4 KCoreGraphChi filename nbrOfShards filetype");
    	}

        String fileName = args[0];
        int nShards = Integer.parseInt(args[1]);
        String fileType = args[2];
        
        //Disable compression because with compression it becomes much slower, twice slower almost.
        CompressedIO.disableCompression();

        //String inputFilePath = inputDirectory + fileName;
        String inputFilePath = fileName;
        
        /* Preprocessing graph : Making shards */
        @SuppressWarnings("rawtypes")
		FastSharder sharder = createSharder(inputFilePath, nShards);
        if (inputFilePath.equals("pipein")) {     // Allow piping graph in
            sharder.shard(System.in, fileType);
        } else {
            if (!new File(ChiFilenames.getFilenameIntervals(inputFilePath, nShards)).exists()) {
                sharder.shard(new FileInputStream(new File(inputFilePath)), fileType);
            } else {
                logger.info("Found shards -- no need to preprocess");
            }
        }

        /* Running GraphChi */
        GraphChiEngine<Integer, Integer> engine =
                new GraphChiEngine<Integer, Integer>(inputFilePath, nShards);
        engine.setSkipZeroDegreeVertices(true);
        engine.setEnableScheduler(true);
        engine.setEdataConverter(new IntConverter());
        engine.setVertexDataConverter(new IntConverter());

        KCoreGC_M kc = new KCoreGC_M();
        
        engine.run(kc, INFINITY);

        logger.info("Ready.");
       
        
        /* Outputting Core Values */
        BufferedWriter bw;
        bw = new BufferedWriter( new FileWriter (fileName + ".cores"));
        //bw.write(kc.nVertexes + "\n");

        VertexIdTranslate trans = engine.getVertexIdTranslate();
        TreeSet<IdInt> topToBottom = Toplist.topListInt(inputFilePath,
                engine.numVertices(), engine.numVertices());

        for(IdInt walker : topToBottom) {
            float coreValue = walker.getValue();
            bw.write(trans.backward(walker.getVertexId()) + ", " + String.valueOf((int)coreValue) + "\n");
        }
        
        bw.close();
        /* End of outputting Core Values */ 

        System.out.println("Vertexes Processed: " + engine.numVertices());
        System.out.println("Edges Processed: " + engine.numEdges()) ;

        System.out.println("nIterations: " + kc.nIterations);
        System.out.println("Success!");
        
        long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println("Time elapsed = " + estimatedTime/1000.0);
        System.out.println("In the experiments we consider the user+system time produced by the Unix time command."); 
    }
}