package views;

import java.util.ArrayList;
import java.util.HashMap;

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
import helper.TimeTracker;
import prefilter.FilterBuilder;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import simfilter.DagSimGraFilter;
import tupleEnumerator.HybTupleEnumer;

public class getAnsGrViews {

	Query mQuery;
	ArrayList<ArrayList<GraphNode>> mInvLsts;
	BFLIndex mBFL;
	TimeTracker tt;
	GraphNode[] nodes;
	FilterBuilder mFB;
	ArrayList<Pool> mPool;
	ArrayList<Pool> mPool_ansgr;
	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	ArrayList<RoaringBitmap> mBitsByIDArr;
	boolean simfilter = true;
	HybTupleEnumer tenum;
	ArrayList<MatArray> mCandLists;

	public getAnsGrViews(Query query, FilterBuilder fb, BFLIndex bfl) {

		mQuery = query;
		mBFL = bfl;
		nodes = mBFL.getGraphNodes();
		mFB = fb;
		tt = new TimeTracker();

	}

	public ArrayList<nodeset> run() throws LimitExceededException {

		mFB.oneRun();
		mCandLists = null; 

		if (simfilter) {
			tt.Start();
			mInvLstsByID = mFB.getInvLstsByID();
			mBitsByIDArr = mFB.getBitsByIDArr();
			DagSimGraFilter filter = new DagSimGraFilter(mQuery, nodes, mInvLstsByID, mBitsByIDArr, mBFL, true);
			filter.prune();
			mCandLists = filter.getCandList();
		}
		else
			mCandLists = mFB.getCandLists();
		
		////GET OCCURRENCE LISTS
		//create simulation graph object
		HybAnsGraphBuilder agBuilder = new HybAnsGraphBuilder(mQuery, mBFL, mCandLists);
		mPool = agBuilder.run();

		//run MIjoin to get answer
		double numOutTuples_0;
		tenum = new HybTupleEnumer(mQuery, mPool);
		numOutTuples_0 = tenum.enumTuples();
		
		//then get unique values in answer to get occ sets
		ArrayList<MatArray> mOcc;
		mOcc = tenum.getAnswer();
		
		//get answer graph using algo that outputs simulation graph
		tt.Start();
		HybAnsGraphBuilder agBuilder_2 = new HybAnsGraphBuilder(mQuery, mBFL, mOcc);
		mPool_ansgr = agBuilder_2.run();
		
//		clear();
		
		//return as adj lists. hash table where key is graph node ID, value is obj of 2 adj lists
		//for each pool entry, get their graph node ID
		//for each entry in poolentry's adj list, get its graph ID
		//put graph ID into adj list of graph node ID obj
		
		//create new attribute inside poolentry that stores the graphnode lists
		
		//an arraylist of nodesets, each with its own fwdadjlists for each graphnode in it
		ArrayList<nodeset> matView = new ArrayList<nodeset>();
		for (Pool pl : mPool_ansgr) {
			nodeset ns = new nodeset();
			for (PoolEntry pe : pl.elist()) {
				GraphNode gn = pe.mValue;
				ns.gnodes.add(gn);
				if (pe.mFwdEntries != null){
					HashMap<Integer, ArrayList<GraphNode>> fal = new HashMap<Integer, ArrayList<GraphNode>>();
//					HashMap<Integer, RoaringBitmap> fal = new HashMap<Integer, RoaringBitmap>();
					for (Integer key : pe.mFwdEntries.keySet()) {
						ArrayList<GraphNode> x = new ArrayList<GraphNode>();
//						RoaringBitmap x = new RoaringBitmap();
						ArrayList<PoolEntry> nodeFwd = pe.mFwdEntries.get(key);
						for (PoolEntry peTo : nodeFwd) {
							x.add(peTo.mValue);
//							x.add(peTo.mValue.pos);
						}
						fal.put(key, x);
					}
					ns.fwdAdjLists.put(gn.pos, fal);
				} else {
					ns.fwdAdjLists = (HashMap<Integer, HashMap<Integer, ArrayList<GraphNode>>>) null;
//					ns.fwdAdjLists = (HashMap<Integer, HashMap<Integer, RoaringBitmap>>) null;
				}
			}
			matView.add(ns);
		}
		
		return matView;

	}
	
	public ArrayList<MatArray> getCandLists(){
		return mCandLists;
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
