package randomQueryGenerator;

import java.util.ArrayList;



public class Query {

	public int V, E;
	public QNode[] nodes;

	public ArrayList<QNode> sources, sinks; // source and sink nodes of the graph

	static final String NEWLINE = System.getProperty("line.separator");
	
	public Query(){
		
		
	}
	
	public Query(int V, int E, QNode[] nodes) {

		this.V = V;
		this.E = E;
		this.nodes = nodes;
	}

	public QNode[] getNodes() {

		return nodes;
	}
	
	public QNode getNode(int id){
		
		return nodes[id];
	}

	public boolean isTree(){
		
		for(QNode n: nodes){
			
			if(n.N_I_SZ>1)
				return false;
		}
		
		if(E!= V-1)
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

	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(V + " vertices, " + E + " edges " + NEWLINE);
		for (int v = 0; v < V; v++) {
			s.append(String.format("%d: ", v));
			QNode n_v = nodes[v];
			if(n_v.N_O_SZ>0)
			for (QNode w : n_v.N_O) {
				s.append(w + " ");
			}
			s.append(NEWLINE);
		}
		return s.toString();
	}
	
	public static void main(String[] args) {

	}

}
