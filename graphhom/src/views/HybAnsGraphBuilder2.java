package views;

import java.util.ArrayList;
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


//take in a set of views. for each graph node pair, check if there is a view which contains it. 
//instead of referring to nodes for view ansgr, only use A(V) to decide what graph node pairs to check
//each edge has 2 node sets. instead of looping thru larger node sets given by mPool, use the node sets
//accumulated from the set of views

public class HybAnsGraphBuilder2 {

	Query mQuery;
	ArrayList<Pool> mPool;
	BFLIndex mBFL;
	ArrayList<MatArray> mCandLists;
	ArrayList<ArrayList<PoolEntry>> mQcosNodeSets;
	
	public HybAnsGraphBuilder2(Query query, BFLIndex bfl,
			ArrayList<MatArray> candLsts, ArrayList<ArrayList<PoolEntry>> QcosNodeSets) {

		mQuery = query;
		mBFL = bfl;
		mCandLists = candLsts;
		mQcosNodeSets = QcosNodeSets;
	}

	public ArrayList<Pool> run() {

		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V];
		initPool(tBitsIdxArr);  //find all node sets for each pattern node

		for(QEdge edge: mQuery.edges){
			
			linkOneStep(edge,tBitsIdxArr); //find all graph edges that match to pattern edge
		}
		
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

		mPool = new ArrayList<Pool>(mQuery.V);
		QNode[] qnodes = mQuery.nodes;
		for (int i = 0; i < mQuery.V; i++) {
			Pool qAct = new Pool();
			mPool.add(qAct);
			MatArray mli = mCandLists.get(i);
			ArrayList<GraphNode> elist = mli.elist();
			QNode qn = qnodes[i];
			RoaringBitmap t_bits = new RoaringBitmap();
			tBitsIdxArr[i] = t_bits;
			int pos = 0; 
			for (GraphNode n : elist) {
				PoolEntry actEntry = new PoolEntry(pos++, qn, n);
				qAct.addEntry(actEntry);
				t_bits.add(actEntry.getValue().L_interval.mStart);
			}

		}

	}

	private void linkOneStep(QEdge edge, RoaringBitmap[] tBitsIdxArr) {

		int from = edge.from, to = edge.to;
		AxisType axis = edge.axis;
//		Pool pl_f = mPool.get(from), pl_t = mPool.get(to);  //node sets of To and From nodes
		ArrayList<PoolEntry> pl_f = mQcosNodeSets.get(from), pl_t = mQcosNodeSets.get(to);
		
		//using View, no need to compute

		//for every node pair in the 2 node sets, check if there is reachability b/w them
//		for (PoolEntry e_f : pl_f.elist()) {
		for (PoolEntry e_f : pl_f) {
			if (axis == AxisType.child) //for every To node, check if exists in From node's adj list
//				linkOneStep(e_f, tBitsIdxArr[to], pl_t.elist()); 
				linkOneStep(e_f, tBitsIdxArr[to], pl_t); 
			else { //for every To node, check path reachability 
				
				GraphNode n_f = e_f.getValue();
//				for (PoolEntry e_t : pl_t.elist()) {
				for (PoolEntry e_t : pl_t) {

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
		
		RoaringBitmap rs_and = RoaringBitmap.and(s.adj_bits_o, t_bits);

		if (rs_and.isEmpty())
			return false;

		for (int ti : rs_and) {
			PoolEntry e = list.get(t_bits.rank(ti) - 1);
			r.addChild(e);
			e.addParent(r);
		}

		return true;

	}

	public static void main(String[] args) {

	}

}
