package evaluator;

import java.util.ArrayList;
import org.roaringbitmap.RoaringBitmap;

//import answerGraph.EdgeAnsGraphBuilder;
import answerGraphOPT.EdgeAnsGraphBuilder;
import dao.BFLIndex;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import graph.GraphNode;
import helper.LimitExceededException;
import helper.QueryEvalStat;
import helper.TimeTracker;
import query.graph.QNode;
import query.graph.Query;
//import simfilter.CycSimFilter;
import simfilterOPT.CycSimFilter;
//import simfilter.EdgeSimFilter;
import simfilterOPT.EdgeSimFilter;
//import tupleEnumerator.HybTupleEnumer;
import tupleEnumeratorOPT.HybTupleEnumer;


public class EdgeHomIE {

	Query mQuery;
	ArrayList<Pool> mPool;
	ArrayList<MatArray> mCandLists;
	BFLIndex mBFL;
	TimeTracker tt;

	GraphNode[] mGraNodes;
	ArrayList<Integer> nodesTopoList;

	int passNum = 0;

	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	RoaringBitmap[] mBitsIdxArr;

	ArrayList<RoaringBitmap> mBitsByIDArr;
	double mTupleCount;

	HybTupleEnumer tenum;
	// HybTupleEnumCache tenum;
	boolean simfilter = true;
	boolean useCycfilter = false;

	public EdgeHomIE(Query query, GraphNode[] graNodes, ArrayList<ArrayList<GraphNode>> invLstsByID,
			ArrayList<RoaringBitmap> bitsByIDArr) {

		mQuery = query;
		mGraNodes = graNodes;
		mBitsByIDArr = bitsByIDArr;
		mInvLstsByID = invLstsByID;
		tt = new TimeTracker();

	}

	public boolean run(QueryEvalStat stat) throws LimitExceededException {

		stat.setTotNodesInv(calTotInvNodes());

		RoaringBitmap[] mCandBitsArr = null;

		if (simfilter) {
			tt.Start();

			if (useCycfilter) {
				CycSimFilter filter = new CycSimFilter(mQuery, mGraNodes, mInvLstsByID, mBitsByIDArr);
				filter.prune();
				mCandBitsArr = filter.getCandBits();
			} else {

				EdgeSimFilter filter = new EdgeSimFilter(mQuery, mGraNodes, mInvLstsByID, mBitsByIDArr);
				filter.prune();
				mCandBitsArr = filter.getCandBits();
			}

			double prunetm = tt.Stop() / 1000;
			stat.setFltTime(prunetm);
			System.out.println("Prune time:" + prunetm + " sec.");
		} else
			mCandBitsArr = getCandBitsArr();

		stat.setTotNodesSim(calTotCandSolnNodes(mCandBitsArr));

		tt.Start();
		EdgeAnsGraphBuilder agBuilder = new EdgeAnsGraphBuilder(mQuery, mGraNodes, mCandBitsArr);

		mPool = agBuilder.run();

		double buildtm = tt.Stop() / 1000;
		stat.calAnsGraphSize(mPool);
		stat.setBuildTime(buildtm);
		System.out.println("Answer graph build time:" + buildtm + " sec.");

		tt.Start();
		tenum = new HybTupleEnumer(mQuery, mPool);
		// tenum = new HybTupleEnumCache(mQuery, mPool);
		// if (mQuery.isTree()) {
		// mTupleCount = calTotTreeSolns();
		// } else

		mTupleCount = tenum.enumTuples();

		double enumtm = tt.Stop() / 1000;
		stat.setEnumTime(enumtm);
		System.out.println("Tuple enumeration time:" + enumtm + " sec.");

		stat.setNumSolns(mTupleCount);
		clear();

		return true;
	}

	public double getTupleCount() {

		if (tenum != null)
			return tenum.getTupleCount();
		return 0;
	}

	public void clear() {
		if (mPool != null)
			for (Pool p : mPool)
				p.clear();
	}

	private RoaringBitmap[] getCandBitsArr() {

		RoaringBitmap[] bitsByIDArr = new RoaringBitmap[mQuery.V];

		for (int i = 0; i < mQuery.V; i++) {
			QNode q = mQuery.nodes[i];
			RoaringBitmap bits = new RoaringBitmap();
			ArrayList<GraphNode> invLst = mInvLstsByID.get(q.lb);
			for (GraphNode n : invLst) {
				bits.add(n.id);
			}
			bitsByIDArr[q.id] = bits;
		}
		return bitsByIDArr;

	}

	private double calTotCandSolnNodes(RoaringBitmap[] candBitsArr) {

		double totNodes = 0.0;

		for (RoaringBitmap bits : candBitsArr) {

			totNodes += bits.getCardinality();
		}

		System.out.println("total after nodes=" + totNodes);
		return totNodes;
	}

	private double calTotInvNodes() {

		double totNodes_before = 0.0;

		for (QNode q : mQuery.nodes) {

			ArrayList<GraphNode> invLst = mInvLstsByID.get(q.lb);
			totNodes_before += invLst.size();
		}
		System.out.println("total before nodes=" + totNodes_before);
		return totNodes_before;
	}


	public void printSolutions(ArrayList<PoolEntry> elist) {

		if (elist.isEmpty())
			return;

		for (PoolEntry r : elist) {

			System.out.println(r);

		}

	}

	public static void main(String[] args) {

	}

}
