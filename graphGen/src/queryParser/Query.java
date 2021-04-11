package queryParser;

import java.util.ArrayList;



public class Query {

	public int V, E;
	public QNode[] nodes;
	public QEdge[] edges;
	public int maxout = 0, maxin = 0, level = 0;

	public ArrayList<QNode> sources, sinks; // source and sink nodes of the
											// graph
    public int pcs = 0; // number of child edges    
	
	static final String NEWLINE = System.getProperty("line.separator");

	public Query() {

	}

	public Query(int V, int E, QNode[] nodes) {

		this.V = V;
		this.E = E;
		this.nodes = nodes;
	}
	
	public Query(int V, int E, QNode[] nodes, QEdge[] edges) {

		this.V = V;
		this.E = E;
		this.nodes = nodes;
		this.edges = edges;
	}

    public void printStats(int id){
    	
    	getSources();
    	for(QNode q: sources){
    		
    		dfs(q);
    		if(level < q.level)
    			level = q.level;
    	}
    	
    	//System.out.println(id+ "," + "V = " + V + "," + "E=" + E + "," + "level =" + level + "," + "pcs = " + pcs + "," + "maxout= " + maxout + "," + "maxin = " + maxin);
        
    	System.out.println(id+ "," + V + "," +  E + "," +  level + "," +  pcs + "," + maxout + "," + maxin);
    }
    
	public QNode[] getNodes() {

		return nodes;
	}

	public QNode getNode(int id) {

		return nodes[id];
	}

	public boolean isTree() {

		for (QNode n : nodes) {

			if (n.N_I_SZ > 1)
				return false;
		}

		if (E != V - 1)
			return false;
		return true;
	}


	public ArrayList<QNode> getSources() {
		if (sources == null) {

			sources = new ArrayList<QNode>(5);
			for (QNode node : nodes) {
				if (node.N_I_SZ == 0)
					sources.add(node);

			}

		}

		return sources;
	}

	public ArrayList<QNode> getSinks() {
		if (sinks == null) {

			sinks = new ArrayList<QNode>(5);
			for (QNode node : nodes) {
				if (node.N_O_SZ == 0)
					sinks.add(node);

			}

		}

		return sinks;
	}

	/**
	 * Returns the number of vertices in this digraph.
	 *
	 * @return the number of vertices in this digraph
	 */
	public int V() {
		return V;
	}

	/**
	 * Returns the number of edges in this digraph.
	 *
	 * @return the number of edges in this digraph
	 */
	public int E() {
		return E;
	}
	
	public int degree(int id) {

		QNode q = nodes[id];

		return q.N_I_SZ + q.N_O_SZ;
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(V + " vertices, " + E + " edges " + NEWLINE);
		for (int v = 0; v < V; v++) {
			s.append(String.format("%d: ", v));
			QNode n_v = nodes[v];
			if (n_v.N_O_SZ > 0)
				for (QNode w : n_v.N_O) {
					s.append(w + " ");
				}
			s.append(NEWLINE);
		}
		return s.toString();
	}

	private void dfs(QNode q) {

		if (q.N_O_SZ > maxout)
			maxout = q.N_O_SZ;
		if (q.N_I_SZ > maxin)
			maxin = q.N_I_SZ;
		if (q.N_O_SZ == 0){
			q.level = 0;
		    return;
		}
		ArrayList<QNode> children = q.N_O;
		int max = -1;
		for (QNode c : children) {
			dfs(c);
			if (c.level > max)
				max = c.level;
		

		}
		q.level = max+1;

	}

	
	public static void main(String[] args) {

	}

}
