package answerGraph;

import java.util.ArrayList;
import java.util.HashMap;

import org.roaringbitmap.RoaringBitmap;
import org.roaringbitmap.RoaringBatchIterator;

import dao.BFLIndex;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import global.Consts.AxisType;
import graph.GraphNode;
import helper.TimeTracker;
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
	HashMap<Integer, GraphNode> LintToGN;
	ArrayList<Integer> nodesToCompute;
	ArrayList<nodeset> intersectedAnsGr;
	
	public uncoveredSGBuild(Query query, BFLIndex bfl, ArrayList<MatArray> candLsts, 
			ArrayList<QEdge> INuncoveredEdges, HashMap<Integer, GraphNode> INLintToGN, 
			ArrayList<nodeset> INintersectedAnsGr) {

		mQuery = query;
		mBFL = bfl;
		mCandLists = candLsts;
		uncoveredEdges = INuncoveredEdges;
		intersectedAnsGr = INintersectedAnsGr;
		LintToGN = INLintToGN;
	}

	public ArrayList<nodeset> run() {
		
//		TimeTracker tt;
//		tt = new TimeTracker();
//		tt.Start();

		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V];
		initPool(tBitsIdxArr);

		for(QEdge edge: uncoveredEdges){
//		for (QEdge edge : mQuery.edges ) {
			linkOneStep(edge,tBitsIdxArr);
		}
		

		
		ArrayList<nodeset> matView = new ArrayList<nodeset>();
		for (Pool pl : mPool) {
			nodeset ns = new nodeset();
			for (PoolEntry pe : pl.elist()) {
				GraphNode gn = pe.mValue;
//				posToGN.put(gn.pos, gn);
				LintToGN.put(gn.L_interval.mStart, gn);
//				ns.gnodesBits.add(gn.pos);
				ns.gnodesBits.add(gn.L_interval.mStart);
				if (pe.mFwdEntries != null){
					HashMap<Integer, RoaringBitmap> fal = new HashMap<Integer, RoaringBitmap>();
					for (Integer key : pe.mFwdEntries.keySet()) {
						RoaringBitmap newBitmap = new RoaringBitmap();
						ArrayList<PoolEntry> nodeFwd = pe.mFwdEntries.get(key);
						for (PoolEntry peTo : nodeFwd) {
//							newBitmap.add(peTo.mValue.pos);
							newBitmap.add(peTo.mValue.L_interval.mStart);
						}
						fal.put(key, newBitmap);
					}
//					ns.fwdAdjLists.put(gn.pos, fal);
					ns.fwdAdjLists.put(gn.L_interval.mStart, fal);
				} else {
					ns.fwdAdjLists = (HashMap<Integer, HashMap<Integer, RoaringBitmap>>) null;
				}
			}
			matView.add(ns);
		}
//		double midTM = tt.Stop() / 1000;
//		System.out.printf("%.5f", midTM);
//		System.out.println(" mid time");
		
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
			
			QNode qn = qnodes[i];
			RoaringBitmap t_bits = new RoaringBitmap();
			tBitsIdxArr[i] = t_bits;
			int pos = 0; 
			
//			MatArray mli = mCandLists.get(i);
//			ArrayList<GraphNode> elist = mli.elist();
//			for (GraphNode gn : elist) {
//				if (posToGN.containsKey(gn.pos)) {
//					System.out.println("RIGHT L_interval.mStart");
//					System.out.println(gn.pos);
//					System.out.println(gn.L_interval.mStart);
//					GraphNode gn1 = LintToGN.get(gn.L_interval.mStart);
//					System.out.println(gn1.pos);
//					PoolEntry actEntry = new PoolEntry(pos++, qn, gn);
//					qAct.addEntry(actEntry);
//					t_bits.add(actEntry.getValue().L_interval.mStart);
//				}
//			}
//			
//			nodeset intersectedNS = intersectedAnsGr.get(i);
//			for (int n : intersectedNS.gnodesBits) {
//				System.out.println("WRONG L_interval.mStart");
//				System.out.println(n);
//				GraphNode gn = posToGN.get(n);
//				System.out.println(gn.L_interval.mStart);
//				GraphNode gn1 = LintToGN.get(gn.L_interval.mStart);
//				System.out.println(gn1.pos);
//			}
//			
//			GraphNode A1 = LintToGN.get(124);
//			GraphNode A2 = LintToGN.get(23); //from pos 1 in 
//			
//			int x=1;
			
			nodeset intersectedNS = intersectedAnsGr.get(i);
			if (intersectedNS.hasNodes) {
				for (int n : intersectedNS.gnodesBits) { 
//					GraphNode gn = posToGN.get(n);
					GraphNode gn = LintToGN.get(n);
					PoolEntry actEntry = new PoolEntry(pos++, qn, gn);
					qAct.addEntry(actEntry);
					t_bits.add(actEntry.getValue().L_interval.mStart);
				}
			} else {
				MatArray mli = mCandLists.get(i);
				ArrayList<GraphNode> elist = mli.elist();
				for (GraphNode n : elist) {
					PoolEntry actEntry = new PoolEntry(pos++, qn, n);
					qAct.addEntry(actEntry);
					t_bits.add(actEntry.getValue().L_interval.mStart);
				}
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

//		for (int ti : rs_and) {
//			PoolEntry e = list.get(t_bits.rank(ti) - 1);
//			r.addChild(e);
//			e.addParent(r);
//		}
		
		RoaringBatchIterator it = rs_and.getBatchIterator();
		int[] buffer = new int[256];

		while (it.hasNext()) {

			int batch = it.nextBatch(buffer);
			for(int i = 0; i<batch; ++i) {
				PoolEntry e = list.get(t_bits.rank(buffer[i]) - 1);
				r.addChild(e);
				e.addParent(r);
				
			}

		}

		return true;

	}

	public static void main(String[] args) {

	}

}
