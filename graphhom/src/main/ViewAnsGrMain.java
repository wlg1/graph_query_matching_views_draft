package main;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import dao.BFLIndex;
import dao.DaoController;
import dao.Pool;
import dao.PoolEntry;
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
import prefilter.FilterBuilder;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import query.graph.QueryDirectedCycle;
import query.graph.QueryParser;
import query.graph.TransitiveReduction;
import views.ansgraphExclViews3;
import views.ansgraphExclViews4;
import views.getAnsGr;

public class ViewAnsGrMain {

	ArrayList<Query> queries, views;
	HashMap<String, Integer> l2iMap;
	String queryFileN, dataFileN, outFileN, viewFileN;
	ArrayList<ArrayList<GraphNode>> invLsts;
	BFLIndex bfl;
	TimeTracker tt;
	QueryEvalStats stats;
	Digraph g;

	public ViewAnsGrMain(String dataFN, String queryFN, String viewFN) {

		queryFileN = Consts.INDIR + queryFN;
		dataFileN = Consts.INDIR + dataFN;
		viewFileN = Consts.INDIR + viewFN;
		String suffix = ".csv";
		String fn = queryFN.substring(0, queryFN.lastIndexOf('.'));
		String datafn = dataFN.substring(0, dataFN.lastIndexOf('.'));
		outFileN = Consts.OUTDIR + datafn + "_" + fn + "__ansgrBYVIEWS" + suffix;
		stats = new QueryEvalStats(dataFileN, queryFileN, "DagEval_ansgr");

	}

	public void run() throws LimitExceededException {

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
		evaluate();
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
		int count = 0;

		while ((query = queryParser.readNextQuery()) != null) {
			// System.out.println(query);
			TransitiveReduction tr = new TransitiveReduction(query);
			tr.reduce();
			System.out.println(query);
			checkQueryType(query);
			//if (!query.childOnly && !query.hasCycle) {

				queries.add(query);
				count++;
			//}
			if(query.childOnly){
				
				System.out.println("Child only query:" + query.Qid);
				
			}
		}

		System.out.println("Total valid queries: " + count);
	}
	
	private void readViews() {
		views = new ArrayList<Query>();
		QueryParser queryParser = new QueryParser(viewFileN, l2iMap);
		Query view = null;
		while ((view = queryParser.readNextQuery()) != null) {
//			TransitiveReduction tr = new TransitiveReduction(view);
//			tr.reduce();
			views.add(view);
		}
	}

	private void evaluate() throws LimitExceededException {
		
		ArrayList<ArrayList<Query>> viewsOfQueries = new ArrayList<ArrayList<Query>>();
		//look up table for view answer graph using Qid of view
		Map<Integer, ArrayList<Pool>> qid_Ansgr = new HashMap<>();
		for (Query view : this.views) {
			FilterBuilder fbV = new FilterBuilder(g, view);
			getAnsGr ansgrBuilder = new getAnsGr(view, fbV, bfl);
			//add view to list, then assoc it with an Qid. Add Qid to viewsOfQuery
			qid_Ansgr.put(view.Qid, ansgrBuilder.run() );
		}

		TimeTracker tt = new TimeTracker();

//		ArrayList<ArrayList<Pool>> queryAnsGraphs = new ArrayList<ArrayList<Pool>>();
		for (int i = 0; i < Flags.REPEATS; i++) {
			for (int Q = 0; Q < queries.size(); Q++) {

				Query query = queries.get(Q);
				System.out.println("\nEvaluating query " + Q + " ...");
				double totNodes_before = getTotNodes(query);
//				FilterBuilder fb = new FilterBuilder(g, query);
				java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor();
				SimpleTimeLimiter timeout = new SimpleTimeLimiter(executor);
				
				ArrayList<Query> viewsOfQuery = new ArrayList<Query>();
				for (Query view : this.views) {
					if (checkHom(view, query)) {
						viewsOfQuery.add(view);
					}
				}
				viewsOfQueries.add(viewsOfQuery);
				
				ansgraphExclViews4 eva = new ansgraphExclViews4(query, viewsOfQuery, qid_Ansgr);
//				ArrayList<Pool> qryAnsGr = eva.getAnsGr();
//				queryAnsGraphs.add(qryAnsGr);
				
				//compare to result using algo. They will not be equal due to having diff objs
				//try using small data graph as example
				
				QueryEvalStat stat = null;
				final QueryEvalStat s = new QueryEvalStat();
				s.totNodesBefore = totNodes_before;
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
					stat= new QueryEvalStat(s);	
					stat.totTime = tt.Stop()/1000;
					stat.setStatus(status_vals.exceedLimit);
					stats.add(i, Q, stat);
					//e.printStackTrace();
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

		return totNodes;
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

	private boolean checkHom(Query view, Query query) { 		//this is exhaustive, iterative
		//1. For each view node, get all query nodes with same labels 
		ArrayList<ArrayList<Integer>> nodeMatch = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < view.V; i++) {
			  ArrayList<Integer> vMatches = new ArrayList<Integer>(); 
			  for (int j = 0; j < query.V; j++) {
				  //check if same label as query
				  if (view.nodes[i].lb == query.nodes[j].lb) {
					  vMatches.add(query.nodes[j].id);
				  }
			  }  //end check qry candmatches for viewnode i
			  if (vMatches.isEmpty()) {
				return false; //at least 1 viewnode has no candmatches	
			  }
			  nodeMatch.add(vMatches); 
		} // end checking candmatches for all view nodes

		// 2. Convert query into graph and get Closure
		TransitiveReduction tr = new TransitiveReduction(query);
		AxisType[][] Qclosure = tr.pathMatrix;  // by comparing closure to compare to orig.edges, see that closure's new edges are desc edges, and doesn't change child edges
		
		//3. Given a node mapping h: for each view child edge, check if (h(x), h(y)) is a child edge
		// Try an initial mapping. 
		int[] candHom = new int[nodeMatch.size()]; 
		for (int i = 0; i < nodeMatch.size(); i++) {
			candHom[i] = nodeMatch.get(i).get(0);
		}
		
		//NOTE: ENSURE that 2 view nodes don't map to same query node. If they do, try next mapping.
		
		//keep row and col pointers on which match to change in candHom for next mapping.
		//if fail, move the col pointer right. if col pointer > col size, set cols of all rows below row pointer 
		//to 0 and move row pointer up (-1) and its col pointer right. then, set row pointer back to lowest row
		int rowChangeNext = nodeMatch.size() - 1;  //row pointer
		int colChangeToNext = 0; //col pointer
		
		while (true) {
			for (QEdge edge : view.edges) {
				String vEdgeType = edge.axis.toString();
				int viewHnode = edge.from; //head node of view
				int viewTnode = edge.to; //tail node of view
				int qryHnode = candHom[viewHnode]; // h(head node)
				int qryTnode = candHom[viewTnode]; // h(tail node)
				String qEdgeType = Qclosure[qryHnode][qryTnode].toString();
				
				if (!vEdgeType.equals(qEdgeType)) {
					//mapping failed,  so try another
					++colChangeToNext;
					
					//make sure there is next match for curr col. if not, go to row above to move it right
					if (colChangeToNext > nodeMatch.get(rowChangeNext).size() - 1) {
						while (colChangeToNext > nodeMatch.get(rowChangeNext).size() - 1){
							//move row pointer up (-1) and its col pointer right
							--rowChangeNext;
							
							if (rowChangeNext < 0) {
								return false; //'all mappings tried'
							}
							
							//get curr col pointer of new row pointer
							colChangeToNext = nodeMatch.get(rowChangeNext).indexOf(candHom[rowChangeNext]);
							++colChangeToNext;
							
							//check if col has another match to the right
							if (colChangeToNext <= nodeMatch.get(rowChangeNext).size() - 1) {
								
								//set cols of all rows below row pointer to 0. only do this if -1 to row pointer
								for (int i = nodeMatch.size() - 1; i > rowChangeNext; i--) {
									candHom[i] = nodeMatch.get(i).get(0);
								} //end for: reseting col indices
								
								candHom[rowChangeNext]= nodeMatch.get(rowChangeNext).get(colChangeToNext);
								rowChangeNext = nodeMatch.size() - 1; //reset col pointer and row pointer
								colChangeToNext = 0; 
							} // end if: check col has another match to the right
						} //end while: checking if col reached end
						
					} else {
						candHom[rowChangeNext]= nodeMatch.get(rowChangeNext).get(colChangeToNext);
					}
					break; //break out 'end of check each edge' and try new candHom mapping
				} //end if (!vEdgeType.equals(qEdgeType)): check edge consistency
			} // end for (QEdge edge : view.edges): check each edge consistency
			// Found mapping with all edges consistent -> use view for query, add to list of query's views
			return true; //all edges passed, so use this mapping
		} // end of while loop
	} //end of checkHom()
	
	public static void main(String[] args) throws LimitExceededException {

		String dataFileN = args[0], queryFileN = args[1], viewFileN = args[2];
		ViewAnsGrMain demain = new ViewAnsGrMain(dataFileN, queryFileN, viewFileN);

		demain.run();
	}

}
