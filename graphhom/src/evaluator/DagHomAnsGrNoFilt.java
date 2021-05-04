package evaluator;

import java.util.ArrayList;
import java.util.Collections;

import org.roaringbitmap.RoaringBitmap;
import answerGraph.HybAnsGraphBuilder;
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
import simfilter.DagSimGraFilter;
import tupleEnumerator.HybTupleEnumCache;
import tupleEnumerator.HybTupleEnumer;

public class DagHomAnsGrNoFilt {

	Query mQuery;
	ArrayList<Pool> mPool;
	ArrayList<Pool> mPool_ansgr;
	ArrayList<MatArray> mCandLists;

	BFLIndex mBFL;
	TimeTracker tt;

	GraphNode[] mGraNodes;
	// Iterable<Integer> nodesOrder;

	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	ArrayList<RoaringBitmap> mBitsByIDArr;

	double numOutTuples;
	boolean simfilter = false;

	HybTupleEnumer tenum;
	HybTupleEnumer tenum_2;

	// query is a dag

	public DagHomAnsGrNoFilt(Query query, ArrayList<ArrayList<GraphNode>> invLstsByID, ArrayList<RoaringBitmap> bitsByIDArr,
			BFLIndex bfl) {

		mQuery = query;
		mBFL = bfl;
		mGraNodes = mBFL.getGraphNodes();
		mBitsByIDArr = bitsByIDArr;
		mInvLstsByID = invLstsByID;
		tt = new TimeTracker();

	}

	public boolean run(QueryEvalStat stat) throws LimitExceededException {

		stat.setTotNodesBefore(calTotInvNodes());
		if (simfilter) {
			tt.Start();
			// prune();
			// DagSimFilter filter = new DagSimFilter(mQuery, mInvLsts, mBFL);
			// mCandLists = filter.prune();;
			// DagSimFilterBits filter = new DagSimFilterBits(mQuery, mInvLsts,
			// mBFL);
			// DagSimFilterHyb filter = new DagSimFilterHyb(mQuery, mGraNodes,
			// mInvLstsByID, mBitsByIDArr, mBFL);

			DagSimGraFilter filter = new DagSimGraFilter(mQuery, mGraNodes, mInvLstsByID, mBitsByIDArr, mBFL);

			// GraSimGraFilter filter = new GraSimGraFilter(mQuery, mGraNodes,
			// mInvLstsByID, mBitsByIDArr, mBFL);
			filter.prune();

			// GraSimFilterBits filter = new GraSimFilterBits(mQuery, mInvLsts,
			// mBFL);
			// filter.filter();
			mCandLists = filter.getCandList();

			// SimFilter filter = new SimFilter(mQuery, mInvLsts, mBFL);
			// mCandLists = filter.prune();

			double prunetm = tt.Stop() / 1000;
			stat.setPreTime(prunetm);
			System.out.println("Prune time:" + prunetm + " sec.");
		} else
			mCandLists = getCandList();

		////GET OCCURRENCE LISTS
		tt.Start();
		//create simulation graph object
		HybAnsGraphBuilder agBuilder = new HybAnsGraphBuilder(mQuery, mBFL, mCandLists);
		mPool = agBuilder.run();

		//run MIjoin to get answer
		tenum = new HybTupleEnumer(mQuery, mPool);
		double numOutTuples_0 = tenum.enumTuples();
		
		//then get unique values in answer to get occ sets
		ArrayList<MatArray> mOcc;
		mOcc = tenum.getAnswer();
		
		//get answer graph using algo that outputs simulation graph
//		tt.Start();
		HybAnsGraphBuilder agBuilder_2 = new HybAnsGraphBuilder(mQuery, mBFL, mOcc);
		mPool_ansgr = agBuilder_2.run();
		
		double buildtm = tt.Stop() / 1000;
		stat.setMatchTime(buildtm);
		stat.calAnsGraphSize(mPool_ansgr);
		stat.setTotNodesAfter(calTotCandSolnNodes());
		System.out.println("Answer graph build time:" + buildtm + " sec.");

		//run MIjoin using occurrence lists to get answer again
		double numOutTuples;
		tt.Start();
		tenum_2 = new HybTupleEnumer(mQuery, mPool_ansgr);
		numOutTuples = tenum_2.enumTuples();
		
		double enumtm = tt.Stop() / 1000;
		stat.setEnumTime(enumtm);
		System.out.println("Tuple enumeration time:" + enumtm + " sec.");

		stat.setNumSolns(numOutTuples);
		clear();

		return true;
	}

	public void clear() {
		if (mPool != null)
			for (Pool p : mPool)
				p.clear();
		
		if (mPool_ansgr != null)
			for (Pool p : mPool_ansgr)
				p.clear();
		
	}

	public double getTupleCount() {

		if (tenum != null)
			return tenum.getTupleCount();
		return 0;
	}

	private ArrayList<MatArray> getCandList() {
		mCandLists = new ArrayList<MatArray>(mQuery.V);
		for (QNode q : mQuery.nodes) {
			// in the order of qid
			ArrayList<GraphNode> list = this.mInvLstsByID.get(q.lb);
			MatArray matArr = new MatArray();
			matArr.addList(list);
			Collections.sort(matArr.elist());
			mCandLists.add(matArr);

		}
		return mCandLists;
	}

	private double calTotInvNodes() {

		double totNodes_before = 0.0;

		for (QNode q : mQuery.nodes) {

			ArrayList<GraphNode> invLst = mInvLstsByID.get(q.lb);
			totNodes_before += invLst.size();
		}

		return totNodes_before;
	}

	private double calTotCandSolnNodes() {

		double totNodes = 0.0;
		for (Pool pool : mPool_ansgr) {
			ArrayList<PoolEntry> elist = pool.elist();
			totNodes += elist.size();

		}
		return totNodes;
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
