package answerGraph;

import java.util.ArrayList;
import java.util.HashMap;

import org.roaringbitmap.RoaringBitmap;

import dao.BFLIndex;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import global.Consts.AxisType;
import graph.GraphNode;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import queryPlan.PlanGenerator;


public class ViewsRIsumGraph {

	Query mQuery;
	ArrayList<Pool> mPool;
	BFLIndex mBFL;
	ArrayList<MatArray> mCandLists;
	ArrayList<QEdge> uncoveredEdges;
	HashMap<Integer, GraphNode> posToGN;
	ArrayList<Pool> correct_mPool;
	
	public ViewsRIsumGraph(Query query, BFLIndex bfl, ArrayList<MatArray> candLsts, 
			ArrayList<Pool> INmPool, ArrayList<QEdge> INuncoveredEdges, HashMap<Integer, GraphNode> INposToGN) {

		mQuery = query;
		mBFL = bfl;
		mCandLists = candLsts;
		mPool = INmPool;
		uncoveredEdges = INuncoveredEdges;
		posToGN = INposToGN;
		
		HybAnsGraphBuilder agBuilder = new HybAnsGraphBuilder(mQuery, mBFL, mCandLists);
		correct_mPool = agBuilder.run();
	}

	public ArrayList<Pool> run() {

		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V];
		initPool(tBitsIdxArr);

		for(QEdge edge: uncoveredEdges){
//		for(QEdge edge: mQuery.edges){
			
			linkOneStep(edge,tBitsIdxArr);
		}
		
//		for (int i = 0; i < mQuery.V; i++) {
//			Pool qAct = mPool.get(i);
//			Pool corr_qAct = mPool.get(i);
//			for (PoolEntry pe : qAct.elist()) { //head node
//				if (pe.mFwdEntries == null) {
//					continue;
//				}
//				for (PoolEntry corr_pe : corr_qAct.elist()) { //head node
//					if (pe.mValue.pos == corr_pe.mValue.pos) { //find matching head node
//						for (Integer key : pe.mFwdEntries.keySet()) {
//							for (PoolEntry targetNode : pe.mFwdEntries.get(key)) { //tail nodes of test
//								boolean itExists = false;
//								for (PoolEntry tailPE : corr_pe.mFwdEntries.get(key)) {
//									if (targetNode.mValue.pos == tailPE.mValue.pos) {
//										itExists = true;
//									}
//								}
//								if (itExists == false) {
//									int x = 1; //debug why there's no match
//								}
//							}
//						}
//					}
//				}
//			}
//		}
		
		return mPool;

	}
	
	
	// for tree/dag
	public ArrayList<Pool> runBUP(){
		
		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V];
		initPool(tBitsIdxArr);
		
		int[] order = PlanGenerator.generateTopoQueryPlan(mQuery);
		
		for(int i=mQuery.V-1; i>=0; i--){
			QNode q = mQuery.getNode(order[i]);
		    ArrayList<QEdge> edges = q.E_I;
		    if(edges!=null)
		    for(QEdge edge: edges){
				
				linkOneStep(edge,tBitsIdxArr);
			}
		}
		
		return mPool;
	}

	public ArrayList<Pool> runTDW(){
		
		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V];
		initPool(tBitsIdxArr);
		
		int[] order = PlanGenerator.generateTopoQueryPlan(mQuery);
		
		for(int i=0; i<mQuery.V; i++){
			QNode q = mQuery.getNode(order[i]);
		    ArrayList<QEdge> edges = q.E_I;
		    if(edges!=null)
		    for(QEdge edge: edges){
				
				linkOneStep(edge,tBitsIdxArr);
			}
		}
		
		return mPool;
	}

	
	private void initPool(RoaringBitmap[] tBitsIdxArr) {
		
		//only get pools for nodes with uncovered edges
		//union this pool with existing pool, as some graph nodes with edges may not be there
		
		ArrayList<Integer> nodesToCompute = new ArrayList<Integer>();  
		for (QEdge edge : uncoveredEdges) {
			if (!nodesToCompute.contains(edge.from)) {
				nodesToCompute.add(edge.from);
			}
			if (!nodesToCompute.contains(edge.to)) {
				nodesToCompute.add(edge.to);
			}
		}
		
		QNode[] qnodes = mQuery.nodes;
		
//		mPool = new ArrayList<Pool>(mQuery.V);
		//mPool already contains a Pool for every query node, but a Pool may be empty
		for (int i = 0; i < mQuery.V; i++) {
//		for (Integer i : nodesToCompute) {
			Pool qAct = mPool.get(i);
//			Pool qAct = new Pool();
//			mPool.add(qAct);
			MatArray mli = mCandLists.get(i);
			ArrayList<GraphNode> elist = mli.elist();
			QNode qn = qnodes[i];
//			RoaringBitmap t_bits = new RoaringBitmap();
//			tBitsIdxArr[i] = t_bits;
			int pos = qAct.size();
//			int pos = 0;
//			System.out.println(qAct.elist().size());
//			System.out.println(elist.size());
			for (GraphNode n : elist) {
				boolean addNode = true;
				for (PoolEntry pe : qAct.elist()) {
					Integer gnPos = pe.mValue.pos;
					if (gnPos == n.pos) {
						addNode = false;
					}
				}
				if (addNode) {
					PoolEntry actEntry = new PoolEntry(pos++, qn, n);
					qAct.addEntry(actEntry);
//					t_bits.add(actEntry.getValue().L_interval.mStart);
				}
//				if (!posToGN.containsKey(n.pos)) { // only add new node if it doesn't already exist 
//					PoolEntry actEntry = new PoolEntry(pos++, qn, n);
//					qAct.addEntry(actEntry);
////					t_bits.add(actEntry.getValue().L_interval.mStart);
//				} else {
//					GraphNode x= posToGN.get(n.pos);
//					int w = 1;
//				}
			}

		}
		
		for (int i=0; i<mQuery.V; i++) {
			Pool qAct = mPool.get(i);
			RoaringBitmap t_bits = new RoaringBitmap();
			tBitsIdxArr[i] = t_bits;
//			for (PoolEntry actEntry : qAct.elist()) {
//				t_bits.add(actEntry.getValue().L_interval.mStart);
//			}
			for (int pos=0; pos < qAct.size(); pos++) {
				PoolEntry actEntry = qAct.elist().get(pos);
//				System.out.println(actEntry.getValue().L_interval);
				t_bits.add(actEntry.getValue().L_interval.mStart);
			}
		}
		
//		mPool = new ArrayList<Pool>(mQuery.V);
//		QNode[] qnodes = mQuery.nodes;
//		for (int i = 0; i < mQuery.V; i++) {
//			Pool qAct = new Pool();
//			mPool.add(qAct);
//			MatArray mli = mCandLists.get(i);
//			ArrayList<GraphNode> elist = mli.elist();
//			QNode qn = qnodes[i];
//			RoaringBitmap t_bits = new RoaringBitmap();
//			tBitsIdxArr[i] = t_bits;
//			int pos = 0; 
//			for (GraphNode n : elist) {
//				PoolEntry actEntry = new PoolEntry(pos++, qn, n);
//				qAct.addEntry(actEntry);
//				t_bits.add(actEntry.getValue().L_interval.mStart);
//			}
//
//		}

	}

	private void linkOneStep(QEdge edge, RoaringBitmap[] tBitsIdxArr) {

		//for every e_f, poolentry in nodeset that's head to query edge
		//tBitsIdxArr[to] is bitmap of nodeset that's tail to query edge 
		//pl_t.elist() is poolentries of nodeset that's tail to query edge 
		
		int from = edge.from, to = edge.to;
		AxisType axis = edge.axis;
		Pool pl_f = mPool.get(from), pl_t = mPool.get(to);

		
		for (PoolEntry e_f : pl_f.elist()) {
			if (axis == AxisType.child) 
				linkOneStep(e_f, tBitsIdxArr[to], pl_t.elist());
			else {
				
				GraphNode n_f = e_f.getValue();
				for (PoolEntry e_t : pl_t.elist()) {

					GraphNode n_t = e_t.getValue();
					if (n_f.id == n_t.id)
						continue;
					if (n_f.L_interval.mEnd < n_t.L_interval.mStart)
						break;
					if (mBFL.reach(n_f, n_t) == 1) {

						e_f.addChild(e_t);
						e_t.addParent(e_f);

					}

				}
			}

		}

		
	}
	
	
	private void linkOneStep(int from, int to, AxisType axis, RoaringBitmap[] tBitsIdxArr) {
		
		Pool pl_f = mPool.get(from), pl_t = mPool.get(to);

		
		for (PoolEntry e_f : pl_f.elist()) {
			if (axis == AxisType.child) 
				linkOneStep(e_f, tBitsIdxArr[to], pl_t.elist());
			else {
				
				GraphNode n_f = e_f.getValue();
				for (PoolEntry e_t : pl_t.elist()) {

					GraphNode n_t = e_t.getValue();
					if (n_f.id == n_t.id)
						continue;
					if (n_f.L_interval.mEnd < n_t.L_interval.mStart)
						break;
					if (mBFL.reach(n_f, n_t) == 1) {

						e_f.addChild(e_t);
						e_t.addParent(e_f);

					}

				}
			}

		}

		
	}

	private boolean linkOneStep(PoolEntry r, RoaringBitmap t_bits, ArrayList<PoolEntry> list) {

		GraphNode s = r.getValue();
		
		//s.adj_bits_o is bitmap of head graph node's 'to' list
		//t_bits is tBitsIdxArr[to] is bitmap of SG nodeset that's tail to query edge 
		RoaringBitmap rs_and = RoaringBitmap.and(s.adj_bits_o, t_bits);

		if (rs_and.isEmpty())
			return false;

		for (int ti : rs_and) {
			PoolEntry e = list.get(t_bits.rank(ti) - 1);
			r.addChild(e);
			e.addParent(r);
			
//			boolean addChild = true;
//			ArrayList<PoolEntry> subs = r.mFwdEntries.get(e.getQID());
//			for (PoolEntry pe : subs) {
//				if (pe.mValue.pos == e.mValue.pos) {
//					addChild = false;
//				}
//			}
//			if (addChild) {
//				r.addChild(e);
//				e.addParent(r);
//				
//				boolean itExists = false;
//				Pool corr_qAct = mPool.get(r.getQID());
//				for (PoolEntry corr_pe : corr_qAct.elist()) { //head node
//					if (r.mValue.pos == corr_pe.mValue.pos) { //find matching head node
//						for (PoolEntry targetNode : corr_pe.mFwdEntries.get(e.getQID())) { //tail nodes of test
//							if (targetNode.mValue.pos == e.mValue.pos) {
//								itExists = true;
//							}
//						}
//						if (itExists == false) {
//							int x = 1; //debug why there's no match
//						}
//					}
//				}
//			}
			
		}

		return true;

	}

	public static void main(String[] args) {

	}

}
