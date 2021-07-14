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
import evaluator.PartialViewAnsGr;
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
import views.getAnsGrViews;
import views.nodeset;

public class PartialViewAnsGrMain {

	ArrayList<Query> queries, views;
	HashMap<String, Integer> l2iMap;
	String queryFileN, dataFileN, outFileN, viewFileN;
	ArrayList<ArrayList<GraphNode>> invLsts;
	BFLIndex bfl;
	TimeTracker tt;
	QueryEvalStats stats;
	Digraph g;
	boolean useAnsGr;
	boolean rmvEmpty;
	boolean simfilter;
	
	public PartialViewAnsGrMain(String dataFN, String queryFN, String viewFN, boolean INuseAnsGr, 
			boolean INrmvEmpty, boolean INsimfilter) {

		queryFileN = Consts.INDIR + queryFN;
		dataFileN = Consts.INDIR + dataFN;
		viewFileN = Consts.INDIR + viewFN;
		String suffix = ".csv";
		String fn = queryFN.substring(0, queryFN.lastIndexOf('.'));
		String datafn = dataFN.substring(0, dataFN.lastIndexOf('.'));
		useAnsGr = INuseAnsGr;
		rmvEmpty = INrmvEmpty;
		simfilter = INsimfilter;
		
		if (useAnsGr) {
			outFileN = Consts.OUTDIR + datafn + "_" + fn + "__ansgrBYVIEWS";
			stats = new QueryEvalStats(dataFileN, queryFileN, "DagEval_ansgr");
		} else {
			outFileN = Consts.OUTDIR + datafn + "_" + fn + "__simgrBYVIEWS";
			stats = new QueryEvalStats(dataFileN, queryFileN, "DagEval_simgr");
		}
		
		if (rmvEmpty) {
			outFileN = outFileN + "_rmvEmpty";
		}
		
		if (simfilter) {
			outFileN = outFileN + "_FLTSIM";
		} else {
			outFileN = outFileN + "_FLT";
		}
		outFileN = outFileN + suffix;
	}

	public void run() throws Exception {

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
//		System.exit(0);
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

	private void evaluate() throws Exception {
		
//		ArrayList<ArrayList<Query>> viewsOfQueries = new ArrayList<ArrayList<Query>>();
		HashMap<Integer, GraphNode> posToGN = new HashMap<Integer, GraphNode>(); 
		Map<Integer, ArrayList<nodeset>> qid_Ansgr = new HashMap<>(); //look up table for view answer graph using Qid of view
		for (Query view : this.views) {
			QueryEvalStat stat = null;
			final QueryEvalStat sV = new QueryEvalStat();
			double totNodes_before = getTotNodes(view);
			sV.totNodesBefore = totNodes_before;
			
			FilterBuilder fbV = new FilterBuilder(g, view);
			getAnsGrViews ansgrBuilder = new getAnsGrViews(view, fbV, bfl, posToGN, useAnsGr);
			//add view to list, then assoc it with an Qid. Add Qid to viewsOfQuery
			qid_Ansgr.put(view.Qid, ansgrBuilder.run(sV) );
			posToGN = ansgrBuilder.posToGN;
			
			stat = new QueryEvalStat(sV);
			stats.addView(stat);
		}

		TimeTracker tt = new TimeTracker();

//		ArrayList<ArrayList<Pool>> queryAnsGraphs = new ArrayList<ArrayList<Pool>>();
		for (int i = 0; i < Flags.REPEATS; i++) {
			for (int Q = 0; Q < queries.size(); Q++) {

				Query query = queries.get(Q);
				System.out.println("\nEvaluating query " + Q + " ...");
				double totNodes_before = getTotNodes(query);
				java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor();
				SimpleTimeLimiter timeout = new SimpleTimeLimiter(executor);
				
				ArrayList<Query> viewsOfQuery = new ArrayList<Query>();
				for (Query view : this.views) {
					if (checkHom(view, query)) {
						viewsOfQuery.add(view);
					}
				}
//				viewsOfQueries.add(viewsOfQuery);
				
				//compare to result using algo. They will not be equal due to having diff objs
				//try using small data graph as example
				
				QueryEvalStat stat = null;
				final QueryEvalStat s = new QueryEvalStat();
				s.totNodesBefore = totNodes_before;
				
				FilterBuilder fb = new FilterBuilder(g, query);
				PartialViewAnsGr eva = new PartialViewAnsGr(query, viewsOfQuery, qid_Ansgr, posToGN,
						fb, bfl, rmvEmpty, simfilter);
				
//				queryAnsGraphs.add(mPool);
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
			stats.printToFileViews(opw);
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
			  ArrayList<Integer> vMatches = new ArrayList<Integer>(); //a view's match cand list
			  for (int j = 0; j < query.V; j++) {
				  //check if same label as query
				  if (view.nodes[i].lb == query.nodes[j].lb) {
					  vMatches.add(query.nodes[j].id);
				  }
			  }  //end check qry candmatches for viewnode i

			  nodeMatch.add(vMatches); 
		} // end checking candmatches for all view nodes

		// 2. Convert query into graph and get Closure. Same for view
		TransitiveReduction tr = new TransitiveReduction(query);
		AxisType[][] Qclosure = tr.pathMatrix;  // by comparing closure to compare to orig.edges, see that closure's new edges are desc edges, and doesn't change child edges
		
		TransitiveReduction trV = new TransitiveReduction(view);
		AxisType[][] Vclosure = trV.pathMatrix;
		
		//3. Given a node mapping h: for each view child edge, check if (h(x), h(y)) is a child edge
		// Try an initial mapping using the first query node of every view's cand list. 
		int[] candHom = new int[nodeMatch.size()]; 
		for (int i = 0; i < nodeMatch.size(); i++) {
			candHom[i] = nodeMatch.get(i).get(0);
		}
		
		//one map should be view to query, another is query to view. output latter.
		
		//NOTE: ENSURE that 2 view nodes don't map to same query node. If they do, try next mapping.
		
		//keep row and col pointers on which match to change in candHom for next mapping.
		//if fail, move the col pointer right. if col pointer > col size, set cols of all rows below row pointer 
		//to 0 and move row pointer up (-1) and its col pointer right. then, set row pointer back to lowest row
		int rowChangeNext = nodeMatch.size() - 1;  //row pointer for view node
		int colChangeToNext = 0; //col pointer for candidate query nodes for current view row
		
		//try various permutations of matches
		while (true) {
			//try new mapping: for each edge, make sure matched head+tail nodes so far give consistent edge type
			boolean passFlag = true;
			
			//check if each value in candHom is unique. no 2 view nodes can map to the same query node.
			ArrayList<Integer> coveringsSoFar = new ArrayList<Integer>(); //use list bc it has .contains()
			for (int i = 0; i < candHom.length; i++) {
				if (i > 0) {
					if (coveringsSoFar.contains(candHom[i])) {
						passFlag = false;
						break;
					}
				}
				coveringsSoFar.add(candHom[i]);
			}
			
			for (QEdge edge : view.edges) {  //match the nodes in each edge
				int viewHnode = edge.from; //head node of view
				int viewTnode = edge.to; //tail node of view
//				String vEdgeType = edge.axis.toString();
				String vEdgeType = Vclosure[viewHnode][viewTnode].toString();
				int qryHnode = candHom[viewHnode]; // h(head node)
				int qryTnode = candHom[viewTnode]; // h(tail node)
				String qEdgeType = Qclosure[qryHnode][qryTnode].toString();
				
				if (!vEdgeType.equals(qEdgeType)) {
					passFlag = false;
				} //end if (!vEdgeType.equals(qEdgeType)): check edge consistency
				//else consistent edge type for this edge, so don't change mapping b/c it's good
			} // end for (QEdge edge : view.edges): check each edge consistency
			
			if (passFlag) {
				return true; //get out of while loop to test a new mapping
			}
			
			//else: mapping failed,  so try another
			++colChangeToNext; //try new query node for curr view row
			//make sure there is next match for curr row. if not, go to row above to move it right
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
					//else: move row pointer down again
				} //end while: checking if col reached end
			} else {  //try the next match for this row
				candHom[rowChangeNext]= nodeMatch.get(rowChangeNext).get(colChangeToNext);
			}
		} // end of while loop to test a new mapping
	} //end of getHom()
	
	public static void main(String[] args) throws Exception {

		String dataFileN = args[0], queryFileN = args[1], viewFileN = args[2];
		boolean useAnsGr = false;
		boolean rmvEmpty = true;
		boolean simfilter = true;
		PartialViewAnsGrMain demain = new PartialViewAnsGrMain(dataFileN, queryFileN, viewFileN, 
				useAnsGr, rmvEmpty, simfilter);

		demain.run();
	}

}
