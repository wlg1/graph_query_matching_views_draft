package mv.queryEvaluator;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.roaringbitmap.RoaringBitmap;

import answerGraphOPT.HybAnsGraphBuilder;
import dao.BFLIndex;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import global.Consts;
import global.Consts.AxisType;
import graph.Digraph;
import graph.GraphNode;
import helper.LimitExceededException;
import helper.QueryEvalStat;
import helper.TimeTracker;
import mv.answerGraph.HybAGMVBuilder;
import mv.queryCovering.CoverResult;
import mv.simfilter.DagSimMVFilter;
import mv.viewManager.ViewManager;
import prefilter.FilterBuilder;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
//import simfilter.DagSimFilter;
import simfilterOPT.DagSimFilter;
//import simfilter.DagSimFilterNaive;
import simfilterOPT.DagSimFilterNaive;
//import simfilter.DagSimMapFilter;
import simfilterOPT.DagSimMapFilter;
//import simfilter.GraSimFilterNaive;
import simfilterOPT.GraSimFilterNaive;
//import simfilter.SimFilterBas;
import simfilterOPT.SimFilterBas;
//import simfilter.SimFilterNav;
import simfilterOPT.SimFilterNav;
//import tupleEnumerator.HybTupleEnumer;
import tupleEnumeratorOPT.HybTupleEnumer;

public class DagEvalMVFltSim {

	Query mQuery;

	BFLIndex mBFL;
	TimeTracker tt;
	Digraph mG;
	GraphNode[] mGraNodes;
	FilterBuilder mFB;
	ArrayList<Pool> mPool;

	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	ArrayList<RoaringBitmap> mBitsByIDArr;
	ArrayList<MatArray> mCandListByST;
	ArrayList<RoaringBitmap> mCandBitsByIDArr;
	HybTupleEnumer tenum;

	ArrayList<Query> mViews;
	ArrayList<ArrayList<Pool>> mViewPools;
	ArrayList<CoverResult> mCovers;

	ArrayList<RoaringBitmap> mViewBitsByIDArr;
	ArrayList<HashMap<Integer, HashSet<Integer>>> mCoverViewNodes;
	ArrayList<HashMap<Integer, ArrayList<Pool>>> mViewNodePools;


	public DagEvalMVFltSim(Query query, ArrayList<Query> views, Digraph g, BFLIndex bfl) {

		mQuery = query;
		mViews = views;
		mG = g;
		mBFL = bfl;
		mGraNodes = mBFL.getGraphNodes();
		tt = new TimeTracker();
		mCovers = ViewManager.genCovers(mViews, mQuery);
		mViewPools = ViewManager.genViewMat(mViews, mG, mBFL);
		mCoverViewNodes = ViewManager.genCoverViewNodes(mQuery, mCovers);
		mViewNodePools = ViewManager.genViewNodePools(mQuery, mCovers, mViewPools);
	}



	
	public boolean run(QueryEvalStat stat) throws LimitExceededException {
		mFB = new FilterBuilder(mG, mQuery);
		mFB.oneRun();
		double prunetm = mFB.getBuildTime();
		double totNodesFlt = mFB.getTotNodes();
		stat.setTotNodesFlt(totNodesFlt);
	
		stat.setFltTime(prunetm);

		System.out.println("Preflt time:" + prunetm + " sec.");

		mInvLstsByID = mFB.getInvLstsByID();
		mBitsByIDArr = mFB.getBitsByIDArr();
		
		System.out.println("Before ANDing with view mats.");
		print(mQuery, mInvLstsByID, mBitsByIDArr);
		// getQBits(mBitsByIDArr);
		System.out.println("generating candidates using views.");
		tt.Start();
		getQCands(mBitsByIDArr, mInvLstsByID);
		double candGentm = tt.Stop() / 1000;
		System.out.println("Candidates generating time:" + candGentm + " sec.");
		double totalNodesMV = getTotNodes(mBitsByIDArr);
		stat.setTotNodesMV(totalNodesMV);
		prunetm += candGentm;
		System.out.println("After ANDing with view mats.");
		print(mQuery, mInvLstsByID, mBitsByIDArr);

		// SimFilterNav filter = new SimFilterNav(mQuery, mGraNodes, mInvLstsByID,
		// mBitsByIDArr, mBFL, true);
		// SimFilterBas filter = new SimFilterBas(mQuery, mGraNodes, mInvLstsByID,
		// mBitsByIDArr, mBFL, true);
		// GraSimFilterNaive filter = new GraSimFilterNaive(mQuery, mGraNodes,
		// mInvLstsByID, mBitsByIDArr, mBFL, true);
		// DagSimFilterNaive filter= new DagSimFilterNaive(mQuery, mGraNodes,
		// mInvLstsByID, mBitsByIDArr, mBFL, true);
		//DagSimFilter filter = new DagSimFilter(mQuery, mGraNodes, mInvLstsByID, mBitsByIDArr, mBFL, true);
		//DagSimMapFilter filter = new DagSimMapFilter(mQuery, mGraNodes, mInvLstsByID,	mBitsByIDArr, mBFL, true);
		
		DagSimMVFilter filter = new DagSimMVFilter(mQuery, mGraNodes,mInvLstsByID,mBitsByIDArr, mBFL, mViews, mViewPools,
				mCovers, mViewNodePools, mCoverViewNodes, true);
	
		tt.Start();
		filter.prune();
		double simtm = tt.Stop() / 1000;
		System.out.println("Sim time:" + simtm + " sec.");
		prunetm += simtm;
		stat.setSimTime(simtm);
		mCandListByST = filter.getCandListByST();
		mCandBitsByIDArr = filter.getBitsByIDArr();
		print();
		System.out.println("Prune time:" + prunetm + " sec.");

		tt.Start();
		
		HybAGMVBuilder agBuilder = new HybAGMVBuilder(mQuery, mGraNodes, mBFL, mCandListByST, mCandBitsByIDArr, mViews,
				mViewPools, mCovers, mViewNodePools, mCoverViewNodes);
		// HybAnsGraphBuilder agBuilder = new HybAnsGraphBuilder(mQuery, mBFL,
		// mCandListByST, mCandBitsByIDArr);
		agBuilder.run();

		double buildtm = tt.Stop() / 1000;
		mPool = agBuilder.getAnsGraph();
		stat.setBuildTime(buildtm);
		stat.calAnsGraphSize(mPool);
		stat.setTotNodesSim(calTotCandSolnNodes());
		System.out.println("Answer graph build time:" + buildtm + " sec.");

		double numOutTuples;

		tenum = new HybTupleEnumer(mQuery, mPool);
		tt.Start();
		
		numOutTuples = tenum.enumTuples();

		double enumtm = tt.Stop() / 1000;
		stat.setEnumTime(enumtm);
		System.out.println("Tuple enumeration time:" + enumtm + " sec.");

		stat.setNumSolns(numOutTuples);
		clear();

		return true;

	}

	private ArrayList<MatArray> getCandListByST() {
		ArrayList<MatArray> candLists = new ArrayList<MatArray>(mQuery.V);
		for (QNode q : mQuery.nodes) {
			// in the order of qid
			ArrayList<GraphNode> list = this.mInvLstsByID.get(q.lb);
			MatArray matArr = new MatArray();
			matArr.addList(list);
			Collections.sort(matArr.elist());
			candLists.add(matArr);

		}
		return candLists;
	}

	private void print(Query query, ArrayList<ArrayList<GraphNode>> invLstsByID, ArrayList<RoaringBitmap> bitsByIDArr) {

		for (int i = 0; i < query.V; i++) {
			QNode q = query.nodes[i];
			//System.out.println("qid = " + q.id + " " + " list = " + bitsByIDArr.get(q.id).getCardinality() + "/"
			//		+ invLstsByID.get(q.id).size());
			System.out.println("qid = " + q.id + " " + " list = " + bitsByIDArr.get(q.id).getCardinality());
		}

	}

	private void print() {

		for (int i = 0; i < mQuery.V; i++) {
			QNode q = mQuery.nodes[i];
			ArrayList<GraphNode> list = mCandListByST.get(i).elist();
			System.out.println("qid = " + q.id + " " + " list = " + this.mCandListByST.get(q.id).elist().size() + "/"
					+ this.mInvLstsByID.get(q.id).size());
		}

		// ArrayList<GraphNode> list = mCandLists.get(1).elist();

		// for(GraphNode n:list){

		// System.out.println(n);
		// }

	}

	public void clear() {

		tt = null;
		if (mCandListByST != null) {

			for (MatArray m : mCandListByST)
				m.clear();
		}

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

	public void printSolutions(ArrayList<PoolEntry> elist) {

		if (elist.isEmpty())
			return;

		for (PoolEntry r : elist) {

			System.out.println(r);

		}

	}

	private void getQCands(ArrayList<RoaringBitmap> bitsByIDArr, ArrayList<ArrayList<GraphNode>> invLstsByID) {

		for (CoverResult cover : mCovers) {

			int vid = cover.getViewId();
			BitSet coverBits = cover.getCoverBits();

			ArrayList<Pool> vpList = mViewPools.get(vid);
			for (int qid = 0; qid < mQuery.V; qid++) {
				if (!coverBits.get(qid))
					continue;
				RoaringBitmap qBits = bitsByIDArr.get(qid);
				int card1 = qBits.getCardinality();
				// System.out.println("Before Anding, qid=" + qid + ", card= " + card1);

				HashSet<Integer> vnodesList = cover.getVNodeList(qid);
				for (int vnid : vnodesList) {
					Pool vp = vpList.get(vnid);
					RoaringBitmap vBits = vp.getIDBits();
					qBits.and(vBits);
				}
				int card2 = qBits.getCardinality();
				// System.out.println("After Anding, qid=" + qid + ", card= " + card2);
				if (card1 > card2) {
					ArrayList<GraphNode> qList = invLstsByID.get(qid);
					qList.clear();
					qList = bits2list(qBits);
					invLstsByID.set(qid, qList);
				}
			}

		}
	}

	private void getQBits(ArrayList<RoaringBitmap> bitsByIDArr) {

		for (CoverResult cover : mCovers) {

			int vid = cover.getViewId();
			ArrayList<Pool> vpList = mViewPools.get(vid);
			for (int qid = 0; qid < mQuery.V; qid++) {
				RoaringBitmap qBits = bitsByIDArr.get(qid);
				System.out.println("Before Anding, qid=" + qid + ", card= " + qBits.getCardinality());

				HashSet<Integer> vnodesList = cover.getVNodeList(qid);
				for (int vnid : vnodesList) {
					Pool vp = vpList.get(vnid);
					RoaringBitmap vBits = vp.getIDBits();
					qBits.and(vBits);
				}
				System.out.println("After Anding, qid=" + qid + ", card= " + qBits.getCardinality());

			}

		}
	}

	private ArrayList<GraphNode> bits2list(RoaringBitmap bits) {

		ArrayList<GraphNode> list = new ArrayList<GraphNode>();
		for (int i : bits) {

			list.add(mGraNodes[i]);
		}

		return list;

	}
	
	private double getTotNodes(ArrayList<RoaringBitmap> bitsByIDArr) {

		double totNodes = 0.0;
		for (int i = 0; i < mQuery.V; i++) {
			RoaringBitmap bits = bitsByIDArr.get(i);
			totNodes += bits.getCardinality();
		}

		return totNodes;
	}

	public static void main(String[] args) {

	}

}
