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
import prefilter.FilterBuilder;
import query.graph.QNode;
import query.graph.Query;
import simfilter.CycSimFilter;
import simfilter.EdgeSimFilter;
//import tupleEnumerator.HybTupleEnumer;
import tupleEnumeratorOPT.HybTupleEnumer;

public class EdgeHomIEFltSim {

	Query mQuery;
	ArrayList<Pool> mPool;
	ArrayList<MatArray> mCandLists;
	BFLIndex mBFL;
	TimeTracker tt;

	GraphNode[] mGraNodes;
	FilterBuilder mFB;
	ArrayList<Integer> nodesTopoList;

	int passNum = 0;

	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	RoaringBitmap[] mBitsIdxArr;

	ArrayList<RoaringBitmap> mBitsByIDArr;
	double mTupleCount;
	boolean simfilter = true;

	HybTupleEnumer tenum;
	// HybTupleEnumCache tenum;

	public EdgeHomIEFltSim(Query query, GraphNode[] graNodes, FilterBuilder fb) {
		mQuery = query;
		mGraNodes = graNodes;
		mFB = fb;
		tt = new TimeTracker();

	}

	public boolean run(QueryEvalStat stat) throws LimitExceededException {
		mFB.oneRun();
		double prunetm = mFB.getBuildTime();
		mInvLstsByID = mFB.getInvLstsByID();
		RoaringBitmap[] mCandBitsArr = null;
		System.out.println("PrePrune time:" + prunetm + " sec.");

		if (simfilter) {
			mBitsByIDArr = mFB.getBitsByIDArr();
			tt.Start();

			if (mQuery.hasCycle) {
				CycSimFilter filter = new CycSimFilter(mQuery, mGraNodes, mInvLstsByID, mBitsByIDArr, true);
				filter.prune();
				mCandBitsArr = filter.getCandBits();
			} else {
				EdgeSimFilter filter = new EdgeSimFilter(mQuery, mGraNodes, mInvLstsByID, mBitsByIDArr, true);
				filter.prune();
				mCandBitsArr = filter.getCandBits();
			}

			prunetm += tt.Stop() / 1000;
			stat.setPreTime(prunetm);
			System.out.println("Prune time:" + prunetm + " sec.");
		}
		
		else
			mCandBitsArr= mFB.getCandBitsArr();

		stat.setTotNodesAfter(calTotCandSolnNodes(mCandBitsArr));

		tt.Start();
		EdgeAnsGraphBuilder agBuilder = new EdgeAnsGraphBuilder(mQuery, mGraNodes, mCandBitsArr);

		mPool = agBuilder.run();

		double buildtm = tt.Stop() / 1000;
		stat.calAnsGraphSize(mPool);
		stat.setMatchTime(buildtm);
		System.out.println("Answer graph build time:" + buildtm + " sec.");

		tt.Start();
		tenum = new HybTupleEnumer(mQuery, mPool);
		// tenum = new HybTupleEnumCache(mQuery, mPool);
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

	private double calTotCandSolnNodes(RoaringBitmap[] candBitsArr) {

		double totNodes = 0.0;

		for (RoaringBitmap bits : candBitsArr) {

			totNodes += bits.getCardinality();
		}
		return totNodes;
	}

	private double calTotInvNodes() {

		double totNodes_before = 0.0;

		for (QNode q : mQuery.nodes) {

			ArrayList<GraphNode> invLst = mInvLstsByID.get(q.lb);
			totNodes_before += invLst.size();
		}

		return totNodes_before;
	}

	private double calTotTreeSolns() {

		QNode root = mQuery.getSources().get(0);
		Pool rPool = mPool.get(root.id);
		double totTuples = 0;
		ArrayList<PoolEntry> elist = rPool.elist();
		for (PoolEntry r : elist) {

			totTuples += r.size();

		}
		System.out.println("total number of solution tuples: " + totTuples);
		return totTuples;

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
