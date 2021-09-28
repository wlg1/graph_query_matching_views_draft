package evaluator;

import java.util.ArrayList;
import java.util.Collections;

import org.roaringbitmap.RoaringBitmap;

import answerGraphOPT.HybAnsGraphBuilder;
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

public class DagHomIEFltSim {

	Query mQuery;
	BFLIndex mBFL;
	TimeTracker tt;
	GraphNode[] nodes;
	FilterBuilder mFB;
	ArrayList<Pool> mPool;
	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	ArrayList<RoaringBitmap> mBitsByIDArr;
	ArrayList<MatArray> mCandLists;
	ArrayList<RoaringBitmap> mCandBitsByIDArr;
	boolean simfilter = true;
	HybTupleEnumer tenum;
	// HybTupleEnumCache tenum;

	public DagHomIEFltSim(Query query, FilterBuilder fb, BFLIndex bfl) {

		mQuery = query;
		mBFL = bfl;
		nodes = mBFL.getGraphNodes();
		mFB = fb;
		tt = new TimeTracker();

	}
	

	
	public boolean run(QueryEvalStat stat) throws LimitExceededException {

		mFB.oneRun();
		double prunetm = mFB.getBuildTime();
		double totNodesFlt = mFB.getTotNodes();
		stat.setTotNodesFlt(totNodesFlt);
		stat.setTotNodesMV(totNodesFlt);
		stat.setFltTime(prunetm);
		
		
		System.out.println("FltPrune time:" + prunetm + " sec.");
		

		if (simfilter) {
			
			mInvLstsByID = mFB.getInvLstsByID();
			mBitsByIDArr = mFB.getBitsByIDArr();
			//SimFilterNav filter = new SimFilterNav(mQuery, nodes, mInvLstsByID, mBitsByIDArr, mBFL, true);
			//SimFilterBas filter = new SimFilterBas(mQuery, nodes, mInvLstsByID, mBitsByIDArr, mBFL, true);
			//GraSimFilterNaive filter = new GraSimFilterNaive(mQuery, nodes, mInvLstsByID, mBitsByIDArr, mBFL, true);
			//DagSimFilterNaive filter= new DagSimFilterNaive(mQuery, nodes, mInvLstsByID, mBitsByIDArr, mBFL, true);
			DagSimFilter filter = new DagSimFilter(mQuery, nodes, mInvLstsByID, mBitsByIDArr, mBFL, true);
			//DagSimMapFilter filter = new DagSimMapFilter(mQuery, nodes, mInvLstsByID, mBitsByIDArr, mBFL, true);
			tt.Start();
			filter.prune();
			double simtm = tt.Stop() / 1000;
			prunetm += simtm;
			stat.setSimTime(simtm);
			mCandLists = filter.getCandListByST();
			mCandBitsByIDArr = filter.getBitsByIDArr();
			print();
			System.out.println("Sim time:" + simtm + " sec.");
			System.out.println("Prune time:" + prunetm + " sec.");
		}
		else{
			mCandLists = mFB.getCandLists();
			mCandBitsByIDArr = mFB.getBitsByIDArr();
		}
		
		tt.Start();
		HybAnsGraphBuilder agBuilder = new HybAnsGraphBuilder(mQuery, mBFL, mCandLists, mCandBitsByIDArr);
		agBuilder.run();

		double buildtm = tt.Stop() / 1000;
		mPool = agBuilder.getAnsGraph();
		stat.setBuildTime(buildtm);
		stat.calAnsGraphSize(mPool);
		double totNodesSim = calTotCandSolnNodes();
		stat.setTotNodesSim(totNodesSim);
		
		System.out.println("Answer graph build time:" + buildtm + " sec.");

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

	private void print(){
		
		for (int i = 0; i < mQuery.V; i++) {
			QNode q = mQuery.nodes[i];
			ArrayList<GraphNode> list = mCandLists.get(i).elist();
			System.out.println("qid = " + q.id +" " +  " list = " +
			 this.mCandLists.get(q.id).elist().size() + "/" + this.mInvLstsByID.get(q.id).size());
		}
		
	//	ArrayList<GraphNode>  list = mCandLists.get(1).elist();
		
	//	for(GraphNode n:list){
			
	//		System.out.println(n);
	//	}
		
	}
	
	public void clear() {
		
		tt=null;
		if(mCandLists !=null){
			
			for(MatArray m: mCandLists)
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

	public static void main(String[] args) {

	}

}
