import java.io.File;
import java.io.PrintStream;

import it.unimi.dsi.webgraph.ImmutableGraph;

/**
 * K-core decomposition algorithm
 *
 * Outputs: array "int[] res" containing the core values for each vertex.  
 * The cores are stored in the <basename>.cores file. 
 * This is a text file where each line is of the form <vertex-id>:<core number>
 *
 * This is an implementation (with optimizations) of the algorithm given in: 
 * A. Montresor, F. De Pellegrini, and D. Miorandi. Distributed k-core decomposition. 
 * Parallel and Distributed Systems, IEEE Trans., 24(2), 2013.
 * 
 * The graph is stored using Webgraph 
 * (see P. Boldi and S. Vigna. The webgraph framework I: compression techniques. WWW'04.)
 *
 * @author Alex Thomo, thomo@uvic.ca, 2015
 */

public class KCoreWG_M {
	ImmutableGraph G;
	int n;
	int E;
	int md; //max degree
	
	int[] core;
	boolean[] scheduled;
	boolean printprogress = false; 
	int iteration = 0;
	boolean change = false;
	
	public KCoreWG_M(String basename) throws Exception {
			G = ImmutableGraph.loadMapped(basename);
		
		n = G.numNodes();
		core = new int[n];
		
		md = 0;
		scheduled = new boolean[n];
		for(int v=0; v<n; v++) {
			
			int degree = G.outdegree(v);
			if(degree > md)
				md = degree;
			
			scheduled[v] = true;
		}
	}
	
	void update(int v) {
		if(iteration == 0) {
			core[v] = G.outdegree(v);
			scheduled[v]=true;
			change = true;
		}
		else {
			int d_v = G.outdegree(v);
			int[] N_v = G.successorArray(v);
			int localEstimate = computeUpperBound(v,d_v,N_v);
			if(localEstimate < core[v]) {
				core[v] = localEstimate;
				change = true;
				
				for(int i=0; i<d_v; i++) {
					int u = N_v[i];
					if(core[v]<=core[u])
						scheduled[u] = true;
				}
			}
		}
	}
	
	int computeUpperBound(int v, int d_v, int[] N_v) {
		int[] c = new int[core[v]+1];
		for(int i=0; i<d_v; i++) {
			int u = N_v[i];
			int j = Math.min(core[v], core[u]);
			c[j]++;
		}
		
		int cumul = 0;
		for(int i=core[v]; i>=2; i--) {
			cumul = cumul + c[i];
			if (cumul >= i)
				return i;
		}
		
		return d_v;
	}
	
	
	public int[] KCoreCompute () {
		while(true) {
			System.out.print("Iteration " + iteration);
			
			int num_scheduled=0;
			boolean[] scheduledNow = scheduled.clone();
			for(int v=0; v<n; v++) 
				scheduled[v] = false;
			
			for(int v=0; v<n; v++) {
				if(scheduledNow[v] == true) {
					num_scheduled++;
					update(v);
				}
			}
			System.out.println( "\t\t" + ((100.0*num_scheduled)/n) + "%\t of nodes were scheduled this iteration.");
			iteration++;
			if(change == false)
				break;
			else
				change = false;
		}
		
		return core;
	}
	
    
	public static void main(String[] args) throws Exception {
		
		long startTime = System.currentTimeMillis();
		
		//args = new String[] {"simplegraph"};
		
		if(args.length != 1) {
			System.err.println("Usage: java KCoreWG_M basename");
			System.exit(1);
		}
		
		String basename = args[0];
		
		System.out.println("Starting " + basename);
		KCoreWG_M kc = new KCoreWG_M(basename);
		
		//storing the core value for each node in a file.
		PrintStream ps = new PrintStream(new File(basename+".cores"));
		
		int[] res = kc.KCoreCompute();
		int kmax = -1;
		double sum = 0;
		int cnt = 0;
		for(int i=0; i<res.length; i++) {
			ps.println(i+":" + res[i] + " ");
			if(res[i] > kmax) 
				kmax = res[i];
			sum += res[i];
			if(res[i] > 0) cnt++;
		}
		System.out.println("|V|	|E|	dmax	kmax	kavg");
		System.out.println(cnt + "\t" + (kc.E/2) + "\t" + kc.md + "\t" + kmax + "\t" + (sum/cnt) );
		
        System.out.println(args[0] + ": Time elapsed (sec) = " + (System.currentTimeMillis() - startTime)/1000.0);		
	}
}
