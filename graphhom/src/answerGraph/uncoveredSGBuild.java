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
//	HashMap<Integer,Integer> oldNewVertices;
	
	public uncoveredSGBuild(Query query, BFLIndex bfl, ArrayList<MatArray> candLsts, 
			ArrayList<QEdge> INuncoveredEdges, HashMap<Integer, GraphNode> INLintToGN, 
//			ArrayList<nodeset> INintersectedAnsGr, HashMap<Integer,Integer> INoldNewVertices) {
			ArrayList<nodeset> INintersectedAnsGr) {

		mQuery = query;
		mBFL = bfl;
		mCandLists = candLsts;
		uncoveredEdges = INuncoveredEdges;
		intersectedAnsGr = INintersectedAnsGr;
		LintToGN = INLintToGN;
//		oldNewVertices = INoldNewVertices;
	}

	public ArrayList<Pool> run() {
		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V];
		initPool(tBitsIdxArr);

		// THIS IS THE MOST COSTLY STEP
		for(QEdge edge: uncoveredEdges){
			linkOneStep(edge,tBitsIdxArr);
		}
		
		return mPool;

	}
	
	private void initPool(RoaringBitmap[] tBitsIdxArr) {
		
		//only get pools for nodes with uncovered edges
		//union this pool with existing pool, as some graph nodes with edges may not be there
		
		//re-NUMBER NODES AND EDGE'S NODES. Get QNode object
		
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
			
			nodeset intersectedNS = intersectedAnsGr.get(i);
			
			if (mCandLists != null) {  // there exist nodes uncovered
				MatArray mli = mCandLists.get(i);
//				MatArray mli = mCandLists.get(oldNewVertices.get(i));
				ArrayList<GraphNode> elist = mli.elist();
				
				if (intersectedNS.gnodesBits.getCardinality() > 0 && intersectedNS.gnodesBits.getCardinality() < elist.size()) {
					for (int n : intersectedNS.gnodesBits) {
						GraphNode gn = LintToGN.get(n);
						PoolEntry actEntry = new PoolEntry(pos++, qn, gn);
						qAct.addEntry(actEntry);
						t_bits.add(actEntry.getValue().L_interval.mStart);
					}
				} else {
					for (GraphNode n : elist) {
						PoolEntry actEntry = new PoolEntry(pos++, qn, n);
						qAct.addEntry(actEntry);
						t_bits.add(actEntry.getValue().L_interval.mStart);
					}
				}
				
			} else {  // all nodes covered
				for (int n : intersectedNS.gnodesBits) {
					GraphNode gn = LintToGN.get(n);
					PoolEntry actEntry = new PoolEntry(pos++, qn, gn);
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
