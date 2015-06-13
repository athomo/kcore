/**
 * K-core decomposition algorithm
 *
 * Outputs: array "int[] res" containing the core values for each vertex.  
 *
 * This is an implementation (with optimizations) of the algorithm given in: 
 * A. Montresor, F. De Pellegrini, and D. Miorandi. Distributed k-core decomposition. 
 * Parallel and Distributed Systems, IEEE Trans., 24(2), 2013.
 * 
 * The graph is stored using Webgraph (see P. Boldi and S. Vigna. The webgraph framework I: compression techniques. WWW’04.)
 *
 * @author Alex Thomo, thomo@uvic.ca, 2015
 */

public class KCoreWG_M {
	Graph G;
	int[] core;
	boolean[] scheduled;
	int n;
	boolean printprogress = false; 
	int iteration = 0;
	boolean change = false;
	
	public KCoreWG_M(String edgesfilename, String storageType) throws Exception {
		if (storageType.equals("webgraph"))
			G = new GraphWebgraph(edgesfilename, "memory-mapped");
		
		n = G.size();
		core = new int[n];
		
		scheduled = new boolean[n];
		for(int v=0; v<n; v++) 
			scheduled[v] = true;
	}
	
	void update(int v) {
		if(iteration == 0) {
			core[v] = G.outdegree(v);
			scheduled[v]=true;
			change = true;
		}
		else {
			int d_v = G.outdegree(v);
			int[] N_v = G.getNeighbors(v);
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
		
		System.out.println("Starting " + args[0]);
		KCoreWG_M kc4 = new KCoreWG_M(args[0], "webgraph");
		kc4.KCoreCompute();
		System.out.println("Number of iterations="+kc4.iteration);
		
		/*Uncomment if you want to print the core numbers for each vertex.
		int[] res = kc4.KCoreCompute();
		for(int i=0; i<res.length; i++)
			System.out.print(i+":" + res[i] + " ");
		*/
		
		long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println(args[0] + ": Time elapsed = " + estimatedTime/1000.0);
	}
    
}
