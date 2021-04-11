package randomGraphGenerator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.util.SupplierUtil;

import edu.cornell.lassp.houle.RngPack.RandomElement;
import edu.cornell.lassp.houle.RngPack.RandomSeedable;
import edu.cornell.lassp.houle.RngPack.Ranmar;

public class RandDigraphGen {

	private String mOutFileName;
	private int numlbs;

	private PrintWriter opw;
	private int V; // number of vertices in this digraph
	private int E; // number of edges in this digraph

	private int[] labels;

	//private static final long SEED = 5;

	public RandDigraphGen() {
	}

	public void generateDiGraph(String outFN, int numOfVertex, int numOfEdges, int numOfLbs) {

		init(outFN, numOfVertex, numOfEdges, numOfLbs);
		System.out.println("Generating graph...");
		Graph<Integer, DefaultEdge> g = generateGnmRandomDiGraph();
		System.out.println("Done.");
		System.out.println("Writing graph to disk...");
		writeGraphToFile(g);
		System.out.println("Done.");
	}

	public void generateDiGraph(String outFN, int numOfVertex, int numOfEdges, int numOfLbs, long seed) {

		init(outFN, numOfVertex, numOfEdges, numOfLbs);
		System.out.println("Generating graph...");
		Graph<Integer, DefaultEdge> g = generateGnmRandomDiGraph(seed);
		System.out.println("Done.");
		System.out.println("Writing graph to disk...");
		writeGraphToFile(g);
		System.out.println("Done.");
	}

	public void generateDag(String outFN, int numOfVertex, int numOfEdges, int numOfLbs) {

		init(outFN, numOfVertex, numOfEdges, numOfLbs);
		System.out.println("Generating graph...");
		Graph<Integer, DefaultEdge> g = generateGnmRandomDAG();
		System.out.println("Done.");
		System.out.println("Writing graph to disk...");
		writeGraphToFile(g);
		System.out.println("Done.");
	}

	public void generateDag(String outFN, int numOfVertex, int numOfEdges, int numOfLbs, long seed) {

		init(outFN, numOfVertex, numOfEdges, numOfLbs);
		System.out.println("Generating graph...");
		Graph<Integer, DefaultEdge> g = generateGnmRandomDAG(seed);
		System.out.println("Done.");
		System.out.println("Writing graph to disk...");
		writeGraphToFile(g);
		System.out.println("Done.");
	}

	
	private void writeGraphToFile(Graph<Integer, DefaultEdge> g) {

		opw.println("DiGraph");
		Set<Integer> vertexes = g.vertexSet();

		for (int v : vertexes) {

			opw.println("v" + " " + v + " " + labels[v]);
		}

		opw.flush();

		Set<DefaultEdge> edges = g.edgeSet();
		for (DefaultEdge e : edges) {

			opw.println("e" + " " + g.getEdgeSource(e) + " " + g.getEdgeTarget(e));
		}

		opw.close();

	}

	private Graph<Integer, DefaultEdge> generateGnmRandomDiGraph() {

		GraphGenerator<Integer, DefaultEdge, Integer> randomGen = new GnmRandomGraphGenerator<>(V, E);
		Graph<Integer, DefaultEdge> g = new SimpleDirectedGraph<>(SupplierUtil.createIntegerSupplier(),
				SupplierUtil.DEFAULT_EDGE_SUPPLIER, false);

		randomGen.generateGraph(g);

		return g;
	}

	private Graph<Integer, DefaultEdge> generateGnmRandomDiGraph(long seed) {

		GraphGenerator<Integer, DefaultEdge, Integer> randomGen = new GnmRandomGraphGenerator<>(V, E, seed);
		Graph<Integer, DefaultEdge> g = new SimpleDirectedGraph<>(SupplierUtil.createIntegerSupplier(),
				SupplierUtil.DEFAULT_EDGE_SUPPLIER, false);

		randomGen.generateGraph(g);

		return g;
	}

	private Graph<Integer, DefaultEdge> generateGnmRandomDAG() {

		GraphGenerator<Integer, DefaultEdge, Integer> randomGen = new GnmRandomGraphGenerator<>(V, E);

		Graph<Integer, DefaultEdge> g = new DirectedAcyclicGraph<>(SupplierUtil.createIntegerSupplier(),
				SupplierUtil.DEFAULT_EDGE_SUPPLIER, false);

		randomGen.generateGraph(g);

		return g;
	}

	private Graph<Integer, DefaultEdge> generateGnmRandomDAG(long seed) {

		GraphGenerator<Integer, DefaultEdge, Integer> randomGen = new GnmRandomGraphGenerator<>(V, E, seed);

		Graph<Integer, DefaultEdge> g = new DirectedAcyclicGraph<>(SupplierUtil.createIntegerSupplier(),
				SupplierUtil.DEFAULT_EDGE_SUPPLIER, false);

		randomGen.generateGraph(g);

		return g;
	}

	private void genRandGaussian() {

		labels = new int[V];
		long seed = RandomSeedable.ClockSeed();

		RandomElement e = new Ranmar(seed);
		double[] x = new double[V];
		double min = 0.0, max = 0.0;
		for (int i = 0; i < V; i++) {
			x[i] = e.gaussian();
			if (x[i] < min)
				min = x[i];
			if (x[i] > max)
				max = x[i];
			// System.out.println(x[i]);
		}

		double steplen = (max - min) / numlbs;
		// System.out.println("min = " + min + " max = " + max + " len=" +
		// steplen);

		for (int i = 0; i < V; i++) {
			int slot = (int) ((x[i] - min) / steplen - 0.5);
			labels[i] = slot;
			// System.out.println(x[i] + " in slot " + slot);
		}

	}

	private void init(String outFN, int numOfVertex, int numOfEdges, int numOfLbs) {
		mOutFileName = outFN;
		numlbs = numOfLbs;
		V = numOfVertex;
		E = numOfEdges;
		try {
			opw = new PrintWriter(new FileOutputStream(mOutFileName, false));
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
		genRandGaussian();

		System.out.println("labels have been generated!");
	}

	public static void main(String[] args) {

		RandDigraphGen gen = new RandDigraphGen();
		
		//gen.generateDag(".//data//randv3a2t1k.dag", 300000, 600000, 1000);
		gen.generateDiGraph
		  ("D:\\Documents\\_prog\\prog_cust\\eclipse-workspace\\graphGen\\src\\randv10a2t5k.lg", 1000000, 2000000, 5000);
		//(".//data//randv3a2t5k.lg", 300000, 600000, 5000);
	    //(".//data//randv5a2t5k.lg", 500000, 1000000, 5000);
		//(".//data//randv7a2t5k.lg", 700000, 1400000, 5000);
		//(".//data//randv15a2t5k.lg", 1500000, 3000000, 5000);
		//(".//data//randv15a2t3k.lg", 1500000, 3000000, 3000);
		//(".//data//randv3a2t1k.lg", 300000, 600000, 1000);
	}

}
