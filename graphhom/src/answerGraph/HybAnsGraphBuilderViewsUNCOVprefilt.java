package answerGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import prefilter.FilterBuilder;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import query.graph.QueryParser;
import query.graph.TransitiveReduction;
import queryPlan.PlanGenerator;
import simfilter.DagSimGraFilter;
import views.nodeset;
import answerGraph.uncoveredSGBuild;

//only add edge to nodes if covering edge has it

public class HybAnsGraphBuilderViewsUNCOVprefilt {

	Query mQuery;
	ArrayList<Pool> mPool;
	ArrayList<Query> viewsOfQuery;
	Map<Integer, ArrayList<nodeset>> qid_Ansgr;
	HashMap<Integer, ArrayList<HashMap<Integer, Integer>>> viewHoms;
	ArrayList<nodeset> intersectedAnsGr;
	TimeTracker tt;
	ArrayList<QEdge> uncoveredEdges;
	BFLIndex mBFL;
	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	GraphNode[] Gnodes;
	HashMap<Integer, GraphNode> LintToGN;
	Query uncov;
	double prunetm;
	QueryEvalStat stat;
	
	public HybAnsGraphBuilderViewsUNCOVprefilt(Query query, ArrayList<Query> viewsOfQuery_in,
			Map<Integer, ArrayList<nodeset>> qid_Ansgr_in, HashMap<Integer, GraphNode> INLintToGN,
			BFLIndex bfl, GraphNode[] INnodes, double INprunetm, 
			ArrayList<ArrayList<GraphNode>> InvLstsByID, QueryEvalStat INstat) {
		
		mQuery = query;
		viewsOfQuery = viewsOfQuery_in;
		qid_Ansgr = qid_Ansgr_in;
		uncoveredEdges = new ArrayList<QEdge>();
		for (QEdge edge : query.edges) {
			uncoveredEdges.add(edge);
		}
		mBFL = bfl;
		mInvLstsByID = InvLstsByID;
		Gnodes = INnodes;
		LintToGN = INLintToGN;
		prunetm = INprunetm;
		stat = INstat;
	}

	public ArrayList<Pool> run() {
		
		tt = new TimeTracker();
		tt.Start();
		
		//each view can have more than 1 hom to the query
		viewHoms = new HashMap<Integer, ArrayList<HashMap<Integer, Integer>>>();
		//store hom for every view used in this query
		//key is view ID, value is hom HashMap<Integer, Integer>
		//hom: key is query node #, value is view node # 
		for (int i = 0; i < mQuery.V; i++) { // i is query node ID. for each node in query
			for (int v = 0; v < viewsOfQuery.size(); v++) {
				Query view = viewsOfQuery.get(v);
				ArrayList<HashMap<Integer, Integer>> homsList = new ArrayList<HashMap<Integer, Integer>>();
				viewHoms.put(view.Qid, homsList);
				while (true) {
					HashMap<Integer, Integer> hom = getHom(view, mQuery);  //key is query nodeset, value is view nodeset
					if (!hom.isEmpty()){  //only empty if there doesn't exist any more homs
						homsList.add(hom);
						
						for (QEdge edge : mQuery.getEdges() ) {
							for (QEdge Vedge : view.getEdges() ) {
								Integer covVhead = hom.get(edge.from);
								Integer covVtail = hom.get(edge.to);
								if (covVhead == null || covVtail == null) {
									continue;
								}
								if (Vedge.from == covVhead && Vedge.to == covVtail && uncoveredEdges.contains(edge)) {
									uncoveredEdges.remove(edge);
								}
							}
						}
						
					} else {  //exit finding homs from v to q b/c no more
						break;
					}
				}
				viewHoms.put(view.Qid, homsList);
			}
		}
		
		initNodes(); 
		
		double buildtm = tt.Stop() / 1000;
//		System.out.println("initNodes time:" + buildtm + " sec.");
		
		/// FILTER ///
//		tt.Start();
//		
		HashMap<Integer,Integer> oldNewVertices = new HashMap<Integer,Integer>();
		HashMap<Integer,Integer> newOldVertices = new HashMap<Integer,Integer>();
		Integer newVertNum = 0;
		
		ArrayList<Integer> nodesToCompute = new ArrayList<Integer>();  
		for (QEdge edge : uncoveredEdges) {
			if (!nodesToCompute.contains(edge.from)) {
				nodesToCompute.add(edge.from);
				oldNewVertices.put(edge.from, newVertNum);
				newOldVertices.put(newVertNum, edge.from);
				newVertNum += 1;
			}
			if (!nodesToCompute.contains(edge.to)) {
				nodesToCompute.add(edge.to);
				oldNewVertices.put(edge.to, newVertNum);
				newOldVertices.put(newVertNum, edge.to);
				newVertNum += 1;
			}
		}
		
		//use cand occ list from nodeset
		ArrayList<ArrayList<GraphNode>> cand_occ_lsts = new ArrayList<ArrayList<GraphNode>>();
		for (int i=0; i < nodesToCompute.size(); i++) {
			Integer nsNum = newOldVertices.get(i);
			nodeset ns = intersectedAnsGr.get(nsNum);
			ArrayList<GraphNode> cand_occ_l = new ArrayList<GraphNode>();
			for (int lint : ns.gnodesBits) {
				GraphNode graphN = LintToGN.get(lint);
				cand_occ_l.add(graphN);
			}
			if (cand_occ_l.isEmpty()) {
				cand_occ_l = mInvLstsByID.get(mQuery.getNodes()[nsNum].lb);
//				cand_occ_l = mInvLstsByID.get(nsNum);  //if using FLT
			}
			
//			if (ns.gnodesBits.getCardinality() < mInvLstsByID.get(nsNum).size()) {
//			if (cand_occ_l.size() < mInvLstsByID.get(nsNum).size()) {
//				cand_occ_l = mInvLstsByID.get(nsNum);
//			}
			
			cand_occ_lsts.add(cand_occ_l);
		}
		
		int V = nodesToCompute.size();
		
		QNode[] mQNodes = mQuery.getNodes();
		
		QNode[] uncvNodes = new QNode[V];
		for (int i = 0; i < V; i++) {
			Integer newVert = newOldVertices.get(i);
			QNode oldNode = mQNodes[newVert];
			QNode newNode = new QNode();
			newNode.id = i;
			newNode.lb = oldNode.lb;
//			newNode.N_O_SZ = oldNode.N_O_SZ;
//			newNode.N_I_SZ = oldNode.N_I_SZ;
//			newNode.N_O = oldNode.N_O;
//			newNode.N_I = oldNode.N_I;
//			newNode.N_I = oldNode.N_I;
//			newNode.E_I = oldNode.E_I;
//			newNode.E_O = oldNode.E_O;
			uncvNodes[i] = newNode;
		}
		
		ArrayList<RoaringBitmap> bitsByIDArr = new ArrayList<RoaringBitmap>(V);
		for (QNode q : uncvNodes) {
			RoaringBitmap bits = new RoaringBitmap();
			ArrayList<GraphNode> invLst = cand_occ_lsts.get(q.id);
			for (GraphNode n : invLst) {
				bits.add(n.id);
			}
			bitsByIDArr.add(bits);
		}
		
		int E = uncoveredEdges.size();		
		
		QEdge[] uncvEdges = new QEdge[E];
		int ueIndex = 0;
		for (QEdge oldEdge : uncoveredEdges) {
			Integer newFrom = oldNewVertices.get(oldEdge.from);
			Integer newTo = oldNewVertices.get(oldEdge.to);
			int ax = 1;
			if(oldEdge.axis == AxisType.child)
				ax = 0;
			QEdge newEdge = new QEdge(newFrom, newTo, ax);
			newEdge.eid = oldEdge.eid;
			uncvEdges[ueIndex] = newEdge;
			ueIndex += 1;
		}
		
//		String myDirectoryPath = "D:\\Documents\\_prog\\prog_cust\\eclipse-workspace\\graph_expr\\input_files\\";
//	    try {
//	        File myObj = new File(myDirectoryPath+"uncovered.vw");
//	        if (myObj.createNewFile()) {
//	          System.out.println("File created: " + myObj.getName());
//	        } else {
//	          System.out.println("File already exists.");
//	        }
//	        
//	        FileWriter myWriter = new FileWriter(myDirectoryPath+"uncovered.vw");
//	        myWriter.write("q # 0" + "\r\n");
//	        int vNum = 0;
//	        for (QNode node : uncvNodes) {
//	        	String s1 = Integer.toString(vNum);
//	        	String s2 = Integer.toString(node.lb);
//	        	myWriter.write("v " + s1 + " " + s2 + "\r\n");
//	        	vNum += 1;
//	        }
//	        
//	        for (QEdge edge : uncvEdges) {
//	        	String s1 = Integer.toString(edge.from);
//	        	String s2 = Integer.toString(edge.to);
//				int ax = 1;
//				if(edge.axis == AxisType.child)
//					ax = 0;
//	        	String s3 = Integer.toString(ax);
//	        	myWriter.write("e " + s1 + " " + s2 + " " + s3 + "\r\n");
//	        	vNum += 1;
//	        }
//	        
//	        myWriter.close();
//	        System.out.println("Successfully wrote to the file.");
//	        
//	      } catch (IOException e) {
//	        System.out.println("An error occurred.");
//	        e.printStackTrace();
//	      }
//	    
//	    readUncovered(myDirectoryPath+"uncovered.vw");
				
		Query uncovQ = new Query(V, E, uncvNodes, uncvEdges);
		
		DagSimGraFilter filter = new DagSimGraFilter(uncovQ, Gnodes, cand_occ_lsts, bitsByIDArr, mBFL, true);
		filter.prune();
		ArrayList<MatArray> mCandLists = null;
		mCandLists = filter.getCandList();

		prunetm += tt.Stop() / 1000;
		stat.setPreTime(prunetm);
		
		ArrayList<ArrayList<GraphNode>> cand_occ_lsts2 = new ArrayList<ArrayList<GraphNode>>();
		for (int i=0; i < mQuery.V; i++) {
			nodeset ns = intersectedAnsGr.get(i);
			ArrayList<GraphNode> cand_occ_l = new ArrayList<GraphNode>();
			for (int lint : ns.gnodesBits) {
				GraphNode graphN = LintToGN.get(lint);
				cand_occ_l.add(graphN);
			}
			if (cand_occ_l.isEmpty()) {
				cand_occ_l = mInvLstsByID.get(mQuery.getNodes()[i].lb);
			}
			cand_occ_lsts2.add(cand_occ_l);
		}
		
		ArrayList<RoaringBitmap> bitsByIDArr2 = new ArrayList<RoaringBitmap>(V);
		for (QNode q : mQuery.getNodes()) {
			RoaringBitmap bits = new RoaringBitmap();
			ArrayList<GraphNode> invLst = cand_occ_lsts2.get(q.id);
			for (GraphNode n : invLst) {
				bits.add(n.id);
			}
			bitsByIDArr2.add(bits);
		}
		
		DagSimGraFilter filter2 = new DagSimGraFilter(mQuery, Gnodes, cand_occ_lsts2, bitsByIDArr2, mBFL, true);
		filter2.prune();
		ArrayList<MatArray> mCandLists2 = null;
		mCandLists2 = filter2.getCandList();

		/// END FILTER ///
		
		tt.Start();
		
		//send uncoveredEdges to uncoveredSGBuild to get "view" for them
		//THIS IS FAST
		uncoveredSGBuild partialSG = new uncoveredSGBuild(mQuery, mBFL, mCandLists, uncoveredEdges, LintToGN, 
				intersectedAnsGr, oldNewVertices);
		qid_Ansgr.put(-1, partialSG.run() );  //add uncovered SG 
		// RUNNING UNCOVERED IS THE MOST COSTLY STEP
		
		// since mQuery is the input in, the mapping is the same but is null for nodes not in uncovered edges
		ArrayList<HashMap<Integer, Integer>> homsList = new ArrayList<HashMap<Integer, Integer>>();
		HashMap<Integer, Integer> hom = new HashMap<Integer, Integer>();
		for (int i = 0; i < mQuery.V; i++) { 
			if (partialSG.nodesToCompute.contains(i)) {
				hom.put(i, i);
			} 
		}
		homsList.add(hom);
		viewHoms.put(-1, homsList);
		
		intersectUncovered(); 
		
//		double midtm = tt.Stop() / 1000;
//		System.out.println("intUnc time:" + midtm + " sec.");
		
		for (int i = 0; i < mQuery.V; i++) { //init fwdadjlist data structure
			nodeset intersectedNS = intersectedAnsGr.get(i);
			intersectedNS.createFwdAL();
		}
		
		initEdges(); 
		
//		midtm = tt.Stop() / 1000;
//		System.out.println("initEdges time:" + midtm + " sec.");

		mPool = new ArrayList<Pool>(mQuery.V);
		QNode[] qnodes = mQuery.nodes;
		for (int i = 0; i < mQuery.V; i++) {
			QNode qn = qnodes[i];
			int pos = 0; 
			Pool qAct = new Pool();
			mPool.add(qAct);
			nodeset intersectedNS = intersectedAnsGr.get(i);
			for (int n : intersectedNS.gnodesBits) { 
//				GraphNode gn = posToGN.get(n);
				GraphNode gn = LintToGN.get(n);
				PoolEntry actEntry = new PoolEntry(pos++, qn, gn);
				qAct.addEntry(actEntry);
				
				//map intersectedAnsGr to mPool: create hashmap of graphnode to poolentry
				intersectedAnsGr.get(i).GNtoPE.put(n, actEntry);
			}
		}
		
		for(QEdge edge: mQuery.edges){  
			linkOneStep(edge);
		}
		
		buildtm += tt.Stop() / 1000;
		stat.setMatchTime(buildtm);
		

		return mPool;
	}
	
//	private void readUncovered(String uncovFileN) {
//		QueryParser queryParser = new QueryParser(uncovFileN, l2iMap);
//		Query view = null;
//		uncov = queryParser.readNextQuery();
//		TransitiveReduction tr = new TransitiveReduction(uncov);
//		tr.reduce();
//	}

	public ArrayList<QEdge> getUncoveredEdges(){
		return uncoveredEdges;
	}
	
	private void initNodes() {
		
		//first get all graph nodes of nodesets of covering views
		//these are less of these than the candidate nodes that come from mCandLists.get(i)

		intersectedAnsGr = new ArrayList<nodeset>();
		for (int i = 0; i < mQuery.V; i++) { // i is nodeset ID of query
			
			//get intersection of all covering nodesets of this nodeset
			nodeset intersectedNS = new nodeset();
			for (Integer key : viewHoms.keySet()) {
				for (HashMap<Integer, Integer> hom : viewHoms.get(key)) {
					Integer viewNodesetID = hom.get(i);
					if (viewNodesetID == null) {  //this view doesn't cover this query's node
						intersectedNS.hasNodes = false;
						continue;
					}
					ArrayList<nodeset> viewAnsgr = qid_Ansgr.get(key); //node sets of view
					RoaringBitmap coveringNSbits = viewAnsgr.get(viewNodesetID).gnodesBits;
					if (intersectedNS.gnodesBits.isEmpty()) {
						intersectedNS.gnodesBits = getGNList(coveringNSbits);
					} else {
						intersectedNS.gnodesBits.and(coveringNSbits);
					}
				}
			}
//			intersectedNS.createFwdAL();
			intersectedAnsGr.add(intersectedNS);
		}

	}
	
	private void intersectUncovered() {
		
		for (int i = 0; i < mQuery.V; i++) { 
			nodeset intersectedNS = intersectedAnsGr.get(i);
			HashMap<Integer, Integer> hom = viewHoms.get(-1).get(0); //just intersect with the newly created uncovered SG
			
			Integer viewNodesetID = hom.get(i);  // hom(query's nodeset ID) = view's nodeset ID
			if (viewNodesetID == null) {  //this view doesn't cover this query's node
				continue;
			}
			ArrayList<nodeset> viewAnsgr = qid_Ansgr.get(-1); //node sets of view
			RoaringBitmap coveringNSbits = viewAnsgr.get(viewNodesetID).gnodesBits;
			if (coveringNSbits.isEmpty()) { //this view's nodeset is empty despite covering it
				continue;
			}
			if (intersectedNS.gnodesBits.isEmpty()) {
				intersectedNS.gnodesBits = getGNList(coveringNSbits);
			} else {
				intersectedNS.gnodesBits.and(coveringNSbits);
			}
			
		}

	}
	
	private void initEdges() {
		for (QEdge qEdge : mQuery.edges ) {
			for (Integer viewID : viewHoms.keySet()) {
				
				if (!uncoveredEdges.contains(qEdge) && viewID == -1) {
					continue;
				}
				
				for (HashMap<Integer, Integer> hom : viewHoms.get(viewID)) {
					int from = qEdge.from, to = qEdge.to;
					Integer vHead = hom.get(from), vTail = hom.get(to);

					if (vHead == null || vTail == null){
						continue; //view lacks the covering nodes for this qedge
					}
					
					ArrayList<nodeset> viewAnsgr = qid_Ansgr.get(viewID); //node sets of view
					nodeset viewHeadNS = viewAnsgr.get(vHead);
					
					//intersectedAnsGr's nodesets contain candCoveringEdges, or cos
					//intersectedAnsGr.get(vHead) is nodeset of head graph nodes
					//HashMap<GraphNode, HashMap<Integer, RoaringBitmap>> fwdAdjLists : key is head graph node
					//HashMap<Integer, RoaringBitmap> : key is to nodeset, value is toNS's graph nodes
					nodeset queryHeadNS = intersectedAnsGr.get(from);
					for (int gn : queryHeadNS.gnodesBits) {
						
						//check if view contains an edge that covers this qEdge. if not, skip it.
						if (viewHeadNS.fwdAdjLists == null) {
							continue;
						}
						if (!viewHeadNS.fwdAdjLists.containsKey(gn)) { //not in intersection of nodeset, so skip
							continue;
						}
						RoaringBitmap viewToGNs = viewHeadNS.fwdAdjLists.get(gn).get(vTail); //U: edges b/w headGN to tail NS
						if (viewToGNs == null) {
							continue;
						}
						
						HashMap<Integer, RoaringBitmap> queryEdgesHM = queryHeadNS.fwdAdjLists.get(gn);
//						if (queryEdgesHM == null) {
//							continue;
//						}
						
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
		
		//at end, for each edge, for each head node's AL, intersect with new tail nodeset 
		//perform several passes
		boolean stopFlag = true;
		while (stopFlag) {
			stopFlag = false;
			for (QEdge qEdge : mQuery.edges ) {
				int from = qEdge.from, to = qEdge.to;
				nodeset queryHeadNS = intersectedAnsGr.get(from);
				nodeset newNS = new nodeset();
				for (int gn : queryHeadNS.gnodesBits) {
					HashMap<Integer, RoaringBitmap> queryEdgesHM = queryHeadNS.fwdAdjLists.get(gn);
					if (queryEdgesHM == null) {
						continue;
					}
					
					if (queryEdgesHM.containsKey(to)) {
						RoaringBitmap queryToGNs = queryEdgesHM.get(to);
						RoaringBitmap toGNs = intersectedAnsGr.get(to).gnodesBits;
						queryToGNs.and(toGNs);
						if (!queryToGNs.isEmpty()) {
							newNS.gnodesBits.add(gn);
							newNS.fwdAdjLists.put(gn, queryEdgesHM);
						} else {
							stopFlag = true;
						}
					}
				}
				intersectedAnsGr.set(from, newNS);
			}
		}
		
	}
	
	private RoaringBitmap getGNList(RoaringBitmap INGN) {
		RoaringBitmap newGN = INGN.clone();
		return newGN;
	}
	
	private void linkOneStep(QEdge edge) {

		int from = edge.from, to = edge.to;
		Pool pl_f = mPool.get(from);
		
		//given covering view edges, get all graph nodes in the covering edge's nodesets
		//view adj lists: key is head node, value is list of tail nodes. if tail in list, add it
		//view ONLY has graph nodes, so must still create poolentries

		//instead of looping thru ALL tail nodes, only loop thru adj list of that poolentry
		for (PoolEntry e_f : pl_f.elist()) {
			GraphNode headGN = e_f.getValue();
			nodeset headNS = intersectedAnsGr.get(from);
//			RoaringBitmap ToAdjList = headNS.fwdAdjLists.get(headGN.pos).get(to);
			RoaringBitmap ToAdjList = headNS.fwdAdjLists.get(headGN.L_interval.mStart).get(to);
			
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
			
			// Found mapping with all edges consistent -> use view for query, add to list of query's views
			//check if output is a hom that already exists
			//bc we dont want to delete keys from output (not all query nodes will have a match), re-create output
			//  for each new candHom instead of updating it alongside candHom
			HashMap<Integer, Integer> output = new HashMap<Integer, Integer>(); //key is query nodeset, value is view nodeset
			for (int i = 0; i < candHom.length; i++) {
				output.put(candHom[i], i);
			}
			
			if (passFlag && !viewHoms.get(view.Qid).contains(output)) {
				return output; //get out of while loop to test a new mapping
			}
			
			//else: mapping failed,  so try another
			++colChangeToNext; //try new query node for curr view row
			//make sure there is next match for curr row. if not, go to row above to move it right
			if (colChangeToNext > nodeMatch.get(rowChangeNext).size() - 1) {
				while (colChangeToNext > nodeMatch.get(rowChangeNext).size() - 1){
					//move row pointer up (-1) and its col pointer right
					--rowChangeNext;
					
					if (rowChangeNext < 0) {
						HashMap<Integer, Integer> noMoreHoms = new HashMap<Integer, Integer>();
						return noMoreHoms; //'all mappings tried'
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
	
	public static void main(String[] args) {

	}

}
