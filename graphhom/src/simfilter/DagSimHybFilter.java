package simfilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.roaringbitmap.RoaringBitmap;

import dao.BFLIndex;
import dao.MatArray;
import global.Consts;
import global.Consts.AxisType;
import global.Flags;
import graph.GraphNode;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import query.graph.QueryHandler;

public class DagSimHybFilter {

	Query mQuery;
	ArrayList<MatArray> mCandLists;
	BFLIndex mBFL;
	ArrayList<Integer> nodesTopoList;
	int passNum = 0;
	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	ArrayList<RoaringBitmap> mBitsByIDArr;
	RoaringBitmap[] mCandBitsArr;
	GraphNode[] mGraNodes;

	public DagSimHybFilter(Query query, GraphNode[] graNodes, ArrayList<ArrayList<GraphNode>> invLstsByID,
			ArrayList<RoaringBitmap> bitsByIDArr, BFLIndex bfl) {

		mQuery = query;
		mBFL = bfl;
		mInvLstsByID = invLstsByID;
		mGraNodes = graNodes;
		mBitsByIDArr = bitsByIDArr;
		init();
	}

	public void prune() {
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

	}

	public ArrayList<MatArray> getCandList() {

		for (int i = 0; i < mQuery.V; i++) {
			QNode q = mQuery.nodes[i];
			ArrayList<GraphNode> list = mCandLists.get(i).elist();
			Collections.sort(list);
			// System.out.println("qid = " + i +" " + " inv= " +
			// this.mInvLstsByID.get(q.lb).size()+ " original bits = " +
			// this.mBitsByIDArr.get(q.lb).getCardinality() + " bits = " +
			// this.mCandBitsArr[i].getCardinality() + " list = " +
			// this.mCandLists.get(i).elist().size());
		}
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

	private ArrayList<GraphNode> bits2list(RoaringBitmap bits) {

		ArrayList<GraphNode> list = new ArrayList<GraphNode>();
		for (int i : bits) {

			list.add(mGraNodes[i]);
		}

		return list;

	}

	private boolean pruneOneStepBUP(int qid, boolean[] changed) {

		QNode[] qnodes = mQuery.nodes;
		QNode parent = qnodes[qid];
		if (parent.isSink())
			return false;

		RoaringBitmap candBits = mCandBitsArr[parent.id];
		int card = candBits.getCardinality();
		MatArray mli = mCandLists.get(parent.id);
		ArrayList<GraphNode> elist = mli.elist();
		ArrayList<QEdge> o_edges = parent.E_O;
		ArrayList<QNode> qnodes_c = new ArrayList<QNode>(o_edges.size()),
				qnodes_d = new ArrayList<QNode>(o_edges.size());

		for (QEdge o_edge : o_edges) {
			int cid = o_edge.to;
			AxisType axis = o_edge.axis;
			QNode child = qnodes[cid];
			if (axis == AxisType.child)
				qnodes_c.add(child);
			else
				qnodes_d.add(child);
		}

		if (qnodes_c.size() > 0) {

			pruneOneStepBUP_c(qnodes_c, elist, candBits, changed);

			mli.setList(bits2list(candBits));
		}

		if (qnodes_d.size() > 0) {

			pruneOneStepBUP_d(qnodes_d, elist, candBits, changed);
			
		}

		boolean hasChange = card > candBits.getCardinality() ? true : false;

		if (hasChange)
			changed[qid] = true;
		else
			changed[qid] = false;
		return hasChange;

	}

	private void pruneOneStepBUP_c(ArrayList<QNode> qnodes_c, ArrayList<GraphNode> candList, RoaringBitmap candBits,
			boolean[] changed) {

		// RoaringBitmap rmvBits = candBits.clone();

		for (QNode child : qnodes_c) {
			if (passNum > 1 && !changed[child.id]) {
				// System.out.println("Yes pruneOneStepBUP!");
				continue;
			}
			RoaringBitmap union = unionBackAdj(child);
			candBits.and(union);
		}

		// rmvBits.xor(candBits);
		// ArrayList<GraphNode> rmvList = bits2list(rmvBits);
		// candList.removeAll(rmvList);

	}

	private RoaringBitmap unionBackAdj(QNode child) {

		RoaringBitmap candBits = mCandBitsArr[child.id];
		RoaringBitmap union = new RoaringBitmap();

		for (int i : candBits) {

			GraphNode gn = this.mGraNodes[i];
			union.or(gn.adj_bits_id_i);

		}

		return union;

	}

	private RoaringBitmap unionFwdAdj(QNode parent) {

		RoaringBitmap candBits = mCandBitsArr[parent.id];

		RoaringBitmap union = new RoaringBitmap();

		for (int i : candBits) {

			GraphNode gn = this.mGraNodes[i];
			union.or(gn.adj_bits_id_o);

		}

		return union;

	}

	private void pruneOneStepBUP_d(ArrayList<QNode> qnodes_d, ArrayList<GraphNode> candList_p, RoaringBitmap candBits_p,
			boolean[] changed) {

		ArrayList<GraphNode> rmvList = new ArrayList<GraphNode>();
		RoaringBitmap rmvBits = new RoaringBitmap();
		for (GraphNode gn : candList_p) {
			for (QNode child : qnodes_d) {
				boolean found = false;
				if (passNum > 1 && !changed[child.id]) {
					// System.out.println("Yes pruneOneStepBUP!");
					continue;
				}

				MatArray mli = mCandLists.get(child.id);
				for (GraphNode ni : mli.elist()) {

					if (gn.id == ni.id)
						continue;

					if (mBFL.reach(gn, ni) == 1) {
						found = true;
						break;
					}

				}

				if (!found) {
					rmvList.add(gn);
					rmvBits.add(gn.id);
					break;
				}
			}

		}
		candBits_p.xor(rmvBits);
		candList_p.removeAll(rmvList);

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

		QNode child = qnodes[cid];

		if (child.isSource())
			return false;

		RoaringBitmap candBits = mCandBitsArr[child.id];
		int card = candBits.getCardinality();
		MatArray mli = mCandLists.get(child.id);
		ArrayList<GraphNode> elist = mli.elist();
		ArrayList<QEdge> i_edges = child.E_I;
		ArrayList<QNode> qnodes_c = new ArrayList<QNode>(i_edges.size()),
				qnodes_d = new ArrayList<QNode>(i_edges.size());

		for (QEdge i_edge : i_edges) {
			int pid = i_edge.from;
			AxisType axis = i_edge.axis;
			QNode parent = qnodes[pid];
			if (axis == AxisType.child)
				qnodes_c.add(parent);
			else
				qnodes_d.add(parent);
		}

		if (qnodes_c.size() > 0) {

			pruneOneStepTDW_c(qnodes_c, elist, candBits, changed);
			mli.setList(bits2list(candBits));
		}

		if (qnodes_d.size() > 0) {

			pruneOneStepTDW_d(qnodes_d, elist, candBits, changed);
			
		}

		boolean hasChange = card > candBits.getCardinality() ? true : false;
		if (hasChange)
			changed[cid] = true;
		else
			changed[cid] = false;
		return hasChange;

	}

	private void pruneOneStepTDW_c(ArrayList<QNode> qnodes_c, ArrayList<GraphNode> candList, RoaringBitmap candBits,
			boolean[] changed) {

		// RoaringBitmap rmvBits = candBits.clone();

		for (QNode parent : qnodes_c) {
			if (passNum > 1 && !changed[parent.id]) {
				// System.out.println("Yes pruneOneStepBUP!");
				continue;
			}
			RoaringBitmap union = unionFwdAdj(parent);
			candBits.and(union);
		}

		// rmvBits.xor(candBits);
		// ArrayList<GraphNode> rmvList = bits2list(rmvBits);
		// candList.removeAll(rmvList);

	}

	private void pruneOneStepTDW_d(ArrayList<QNode> qnodes_d, ArrayList<GraphNode> candList_c, RoaringBitmap candBits_c,
			boolean[] changed) {

		ArrayList<GraphNode> rmvList = new ArrayList<GraphNode>();
		RoaringBitmap rmvBits = new RoaringBitmap();
		for (GraphNode gn : candList_c) {
			for (QNode parent : qnodes_d) {
				boolean found = false;
				if (passNum > 1 && !changed[parent.id]) {
					// System.out.println("Yes pruneOneStepBUP!");
					continue;
				}

				MatArray mli = mCandLists.get(parent.id);
				for (GraphNode par : mli.elist()) {

					if (gn.id == par.id)
						continue;

					if (mBFL.reach(par, gn) == 1) {
						found = true;
						break;
					}

				}

				if (!found) {
					rmvList.add(gn);
					rmvBits.add(gn.id);
					break;
				}
			}

		}
		candBits_c.xor(rmvBits);
		candList_c.removeAll(rmvList);

	}

	private void init() {

		QueryHandler qh = new QueryHandler();
		// nodesOrder = qh.topologyQue(mQuery);
		nodesTopoList = qh.topologyList(mQuery);

		int size = mQuery.V;
		mCandLists = new ArrayList<MatArray>(size);

		mCandBitsArr = new RoaringBitmap[size];

		QNode[] qnodes = mQuery.nodes;
		for (int i = 0; i < qnodes.length; i++) {
			QNode q = qnodes[i];

			ArrayList<GraphNode> invLst = mInvLstsByID.get(q.lb);
			MatArray mlist = new MatArray();
			mlist.addList(invLst);
			mCandLists.add(q.id, mlist);
			RoaringBitmap t_bits = mBitsByIDArr.get(q.lb);
			mCandBitsArr[q.id] = t_bits.clone();
		}

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
