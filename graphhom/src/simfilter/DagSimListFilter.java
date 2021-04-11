package simfilter;

import java.util.ArrayList;
import java.util.Arrays;

import org.roaringbitmap.RoaringBitmap;

import dao.BFLIndex;
import dao.MatArray;
import global.Consts;
import global.Flags;
import global.Consts.AxisType;
import graph.GraphNode;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import query.graph.QueryHandler;

public class DagSimListFilter {

	Query mQuery;
	ArrayList<MatArray> mCandLists;
	BFLIndex mBFL;
	ArrayList<Integer> nodesTopoList;
	int passNum = 0;
	ArrayList<ArrayList<GraphNode>> mInvLsts;
	RoaringBitmap[] mBitsIdxArr;
	
	
	
	public DagSimListFilter(Query query, ArrayList<ArrayList<GraphNode>> invLsts, BFLIndex bfl){
		
		mQuery = query;
		mBFL = bfl;
		mInvLsts = invLsts;
		init();
	}
	
	
	public ArrayList<MatArray> prune() {
		boolean[] changed = new boolean[mQuery.V];
		passNum = 0;
		Arrays.fill(changed, true);

		boolean hasChange = pruneBUP(changed);

		do {
			if(Flags.PRUNELIMIT&&passNum>Consts.PruneLimit)
				break;
			hasChange = pruneTDW(changed);
			
			if (hasChange) {
				hasChange = pruneBUP(changed);
				
			}

		} while (hasChange);

		System.out.println("Total passes: " + passNum);
		
		return mCandLists;
	}

	
	private boolean pruneBUP(boolean[] changed) {

		boolean hasChange = false;

		passNum++;
		for (int i = mQuery.V - 1; i >= 0; i--) {
			int qid = nodesTopoList.get(i);
			boolean result = pruneOneStepBUP(qid, changed);
			hasChange = hasChange || result;
		}

		return hasChange;
	}
	
	private boolean pruneOneStepBUP(int qid, boolean[] changed) {

		QNode[] qnodes = mQuery.nodes;
		boolean hasChange = false;

		QNode parent = qnodes[qid];
		if (parent.isSink())
			return false;
		MatArray mli = mCandLists.get(qid);
		ArrayList<GraphNode> elist = mli.elist();
		ArrayList<GraphNode> rmvList = new ArrayList<GraphNode>();
		for (GraphNode qn : elist) {

			boolean found = pruneOneStepBUP(parent, qn, changed);

			if (!found) {
				rmvList.add(qn);
				mBitsIdxArr[qid].remove(qn.L_interval.mStart);
             
			}
		}
		elist.removeAll(rmvList);
		if (rmvList.size() > 0) {
			changed[qid] = true;
			hasChange = true;
		} else {
			changed[qid] = false;
			hasChange = false;
		}

		return hasChange;

	}

	
	private boolean pruneOneStepBUP(QNode parent, GraphNode gn, boolean[] changed) {

		QNode[] qnodes = mQuery.nodes;
		ArrayList<QEdge> o_edges = parent.E_O;
		for (QEdge o_edge : o_edges) {
			int cid = o_edge.to;
			if (passNum > 1 && !changed[cid]) {
				//System.out.println("Yes pruneOneStepBUP!");
				continue;
			}
			AxisType axis = o_edge.axis;
			QNode child = qnodes[cid];
			MatArray mli = mCandLists.get(cid);
			boolean found = false;

			if (axis == AxisType.child){
				found = pruneOneStepBUP(gn, mBitsIdxArr[cid]);
			} else

				for (GraphNode ni : mli.elist()) {

					if (gn.id == ni.id)
						continue;

					if (gn.L_interval.mEnd < ni.L_interval.mStart) {
						if (!found) {
							return false;
						}
					}

					if (mBFL.reach(gn, ni) == 1) {
						found = true;
						break;
					}

				}

			if (!found)
				return false;

		}

		return true;
	}

	private boolean pruneOneStepBUP(GraphNode s, RoaringBitmap t_bits) {

		if (s.N_O_SZ == 0)
			return false;

		return RoaringBitmap.andCardinality(s.adj_bits_o, t_bits) > 0 ? true : false;

	}

	private boolean pruneTDW(boolean[] changed) {

		passNum++;
		boolean hasChange = false;

		for (int qid : nodesTopoList) {
			boolean result = pruneOneStepTDW(qid, changed);
			hasChange = hasChange || result;
		}

		return hasChange;
	}

	private boolean pruneOneStepTDW(int cid, boolean[] changed) {

		QNode[] qnodes = mQuery.nodes;
		boolean hasChange = false;

		QNode child = qnodes[cid];

		if (child.isSource())
			return false;
		MatArray mli = mCandLists.get(cid);
		ArrayList<GraphNode> elist = mli.elist();
		ArrayList<GraphNode> rmvList = new ArrayList<GraphNode>();
		for (GraphNode qn : elist) {

			boolean found = pruneOneStepTDW(child, qn, changed);
			if (!found) {

				rmvList.add(qn);
				mBitsIdxArr[cid].remove(qn.L_interval.mStart);

			}

		}

		elist.removeAll(rmvList);
		if (rmvList.size() > 0) {
			changed[cid] = true;
			hasChange = true;

		}
		else{
			
			changed[cid] = false;
			hasChange = false;
		}
		return hasChange;

	}

	private boolean pruneOneStepTDW(QNode child, GraphNode gn, boolean[] changed) {

		QNode[] qnodes = mQuery.nodes;
		ArrayList<QEdge> i_edges = child.E_I;

		for (QEdge i_edge : i_edges) {

			int pid = i_edge.from;
			if (passNum > 1 && !changed[pid]) {
				//System.out.println("Yes pruneOneStepTDW!");
				continue;
			}
			AxisType axis = i_edge.axis;
			QNode parent = qnodes[pid];
			MatArray mli = mCandLists.get(pid);
			boolean found = false;

			if (axis == AxisType.child){
				found = pruneOneStepTDW(gn, mBitsIdxArr[pid]);
			} else
				for (GraphNode par : mli.elist()) {
					if (gn.id == par.id)
						continue;
					if (mBFL.reach(par, gn) == 1) {
						found = true;
					}

					if (found)
						break;
				}

			if (!found)
				return false;

		}

		return true;

	}

	private boolean pruneOneStepTDW(GraphNode s, RoaringBitmap t_bits) {

		if (s.N_I_SZ == 0)
			return false;

		return RoaringBitmap.andCardinality(s.adj_bits_i, t_bits) > 0 ? true : false;

	}

	private void init() {

		QueryHandler qh = new QueryHandler();
		// nodesOrder = qh.topologyQue(mQuery);
		nodesTopoList = qh.topologyList(mQuery);
	
		int size = mQuery.V;
		mCandLists = new ArrayList<MatArray>(size);
		
		mBitsIdxArr = new RoaringBitmap[size];

		QNode[] qnodes = mQuery.nodes;
		for (int i = 0; i < qnodes.length; i++) {
			QNode q = qnodes[i];

			ArrayList<GraphNode> invLst = mInvLsts.get(q.lb);
			MatArray mlist = new MatArray();
			mlist.addList(invLst);
			mCandLists.add(q.id, mlist);

			RoaringBitmap t_bits = new RoaringBitmap();
			for (GraphNode e : invLst) {
				t_bits.add(e.L_interval.mStart);

			}
			mBitsIdxArr[i] = t_bits;

		}

	
	}

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
