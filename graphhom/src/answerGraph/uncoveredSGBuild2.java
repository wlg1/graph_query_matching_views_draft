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
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import queryPlan.PlanGenerator;
import views.nodeset;


public class uncoveredSGBuild2 {

	Query mQuery;
	ArrayList<nodeset> matView;
	BFLIndex mBFL;
	ArrayList<MatArray> mCandLists;
	ArrayList<QEdge> uncoveredEdges;
	HashMap<Integer, GraphNode> LintToGN;
	ArrayList<Integer> nodesToCompute;
	ArrayList<nodeset> intersectedAnsGr;
	
	public uncoveredSGBuild2(Query query, BFLIndex bfl, ArrayList<MatArray> candLsts, 
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

		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V];
		initPool(tBitsIdxArr);

		for(QEdge edge: uncoveredEdges){
			linkOneStep(edge,tBitsIdxArr);
		}
		
		return matView;

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

		matView = new ArrayList<nodeset>();
		for (int i = 0; i < mQuery.V; i++) {			
			nodeset ns = new nodeset();
			if (!nodesToCompute.contains(i)) {
				continue;
			}
			
			RoaringBitmap t_bits = new RoaringBitmap();
			tBitsIdxArr[i] = t_bits;
			
			nodeset intersectedNS = intersectedAnsGr.get(i);
			if (intersectedNS.hasNodes) {
				for (int n : intersectedNS.gnodesBits) {
					GraphNode gn = LintToGN.get(n);
					ns.gnodesBits.add(gn.L_interval.mStart);
					ns.fwdAdjLists = (HashMap<Integer, HashMap<Integer, RoaringBitmap>>) null;
					t_bits.add(gn.L_interval.mStart);
				}
			} else {
				MatArray mli = mCandLists.get(i);
				ArrayList<GraphNode> elist = mli.elist();
				for (GraphNode gn : elist) {
					ns.gnodesBits.add(gn.L_interval.mStart);
					ns.fwdAdjLists = (HashMap<Integer, HashMap<Integer, RoaringBitmap>>) null;
					t_bits.add(gn.L_interval.mStart);
				}
			}
			matView.add(ns);
		}
	}

	private void linkOneStep(QEdge edge, RoaringBitmap[] tBitsIdxArr) {

		//for every e_f, poolentry in nodeset that's head to query edge
		//tBitsIdxArr[to] is bitmap of nodeset that's tail to query edge 
		//pl_t.elist() is poolentries of nodeset that's tail to query edge 
		
		int from = edge.from, to = edge.to;
		AxisType axis = edge.axis;
		nodeset ns_f = matView.get(from), ns_t = matView.get(to);

		
		for (int n : ns_f.gnodesBits) {
			GraphNode e_f =  LintToGN.get(n);
			if (axis == AxisType.child) 
				linkOneStep(e_f, tBitsIdxArr[to], ns_t);
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

	private boolean linkOneStep(GraphNode s, RoaringBitmap t_bits, nodeset list) {
		
		RoaringBitmap rs_and = RoaringBitmap.and(s.adj_bits_o, t_bits);

		if (rs_and.isEmpty())
			return false;
		
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
