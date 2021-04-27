package answerGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.roaringbitmap.RoaringBitmap;

import dao.BFLIndex;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import global.Consts.AxisType;
import graph.GraphNode;
import helper.QueryEvalStat;
import helper.TimeTracker;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import query.graph.TransitiveReduction;
import queryPlan.PlanGenerator;
import views.nodeset;

//only add edge to nodes if covering edge has it

public class HybAnsGraphBuilderViews {

	Query mQuery;
	ArrayList<Pool> mPool;
	ArrayList<Query> viewsOfQuery;
	Map<Integer, ArrayList<nodeset>> qid_Ansgr;
	HashMap<Integer, HashMap<Integer, Integer>> viewHoms;
	ArrayList<nodeset> intersectedAnsGr;
	TimeTracker tt;
	QueryEvalStat stat;
	HashMap<Integer, GraphNode> posToGN;
	
	public HybAnsGraphBuilderViews(Query query, ArrayList<Query> viewsOfQuery_in,
			Map<Integer, ArrayList<nodeset>> qid_Ansgr_in, HashMap<Integer, GraphNode> INposToGN) {
		
		mQuery = query;
		viewsOfQuery = viewsOfQuery_in;
		qid_Ansgr = qid_Ansgr_in;
		posToGN = INposToGN;
	}

	public ArrayList<Pool> run(QueryEvalStat INstat) {
		stat = INstat;
		tt = new TimeTracker();
		tt.Start();
		
		viewHoms = new HashMap<Integer, HashMap<Integer, Integer>>();
		//store hom for every view used in this query
		//key is view ID, value is hom HashMap<Integer, Integer>
		//hom: key is query node #, value is view node # 
		for (int i = 0; i < mQuery.V; i++) { // i is query node ID. for each node in query
			for (int v = 0; v < viewsOfQuery.size(); v++) {
				Query view = viewsOfQuery.get(v);
				HashMap<Integer, Integer> hom = getHom(view, mQuery);  //key is view node ID, value is query node ID
				viewHoms.put(view.Qid, hom);
			}
		}

		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V]; //each nodeset has its own bitmap of GN in it
		initPool(tBitsIdxArr); 
//		double buildtm = tt.Stop() / 1000;
//		stat.setMatchTime(buildtm);
		
		initEdges(); 
//		double buildtm = tt.Stop() / 1000;
//		stat.setMatchTime(buildtm);
		
		for(QEdge edge: mQuery.edges){  
			linkOneStep(edge,tBitsIdxArr);
		}
		
		double buildtm = tt.Stop() / 1000;
		stat.setMatchTime(buildtm);
		stat.calAnsGraphSize(mPool);
		stat.setTotNodesAfter(calTotCandSolnNodes());
//		System.out.println("Answer graph build time:" + buildtm + " sec.");
		return mPool;
	}
	
	private double calTotCandSolnNodes() {
		double totNodes = 0.0;
		for (Pool pool : mPool) {
			ArrayList<PoolEntry> elist = pool.elist();
			totNodes += elist.size();
		}
		return totNodes;
	}
	
	private void initPool(RoaringBitmap[] tBitsIdxArr) {
		
		//first get all graph nodes of nodesets of covering views
		//these are less of these than the candidate nodes that come from mCandLists.get(i)

		mPool = new ArrayList<Pool>(mQuery.V);
		QNode[] qnodes = mQuery.nodes;
		intersectedAnsGr = new ArrayList<nodeset>();
		for (int i = 0; i < mQuery.V; i++) { // i is nodeset ID of query
			Pool qAct = new Pool();
			mPool.add(qAct);
			
			//get intersection of all covering nodesets of this nodeset
			nodeset intersectedNS = new nodeset();
			for (Integer key : viewHoms.keySet()) {
				HashMap<Integer, Integer> hom = viewHoms.get(key);
				Integer viewNodesetID = hom.get(i);
				if (viewNodesetID == null) {  //this view doesn't cover this query's node
					continue;
				}
				ArrayList<nodeset> viewAnsgr = qid_Ansgr.get(key); //node sets of view
//				ArrayList<GraphNode> coveringNS = viewAnsgr.get(viewNodesetID).gnodes;
				RoaringBitmap coveringNSbits = viewAnsgr.get(viewNodesetID).gnodesBits;
				if (intersectedNS.gnodesBits.isEmpty()) {
					intersectedNS.gnodesBits = getGNList(coveringNSbits);
				} else {
					intersectedNS.gnodesBits.and(coveringNSbits);
				}
			}
			intersectedNS.createFwdAL();
			intersectedAnsGr.add(intersectedNS);
			
			QNode qn = qnodes[i];
			RoaringBitmap t_bits = new RoaringBitmap();
			tBitsIdxArr[i] = t_bits;
			int pos = 0; 
			for (int n : intersectedNS.gnodesBits) { 
				GraphNode gn = posToGN.get(n);
				PoolEntry actEntry = new PoolEntry(pos++, qn, gn);
				qAct.addEntry(actEntry);
				t_bits.add(actEntry.getValue().L_interval.mStart);
				
				//map intersectedAnsGr to mPool: create hashmap of graphnode to poolentry
				intersectedAnsGr.get(i).GNtoPE.put(n, actEntry);
			}
		}

	}
	
	//for every query edge, find its view covering edge using vhead = hom.get(head) and vTail = hom.get(tail)
	//the fwdadjlist of the head should only keep those that exist in other views
	//but graph node may not point to some node set of curr view, so add it on if not in node set
	
	//check each subsection of this fn to find which is slowest (before using bitmaps)
	
	//instead of going thru each head graph node and taking intersection of its edges for each nodeset it points to,
	//perhaps represent edges b/w nodesets in 1 list and just take intersection of these edges
	//THEN figure out which head poolentry to put these edges into
	//right now, there's no list of all edges b/w 2 nodesets, so perhaps make one for materialized views
	//so instead of going thru every poolentry's adj list, just have 1 adj list of edges
	
	private void initEdges() {
		for (QEdge qEdge : mQuery.edges ) {
			for (Query view : viewsOfQuery) {
				HashMap<Integer, Integer> hom = viewHoms.get(view.Qid);
				int from = qEdge.from, to = qEdge.to;
				Integer vHead = hom.get(from), vTail = hom.get(to);

				if (vHead == null || vTail == null){
					continue; //view has no covering edge for this qedge
				}
				
				ArrayList<nodeset> viewAnsgr = qid_Ansgr.get(view.Qid); //node sets of view
				Integer viewHeadNodesetID = hom.get(from);
				nodeset viewHeadNS = viewAnsgr.get(viewHeadNodesetID);
				
				//intersectedAnsGr's nodesets contain candCoveringEdges, or cos
				//intersectedAnsGr.get(vHead) is nodeset of head graph nodes
				//HashMap<GraphNode, HashMap<Integer, RoaringBitmap>> fwdAdjLists : key is head graph node
				//HashMap<Integer, RoaringBitmap> : key is to nodeset, value is toNS's graph nodes
				nodeset queryHeadNS = intersectedAnsGr.get(from);
				for (int gn : queryHeadNS.gnodesBits) {
					if (!viewHeadNS.fwdAdjLists.containsKey(gn)) { //not in intersection of nodeset, so skip
						continue;
					}
					RoaringBitmap viewToGNs = viewHeadNS.fwdAdjLists.get(gn).get(vTail); //U: edges b/w headGN to tail NS
					HashMap<Integer, RoaringBitmap> queryEdgesHM = queryHeadNS.fwdAdjLists.get(gn);
					if (!queryEdgesHM.containsKey(to)) {
						//first, intersect every headGN's adj list by tail NS to ensure only points to nodes inside query ansgr
						//issue: cannot just do edgesHM.put(to, intersectedAnsGr.get(to).gnodes);
						//	b/c that means the to adj list IS THE SAME as tail nodeset so intersection would alter it too

						RoaringBitmap toGNs = intersectedAnsGr.get(to).gnodesBits;
						RoaringBitmap queryToGNs = getGNList(toGNs);
						queryToGNs.and(viewToGNs);
						queryEdgesHM.put(to, queryToGNs);
					} else {
						RoaringBitmap queryToGNs = queryEdgesHM.get(to);
						queryToGNs.and(viewToGNs);
					}
				}
			}
		}
	}
	
	private RoaringBitmap getGNList(RoaringBitmap INGN) {
		RoaringBitmap newGN = INGN.clone();
		return newGN;
	}
	
	private void linkOneStep(QEdge edge, RoaringBitmap[] tBitsIdxArr) {

		int from = edge.from, to = edge.to;
		Pool pl_f = mPool.get(from);
		
		//given covering view edges, get all graph nodes in the covering edge's nodesets
		//view adj lists: key is head node, value is list of tail nodes. if tail in list, add it
		//view ONLY has graph nodes, so must still create poolentries

		//instead of looping thru ALL tail nodes, only loop thru adj list of that poolentry
		for (PoolEntry e_f : pl_f.elist()) {
			GraphNode headGN = e_f.getValue();
			nodeset headNS = intersectedAnsGr.get(from);
			RoaringBitmap ToAdjList = headNS.fwdAdjLists.get(headGN.pos).get(to);
			
			//ASSUME LOOPING THRU BITMAP GETS POSITIONS OF BITMAP, WHICH IS GN POS
//			for (GraphNode tailGN : ToAdjList ) {
			for (int tailGN : ToAdjList ) {
				PoolEntry e_t = intersectedAnsGr.get(to).GNtoPE.get(tailGN);
				e_f.addChild(e_t);
				e_t.addParent(e_f);
			}
		}
	}

	private HashMap<Integer, Integer> getHom(Query view, Query query) { 		//this is exhaustive, iterative
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

		// 2. Convert query into graph and get Closure
		TransitiveReduction tr = new TransitiveReduction(query);
		AxisType[][] Qclosure = tr.pathMatrix;  // by comparing closure to compare to orig.edges, see that closure's new edges are desc edges, and doesn't change child edges
		
		//3. Given a node mapping h: for each view child edge, check if (h(x), h(y)) is a child edge
		// Try an initial mapping using the first query node of every view's cand list. 
		int[] candHom = new int[nodeMatch.size()]; 
		HashMap<Integer, Integer> output = new HashMap<Integer, Integer>(); //key is query nodeset, value is view nodeset
		for (int i = 0; i < nodeMatch.size(); i++) {
			candHom[i] = nodeMatch.get(i).get(0);
			output.put(nodeMatch.get(i).get(0), i);
		}
		
		//one map should be view to query, another is query to view. output latter.
		
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
				
				//try various permutations of matches
				if (!vEdgeType.equals(qEdgeType)) {
					//mapping failed,  so try another
					++colChangeToNext; //try new query node for curr view row
					
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
									output.replace(nodeMatch.get(i).get(0), i);
								} //end for: reseting col indices
								
								candHom[rowChangeNext]= nodeMatch.get(rowChangeNext).get(colChangeToNext);
								output.replace(nodeMatch.get(rowChangeNext).get(colChangeToNext),rowChangeNext );
								rowChangeNext = nodeMatch.size() - 1; //reset col pointer and row pointer
								colChangeToNext = 0; 
							} // end if: check col has another match to the right
						} //end while: checking if col reached end
						
					} else {
						candHom[rowChangeNext]= nodeMatch.get(rowChangeNext).get(colChangeToNext);
						output.replace(nodeMatch.get(rowChangeNext).get(colChangeToNext),rowChangeNext );
					}
					break; //break out 'end of check each edge' and try new candHom mapping
				} //end if (!vEdgeType.equals(qEdgeType)): check edge consistency
			} // end for (QEdge edge : view.edges): check each edge consistency
			// Found mapping with all edges consistent -> use view for query, add to list of query's views
			return output; //all edges passed, so use this mapping
		} // end of while loop
	} //end of getHom()
	
	public static void main(String[] args) {

	}

}
