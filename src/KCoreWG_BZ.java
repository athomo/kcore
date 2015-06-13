/**
 * K-core decomposition algorithm
 *
 * Outputs: array "int[] res" containing the core values for each vertex.  
 *
 * This is an implementation of the algorithm given in: 
 * V. Batagelj and M. Zaversnik. An o (m) algorithm for cores decomposition of networks. CoRR, 2003.
 * 
 * The graph is stored using Webgraph (see P. Boldi and S. Vigna. The webgraph framework I: compression techniques. WWW’04.)
 *
 * @author Alex Thomo, thomo@uvic.ca, 2015
 */

public class KCoreWG_BZ {
	Graph G;
	boolean printprogress = false; 
	long E=0;
	
	public KCoreWG_BZ(String edgesfilename, String storageType) throws Exception {
		if (storageType.equals("webgraph"))
			G = new GraphWebgraph(edgesfilename, "memory");
	}
	
    public int[] KCoreCompute () {
    	int n = G.size(); int md = G.maxDegree(); 
    	int[] vert = new int[n];
    	int[] pos = new int[n];
    	int[] deg = new int[n];
    	int[] bin = new int[md+1]; //md+1 because we can zero degree 

    	for(int d=0; d<=md; d++) 
    		bin[d] = 0;
    	for(int v=0; v<n; v++) { 
    		if (printprogress && v%1000000 == 0) 
    			System.out.println(v);
    		deg[v] = G.outdegree(v); E += deg[v];
    		bin[ deg[v] ]++;
    	}

    	int start = 0; //start=1 in original, but no problem
    	for(int d=0; d<=md; d++) {
    		int num = bin[d];
    		bin[d] = start;
    		start += num;
    	}
    	
    	//bin-sort vertices by degree
    	for(int v=0; v<n; v++) {
    		pos[v] = bin[ deg[v] ];
    		vert[ pos[v] ] = v;
    		bin[ deg[v] ]++;
    	}
    	//recover bin[]
    	for(int d=md; d>=1; d--) 
    		bin[d] = bin[d-1];
    	bin[0] = 0; //1 in original
    	
    	//main algorithm
    	for(int i=0; i<n; i++) {
    		if (printprogress && i%1000000 == 0) 
    			System.out.println(i);
    		int v = vert[i]; //smallest degree vertex
    		int[] N_v = G.getNeighbors(v);
    		for(Integer u : N_v) {
    			if(deg[u] > deg[v]) {
    				int du = deg[u]; int pu = pos[u];
    				int pw = bin[du]; int w = vert[pw];
    				if(u!=w) {
    					pos[u] = pw; vert[pu] = w;
    					pos[w] = pu; vert[pw] = u;
    				}
    				bin[du]++;
    				deg[u]--;
    			}
    		}
    	}
    	
    	return deg;
    }
    
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		
		//args = new String[] {"simplegraph"};
		
		System.out.println("Starting " + args[0]);
		KCoreWG_BZ kc3 = new KCoreWG_BZ(args[0], "webgraph");
		
		int[] res = kc3.KCoreCompute();
		int kmax = -1;
		double sum = 0;
		int cnt = 0;
		for(int i=0; i<res.length; i++) {
			//System.out.print(i+":" + res[i] + " ");
			if(res[i] > kmax) 
				kmax = res[i];
			sum += res[i];
			if(res[i] > 0) cnt++;
		}
		System.out.println("|V|	|E|	dmax	kmax	kavg");
		System.out.println(cnt + "\t" + (kc3.E/2) + "\t" + kc3.G.maxDegree() + "\t" + kmax + "\t" + (sum/cnt) );
		
		long estimatedTime = System.currentTimeMillis() - startTime;
        System.out.println(args[0] + ": Time elapsed = " + estimatedTime/1000.0);
	}
}
