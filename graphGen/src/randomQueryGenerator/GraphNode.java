package randomQueryGenerator;

import java.util.ArrayList;

public class GraphNode {

	public int N_O_SZ = 0, N_I_SZ = 0;
	public ArrayList<Integer> N_O, N_I;
	public int id; // node id
	public int lb; // label id
    public Zipf zipf;
	
	public GraphNode() {

	
	};

	public GraphNode(int id, int lb) {

		this.id = id;
		this.lb = lb;
		
	}
	
  
}
