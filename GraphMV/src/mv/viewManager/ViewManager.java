package mv.viewManager;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;

import org.roaringbitmap.RoaringBitmap;

import answerGraphOPT.HybAnsGraphBuilder;
import dao.BFLIndex;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import graph.Digraph;
import graph.GraphNode;
import helper.TimeTracker;
import mv.queryCovering.CoverResult;
import mv.queryCovering.QueryCoverHandler;
import prefilter.FilterBuilder;
import query.graph.Query;
import query.graph.QueryParser;
import simfilterOPT.DagSimFilter;

public class ViewManager {

	// absolute file name
	public static ArrayList<Query> readViews(String viewFileN) {

		ArrayList<Query> views = new ArrayList<Query>();
		Query view = null;
		QueryParser queryParser = new QueryParser(viewFileN);

		int vid = 0;
		while ((view = queryParser.readNextQuery()) != null) {

			System.out.println(view);
			view.Qid = vid++;
			views.add(view);
		}

		return views;
	}

	public static ArrayList<CoverResult> genCovers(ArrayList<Query> views, Query query) {

		ArrayList<CoverResult> covers = new ArrayList<CoverResult>();

		for (Query view : views) {

			QueryCoverHandler qch = new QueryCoverHandler(view, query);
			qch.run();
			CoverResult cover = qch.getCover();
			covers.add(cover);
			cover.print();
		}

		return covers;
	}

	public static ArrayList<ArrayList<Pool>> genViewMat(ArrayList<Query> views, Digraph g, BFLIndex bfl){
		
		ArrayList<ArrayList<Pool>> pools = new ArrayList<ArrayList<Pool>>();
		
		for(Query view:views) {
			System.out.println("Generating view materialization for view " + view.Qid);
			ArrayList<Pool> pl = genViewMat(view, g, bfl);
			pools.add(pl);		
		}
		
		return pools;		
	}
	
	public static BitSet getCoveredEdges(Query query, ArrayList<CoverResult> covers) {
		
		
	    BitSet coverBits = new BitSet();
	    for(CoverResult cover:covers) {
		
			coverBits.or(cover.getEdgeCoverBits());
		}
	    
	    return coverBits;
	}
	
    public static BitSet getCoveredNodes(Query query, ArrayList<CoverResult> covers) {
		
		
	    BitSet coverBits = new BitSet();
	    for(CoverResult cover:covers) {
		
			coverBits.or(cover.getCoverBits());
		}
	    
	    return coverBits;
	}
	
	public static HashSet<Integer> getUncoveredQids(Query query, ArrayList<CoverResult> covers){
		
	    CoverResult cover = null;
	    BitSet coverBits = new BitSet();
		
		for(int i=0; i< covers.size(); i++) {
			cover = covers.get(i);
			coverBits.or(cover.getCoverBits());
		}
		
		HashSet<Integer> ids = new HashSet<Integer>(query.V);
		for(int i=0; i<query.V; i++) {
			if(!coverBits.get(i))
			   ids.add(i);
		}
		
		return ids;
	}
	
	public static ArrayList<Pool> genViewMat(Query view, Digraph g, BFLIndex bfl) {
		
		
	    FilterBuilder pf = new FilterBuilder(g, view);
		
		pf.oneRun();
		double prunetm = pf.getBuildTime();
		double totNodes_after = pf.getTotNodes();
		System.out.println("View prefiltering time:" + prunetm + " sec.");
		
		ArrayList<ArrayList<GraphNode>> invLstsByID= pf.getInvLstsByID();
		ArrayList<RoaringBitmap> viewBitsByIDArr = pf.getBitsByIDArr();
		
		TimeTracker tt = new TimeTracker();
		
		DagSimFilter sf = new DagSimFilter(view, g.getNodes(), invLstsByID, viewBitsByIDArr, bfl, true);
		
		tt.Start();
		sf.prune();
		prunetm += tt.Stop() / 1000;
		ArrayList<MatArray> candLists = 	sf.getCandListByST();
		ArrayList<RoaringBitmap> candBitsByIDArr = sf.getBitsByIDArr();
		System.out.println("Prune time:" + prunetm + " sec.");
		tt.Start();
		HybAnsGraphBuilder agBuilder = new HybAnsGraphBuilder(view, bfl, candLists, candBitsByIDArr);
		agBuilder.run();
		
		double buildtm = tt.Stop() / 1000;
		System.out.println("Answer graph build time:" + buildtm + " sec.");

		// pool entries are sorted by ID
		ArrayList<Pool> pools = agBuilder.getAnsGraph();
	    return pools;
	}
	
	
	
		
	public static ArrayList<HashMap<Integer, HashSet<Integer>>> genCoverViewNodes(Query query, ArrayList<CoverResult> covers){
		
		ArrayList<HashMap<Integer, HashSet<Integer>>> coverViewNodes = new ArrayList<HashMap<Integer, HashSet<Integer>>>(query.V);
		for (int i = 0; i < query.V; i++) {
			HashMap<Integer, HashSet<Integer>> cvns = getCoverViewNodes(i,covers);
			coverViewNodes.add(cvns);
		}
	
	    return coverViewNodes;
	}
	
	private static HashMap<Integer, HashSet<Integer>> getCoverViewNodes(int qid, ArrayList<CoverResult> covers) {

		// key: view id, value: a list of view nodes
		HashMap<Integer, HashSet<Integer>> cvsHT = new HashMap<Integer, HashSet<Integer>>();

		for (CoverResult cover : covers) {
			int vid = cover.getViewId();
			BitSet coverBits = cover.getCoverBits();
			if (coverBits.get(qid)) {
				HashSet<Integer> vns = cover.getVNodeList(qid);
				cvsHT.put(vid, vns);
			}

		}

		return cvsHT;

	}

	
	public static ArrayList<HashMap<Integer, ArrayList<Pool>>> genViewNodePools(Query query, ArrayList<CoverResult> covers, ArrayList<ArrayList<Pool>> viewPools){
		
		ArrayList<HashMap<Integer, ArrayList<Pool>>> viewNodePools = new ArrayList<HashMap<Integer, ArrayList<Pool>>>(query.V);
		
		for (int i = 0; i < query.V; i++) {
			HashMap<Integer, ArrayList<Pool>> vnpls = getViewNodePools(i, covers, viewPools);
			viewNodePools.add(vnpls);
		}
		return viewNodePools;
	}
		
	
	private static HashMap<Integer, ArrayList<Pool>> getViewNodePools(int qid, ArrayList<CoverResult> covers, ArrayList<ArrayList<Pool>> viewPools) {

		// key: view id, value: a list of view node pools
		HashMap<Integer, ArrayList<Pool>> vplHT = new HashMap<Integer, ArrayList<Pool>>();

		for (CoverResult cover : covers) {
			int vid = cover.getViewId();
			BitSet coverBits = cover.getCoverBits();
			if (!coverBits.get(qid)) {
				continue;
			}
			ArrayList<Pool> vpls = viewPools.get(vid);
			HashSet<Integer> vns = cover.getVNodeList(qid);
			ArrayList<Pool> vnpls = new ArrayList<Pool>(vns.size());
			vplHT.put(vid, vnpls);
			for (int vn : vns) {
				Pool vpl = vpls.get(vn);
				vnpls.add(vpl);
			}

		}

		return vplHT;
	}

	
	private double calTotCandSolnNodes(ArrayList<Pool> pool) {

		double totNodes = 0.0;
		for (Pool pl : pool) {
			ArrayList<PoolEntry> elist = pl.elist();
			totNodes += elist.size();

		}
		return totNodes;
	}

	public static void main(String[] args) {

	}

}
