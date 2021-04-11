package queryParser;

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;


public class QueryParser {

	private LineNumberReader m_in = null;
	private QNode[] nodes;
	private QEdge[] edges;
	private boolean first = true;
	private final int readAheadLimit = 10000;
	private int V; // number of vertices in this digraph
	private int E; // number of edges in this digraph
	
	

	public QueryParser(String fileName) {

		try {
			m_in = new LineNumberReader(new FileReader(fileName));
		
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	

	public Query readNextQuery() {

		Query query = null;
		String line = null;

		if(first){
			readNextLine();
			first = false;
		}
		
		// get total number of nodes
		int[] VE = getVandE();
		V = VE[0];
		E = VE[1];
		if (V == 0)
			return null;
		int eid = 0; // edge id
		
		MyList[] N_O = new MyList[V], N_I = new MyList[V];
		MyList[] E_O = new MyList[V], E_I = new MyList[V];
		String[] buf;
		nodes = new QNode[V];
		edges = new QEdge[E];
		int pcs = 0;
		while ((line = readNextLine()) != null) {
			buf = line.split("\\s+");
			if (line.charAt(0) == 'v') {
				int index = Integer.parseInt(buf[1]);
				String label = buf[2];
				QNode n = new QNode();
				n.id = index;
				n.lb = label;
				nodes[index] = n;
			}
			else
			if (line.charAt(0) == 'e') {

				final int v = Integer.parseInt(buf[1]);
				final int w = Integer.parseInt(buf[2]);
				final int a = Integer.parseInt(buf[3]);
				QEdge e = new QEdge(v,w,a);
				edges[eid++] = e;
				
				if (N_O[v] == null){
					N_O[v] = new MyList();
				    E_O[v] = new MyList();
				}
				nodes[w].axis = a;
				N_O[v].add(nodes[w]);
				E_O[v].add(e);
        
				if (N_I[w] == null){
					N_I[w] = new MyList();
				    E_I[w] = new MyList();
				}
				N_I[w].add(nodes[v]);
				E_I[w].add(e);

				if(a == 0)
					pcs++;
			}
			else
				break; // reach next query in the file
		}
		
		
		for (int u = 0; u < V; u++) {
			if (N_O[u] != null) {
				nodes[u].N_O_SZ = N_O[u].size();
				// nodes.get(u).N_O = new
				// ArrayList<Integer>(N_O.get(u).size());
				nodes[u].N_O = N_O[u].getList();
				nodes[u].E_O = E_O[u].getList();
			}
			if (N_I[u] != null) {
				nodes[u].N_I_SZ = N_I[u].size();
				// nodes.get(u).N_I = new
				// ArrayList<Integer>(N_I.get(u).size());
				nodes[u].N_I = N_I[u].getList();
				nodes[u].E_I = E_I[u].getList();
			}

		}
		
        query = new Query(V,E,nodes,edges);
        query.pcs = pcs;
		return query;
	}
	
	private String readNextLine() {
		String line = null;

		try {
			// read a non-emtpy line
			while ((line = m_in.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0)
					break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return line;
	}

	private int[] getVandE() {
		int[] rs = new int[2];

		String line = null;
		try {
			m_in.mark(readAheadLimit);
			while ((line = readNextLine()) != null) {

				if (line.charAt(0) == 'v')
					rs[0]++;
				else if (line.charAt(0) == 'e')
					rs[1]++;
				else
					break;

			}
			m_in.reset();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return rs;
	}
	
	class MyList<Item>{
		ArrayList<Item> nlist;
		MyList() {

			nlist = new ArrayList<Item>();
		}

		ArrayList<Item> getList() {

			return nlist;
		}

		void add(Item n) {

			nlist.add(n);
		}

		int size() {

			return nlist.size();
		}
			
	}
	
	public static void main(String[] args) {

		
		QueryParser queryParser =  new QueryParser(".\\input\\qs2.txt");
		Query query = null;
		
		
		int count = 0;
		
		
		while((query = queryParser.readNextQuery())!=null){
		 
		   System.out.println("*************Query " + (count++) + "*************");
		   System.out.println(query);
		
				  
		}
		
	}

}
