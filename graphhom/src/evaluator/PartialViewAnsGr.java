package evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.roaringbitmap.RoaringBitmap;

import answerGraph.HybAnsGraphBuilderViews;
import answerGraph.HybAnsGraphBuilderViews4;
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
import tupleEnumerator.HybTupleEnumer;
import views.nodeset;

public class PartialViewAnsGr {

	Query query;
	ArrayList<ArrayList<GraphNode>> mInvLsts;
	BFLIndex mBFL;
	TimeTracker tt;
	GraphNode[] nodes;
	FilterBuilder mFB;
	ArrayList<Pool> mPool;
	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	ArrayList<RoaringBitmap> mBitsByIDArr;
	boolean rmvEmpty;
	HybTupleEnumer tenum;
	boolean simfilter;
	ArrayList<Query> viewsOfQuery;
	Map<Integer, ArrayList<nodeset>> qid_Ansgr;
	HashMap<Integer, GraphNode> posToGN;
	HashMap<Integer, GraphNode> LintToGN;

	public PartialViewAnsGr(Query INquery, ArrayList<Query> viewsOfQuery_in,
			Map<Integer, ArrayList<nodeset>> qid_Ansgr_in, HashMap<Integer, GraphNode> INposToGN,
			FilterBuilder fb, BFLIndex bfl, boolean INrmvEmpty, boolean INsimfilter,
			HashMap<Integer, GraphNode> INLintToGN) {

		query = INquery;
		mBFL = bfl;
		nodes = mBFL.getGraphNodes();
		mFB = fb;
		tt = new TimeTracker();
		rmvEmpty = INrmvEmpty;
		simfilter = INsimfilter;
		viewsOfQuery = viewsOfQuery_in;
		qid_Ansgr = qid_Ansgr_in;
		posToGN = INposToGN;
		LintToGN = INLintToGN;

	}

	public boolean run(QueryEvalStat stat) throws LimitExceededException {

		mFB.oneRun();
		double prunetm = mFB.getBuildTime();
		stat.setPreTime(prunetm);
		
		System.out.println("PrePrune time:" + prunetm + " sec.");
		ArrayList<MatArray> mCandLists = null; 

		if (simfilter) {
			tt.Start();
			mInvLstsByID = mFB.getInvLstsByID();
			mBitsByIDArr = mFB.getBitsByIDArr();
			DagSimGraFilter filter = new DagSimGraFilter(query, nodes, mInvLstsByID, mBitsByIDArr, mBFL, true);
			filter.prune();
			mCandLists = filter.getCandList();
			prunetm += tt.Stop() / 1000;
			stat.setPreTime(prunetm);
			System.out.println("Prune time:" + prunetm + " sec.");
		}
		else
			mCandLists = mFB.getCandLists();
		
		tt = new TimeTracker();
		tt.Start();
//		ArrayList<Pool> partialPool;
//		ArrayList<QEdge> uncoveredEdges = new ArrayList<QEdge>();
		if (rmvEmpty) {
			HybAnsGraphBuilderViews4 BuildViews = new HybAnsGraphBuilderViews4(query, viewsOfQuery, qid_Ansgr, posToGN, 
					mCandLists, mBFL, nodes, LintToGN);	
//			partialPool = BuildViews.run();
//			uncoveredEdges = BuildViews.getUncoveredEdges();
			mPool = BuildViews.run();
		} else {
			HybAnsGraphBuilderViews BuildViews = new HybAnsGraphBuilderViews(query, viewsOfQuery, qid_Ansgr, posToGN);
//			partialPool = BuildViews.run(stat);
//			uncoveredEdges = BuildViews.getUncoveredEdges();
		}
		
		//send mPool to ViewsRIsumGraph. get the uncovered edges
		//this modifies partialPool globally as it turns it into mPool
//		ViewsRIsumGraph finishSG = new ViewsRIsumGraph(query, mBFL, mCandLists, partialPool, uncoveredEdges, posToGN);
//		mPool = finishSG.run();
		
		double buildtm = tt.Stop() / 1000;
		stat.setMatchTime(buildtm);
		stat.calAnsGraphSize(mPool);
		stat.setTotNodesAfter(calTotCandSolnNodes());
//		System.out.println("Answer graph build time:" + buildtm + " sec.");

		double numOutTuples;

		tt.Start();
		tenum = new HybTupleEnumer(query, mPool);
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

	private double calTotInvNodes() {

		double totNodes_before = 0.0;

		for (QNode q : query.nodes) {

			ArrayList<GraphNode> invLst = mInvLstsByID.get(q.lb);
			totNodes_before += invLst.size();
		}

		return totNodes_before;
	}

	private boolean descendantOnly() {
		QEdge[] edges = query.edges;
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

		QNode root = query.getSources().get(0);
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
