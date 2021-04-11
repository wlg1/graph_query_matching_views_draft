package queryParser;

import java.util.ArrayList;

public class QNode {

	public int id; // node id
	public String lb; // label
	public int N_O_SZ = 0, N_I_SZ = 0;
	public ArrayList<QNode> N_O, N_I;
	public ArrayList<QEdge> E_O, E_I; // outgoing and incoming edges
	public int vis;
	public int axis = 0;
    public int level = 0;
	public QNode() {

	};
	
	public QNode(int id, String lb, int axis){
		
		this.id = id;
		this.lb = lb;
		this.axis = axis;
	}

	public boolean isSink() {

		if (N_O_SZ == 0)
			return true;
		return false;
	}

	public void addChild(QNode c) {

		if (N_O == null)
			N_O = new ArrayList<QNode>();
		N_O.add(c);
		N_O_SZ++;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		//toString(this, sb);
		printNodes(this, sb);
		printEdges(this, sb);

		return sb.toString();
	}

	private void printNodes(QNode q, StringBuffer sb) {

		sb.append("v" + " " + q.id + " " + q.lb + "\n");

		for (int i = 0; i < q.N_O_SZ; i++) {

			QNode c = q.N_O.get(i);
			printNodes(c, sb);

		}
	}

	private void printEdges(QNode q, StringBuffer sb) {

		for (int i = 0; i < q.N_O_SZ; i++) {

			QNode c = q.N_O.get(i);
			sb.append("e" + " " + q.id + " " + c.id + " " + c.axis + "\n");
			printEdges(c, sb);
			
		}
	}

	private void toString(QNode q, StringBuffer sb) {

		sb.append("v" + " " + q.id + " " + q.lb + "\n");

		for (int i = 0; i < q.N_O_SZ; i++) {

			QNode c = q.N_O.get(i);
			toString(c, sb);
			sb.append("e" + " " + q.id + " " + c.id + " " + c.axis + "\n");
		}

	}

}
