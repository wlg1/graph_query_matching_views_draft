package views;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import views.HybAnsGraphBuilder2;

public class ansgraphExclViews2 {

	Query query;
	ArrayList<Query> viewsOfQuery;
	Map<Integer, ArrayList<Pool>> qid_Ansgr;
	
	TimeTracker tt;

	public ansgraphExclViews2(Query query_in, ArrayList<Query> viewsOfQuery_in, Map<Integer, ArrayList<Pool>> qid_Ansgr_in) {
		query = query_in;
		viewsOfQuery = viewsOfQuery_in;
		qid_Ansgr = qid_Ansgr_in;
	}

	public ArrayList<ArrayList<PoolEntry>> getAnsGr() {
		tt = new TimeTracker();
		tt.Start();
		
		//instead of edge lists, use poolentry lists, since that's how ansgr is repr
		ArrayList<ArrayList<PoolEntry>> QcosNodeSets = new ArrayList<ArrayList<PoolEntry>>();
		//for each node set in query node, create an arraylist
		for (int i = 0; i < query.V; i++) {
			ArrayList<PoolEntry> cos = new ArrayList<PoolEntry>();
			QcosNodeSets.add(cos);
		}
		
		for (QEdge qEdge : query.edges ) {
			for (Query view : viewsOfQuery) {
				ArrayList<QEdge> coveringEdges = new ArrayList<QEdge>(); //U
				
				int[] hom = getHom(view, query);  //pos is view node ID, value is query node ID
				
				//get QEdge of view using query edge
				int vHead = 0, vTail = 0, len = hom.length, i = 0;
		        while (i < len) {
		            if (hom[i] == qEdge.from) {
		            	vHead = i;
		            } else if (hom[i] == qEdge.to) {
		            	vTail = i;
		            }
		            i = i + 1;
		        }
		        
				for (QEdge vEdge : view.edges){
					if ((vEdge.from == vHead) && (vEdge.to == vTail)) {
						coveringEdges.add(vEdge);
//						break;
					}
				}
				
				if (!coveringEdges.isEmpty() ){
					// loop to intersect occ set of all edges in covering 
					for (QEdge coveringEdge: coveringEdges) {
						//get occ list of coveringEdge
						//get the head node occ set (pool) and find all the edges 
						//to tail node occ set (another pool)
						ArrayList<Pool> viewAnsgr = qid_Ansgr.get(view.Qid); //Arraylist of node set Pools
						Pool headNodes = viewAnsgr.get(coveringEdge.from); //a Pool of elists of head nodes
						Pool tailNodes = viewAnsgr.get(coveringEdge.to);
						
						//add these nodes to the occ sets of the Query node sets that the view nodes cover
						ArrayList<PoolEntry> headNodesToAdd = headNodes.elist();
						ArrayList<PoolEntry> tailNodesToAdd = tailNodes.elist();
						
						//from hashmap of query node sets, get Hom(view node coveringEdge.from)
						ArrayList<PoolEntry> cosH = QcosNodeSets.get(qEdge.from); 
						ArrayList<PoolEntry> cosT = QcosNodeSets.get(qEdge.to); 
						
						//set as occ set if empty; else take intersection with existing
						cosH.addAll(headNodesToAdd);
						cosT.addAll(tailNodesToAdd);
						
						//draw an example to see why use intersection
					}
				} else {
					// Convert view into graph and get Closure
					TransitiveReduction tr = new TransitiveReduction(view);
					AxisType[][] Vclosure = tr.pathMatrix;  // by comparing closure to compare to orig.edges, see that closure's new edges are desc edges, and doesn't change child edges
					
					//convert Vclosure into query form, viewClosure
//					Query viewClosure = view;
//					
//					//find if there's a match. if so, get 1 covering edge from view
//					int[] hom1 = getHom(viewClosure, query);
//					
//					//get QEdge of view using query edge
//					int vHead1 = 0, vTail1 = 0, len1 = hom1.length, i1 = 0;
//			        while (i1 < len1) {
//			            if (hom1[i1] == qEdge.from) {
//			            	vHead1 = i1;
//			            } else if (hom1[i1] == qEdge.to) {
//			            	vTail1 = i1;
//			            }
//			            i1 = i1 + 1;
//			        }
//					
//			        QEdge coveringEdge = null;
//					for (QEdge vEdge : view.edges){
//						if ((vEdge.from == vHead1) && (vEdge.to == vTail1)) {
//							coveringEdge = vEdge;
//							break; //find only 1
//						}
//					}
//					
//					ArrayList<Pool> viewAnsgr = qid_Ansgr.get(view.Qid); //Arraylist of node set Pools
//					Pool headNodes = viewAnsgr.get(coveringEdge.from); //a Pool of elists of head nodes
//					Pool tailNodes = viewAnsgr.get(coveringEdge.to);
//					
//					//add these nodes to the occ sets of the Query node sets that the view nodes cover
//					ArrayList<PoolEntry> headNodesToAdd = headNodes.elist();
//					ArrayList<PoolEntry> tailNodesToAdd = tailNodes.elist();
//					
//					//from hashmap of query node sets, get Hom(view node coveringEdge.from)
//					ArrayList<PoolEntry> cosH = QcosNodeSets.get(qEdge.from); 
//					ArrayList<PoolEntry> cosT = QcosNodeSets.get(qEdge.to); 
//					
//					//set as occ set if empty; else take intersection with existing
//					cosH.addAll(headNodesToAdd);
//					cosT.addAll(tailNodesToAdd);
				}
			}
		}
		//convert QcosNodeSets into an Arraylist of Pools
		
		return QcosNodeSets;
		
//		double ltm = tt.Stop() / 1000;
//		System.out.println("\nTotal time: " + ltm + "sec.");
//		System.exit(0);
	}
	
	private int[] getHom(Query view, Query query) { 		//this is exhaustive, iterative
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
			return candHom; //all edges passed, so use this mapping
		} // end of while loop
	} //end of getHom()
	
	public static void main(String[] args) {

	}

}

