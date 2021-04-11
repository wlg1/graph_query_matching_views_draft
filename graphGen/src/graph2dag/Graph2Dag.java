package graph2dag;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Stack;

public class Graph2Dag {

	int V, E;
	Node[] nodes;

	private String mInFN, mOutFN;
	private PrintWriter opw;

	private ListIterator<Integer>[] out_it;
	
	private int numBackEdges = 0;

	public Graph2Dag(String inF, String outF) {

		mInFN = inF;
		mOutFN = outF;

		try {
			opw = new PrintWriter(new FileOutputStream(mOutFN, false));
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
	}

	public void run() {

		loadGraph();
		check();
        if(numBackEdges>0){
		   	genDag();
		   	System.out.println("Total " + numBackEdges + " back edges are removed!");
        }
        else {
        	
        	System.out.println("The graph " + mInFN + " is already a dag!");
        }
	}

	private void genDag() {

		opw.println("dag");

		// output nodes

		for (Node n : nodes) {

			opw.println("v" + " " + n.id + " " + n.lb);
		}

		opw.flush();

		// output edges

		for (Node v : nodes) {
			if (v.N_O_SZ > 0)
				for (int w : v.N_O) {
					if (w != -1)
					opw.println("e" + " " + v.id + " " + w + " " + 1);

				}
		}

		opw.close();
	}

	private void check() {

		out_it = (ListIterator<Integer>[]) new ListIterator[V];
		for (int v = 0; v < V; v++) {
			Node node = nodes[v];
			if (node.N_O != null)
				out_it[v] = node.N_O.listIterator();
		}

		System.out.println("checking back edges...");
		for (Node n : nodes) {
			if (n.color == Node.Color.white)
				dfs(n);

		}
	}

	private void dfs(Node u) {

		Stack<Node> stack = new Stack<Node>();
		stack.push(u);
		u.color = Node.Color.grey;
		while (!stack.isEmpty()) {

			Node v = stack.peek();

			if (v.N_O_SZ == 0) {
				v.color = Node.Color.black;
				stack.pop();
			} else {
				if (out_it[v.id].hasNext()) {

					Node w = nodes[out_it[v.id].next()];

					switch (w.color) {

					case white:

						w.color = Node.Color.grey;
						stack.push(w);
						break;

					case grey:
						// back edge
						// System.out.println("CYCLE DETECTED!!!");
						System.out.println(v.id + "->" + w.id);
						// remove it
						out_it[v.id].set(-1);
						v.N_O_SZ--;
						numBackEdges++;
					case black:
						break;
					}

					continue;

				}
				v.color = Node.Color.black;
				stack.pop();
			}
		}

	}

	private void loadGraph() {

		try {
			int[] VE = getVandE();
			V = VE[0];
			E = VE[1];
			System.out.println("V=" + V);
			nodes = new Node[V];
			NodeList[] N_O = new NodeList[V];

			BufferedReader in = new BufferedReader(new FileReader(mInFN));
			String line = null;
			while ((line = readNextLine(in)) != null) {
				String[] buf = line.split("\\s+");
				if (line.charAt(0) == 'v') {

					final int index = Integer.parseInt(buf[1]);
					final String label = buf[2];
					Node n = new Node();
					n.id = index;
					n.lb = label;
					nodes[index] = n;

				}
				if (line.charAt(0) == 'e') {

					final int v = Integer.parseInt(buf[1]);
					final int w = Integer.parseInt(buf[2]);
					if(v>=V || w>=V)
					    continue;
						if (N_O[v] == null)
						N_O[v] = new NodeList();
					N_O[v].add(w);

				}
			} // end of reading graph

			for (int u = 0; u < V; u++) {
				if (N_O[u] != null) {
					nodes[u].N_O_SZ = N_O[u].size();
					// nodes.get(u).N_O = new
					// ArrayList<Integer>(N_O.get(u).size());
					nodes[u].N_O = N_O[u].getList();
				}

			}

			in.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private int[] getVandE() throws Exception {
		int[] rs = new int[2];
		BufferedReader bin = new BufferedReader(new FileReader(mInFN));

		String line = null;

		while ((line = readNextLine(bin)) != null) {

			if (line.charAt(0) == 'v')
				rs[0]++;

			if (line.charAt(0) == 'e')
				rs[1]++;

		}

		return rs;
	}

	private static String readNextLine(BufferedReader in) {
		String line = null;

		try {
			// read a non-emtpy line
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0)
					break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return line;
	}

	class NodeList {

		ArrayList<Integer> nlist;

		NodeList() {

			nlist = new ArrayList<Integer>();
		}

		ArrayList<Integer> getList() {

			return nlist;
		}

		void add(int n) {

			nlist.add(n);
		}

		int size() {

			return nlist.size();
		}
	}

	public static void main(String[] args) {

		Graph2Dag g2d = new Graph2Dag
				//(".//data//randv3a2t5k.lg", ".//data//randv3a2t5k.dag");
		        //(".//data//randv5a2t5k.lg", ".//data//randv5a2t5k.dag");
		         // (".//data//randv7a2t5k.lg", ".//data//randv7a2t5k.dag");
		     //  (".//data//randv10a2t5k.lg", ".//data//randv10a2t5k.dag");
				//(".//data//randv15a2t5k.lg", ".//data//randv15a2t5k.dag");
				//(".//data//randv15a2t3k.lg", ".//data//randv15a2t3k.dag");
				//(".//data//randv3a2t1k.lg", ".//data//randv3a2t1k.dag");
				//(".//data//cite_lb1k.lg",".//data//cite_lb1k.lg.dag");
				//(".//data//uniprot22m_lb1k.lg",".//data//uniprot22m_lb1k.dag");
				//(".//data//wordnet.lg", ".//data//wordnet.dag");
				//(".//data//outputacm.lg", ".//data//outputacm.dag");
				//(".//data//dblpcitation.lg", ".//data//dblpcitation.dag");
				//(".//data//mico.dag", ".//data//mico.dag");
				//(".//data//citation-network2.lg", ".//data//citation-network2.dag");
		//("E:\\experiments\\DagOnGraph\\input\\Email_lb200.lg", "E:\\experiments\\DagOnGraph\\input\\Email_lb200.dag");
		//("E:\\experiments\\DagOnGraph\\input\\Email_lb1.lg", "E:\\experiments\\DagOnGraph\\input\\Email_lb1.dag");
		//(".//data//mico.lg", ".//data//mico.dag");
		//(".//data//citeseer2.lg", ".//data//citeseer2.dag");
				//("E:\\experiments\\GraHomMat\\input\\am_lb1.lg", "E:\\experiments\\GraHomMat\\input\\am_lb1.dag");
				("E:\\experiments\\GraHomMat\\input\\am_lb3.lg", "E:\\experiments\\GraHomMat\\input\\am_lb3.dag");
				//("E:\\experiments\\GraHomMat\\input\\youtube.graph", "E:\\experiments\\GraHomMat\\input\\youtube.dag");
		//("E:\\experiments\\GraHomMat\\input\\human.graph", "E:\\experiments\\GraHomMat\\input\\human.dag");
				//("E:\\experiments\\GraHomMat\\input\\ep_lb20.lg", "E:\\experiments\\GraHomMat\\input\\ep_lb20.dag");
		//("E:\\experiments\\GraHomMat\\input\\bs_lb5.lg", "E:\\experiments\\GraHomMat\\input\\bs_lb5.dag");
				//("E:\\experiments\\GraHomMat\\input\\go_lb5.lg", "E:\\experiments\\GraHomMat\\input\\go_lb5.dag");
		
				//("E:\\experiments\\GraHomMat\\input\\Email_lb15.lg", "E:\\experiments\\GraHomMat\\input\\Email_lb15.dag");
				//("E:\\experiments\\GraHomMat\\input\\ep_lb10.lg", "E:\\experiments\\GraHomMat\\input\\ep_lb10.dag");
				
				g2d.run();
	}

}
