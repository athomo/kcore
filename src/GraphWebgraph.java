import java.util.Iterator;
import it.unimi.dsi.logging.ProgressLogger;
import it.unimi.dsi.webgraph.ImmutableGraph;

public class GraphWebgraph implements Graph {
	ImmutableGraph G; //graph
	Integer maxDegree=null;
	
	public GraphWebgraph(String edgesfilename, String mode) throws Exception {
		final ProgressLogger pl = new ProgressLogger();
		pl.logInterval = 1000; //millisec
		
		if(mode.equals("memory")) {
			G = ImmutableGraph.load(edgesfilename, pl);
			System.out.println("Memory loaded graph!");
		}
		else if (mode.equals("memory-mapped")) {
			G = ImmutableGraph.loadMapped(edgesfilename, pl); //We need random access
			System.out.println("Memory-mapped graph!");
		}
	}
	
	public int maxDegree() {
		if(maxDegree != null)
			return maxDegree;
		
		Iterator<Integer> degIter=G.outdegrees();
		maxDegree = -1;
		while(degIter.hasNext()) {
			Integer deg = degIter.next(); 
			if(deg > maxDegree)
				maxDegree = deg;
		}
		return maxDegree;
	}
	
	public int size() {
		return G.numNodes();
	}
	
	public int[] getNeighbors(int u) {
		return G.successorArray(u);
	}
	
	public int outdegree(int u) {
		return G.outdegree(u);
	}
	
	public Iterator<Integer> vertexIterator(){
		return G.nodeIterator();
	}	
}
