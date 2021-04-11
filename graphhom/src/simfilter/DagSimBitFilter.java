package simfilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

public class DagSimBitFilter {

	Query mQuery;
	ArrayList<MatArray> mCandLists;
	BFLIndex mBFL;
	ArrayList<Integer> nodesTopoList;
	int passNum = 0;
	GraphNode[] mGraNodes;
	RoaringBitmap[] mCandBitsArr;
	ArrayList<RoaringBitmap> mBitsByIDArr;


	
	public DagSimBitFilter(Query query, GraphNode[] graNodes, ArrayList<RoaringBitmap> bitsByIDArr, BFLIndex bfl) {

		mQuery = query;
		mBFL = bfl;
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
		//genCandList();

		
		
	}


	public ArrayList<MatArray> getCandList() {

		genCandList();
		return mCandLists;
	}

	private boolean pruneBUP(boolean[] changed) {

		boolean hasChange = false;

		for (int i = mQuery.V - 1; i >= 0; i--) {
			int qid = nodesTopoList.get(i);
			boolean result = pruneOneStepBUP(qid, changed);
			hasChange = hasChange || result;
		}
		passNum++;
		return hasChange;
	}

	private boolean pruneOneStepBUP(int qid, boolean[] changed) {

		QNode[] qnodes = mQuery.nodes;
		QNode parent = qnodes[qid];
		if (parent.isSink())
			return false;

		RoaringBitmap candBits = mCandBitsArr[parent.id];
		int card = candBits.getCardinality();
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

		if (qnodes_c.size() > 0)
			pruneOneStepBUP_c(qnodes_c, candBits, changed);

		if (qnodes_d.size() > 0) {

			pruneOneStepBUP_d(qid, qnodes_d, candBits, changed);
		}

		boolean hasChange = card > candBits.getCardinality() ? true : false;

		if (hasChange)
			changed[qid] = true;
		else
			changed[qid] = false;
		return hasChange;

	}

	private void pruneOneStepBUP_c(ArrayList<QNode> qnodes_c, RoaringBitmap candBits, boolean[] changed) {

		for (QNode child : qnodes_c) {
			if (passNum > 1 && !changed[child.id]) {
				// System.out.println("Yes pruneOneStepBUP!");
				continue;
			}
			RoaringBitmap union = unionBackAdj(child);
			candBits.and(union);
		}

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

	private void pruneOneStepBUP_d(int qid, ArrayList<QNode> qnodes_d, RoaringBitmap candBits_p, boolean[] changed) {

		RoaringBitmap rmBits = new RoaringBitmap();
		
		for (int i : candBits_p) {
        	GraphNode gn = mGraNodes[i];
			for (QNode child : qnodes_d) {
				boolean found = false;
				if (passNum > 1 && !changed[child.id]) {
					// System.out.println("Yes pruneOneStepBUP!");
					continue;
				}

		
				RoaringBitmap candBits = mCandBitsArr[child.id];
				for (int j : candBits) {
					GraphNode nj = mGraNodes[j];

					if (gn.id == nj.id)
						continue;

					if (mBFL.reach(gn, nj) == 1) {
						found = true;
						break;
					}

				}

				if (!found){
					rmBits.add(i);
					break;
				}
			}
		}

		candBits_p.xor(rmBits);
		
	}

	private boolean pruneTDW(boolean[] changed) {

		
		boolean hasChange = false;

		for (int qid : nodesTopoList) {
			boolean result = pruneOneStepTDW(qid, changed);
			hasChange = hasChange || result;
		}
		passNum++;
		return hasChange;
	}

	private boolean pruneOneStepTDW(int cid, boolean[] changed) {

		QNode[] qnodes = mQuery.nodes;

		QNode child = qnodes[cid];

		if (child.isSource())
			return false;

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

		RoaringBitmap candBits = mCandBitsArr[child.id];
		int card = candBits.getCardinality();
		if (qnodes_c.size() > 0)
			pruneOneStepTDW_c(qnodes_c, candBits, changed);

		if (qnodes_d.size() > 0){
		
			pruneOneStepTDW_d(qnodes_d, candBits, changed);
			
		

		}
		boolean hasChange = card > candBits.getCardinality() ? true : false;
		if (hasChange)
			changed[cid] = true;
		else
			changed[cid] = false;
		return hasChange;

	}

	private void pruneOneStepTDW_c(ArrayList<QNode> qnodes_c, RoaringBitmap candBits, boolean[] changed) {

		for (QNode parent : qnodes_c) {
			if (passNum > 1 && !changed[parent.id]) {
				// System.out.println("Yes pruneOneStepBUP!");
				continue;
			}
			RoaringBitmap union = unionFwdAdj(parent);
			candBits.and(union);
		}

	}

	private void pruneOneStepTDW_d(ArrayList<QNode> qnodes_d, RoaringBitmap candBits_c, boolean[] changed) {

	
		RoaringBitmap rmvBits = new RoaringBitmap();
		for (int i : candBits_c) {
        	GraphNode gn = mGraNodes[i];
			for (QNode parent : qnodes_d) {
				boolean found = false;
				if (passNum > 1 && !changed[parent.id]) {
					// System.out.println("Yes pruneOneStepBUP!");
					continue;
				}
				RoaringBitmap candBits = mCandBitsArr[parent.id];
				for (int j : candBits) {
				    GraphNode par = mGraNodes[j];
					if (gn.id == par.id)
						continue;

					if (mBFL.reach(par, gn) == 1) {
						found = true;
					}

					if (found)
						break;

				}

				if (!found){
					rmvBits.add(i);
				    break;
				}


			}

	
		}

		candBits_c.xor(rmvBits);
	}

	private void init() {

		QueryHandler qh = new QueryHandler();
		// nodesOrder = qh.topologyQue(mQuery);
		nodesTopoList = qh.topologyList(mQuery);
		int size = mQuery.V;
		mCandBitsArr = new RoaringBitmap[size];

		QNode[] qnodes = mQuery.nodes;
		for (int i = 0; i < qnodes.length; i++) {
			QNode q = qnodes[i];
			RoaringBitmap t_bits = mBitsByIDArr.get(q.lb);
			mCandBitsArr[q.id] = t_bits.clone();
			
		}
	}

	private void genCandList() {

		QNode[] qnodes = mQuery.nodes;
		mCandLists = new ArrayList<MatArray>(mQuery.V);
		for (int i = 0; i < qnodes.length; i++) {
			QNode q = qnodes[i];
		    MatArray mlist = new MatArray();
	        RoaringBitmap candBits = mCandBitsArr[q.id];
			ArrayList<GraphNode> list = new ArrayList<GraphNode>();
			for (int j : candBits) {
				GraphNode nj = this.mGraNodes[j];
				list.add(nj);
			}
			Collections.sort(list);
			mlist.addList(list);
			mCandLists.add(q.id, mlist);
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
