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
import views.nodeset;


public class uncoveredSGBuild {

	Query mQuery;
	ArrayList<Pool> mPool;
	BFLIndex mBFL;
	ArrayList<MatArray> mCandLists;
	ArrayList<QEdge> uncoveredEdges;
	HashMap<Integer, GraphNode> posToGN;
	ArrayList<Integer> nodesToCompute;
	
	public uncoveredSGBuild(Query query, BFLIndex bfl,
			ArrayList<MatArray> candLsts, ArrayList<QEdge> INuncoveredEdges, HashMap<Integer, GraphNode> INposToGN) {

		mQuery = query;
		mBFL = bfl;
		mCandLists = candLsts;
		uncoveredEdges = INuncoveredEdges;
		posToGN = INposToGN;
	}

	public ArrayList<nodeset> run() {

		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V];
		initPool(tBitsIdxArr);

//		for(QEdge edge: mQuery.edges){
		for(QEdge edge: uncoveredEdges){
			linkOneStep(edge,tBitsIdxArr);
		}
		
		ArrayList<nodeset> matView = new ArrayList<nodeset>();
		for (Pool pl : mPool) {
			nodeset ns = new nodeset();
			for (PoolEntry pe : pl.elist()) {
				GraphNode gn = pe.mValue;
				posToGN.put(gn.pos, gn);
				ns.gnodesBits.add(gn.pos);
				if (pe.mFwdEntries != null){
					HashMap<Integer, RoaringBitmap> fal = new HashMap<Integer, RoaringBitmap>();
					for (Integer key : pe.mFwdEntries.keySet()) {
						RoaringBitmap newBitmap = new RoaringBitmap();
						ArrayList<PoolEntry> nodeFwd = pe.mFwdEntries.get(key);
						for (PoolEntry peTo : nodeFwd) {
							newBitmap.add(peTo.mValue.pos);
						}
						fal.put(key, newBitmap);
					}
					ns.fwdAdjLists.put(gn.pos, fal);
				} else {
					ns.fwdAdjLists = (HashMap<Integer, HashMap<Integer, RoaringBitmap>>) null;
				}
			}
			matView.add(ns);
		}
		
		return matView;

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
		
		nodesToCompute = new ArrayList<Integer>();  
		for (QEdge edge : uncoveredEdges) {
			if (!nodesToCompute.contains(edge.from)) {
				nodesToCompute.add(edge.from);
			}
			if (!nodesToCompute.contains(edge.to)) {
				nodesToCompute.add(edge.to);
			}
		}
		
		mPool = new ArrayList<Pool>(mQuery.V);
		QNode[] qnodes = mQuery.nodes;
		for (int i = 0; i < mQuery.V; i++) {			
			Pool qAct = new Pool();
			mPool.add(qAct);
			if (!nodesToCompute.contains(i)) {
				continue;
			}
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
