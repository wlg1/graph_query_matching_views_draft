package evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.roaringbitmap.RoaringBitmap;

import answerGraph.HybAnsGraphBuilderViews;
import answerGraph.HybAnsGraphBuilderViewsUNCOVprefilt;
import answerGraph.HybAnsGraphBuilderViewsUNCOVprefilt2;
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
import query.graph.TransitiveReduction;
import simfilter.DagSimGraFilter;
import tupleEnumerator.HybTupleEnumer;
import views.nodeset;

public class PartialViewAnsGrUNCOVprefilt {

	Query query;
	BFLIndex mBFL;
	TimeTracker tt;
	GraphNode[] nodes;
	FilterBuilder mFB;
	ArrayList<Pool> mPool;
	ArrayList<RoaringBitmap> mBitsByIDArr;
	boolean rmvEmpty;
	HybTupleEnumer tenum;
	boolean simfilter;
	ArrayList<Query> viewsOfQuery;
	Map<Integer, ArrayList<nodeset>> qid_Ansgr;
	HashMap<Integer, GraphNode> LintToGN;
	HashMap<String, Integer> l2iMap;
	ArrayList<ArrayList<GraphNode>> invLsts;

	public PartialViewAnsGrUNCOVprefilt(Query INquery, ArrayList<Query> viewsOfQuery_in,
			Map<Integer, ArrayList<nodeset>> qid_Ansgr_in, HashMap<Integer, GraphNode> INLintToGN,
			FilterBuilder fb, BFLIndex bfl, boolean INrmvEmpty, boolean INsimfilter, 
			HashMap<String, Integer> INl2iMap, ArrayList<ArrayList<GraphNode>> INinvLsts) {

		query = INquery;
		mBFL = bfl;
		nodes = mBFL.getGraphNodes();
		mFB = fb;
		tt = new TimeTracker();
		rmvEmpty = INrmvEmpty;
		simfilter = INsimfilter;
		viewsOfQuery = viewsOfQuery_in;
		qid_Ansgr = qid_Ansgr_in;
		LintToGN = INLintToGN;
		l2iMap = INl2iMap;
		invLsts = INinvLsts;

	}

	public boolean run(QueryEvalStat stat) throws LimitExceededException {
		
		tt = new TimeTracker();
//		tt.Start();
		
		double prunetm;
//		mFB.oneRun();
//		invLsts = mFB.getInvLstsByID();
//		prunetm = mFB.getBuildTime();  //if using FLT
//		stat.nodesAfterPreFilt = calcTotNodesAfterPreFilt();
		
		prunetm = 0;  //all nodes covered
		
		if (rmvEmpty) {
			HybAnsGraphBuilderViewsUNCOVprefilt2 BuildViews = new HybAnsGraphBuilderViewsUNCOVprefilt2(query, viewsOfQuery, qid_Ansgr, LintToGN,
					mBFL, nodes, prunetm, invLsts, stat);
			mPool = BuildViews.run();
		} else {
			HybAnsGraphBuilderViews BuildViews = new HybAnsGraphBuilderViews(query, viewsOfQuery, qid_Ansgr, LintToGN);
		}
		
//		double buildtm = tt.Stop() / 1000;
//		stat.setMatchTime(buildtm);
		stat.calAnsGraphSize(mPool);
		stat.setTotNodesAfter(calTotCandSolnNodes());

		double numOutTuples;
		tt.Start();
		tenum = new HybTupleEnumer(query, mPool);
		numOutTuples = tenum.enumTuples();

		double enumtm = tt.Stop() / 1000;
		stat.setEnumTime(enumtm);
//		System.out.println("Tuple enumeration time:" + enumtm + " sec.");

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
		for (ArrayList<GraphNode> invL : invLsts) {
			totNodes += invL.size();

		}
		return totNodes;
	}

	private double calTotCandSolnNodes() {

		double totNodes = 0.0;
		for (Pool pool : mPool) {
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
