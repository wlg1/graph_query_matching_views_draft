package views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.roaringbitmap.RoaringBitmap;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

import answerGraph.HybAnsGraphBuilder;
import dao.BFLIndex;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import global.Consts;
import global.Consts.AxisType;
import global.Consts.status_vals;
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
	public HashMap<Integer, GraphNode> posToGN;
	public HashMap<Integer, GraphNode> LintToGN;
	boolean useAnsGr;

	public getAnsGrViews(Query query, FilterBuilder fb, BFLIndex bfl, HashMap<Integer, GraphNode> INposToGN, boolean INuseAnsGr,
			HashMap<Integer, GraphNode> INLintToGN) {

		mQuery = query;
		mBFL = bfl;
		nodes = mBFL.getGraphNodes();
		mFB = fb;
		posToGN = INposToGN;
		tt = new TimeTracker();
		useAnsGr = INuseAnsGr;
		LintToGN = INLintToGN;
	}

	public ArrayList<nodeset> run(QueryEvalStat stat) throws Exception {

		mFB.oneRun();
		double prunetm = mFB.getBuildTime();
		stat.setPreTime(prunetm);
		mCandLists = null; 

		if (simfilter) {
			tt.Start();
			mInvLstsByID = mFB.getInvLstsByID();
			mBitsByIDArr = mFB.getBitsByIDArr();
			DagSimGraFilter filter = new DagSimGraFilter(mQuery, nodes, mInvLstsByID, mBitsByIDArr, mBFL, true);
			filter.prune();
			mCandLists = filter.getCandList();
			prunetm += tt.Stop() / 1000;
			stat.setPreTime(prunetm);
		}
		else
			mCandLists = mFB.getCandLists();
		
		////GET OCCURRENCE LISTS
		//create simulation graph object
		HybAnsGraphBuilder agBuilder = new HybAnsGraphBuilder(mQuery, mBFL, mCandLists);
		tt.Start();
		if (useAnsGr) {
			mPool = agBuilder.run();

			//run MIjoin to get answer
			getAnswer();
			
			//then get unique values in answer to get occ sets
			ArrayList<MatArray> mOcc;
			mOcc = tenum.getAnswer();
			
			//get answer graph using algo that outputs simulation graph
			tt.Start();
			HybAnsGraphBuilder agBuilder_2 = new HybAnsGraphBuilder(mQuery, mBFL, mOcc);
			mPool_ansgr = agBuilder_2.run();
		} else {
			mPool_ansgr = agBuilder.run();
		}
		
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
//				posToGN.put(gn.pos, gn);
				LintToGN.put(gn.L_interval.mStart, gn);
//				ns.gnodesBits.add(gn.pos);
				ns.gnodesBits.add(gn.L_interval.mStart);
				if (pe.mFwdEntries != null){
					HashMap<Integer, RoaringBitmap> fal = new HashMap<Integer, RoaringBitmap>();
					for (Integer key : pe.mFwdEntries.keySet()) {
						RoaringBitmap newBitmap = new RoaringBitmap();
						ArrayList<PoolEntry> nodeFwd = pe.mFwdEntries.get(key);
						for (PoolEntry peTo : nodeFwd) {
//							newBitmap.add(peTo.mValue.pos);
							newBitmap.add(peTo.mValue.L_interval.mStart);
						}
						fal.put(key, newBitmap);
					}
//					ns.fwdAdjLists.put(gn.pos, fal);
					ns.fwdAdjLists.put(gn.L_interval.mStart, fal);
				} else {
					ns.fwdAdjLists = (HashMap<Integer, HashMap<Integer, RoaringBitmap>>) null;
				}
			}
			matView.add(ns);
		}
		
		double buildtm = tt.Stop() / 1000;
		stat.setMatchTime(buildtm);
		stat.calAnsGraphSize(mPool_ansgr);
		stat.setTotNodesAfter(calTotCandSolnNodes());
		
		clear();
		return matView;
	}
	
	public boolean getAnswer() throws Exception {
		java.util.concurrent.ExecutorService executor = Executors.newSingleThreadExecutor();
		SimpleTimeLimiter timeout = new SimpleTimeLimiter(executor);
		try {
			tt.Start();
			timeout.callWithTimeout(new Callable<Boolean>() {
				public Boolean call() throws Exception {
					double numOutTuples_0;
					tenum = new HybTupleEnumer(mQuery, mPool);
					numOutTuples_0 = tenum.enumTuples();
					return true;
				}
			}, Consts.TimeLimit, TimeUnit.MINUTES, false);
		} catch (LimitExceededException e) {
			System.err.println("Exceed Output Limit!");
		}
		return true;
	}
	
	private double calTotCandSolnNodes() {

		double totNodes = 0.0;
		for (Pool pool : mPool_ansgr) {
			ArrayList<PoolEntry> elist = pool.elist();
			totNodes += elist.size();

		}
		return totNodes;
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
