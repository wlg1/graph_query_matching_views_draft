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

import org.roaringbitmap.RoaringBitmap;

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
import tupleEnumerator.HybTupleEnumer;
import views.HybAnsGraphBuilder2;

public class ansgraphExclViews3 {
	Query query;
	ArrayList<Query> viewsOfQuery;
	Map<Integer, ArrayList<Pool>> qid_Ansgr;
	HybTupleEnumer tenum;
	ArrayList<Pool> mPool = new ArrayList<Pool>();
	TimeTracker tt;

	public ansgraphExclViews3(Query query_in, ArrayList<Query> viewsOfQuery_in, Map<Integer, ArrayList<Pool>> qid_Ansgr_in) {
		query = query_in;
		viewsOfQuery = viewsOfQuery_in;
		qid_Ansgr = qid_Ansgr_in;
	}

//	public ArrayList<Pool> getAnsGr() {
	public boolean run(QueryEvalStat stat) throws LimitExceededException {
		tt = new TimeTracker();
		tt.Start();
		
		//1) Take intersection of all views
		//for each node set n_q, intersect the node set covering n_q for all views
		//DO NOT TAKE UNION OF VIEW'S NODE SETS, TAKE INTERSECTION.
		//if a node isn't in the query node set already, don't add it
		//if node is in the query node set, take UNION of its adj list w/ prev node's adj list
		//if qnodeset is empty, initialize it with the nodeset of the first view
		
		//bc we take intersection of adj list, store view nodes as list of its query IDs, instead of 
		//needing to loop thru existing list to check if it exists.
		
		for (int i = 0; i < query.V; i++) { // i is query node ID. for each node in query
			Pool Qnodeset = new Pool(); 
			
			for (Query view : viewsOfQuery) {
				int[] hom = getHom(view, query);  //pos is view node ID, value is query node ID
				ArrayList<Pool> viewAnsgr = qid_Ansgr.get(view.Qid); //node sets of view
				for (int j = 0; j < hom.length; j++) { //j is view node ID. find which j matches i
					if (hom[j] == i) {  //found covering node set
						ArrayList<PoolEntry> nodesToAdd = viewAnsgr.get(j).elist(); //the covering node set

						//if qnodeset is empty, initialize it with the nodeset of the first view
						if (Qnodeset.isEmpty()){
							Qnodeset.setList(nodesToAdd);
							break;
						}
						
						//create functions to take union of adj lists
						//the issue w/ union of adj lists is that it may refer to a node that's no longer there
						
						//this is the intersection
						//for each node to add, if its graphID not in Qnodeset,
						// if it is, union its adj lists w/ existing poolentry
						for (PoolEntry newNode: nodesToAdd) {
//							boolean newNodeFlag = true;
							
							//change all view node IDs in adj lists into query node IDs using Hom
//							j - > i. for each adj list in hashmap, change its old key to hom[old key]
							HashMap<Integer, ArrayList<PoolEntry>> newFwdEntries = (HashMap<Integer, ArrayList<PoolEntry>>) null;
							if (newNode.mFwdEntries != null) {
								newFwdEntries = new HashMap<Integer, ArrayList<PoolEntry>>();
								for (Integer key : newNode.mFwdEntries.keySet()) {
									newFwdEntries.put(hom[key], newNode.mFwdEntries.get(key) );
								}
							}
								
							HashMap<Integer, ArrayList<PoolEntry>> newBwdEntries = (HashMap<Integer, ArrayList<PoolEntry>>) null;
							if (newNode.mBwdEntries != null) {
								newBwdEntries = new HashMap<Integer, ArrayList<PoolEntry>>();
								for (Integer key : newNode.mBwdEntries.keySet()) {
									newBwdEntries.put(hom[key], newNode.mBwdEntries.get(key) );
								}
							} 

							HashMap<Integer, RoaringBitmap> newFwdBits = (HashMap<Integer, RoaringBitmap>) null;
							if (newNode.mFwdBits != null) {
								newFwdBits = new HashMap<Integer, RoaringBitmap>();
								for (Integer key : newNode.mFwdBits.keySet()) {
									newFwdBits.put(hom[key], newNode.mFwdBits.get(key) );
								}
							}

							HashMap<Integer, RoaringBitmap> newBwdBits = (HashMap<Integer, RoaringBitmap>) null;
							if (newNode.mBwdBits != null) {
								newBwdBits = new HashMap<Integer, RoaringBitmap>();
								for (Integer key : newNode.mBwdBits.keySet()) {
									newBwdBits.put(hom[key], newNode.mBwdBits.get(key) );
								}
							}
							
							PoolEntry newEntry = new PoolEntry(newNode.getPos(), newNode.getQNode(), newNode.mValue,
									newFwdEntries, newBwdEntries, newFwdBits, newBwdBits,
									newNode.getNumChildEnties(), newNode.getNumParEnties(), newNode.size() );
							
							Pool newQnodeset = new Pool(); 
							//remove nodes from prev node set which are not in current view's node set
							//loop through old nodeset and if no match to any newval, don't add it
							//keep track of what has a match so far. only add if there's a match.
							
							for (PoolEntry oldNode: Qnodeset.elist()) {  //check if already exists as oldNode
								if (oldNode.mValue.id == newEntry.mValue.id) {
//									newNodeFlag = false;
									
									//remember entries is hashmap, so need to go thru each node it points to
									
									if (newEntry.mBwdEntries != null) {
										if (oldNode.mBwdEntries == null) {
											oldNode.mBwdEntries = newEntry.mBwdEntries;
											oldNode.mBwdBits = newEntry.mBwdBits;
										} else {
											for (Integer key : oldNode.mBwdEntries.keySet()) {
												ArrayList<PoolEntry> nodeBwd = oldNode.mBwdEntries.get(key);
//												nodeBwd.retainAll(newEntry.mBwdEntries.get(key) );
												nodeBwd.addAll(newEntry.mBwdEntries.get(key) );
											}
											for (Integer key : oldNode.mBwdBits.keySet()) {
												RoaringBitmap nodeBwdbits = oldNode.mBwdBits.get(key);
//												nodeBwdbits.and(newEntry.mBwdBits.get(key) );
												nodeBwdbits.or(newEntry.mBwdBits.get(key) );
											}
										}
									}
									
									if (newEntry.mFwdEntries != null) {
										if (oldNode.mFwdEntries == null) {
											oldNode.mFwdEntries = newEntry.mFwdEntries;
											oldNode.mFwdBits = newEntry.mFwdBits;
										} else {
											for (Integer key : oldNode.mFwdEntries.keySet()) {
												ArrayList<PoolEntry> nodeFwd = oldNode.mFwdEntries.get(key);
//												nodeFwd.retainAll(newEntry.mFwdEntries.get(key) );
												nodeFwd.addAll(newEntry.mFwdEntries.get(key) );
											}			
											
											for (Integer key : oldNode.mFwdEntries.keySet()) {
												RoaringBitmap nodeFwdbits = oldNode.mFwdBits.get(key);
//												nodeFwdbits.and(newEntry.mFwdBits.get(key) );
												nodeFwdbits.or(newEntry.mFwdBits.get(key) );
											}	
										}
									}
								newQnodeset.addEntry(oldNode);
								Qnodeset = newQnodeset;
								}
							}
//							if (newNodeFlag) {
//								Qnodeset.addEntry(newEntry); 
//							}
						}
						break;
					}
				}
			}
			mPool.add(Qnodeset);
		}
		//for each edge, if a poolentry does not have that edge, remove it
//		ArrayList<Pool> mPool2 = new ArrayList<Pool>();
//		for ( int i = 0; i < query.V; i++ ) {
//			mPool2.add(new Pool() );
//		}
		
		//only add if the poolentry satisfies ALL edges. so dont loop thru edges; loop thru poolentries.
//		for (QEdge qEdge : query.edges ) {
//			ArrayList<PoolEntry> headNodeSet = mPool.get(qEdge.from).elist();
//			for (PoolEntry Hpe : headNodeSet) {
//				if (qEdge.from == 1 && qEdge.to == 2 && Hpe.mFwdEntries == null) {
//					System.out.println("debug here");
//				}
//				
//				if (Hpe.mFwdEntries != null ) {
//					if (Hpe.mFwdEntries.containsKey(qEdge.to) && !mPool2.get(qEdge.from).elist().contains(Hpe)) { //containsKey only works if !null
//						mPool2.get(qEdge.from).addEntry(Hpe);
//					}
//				}
//				if (Hpe.mValue.id == 8) { //why is this added even tho its fwd lst is null
//					System.out.println(Hpe.mFwdEntries != null );
//					System.out.println("debug here");
//				}
//			}
//			ArrayList<PoolEntry> tailNodeSet = mPool.get(qEdge.to).elist();
//			for (PoolEntry Tpe : tailNodeSet) {
//				if (Tpe.mBwdEntries != null) {
//					if (Tpe.mBwdEntries.containsKey(qEdge.from) && !mPool2.get(qEdge.to).elist().contains(Tpe) ) {
//						mPool2.get(qEdge.to).addEntry(Tpe);
//					}
//				}
//			}
//		}
		
		//issue: rmv nodes doesnt rmv them from adj list of other nodes
		
		//This is the 'intersection' of edges b/w node sets. Above is for indiv poolentries, which is union
		//only keep graph nodes w/ edges on them, and must satisfy all edges their matched query node has
//		for ( int i = 0; i < query.V; i++ ) {
//			ArrayList<PoolEntry> currNodeSet = mPool.get(i).elist();
//			for (PoolEntry pe : currNodeSet) {
//				boolean addNode = true;
//				for (QEdge qEdge : query.edges ) { // if just one edge fails, don't add node 
//					if (qEdge.from == i ) {  //edge uses this node set as head
//						if (pe.mFwdEntries == null ) {
//							addNode = false;
//							break;
//						} else if (!pe.mFwdEntries.containsKey(qEdge.to) ) {
//							addNode = false;
//							break;
//						}
//					} else if (qEdge.to == i ) {  //edge uses this node set as head
//						if (pe.mBwdEntries == null ) {
//							addNode = false;
//							break;
//						} else if (!pe.mBwdEntries.containsKey(qEdge.from) ) {
//							addNode = false;
//							break;
//						}
//					}
//				}
//				if (addNode && !mPool2.get(i).elist().contains(pe)) {
//					mPool2.get(i).addEntry(pe);
//				}
//			}
//		}
		
		//2) for each query edge, check if covered by at least one view. if not, compute occ set of closure
		
		//go to node set of ans gr and find a key To target vertex. if this key exists, that edge is covered.

		//if edge uncovered
//		if (Qnodeset.isEmpty() ){
			// Convert view into graph and get Closure
//			TransitiveReduction tr = new TransitiveReduction(view);
//			AxisType[][] Vclosure = tr.pathMatrix;  // by comparing closure to compare to orig.edges, see that closure's new edges are desc edges, and doesn't change child edges
//			
			//convert Vclosure into query form, viewClosure
//			Query viewClosure = view;
			
//			//find if there's a match. if so, get 1 covering edge from view
//			int[] hom1 = getHom(viewClosure, query);
			
//		}
		
		double buildtm = tt.Stop() / 1000;
		stat.setMatchTime(buildtm);
		stat.calAnsGraphSize(mPool);
		stat.setTotNodesAfter(calTotCandSolnNodes());
		System.out.println("Answer graph build time:" + buildtm + " sec.");
		
		//run MIjoin using occurrence lists to get answer again
		double numOutTuples;
		tt.Start();
		tenum = new HybTupleEnumer(query, mPool);
		numOutTuples = tenum.enumTuples();
		
		double enumtm = tt.Stop() / 1000;
		stat.setEnumTime(enumtm);
		System.out.println("Tuple enumeration time:" + enumtm + " sec.");

		stat.setNumSolns(numOutTuples);
		clear();
		return true;
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
	

	public void clear() {
		if (mPool != null)
			for (Pool p : mPool)
				p.clear();
	}

	public double getTupleCount() {

		if (tenum != null)
			return tenum.getTupleCount();
		return 0;
	}
	
	private double calTotCandSolnNodes() {

		double totNodes = 0.0;
		for (Pool pool : mPool) {
			ArrayList<PoolEntry> elist = pool.elist();
			totNodes += elist.size();

		}
		return totNodes;
	}
	
	public void printSolutions(ArrayList<PoolEntry> elist) {

		if (elist.isEmpty())
			return;

		for (PoolEntry r : elist) {

			System.out.println(r);

		}

	}
	
	public static void main(String[] args) {

	}

}

