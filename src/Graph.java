import java.util.Iterator;

public interface Graph {
	public int maxDegree();
	public int size();
	public int[] getNeighbors(int u);
	public int outdegree(int u);
	public Iterator<Integer> vertexIterator();
}
