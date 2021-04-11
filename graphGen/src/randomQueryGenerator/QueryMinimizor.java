package randomQueryGenerator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class QueryMinimizor {

	public QueryMinimizor() {
	}

	
	public void minimize(String queryFN){
		
		String fn = queryFN.substring(0, queryFN.lastIndexOf('.'));
		String suffix = queryFN.substring(queryFN.lastIndexOf('.'));
		String outFN = new String(fn+"_n"+suffix);
		minimize(queryFN, outFN);
		
	}
	
	public void minimize(String queryFN, String outFN) {

		QueryParser queryParser = new QueryParser(queryFN);
		try {
			PrintWriter pw = new PrintWriter(new FileOutputStream(outFN));
			Query qry_o = null;
			int qid = 0;
			while ((qry_o = queryParser.readNextQuery()) != null) {

				Query qry_n = minimize(qry_o);

				write(qid++, qry_n, pw);

			}

			pw.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Query minimize(Query qry_o) {

		QuerySimulation qs = new QuerySimulation(qry_o);
		boolean[][] mH = qs.findEndomo();
		boolean[] mDel = new boolean[qry_o.V];
		Arrays.fill(mDel, false);

		ArrayList<QNode> nodes_n = new ArrayList<QNode>();
		Query qry_n = new Query();
		QNode q_n = new QNode(qry_n.V++, qry_o.nodes[0].lb, qry_o.nodes[0].axis);
		nodes_n.add(q_n);
		minimize(qry_o.nodes[0], q_n, qry_n, nodes_n, mH, mDel);

		qry_n.nodes = new QNode[qry_n.V];
		for (int i = 0; i < nodes_n.size(); i++) {
			QNode n = nodes_n.get(i);
			qry_n.nodes[i] = n;
		}

		return qry_n;
	}

	private void write(int qid, Query qry, PrintWriter pw) {
		pw.println("q" + qid);
		// output nodes
		QNode[] nodes = qry.nodes;

		for (QNode n : nodes) {

			pw.println("v" + " " + n.id + " " + n.lb);
		}

		pw.flush();

		// output edges

		for (QNode n : nodes) {
			if (n.N_O_SZ > 0)
				for (QNode w : n.N_O) {
					pw.println("e" + " " + n.id + " " + w.id + " " + w.axis);

				}
		}
		pw.flush();
	}

	private void minimize(QNode q_o, QNode q_n, Query qry_n, ArrayList<QNode> nodes_n, boolean[][] mH, boolean[] mDel) {

		if (q_o.N_O_SZ == 0)
			return;

		ArrayList<QNode> children = q_o.N_O;

		for (QNode child : children) {

			if (!isRed(child, mH, mDel)) {

				QNode c_n = new QNode(qry_n.V++, child.lb, child.axis);
				c_n.N_I = new ArrayList<QNode>(1);
				c_n.N_I.add(q_n);
				nodes_n.add(c_n);
				q_n.addChild(c_n);
				minimize(child, c_n, qry_n, nodes_n, mH, mDel);
			}

		}

	}

	private boolean isRed(QNode q, boolean[][] mH, boolean[] mDel) {

		boolean[] row = mH[q.id];
		for (int j = 0; j < row.length; j++) {

			if (q.id != j && row[j]) {
				if (!mDel[j]){
					mark(q, mDel);
					return true;
				}
			
			}
		}

		return false;
	}
	
	// mark the nodes in the subtree rooted at q
	private void mark(QNode q,boolean[] mDel){
		
		mDel[q.id] = true;
		if (q.N_O_SZ == 0){
	
			return;
		}

		ArrayList<QNode> children = q.N_O;

		for (QNode child : children) {
		
			mark(child, mDel);
		}
	}

	public static void main(String[] args) {

		QueryMinimizor qmini = new QueryMinimizor();
		//qmini.minimize(".//data//testq.txt", ".//data//testq_n.txt");
		qmini.minimize(".//data//testq.txt");
	}

}
