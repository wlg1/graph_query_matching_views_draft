package simfilter;

import java.util.ArrayList;
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
import queryPlan.PlanGenerator;

public class GraSimBitFilter {

	Query mQuery;
	ArrayList<MatArray> mCandLists;
	BFLIndex mBFL;
	ArrayList<Integer> nodesTopoList;
	int passNum = 0;
	ArrayList<ArrayList<GraphNode>> mInvLsts;
	RoaringBitmap[] mInvBitsArr, mCandBitsArr;
	int[] order;
	int[] candidates_count;

	public GraSimBitFilter(Query query, ArrayList<ArrayList<GraphNode>> invLsts, BFLIndex bfl) {

		mQuery = query;
		mBFL = bfl;
		mInvLsts = invLsts;
		init();
		// order = getPlan();
	}

	public void filter() {

		QEdge[] edges = mQuery.edges;
		passNum = 0;
		boolean hasChange= false;// = backwardCheck(edges);

		do {
			
			if(Flags.PRUNELIMIT&&passNum>Consts.PruneLimit)
				break;
			hasChange = backwardCheck(edges);
			
			hasChange = hasChange || forwardCheck(edges);

			

		} while (hasChange);
		System.out.println("Total passes: " + passNum);
	}

	
	private void filterBwd_c(QNode child, RoaringBitmap candBits) {

		RoaringBitmap union = unionBackAdj(child);
		candBits.and(union);

	}

	private void filterBwd_d(QNode parent, QNode child, RoaringBitmap candBits) {

		ArrayList<GraphNode> invLst = mInvLsts.get(parent.lb);
		RoaringBitmap invBits = mInvBitsArr[parent.id];
		RoaringBitmap rmBits = new RoaringBitmap();
		for (int i : candBits) {

			GraphNode gn = invLst.get(invBits.rank(i) - 1);
			boolean found = filterBwd_d(child, gn);

			if (!found) {

				// candBits.remove(i); // cannot do it this way! 2020.06.28
				rmBits.add(i);
				;

			}
		}

		candBits.xor(rmBits);
	}

	private boolean filterBwd_d(QNode child, GraphNode gn) {

		boolean found = false;

		ArrayList<GraphNode> invLst = mInvLsts.get(child.lb);
		RoaringBitmap invBits = mInvBitsArr[child.id];
		RoaringBitmap candBits = mCandBitsArr[child.id];

		for (int i : candBits) {
			GraphNode ni = invLst.get(invBits.rank(i) - 1);

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

		return true;
	}

	private boolean backwardCheck(QEdge[] edges) {

		boolean hasChange = false;
		passNum++;
		for (QEdge e : edges) {
			boolean result = backwardCheck(e);
			hasChange = hasChange || result;
		}

		return hasChange;
	}

	private boolean backwardCheck(QEdge e) {

		int from = e.from, to = e.to;
		AxisType axis = e.axis;
		QNode child = mQuery.nodes[to], parent = mQuery.nodes[from];
		RoaringBitmap candBits = mCandBitsArr[from];
		int card = candBits.getCardinality();
		if (axis == AxisType.child)
			filterBwd_c(child, candBits);
		else
			filterBwd_d(parent, child, candBits);
		boolean hasChange = card > candBits.getCardinality() ? true : false;

		return hasChange;
	}

	private boolean forwardCheck(QEdge[] edges) {

		boolean hasChange = false;
		passNum++;
		for (QEdge e : edges) {
			boolean result = forwardCheck(e);
			hasChange = hasChange || result;
		}

		return hasChange;
	}

	private boolean forwardCheck(QEdge e) {

		int from = e.from, to = e.to;
		AxisType axis = e.axis;
		QNode child = mQuery.nodes[to], parent = mQuery.nodes[from];
		RoaringBitmap candBits = mCandBitsArr[to];
		int card = candBits.getCardinality();
		if (axis == AxisType.child)
			filterFwd_c(parent, candBits);
		else
			filterFwd_d(parent, child, candBits);
		boolean hasChange = card > candBits.getCardinality() ? true : false;

		return hasChange;
	}

	private void filterFwd_c(QNode parent, RoaringBitmap candBits) {

		RoaringBitmap union = unionFwdAdj(parent);
		candBits.and(union);

	}

	private void filterFwd_d(QNode parent, QNode child, RoaringBitmap candBits) {

		ArrayList<GraphNode> invLst = mInvLsts.get(child.lb);
		RoaringBitmap invBits = mInvBitsArr[child.id];
		RoaringBitmap rmvBits = new RoaringBitmap();
		for (int i : candBits) {

			GraphNode gn = invLst.get(invBits.rank(i) - 1);

			boolean found = filterFwd_d(parent, gn);

			if (!found) {

				rmvBits.add(i);
			}
		}

		candBits.xor(rmvBits);
	}

	private boolean filterFwd_d(QNode parent, GraphNode gn) {

		boolean found = false;

		ArrayList<GraphNode> invLst = mInvLsts.get(parent.lb);
		RoaringBitmap invBits = mInvBitsArr[parent.id];
		RoaringBitmap candBits = mCandBitsArr[parent.id];
		for (int i : candBits) {
			GraphNode par = invLst.get(invBits.rank(i) - 1);
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

		return true;

	}

	private int[] getPlan() {

		candidates_count = new int[mQuery.V];

		for (int i = 0; i < mQuery.V; i++) {
			candidates_count[i] = mInvLsts.get(i).size();

		}

		// int[] order = PlanGenerator.generateGQLQueryPlan(query,
		// candidates_count);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		// int[] order = PlanGenerator.generateRIQueryPlan(query);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		// int[] order = PlanGenerator.generateHybQueryPlan(query,
		// candidates_count);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		// int[] order = PlanGenerator.generateTopoQueryPlan(query);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		int[] order = PlanGenerator.generateRITOPOQueryPlan(mQuery);
		PlanGenerator.printSimplifiedQueryPlan(mQuery, order);

		return order;
	}

	//////////////////////////////////////////////
	
	public ArrayList<RoaringBitmap[]> getBitsArr() {

		ArrayList<RoaringBitmap[]> pair = new ArrayList<RoaringBitmap[]>();

		pair.add(mInvBitsArr);
		pair.add(mCandBitsArr);

		return pair;
	}

	public ArrayList<MatArray> getCandList() {

		genCandList();
		return mCandLists;
	}



	private RoaringBitmap unionBackAdj(QNode child) {

		RoaringBitmap invBits = mInvBitsArr[child.id];
		RoaringBitmap candBits = mCandBitsArr[child.id];
		ArrayList<GraphNode> invLst = mInvLsts.get(child.lb);
		RoaringBitmap union = new RoaringBitmap();

		for (int i : candBits) {

			GraphNode gn = invLst.get(invBits.rank(i) - 1);
			union.or(gn.adj_bits_i);

		}

		return union;

	}

	private RoaringBitmap unionFwdAdj(QNode parent) {

		RoaringBitmap invBits = mInvBitsArr[parent.id];
		RoaringBitmap candBits = mCandBitsArr[parent.id];
		ArrayList<GraphNode> invLst = mInvLsts.get(parent.lb);

		RoaringBitmap union = new RoaringBitmap();

		for (int i : candBits) {

			GraphNode gn = invLst.get(invBits.rank(i) - 1);
			union.or(gn.adj_bits_o);

		}

		return union;

	}


	private void init() {

		int size = mQuery.V;

		mInvBitsArr = new RoaringBitmap[size];
		mCandBitsArr = new RoaringBitmap[size];
		QNode[] qnodes = mQuery.nodes;
		for (int i = 0; i < qnodes.length; i++) {
			QNode q = qnodes[i];

			ArrayList<GraphNode> invLst = mInvLsts.get(q.lb);
			RoaringBitmap t_bits = new RoaringBitmap();
			for (GraphNode e : invLst) {
				t_bits.add(e.L_interval.mStart);

			}
			mInvBitsArr[q.id] = t_bits;
			mCandBitsArr[q.id] = t_bits.clone();
		}

	}

	private void genCandList() {

		QNode[] qnodes = mQuery.nodes;
		mCandLists = new ArrayList<MatArray>(mQuery.V);
		for (int i = 0; i < qnodes.length; i++) {
			QNode q = qnodes[i];

			ArrayList<GraphNode> invLst = mInvLsts.get(q.lb);
			MatArray mlist = new MatArray();

			RoaringBitmap invBits = mInvBitsArr[q.id];
			RoaringBitmap candBits = mCandBitsArr[q.id];
			ArrayList<GraphNode> list = new ArrayList<GraphNode>();
			for (int j : candBits) {
				GraphNode nj = invLst.get(invBits.rank(j) - 1);
				list.add(nj);
			}
			mlist.addList(list);
			mCandLists.add(q.id, mlist);
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
