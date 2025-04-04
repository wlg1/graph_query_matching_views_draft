package views;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import answerGraph.HybAnsGraphBuilder;
import dao.BFLIndex;
import dao.DaoController;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
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
import prefilter.FilterBuilder;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import query.graph.QueryDirectedCycle;
import query.graph.QueryParser;
import query.graph.TransitiveReduction;
import views.ansgraphExclViews3; 

public class viewhom {

	ArrayList<Query> queries, views;
	HashMap<String, Integer> l2iMap;
	String queryFileN, dataFileN, outFileN, viewFileN;
	ArrayList<ArrayList<GraphNode>> invLsts;
	BFLIndex bfl;
	TimeTracker tt;
	QueryEvalStats stats;
	Digraph g;

	public viewhom(String dataFN, String queryFN, String viewFN) {

		queryFileN = Consts.INDIR + queryFN;
		dataFileN = Consts.INDIR + dataFN;
		viewFileN = Consts.INDIR + viewFN;
//		String suffix = ".csv";
//		String fn = queryFN.substring(0, queryFN.lastIndexOf('.'));
//		String datafn = dataFN.substring(0, dataFN.lastIndexOf('.'));
//		outFileN = Consts.OUTDIR + datafn + "_" + fn + "__sim" + suffix;
		stats = new QueryEvalStats(dataFileN, queryFileN, "vw");

	}

	//Build answer graph of each query on a data graph
	public void run() throws LimitExceededException {
		tt = new TimeTracker();
		tt.Start();
		
		System.out.println("loading graph ...");
		loadData();  //gets l2iMap, which is used in readQueries()
		
		System.out.println("reading queries ...");
		readQueries();
		readViews();
//		for (Query query : this.queries) {
//			for (QEdge qEdge : query.edges ) {
//				int x = 1;
//			}
//		}
		
		//look up table for view answer graph using Qid of view
		Map<Integer, ArrayList<Pool>> qid_Ansgr = new HashMap<>();
		
		//FILTER set of views for query to decide which ones to use to
		ArrayList<ArrayList<Query>> viewsOfQueries = new ArrayList<ArrayList<Query>>();
		ArrayList<ArrayList<MatArray>> qryCandLsts = new ArrayList<ArrayList<MatArray>>();
		for (Query query : this.queries) {  //for each query, select views
			ArrayList<Query> viewsOfQuery = new ArrayList<Query>();
			for (Query view : this.views) {
				if (checkHom(view, query)) {
					viewsOfQuery.add(view);
					//add view to list, then assoc it with an Qid. Add Qid to viewsOfQuery
					
					//for each view that is used in at least 1 query, find its ans graph
					if (!qid_Ansgr.containsKey(view.Qid)) {
						FilterBuilder fb = new FilterBuilder(g, view);
						getAnsGr ansgrBuilder = new getAnsGr(view, fb, bfl);
						qid_Ansgr.put(view.Qid, ansgrBuilder.run() );
						qryCandLsts.add(ansgrBuilder.getCandLists());
					}
				}
			}
			viewsOfQueries.add(viewsOfQuery);
		}
		
//		ArrayList<ArrayList<Pool>> queryAnsGraphs = new ArrayList<ArrayList<Pool>>();
		ArrayList<ArrayList<ArrayList<PoolEntry>>> queryAnsGraphs = new ArrayList<ArrayList<ArrayList<PoolEntry>>>(); 
		// build answer graph of each query exclusively using its views
		for (int i = 0; i < this.queries.size(); i ++) {
			Query query = this.queries.get(i);
			ArrayList<Query> viewsOfQuery = viewsOfQueries.get(i);
			ansgraphExclViews viewCandNodes = new ansgraphExclViews(query, viewsOfQuery, qid_Ansgr);
			ArrayList<ArrayList<PoolEntry>> QcosNodeSets = viewCandNodes.getAnsGr();
//			queryAnsGraphs.add( QcosNodeSets );
			
//			ArrayList<MatArray> mCandLists = qryCandLsts.get(i);
//			HybAnsGraphBuilder2 agBuilder = new HybAnsGraphBuilder2(query, bfl, mCandLists, QcosNodeSets);
//			ArrayList<Pool> qryAnsGr = agBuilder.run();
//			queryAnsGraphs.add(qryAnsGr);
			
			//compare to result using algo. They will not be equal due to having diff objs
			//try using small data graph as example
//			HybAnsGraphBuilder2 agBuilder = new HybAnsGraphBuilder2(query, bfl, mCandLists);
			
		}
		
		System.out.println(1);
//		double ltm = tt.Stop() / 1000;
//		System.out.println("\nTotal time: " + ltm + "sec.");
//		System.exit(0);
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
			TransitiveReduction tr = new TransitiveReduction(query);
			tr.reduce();
//			System.out.println(query);
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

	public static void main(String[] args) throws LimitExceededException {

		String dataFileN = args[0], queryFileN = args[1], viewFileN = args[2];
		viewhom demain = new viewhom(dataFileN, queryFileN, viewFileN);

		demain.run();
	}

}

