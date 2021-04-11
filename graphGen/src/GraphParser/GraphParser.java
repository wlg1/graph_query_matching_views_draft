package GraphParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

import graphStats.Consts;


public class GraphParser {
	private String mFileName;
	private Digraph mG;
	
	private HashMap<String, Integer> l2iMap;
	private HashMap<Integer, String> i2lMap;
	private int LID = 0;// label id;
	private int numLabels;
	private int V; // number of vertices in this digraph
	private int E; // number of edges in this digraph
    private int maxout=0, maxin=0; 
	private double totout =0, totin =0;
	
	
	private GraphNode[] nodes; // nodes of the graph

	
	
	public GraphParser(String filename) {

		mFileName = Consts.INDIR + filename;
		l2iMap = new HashMap<String, Integer>();
		i2lMap = new HashMap<Integer, String>();
		numLabels = 0;
	}
	
	
	public Digraph loadVE() {

		try {
			int[] VE = getVandE();
			V = VE[0];
			E = VE[1];
			nodes = new GraphNode[V];
			NodeList[] N_O = new NodeList[V], N_I = new NodeList[V];

			BufferedReader in = new BufferedReader(new FileReader(mFileName));
			String line = null;
			while ((line = readNextLine(in)) != null) {
				String[] buf = line.split("\\s+");
				if (line.charAt(0) == 'v') {

					final int index = Integer.parseInt(buf[1]);
					final String label = buf[2];
					GraphNode n = new GraphNode();
					n.id = index;
					Integer lid = l2iMap.get(label);
					if (lid == null) {
						n.lb = LID++;
						i2lMap.put(n.lb, label);
						l2iMap.put(label, n.lb);
						numLabels++;
					} else {
						n.lb = lid;
					}
					nodes[index] = n;

				}
				if (line.charAt(0) == 'e') {

					final int v = Integer.parseInt(buf[1]);
					final int w = Integer.parseInt(buf[2]);
					if (N_O[v] == null)
						N_O[v] = new NodeList();
					N_O[v].add(w);

					if (N_I[w] == null)
						N_I[w] = new NodeList();
					N_I[w].add(v);

				}
			} // end of reading graph

			for (int u = 0; u < V; u++) {
				if (N_O[u] != null) {
					nodes[u].N_O_SZ = N_O[u].size();
					totout+= nodes[u].N_O_SZ;
					if(maxout<nodes[u].N_O_SZ)
						maxout = nodes[u].N_O_SZ;
					// nodes.get(u).N_O = new
					// ArrayList<Integer>(N_O.get(u).size());
					nodes[u].N_O = N_O[u].getList();
				}
				if (N_I[u] != null) {
					nodes[u].N_I_SZ = N_I[u].size();
					totin+= nodes[u].N_I_SZ;
					if(maxin<nodes[u].N_I_SZ)
						maxin = nodes[u].N_I_SZ;
					// nodes.get(u).N_I = new
					// ArrayList<Integer>(N_I.get(u).size());
					nodes[u].N_I = N_I[u].getList();
				}

			}
			  
			mG= new Digraph(V,E,nodes);
			mG.setLables(numLabels);
			mG.setRoot();
			
            in.close();
          
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  return mG;

	}
	
	
	private int[] getVandE() throws Exception {
		int[] rs = new int[2];
		BufferedReader bin = new BufferedReader(new FileReader(mFileName));

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
	

	}

}
