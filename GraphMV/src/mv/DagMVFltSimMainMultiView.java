package mv;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import dao.BFLIndex;
import dao.DaoController;
import evaluator.DagHomIEFltSim;
import global.Consts;
import global.Consts.AxisType;
import global.Consts.status_vals;
import global.Flags;
import graph.Digraph;
import graph.GraphNode;
import helper.LimitExceededException;
import helper.QueryEvalStat;
import helper.QueryEvalStats;
import helper.TimeTracker;
import mv.queryEvaluator.DagEvalMVFltSim;
import prefilter.FilterBuilder;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import query.graph.QueryDirectedCycle;
import query.graph.QueryParser;
import query.graph.TransitiveReduction;

public class DagMVFltSimMainMultiView {

	ArrayList<Query> queries, views;
	HashMap<String, Integer> l2iMap;
	String queryFileN, dataFileN, outFileN, viewFileN;
	ArrayList<ArrayList<GraphNode>> invLsts;
	BFLIndex bfl;
	TimeTracker tt;
	QueryEvalStats stats;
	Digraph g;

	public DagMVFltSimMainMultiView(String dataFN, String queryFN, String viewFN) {

		queryFileN = Consts.INDIR + queryFN;
		dataFileN = Consts.INDIR + dataFN;
		viewFileN = Consts.INDIR + viewFN;
		String suffix = ".csv";
//		String fn = queryFN.substring(0, queryFN.lastIndexOf('.'));
		String fn = viewFN.substring(0, viewFN.lastIndexOf('.'));
		String datafn = dataFN.substring(0, dataFN.lastIndexOf('.'));
//		outFileN = Consts.OUTDIR + "sum_" + fn + "dagMVFltSim" + suffix;
		outFileN = Consts.OUTDIR + datafn + "_" + fn + "_MVFltSim" + suffix;
		stats = new QueryEvalStats(dataFileN, queryFileN, "DagEval_MVFltSim");

	}

	public void run() {

		tt = new TimeTracker();

		System.out.println("loading graph ...");
		tt.Start();
		loadData();
		double ltm = tt.Stop() / 1000;
		System.out.println("\nTotal loading and building time: " + ltm + "sec.");

		System.out.println("reading queries ...");
		readQueries();
		
		readViews();

		System.out.println("\nEvaluating queries from " + queryFileN + " ...");
		tt.Start();

		evalOneList();
		System.out.println("\nTotal eval time: " + tt.Stop() / 1000 + "sec.");

		writeStatsToCSV();
		// skip the execution of the timeout tasks;
		System.exit(0);
	}

	private void loadData() {
		DaoController dao = new DaoController(dataFileN, stats);
		dao.loadGraphAndIndex();
		invLsts = dao.invLsts;
		l2iMap = dao.l2iMap;
		bfl = dao.bfl;
		g = dao.G;
	}

	private void readQueries() {

		queries = new ArrayList<Query>();
		QueryParser queryParser = new QueryParser(queryFileN, l2iMap);
		Query query = null;
		while ((query = queryParser.readNextQuery()) != null) {
			TransitiveReduction tr = new TransitiveReduction(query);
			tr.reduce();
			checkQueryType(query);
			queries.add(query);
		}
	}
	
	private void readViews() {
		views = new ArrayList<Query>();
		QueryParser queryParser = new QueryParser(viewFileN, l2iMap);
		Query view = null;
		int viewIDcount = 0;
		while ((view = queryParser.readNextQuery()) != null) {
			TransitiveReduction tr = new TransitiveReduction(view);
			tr.reduce();
			view.Qid = viewIDcount;
			viewIDcount++;
			views.add(view);
		}
	}

	private void evalOneList() {
		TimeTracker tt = new TimeTracker();
		for (int i = 0; i < Flags.REPEATS; i++) {
//			for (int k = 0, Q = 0; k < queries.size(); k += 2, Q++) {
			for (int Q = 0; Q < queries.size(); Q ++) {
				Query query = queries.get(Q);
//				query.Qid = Q;
				
//				Query view = queries.get(k + 1);
//				view.Qid = 0;
//				views = new ArrayList<Query>();
//				views.add(view);

				DagEvalMVFltSim eva = new DagEvalMVFltSim(query, views, g, bfl);

				QueryEvalStat stat = null;
				final QueryEvalStat s = new QueryEvalStat();
				double totNodes_before = getTotNodes(query);
				s.totNodesInv = totNodes_before;

				tt.Start();
				try {
					eva.run(s);
					stat = new QueryEvalStat(s);
					stats.add(i, Q, stat);
				}

				catch (LimitExceededException e) {
					eva.clear();
					s.numSolns = eva.getTupleCount();
					stat = new QueryEvalStat(s);
					stat.totTime = tt.Stop() / 1000;
					stat.setStatus(status_vals.exceedLimit);
					stats.add(i, Q, stat);
					// e.printStackTrace();
					System.err.println("Exceed Output Limit!");
				}

				catch (OutOfMemoryError e) {
					eva.clear();
					s.numSolns = eva.getTupleCount();
					stat = new QueryEvalStat(s);
					stat.setStatus(status_vals.outOfMemory);
					stats.add(i, Q, stat);
					System.err.println("Out of Memory!");
					// System.exit(1);
					// continue;
				}

				catch (Exception e) {
					eva.clear();
					s.numSolns = eva.getTupleCount();
					stat = new QueryEvalStat(s);
					stat.setStatus(status_vals.failure);
					stats.add(i, Q, stat);
					e.printStackTrace();
					System.exit(1);
				}
			}
		}
	}

	private void evalWithView() {
		TimeTracker tt = new TimeTracker();

		Query query = queries.get(0);
		views = new ArrayList<Query>();
	    for(int i=1, j=0; i<queries.size(); i++, j++) {
		//for (int i = 1, j = 0; i < 2; i++, j++) {
			Query view = queries.get(i);
			view.Qid = j;
			views.add(view);

		}

		print(query);
		DagEvalMVFltSim eva = new DagEvalMVFltSim(query, views, g, bfl);

		QueryEvalStat stat = null;
		final QueryEvalStat s = new QueryEvalStat();
		double totNodes_before = getTotNodes(query);
		s.totNodesInv = totNodes_before;

		tt.Start();
		try {
			eva.run(s);
			stat = new QueryEvalStat(s);
			stats.add(0, 0, stat);
		}

		catch (LimitExceededException e) {
			eva.clear();
			s.numSolns = eva.getTupleCount();
			stat = new QueryEvalStat(s);
			stat.totTime = tt.Stop() / 1000;
			stat.setStatus(status_vals.exceedLimit);
			stats.add(0, 0, stat);
			// e.printStackTrace();
			System.err.println("Exceed Output Limit!");
		}

		catch (OutOfMemoryError e) {
			eva.clear();
			s.numSolns = eva.getTupleCount();
			stat = new QueryEvalStat(s);
			stat.setStatus(status_vals.outOfMemory);
			stats.add(0, 0, stat);
			System.err.println("Out of Memory!");
			// System.exit(1);
			// continue;
		}

		catch (Exception e) {
			eva.clear();
			s.numSolns = eva.getTupleCount();
			stat = new QueryEvalStat(s);
			stat.setStatus(status_vals.failure);
			stats.add(0, 0, stat);
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void evaluate() {

		TimeTracker tt = new TimeTracker();

		for (int i = 0; i < Flags.REPEATS; i++) {
			for (int Q = 0; Q < queries.size(); Q++) {

				Query query = queries.get(Q);
				System.out.println("\nEvaluating query " + Q + " ...");
				print(query);
				double totNodes_before = getTotNodes(query);
				FilterBuilder fb = new FilterBuilder(g, query);
				java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor();
				SimpleTimeLimiter timeout = new SimpleTimeLimiter(executor);
				final DagHomIEFltSim eva = new DagHomIEFltSim(query, fb, bfl);

				QueryEvalStat stat = null;
				final QueryEvalStat s = new QueryEvalStat();
				s.totNodesInv = totNodes_before;
				try {
					tt.Start();
					timeout.callWithTimeout(new Callable<Boolean>() {

						public Boolean call() throws Exception {
							return eva.run(s);
						}
					}, Consts.TimeLimit, TimeUnit.MINUTES, false);

					stat = new QueryEvalStat(s);
					stats.add(i, Q, stat);

				} catch (UncheckedTimeoutException e) {
					eva.clear();
					s.numSolns = eva.getTupleCount();
					stat = new QueryEvalStat(s);
					stat.setStatus(status_vals.timeout);
					stats.add(i, Q, stat);
					System.err.println("Time Out!");

				} catch (OutOfMemoryError e) {
					eva.clear();
					s.numSolns = eva.getTupleCount();
					stat = new QueryEvalStat(s);
					stat.setStatus(status_vals.outOfMemory);
					stats.add(i, Q, stat);
					System.err.println("Out of Memory!");
					// System.exit(1);
					// continue;
				}

				catch (LimitExceededException e) {
					eva.clear();
					s.numSolns = eva.getTupleCount();
					stat = new QueryEvalStat(s);
					stat.totTime = tt.Stop() / 1000;
					stat.setStatus(status_vals.exceedLimit);
					stats.add(i, Q, stat);
					// e.printStackTrace();
					System.err.println("Exceed Output Limit!");
				}

				catch (Exception e) {
					eva.clear();
					s.numSolns = eva.getTupleCount();
					stat = new QueryEvalStat(s);
					stat.setStatus(status_vals.failure);
					stats.add(i, Q, stat);
					e.printStackTrace();
					System.exit(1);
				}
			}
		}

	}

	private double getTotNodes(Query qry) {

		double totNodes = 0.0;

		QNode[] nodes = qry.nodes;
		for (int i = 0; i < nodes.length; i++) {
			QNode n = nodes[i];
			ArrayList<GraphNode> invLst = invLsts.get(n.lb);
			totNodes += invLst.size();

		}

		System.out.println("Total number of nodes before evaluation:" + totNodes);
		return totNodes;
	}

	private void print(Query qry) {
		System.out.println("Size of inverted list per node:");
		for (int i = 0; i < qry.V; i++) {
			QNode q = qry.nodes[i];
			ArrayList<GraphNode> list = invLsts.get(q.lb);
			System.out.println("qid = " + i + " " + " list = " + list.size());
		}
	}

	private void writeStatsToCSV() {
		PrintWriter opw;

		try {
			opw = new PrintWriter(new FileOutputStream(outFileN, true));
			stats.printToFile(opw);
			opw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void checkQueryType(Query query) {

		QEdge[] edges = query.edges;
		query.childOnly = true;
		for (QEdge edge : edges) {
			AxisType axis = edge.axis;
			if (axis == Consts.AxisType.descendant) {

				query.childOnly = false;
				break;
			}

		}

		QueryDirectedCycle finder = new QueryDirectedCycle(query);
		if (!finder.hasCycle()) {
			query.hasCycle = false;
		} else
			query.hasCycle = true;
	}

	public static void main(String[] args) {

		String dataFileN = args[0], queryFileN = args[1], viewFileN = args[2]; 
		DagMVFltSimMainMultiView demain = new DagMVFltSimMainMultiView(dataFileN, queryFileN, viewFileN);

		demain.run();
	}

}
