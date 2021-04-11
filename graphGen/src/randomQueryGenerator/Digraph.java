package randomQueryGenerator;

import java.util.ArrayList;

public class Digraph {

	int V; // number of vertices in this digraph
	int E; // number of edges in this digraph
	GraphNode[] nodes;
	int numLabels = 0;

	ArrayList<GraphNode> sources, sinks; // source and sink nodes of the graph
  
	GraphNode root; // virtual root nodes;
	public Digraph() {
	}

	public Digraph(int V, int E, GraphNode[] nodes) {

		this.V = V;
		this.E = E;
		this.nodes = nodes;

	}

	public void setLables(int lbs) {

		numLabels = lbs;

	}
	
	public void setRoot(){
		
		root = new GraphNode();
		getSources();
		
		root.id = -1;
		root.lb = -1;
		root.N_O_SZ = sources.size();
		root.N_O = new ArrayList<Integer>(root.N_O_SZ);
		for(int i=0; i<root.N_O_SZ; i++){
			
			root.N_O.add(sources.get(i).id);
		}
		
	}

	public int getLabels() {

		return numLabels;
	}

	public GraphNode[] getNodes() {

		return nodes;
	}

	public ArrayList<GraphNode> getSources() {
		if (sources == null) {

			sources = new ArrayList<GraphNode>(5);
			for(GraphNode node:nodes){
				if(node.N_I_SZ==0)
					sources.add(node);
				
			}
			
		}
		
		return sources;
	}

	public ArrayList<GraphNode> getSinks() {
		if (sinks == null) {

			sinks = new ArrayList<GraphNode>(5);
			for(GraphNode node:nodes){
				if(node.N_O_SZ==0)
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
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
