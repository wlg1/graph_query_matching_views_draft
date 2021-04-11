package queryParser;

import java.util.ArrayList;

public class TreeQuery {

	public QNode mRoot;
    public int[] mAxis;
	public int[] mParents;
	public ArrayList<Integer> mLeaves;

	public int V, E;
	public QNode[] nodes;
	
	ArrayList<Integer>[] mPathIndices;

	public TreeQuery(int V, int E, QNode[] nodes) {

		this.V = V;
		this.E = E;
		this.nodes = nodes;
		extractQueryInfo();
	}

	public TreeQuery(Query q) {
		V = q.V;
		E = q.E;
		this.nodes = q.nodes;
		extractQueryInfo();
	}


	public void extractQueryInfo() {

		mLeaves = new ArrayList<Integer>();
		for (QNode node : nodes) {
			if (node.N_I_SZ == 0) {
				mRoot = node;
			}

			if (node.N_O_SZ == 0)
				mLeaves.add(node.id);
		}

		mAxis = new int[V];
		for (QNode n : nodes) {
			if (n.N_I_SZ == 0)
				mAxis[n.id] = 1;
			else {

				mAxis[n.id] = n.E_I.get(0).axis;
			}
		}
	}

		
	public static void main(String[] args) {

	}

}
