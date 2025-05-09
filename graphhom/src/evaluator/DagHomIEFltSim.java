package evaluator;

import java.util.ArrayList;

import org.roaringbitmap.RoaringBitmap;

import answerGraph.HybAnsGraphBuilder;
import dao.BFLIndex;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import global.Consts;
import global.Consts.AxisType;
import graph.GraphNode;
import helper.LimitExceededException;
import helper.QueryEvalStat;
import helper.TimeTracker;
import prefilter.FilterBuilder;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import simfilter.DagSimGraFilter;
import tupleEnumerator.HybTupleEnumCache;
import tupleEnumerator.HybTupleEnumer;

public class DagHomIEFltSim {

	Query mQuery;
	ArrayList<ArrayList<GraphNode>> mInvLsts;
	BFLIndex mBFL;
	TimeTracker tt;
	GraphNode[] nodes;
	FilterBuilder mFB;
	ArrayList<Pool> mPool;
	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	ArrayList<RoaringBitmap> mBitsByIDArr;
	boolean simfilter;
	HybTupleEnumer tenum;
	// HybTupleEnumCache tenum;

	public DagHomIEFltSim(Query query, FilterBuilder fb, BFLIndex bfl, boolean INsimfilter) {

		mQuery = query;
		mBFL = bfl;
		nodes = mBFL.getGraphNodes();
		mFB = fb;
		tt = new TimeTracker();
		simfilter = INsimfilter;

	}

	public boolean run(QueryEvalStat stat) throws LimitExceededException {

		mFB.oneRun();
		double prunetm = mFB.getBuildTime();
		// double totNodes_after = mFB.getTotNodes();
		stat.setPreTime(prunetm);
		// stat.setTotNodesAfter(totNodes_after);
		mInvLstsByID = mFB.getInvLstsByID();
		stat.nodesAfterPreFilt = calcTotNodesAfterPreFilt();	
		
//		System.out.println("PrePrune time:" + prunetm + " sec.");
		ArrayList<MatArray> mCandLists = null; 

		if (simfilter) {
			tt.Start();
			mInvLstsByID = mFB.getInvLstsByID();
			mBitsByIDArr = mFB.getBitsByIDArr();
			DagSimGraFilter filter = new DagSimGraFilter(mQuery, nodes, mInvLstsByID, mBitsByIDArr, mBFL, true);
			filter.prune();
			mCandLists = filter.getCandList();
//			prunetm += tt.Stop() / 1000;
			double simtm = tt.Stop() / 1000;
//			stat.setPreTime(prunetm);
			stat.setsimTime(simtm);
//			System.out.println("Prune time:" + prunetm + " sec.");
		}
		else
			mCandLists = mFB.getCandLists();
		tt.Start();
		HybAnsGraphBuilder agBuilder = new HybAnsGraphBuilder(mQuery, mBFL, mCandLists);
		mPool = agBuilder.run();

		double buildtm = tt.Stop() / 1000;
		stat.setMatchTime(buildtm);
		stat.calAnsGraphSize(mPool);
		stat.setTotNodesAfter(calTotCandSolnNodes());
		System.out.println("Answer graph build time:" + buildtm + " sec.");

		stat.nodesAfterVinter = stat.nodesAfterPreFilt;
		
		double numOutTuples;

		tt.Start();
		tenum = new HybTupleEnumer(mQuery, mPool);
		// tenum = new HybTupleEnumCache(mQuery, mPool);
		// if (descendantOnly() && mQuery.isTree()) {
		// numOutTuples = calTotTreeSolns();
		// } else
		numOutTuples = tenum.enumTuples();

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
	}


	public double getTupleCount() {

		if (tenum != null)
			return tenum.getTupleCount();
		return 0;
	}
	
	private double calcTotNodesAfterPreFilt() {
		double totNodes = 0.0;
		for (ArrayList<GraphNode> invL : mInvLstsByID) {
			totNodes += invL.size();

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

	private boolean descendantOnly() {
		QEdge[] edges = mQuery.edges;
		for (QEdge edge : edges) {
			AxisType axis = edge.axis;
			if (axis == Consts.AxisType.child) {

				return false;
			}

		}

		return true;

	}

	private double calTotCandSolnNodes() {

		double totNodes = 0.0;
		for (Pool pool : mPool) {
			ArrayList<PoolEntry> elist = pool.elist();
			totNodes += elist.size();

		}
		return totNodes;
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
