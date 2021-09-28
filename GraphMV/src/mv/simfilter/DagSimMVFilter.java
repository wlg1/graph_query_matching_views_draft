package mv.simfilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.roaringbitmap.FastAggregation;
import org.roaringbitmap.RoaringBatchIterator;
import org.roaringbitmap.RoaringBitmap;

import dao.BFLIndex;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import global.Consts;
import global.Consts.AxisType;
import global.Flags;
import graph.GraphNode;
import helper.TimeTracker;
import mv.queryCovering.CoverResult;
import mv.viewManager.ViewManager;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import query.graph.QueryHandler;

public class DagSimMVFilter {

	Query mQuery;
	ArrayList<MatArray> mCandLists;
	BFLIndex mBFL;
	ArrayList<Integer> nodesTopoList;
	int passNum = 0;
	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	ArrayList<RoaringBitmap> mBitsByIDArr;
	RoaringBitmap[] mCandBitsArr;
	GraphNode[] mGraNodes;
	boolean invLstByQuery = false;

	ArrayList<Query> mViews;
	ArrayList<ArrayList<Pool>> mViewPools;
	ArrayList<CoverResult> mCovers;
	BitSet mEdgeCoverBits;
	// indexed by query node id, hashtable is keyed by view id, values are the set
	// of view nodes covering the query id.
	ArrayList<HashMap<Integer, HashSet<Integer>>> mCoverViewNodes;
	ArrayList<HashMap<Integer, ArrayList<Pool>>> mViewNodePools;

	public DagSimMVFilter(Query query, GraphNode[] graNodes, ArrayList<ArrayList<GraphNode>> invLstsByID,
			ArrayList<RoaringBitmap> bitsByIDArr, BFLIndex bfl, ArrayList<Query> views, ArrayList<ArrayList<Pool>> vpls,
			ArrayList<CoverResult> cvs, ArrayList<HashMap<Integer, ArrayList<Pool>>> viewNodePools,
			ArrayList<HashMap<Integer, HashSet<Integer>>> coverViewNodes, boolean invLstByQuery) {

		mQuery = query;
		mBFL = bfl;
		mInvLstsByID = invLstsByID;
		mGraNodes = graNodes;
		mBitsByIDArr = bitsByIDArr;
		mViewPools = vpls;
		mCovers = cvs;
		mViews = views;
		mCoverViewNodes = coverViewNodes;
		mViewNodePools = viewNodePools;
		mEdgeCoverBits = ViewManager.getCoveredEdges(mQuery, mCovers);
		this.invLstByQuery = invLstByQuery;

		init();
	}

	public void prune() {
		boolean[] changed = new boolean[mQuery.V];
		passNum = 0;
		Arrays.fill(changed, true);

		boolean hasChange = pruneBUP(changed);

		do {
			if (Flags.PRUNELIMIT && passNum > Consts.PruneLimit)
				break;
			hasChange = pruneTDW(changed);
			if (hasChange) {
				hasChange = pruneBUP(changed);

			}

		} while (hasChange);

		System.out.println("Total passes: " + passNum);

	}

	public ArrayList<MatArray> getCandListByST() {
		ArrayList<MatArray> candListByST = new ArrayList<MatArray>(mQuery.V);

		for (int i = 0; i < mQuery.V; i++) {
			QNode q = mQuery.nodes[i];
			ArrayList<GraphNode> list = mCandLists.get(i).elist();
			Collections.sort(list);
			MatArray ma = new MatArray(list);
			candListByST.add(ma);
		}
		return candListByST;
	}

	public ArrayList<MatArray> getCandListByID() {

		return mCandLists;
	}

	public ArrayList<RoaringBitmap> getBitsByIDArr() {

		ArrayList<RoaringBitmap> bitsByIDArr = new ArrayList<RoaringBitmap>(mQuery.V);

		for (int i = 0; i < mQuery.V; i++) {

			// System.out.println("bits card=" + mCandBitsArr[i].getCardinality() + ", list
			// len =" + mCandLists.get(i).elist().size());
			bitsByIDArr.add(mCandBitsArr[i]);
		}
		return bitsByIDArr;

	}

	private boolean pruneBUP(boolean[] changed) {

		boolean hasChange = false;
		// System.out.println("Passnum=" + passNum);
		// System.out.println("Before pruneBUP");
		// printCard();
		for (int i = mQuery.V - 1; i >= 0; i--) {
			int qid = nodesTopoList.get(i);
			boolean result = pruneOneStepBUP(qid, changed);
			hasChange = hasChange || result;

		}
		passNum++;
		// System.out.println("After pruneBUP");

		// printCard();

		return hasChange;
	}

	private void printCard() {

		for (int i = 0; i < mQuery.V; i++) {

			System.out.println("card(" + i + ")=" + this.mCandBitsArr[i].getCardinality());

		}
	}

	private ArrayList<GraphNode> bits2list(RoaringBitmap bits) {

		ArrayList<GraphNode> list = new ArrayList<GraphNode>();
		for (int i : bits) {

			list.add(mGraNodes[i]);
		}

		return list;

	}

	private void pruneOneStepBUP_mv(int from, ArrayList<Integer> tos, RoaringBitmap qCandBits_f, boolean[] changed) {

		HashMap<Integer, ArrayList<Pool>> vplHT_f = mViewNodePools.get(from);
		HashMap<Integer, HashSet<Integer>> cvsHT_f = mCoverViewNodes.get(from);

		for (int to : tos) {
			if (passNum > 1 && !changed[from] && !changed[to]) {
				// System.out.println("Yes pruneOneStepBUP!");

				continue;
			}
			RoaringBitmap qCandBits_t = mCandBitsArr[to];
			HashMap<Integer, ArrayList<Pool>> vplHT_t = mViewNodePools.get(to);
			HashMap<Integer, HashSet<Integer>> cvsHT_t = mCoverViewNodes.get(to);

			Set<Integer> vids_f = cvsHT_f.keySet(), vids_t = cvsHT_t.keySet();
			ArrayList<Integer> vids = intersect(vids_f, vids_t);
			// int vid = vids.get(0);
			// chkFwdBits(from, to, vid, qCandBits_f, qCandBits_t, vplHT_f, cvsHT_t);
			chkFwdBits(from, to, vids, qCandBits_f, qCandBits_t, vplHT_f, cvsHT_t);

		}
	}

	private void pruneOneStepTDW_mv(int to, ArrayList<Integer> froms, RoaringBitmap qCandBits_t, boolean[] changed) {

		HashMap<Integer, ArrayList<Pool>> vplHT_t = mViewNodePools.get(to);
		HashMap<Integer, HashSet<Integer>> cvsHT_t = mCoverViewNodes.get(to);

		for (int from : froms) {
			if (passNum > 1 && !changed[from] && !changed[to]) {
				// System.out.println("Yes pruneOneStepBUP!");

				continue;
			}
			RoaringBitmap qCandBits_f = mCandBitsArr[from];
			HashMap<Integer, ArrayList<Pool>> vplHT_f = mViewNodePools.get(from);
			HashMap<Integer, HashSet<Integer>> cvsHT_f = mCoverViewNodes.get(from);

			Set<Integer> vids_f = cvsHT_f.keySet(), vids_t = cvsHT_t.keySet();
			ArrayList<Integer> vids = intersect(vids_f, vids_t);
			// int vid = vids.get(0);
			// chkBwdBits(from, to, vid, qCandBits_f, qCandBits_t, vplHT_t, cvsHT_f);
			chkBwdBits(from, to, vids, qCandBits_f, qCandBits_t, vplHT_t, cvsHT_f);

		}
	}

	private void chkBwdBits2(int from, int to, ArrayList<Integer> vids, RoaringBitmap qCandBits_f,
			RoaringBitmap qCandBits_t, HashMap<Integer, ArrayList<Pool>> vplHT_t,
			HashMap<Integer, HashSet<Integer>> cvsHT_f) {
		// set backward bits for every entry of node to

		RoaringBitmap rmvBits = new RoaringBitmap();

		RoaringBatchIterator it = qCandBits_t.getBatchIterator();
		int[] buffer = new int[16];
		while (it.hasNext()) {
			int batch = it.nextBatch(buffer);
			for (int i = 0; i < batch; i++) {
				int n_t = buffer[i];
				RoaringBitmap qBwdBits = new RoaringBitmap();
				qBwdBits.or(qCandBits_f);
				for (int vid : vids) {
					// view with id= vid
					// pools of view nodes covering the query node id= to
					ArrayList<Pool> vpls_t = vplHT_t.get(vid);
					// covering view nodes of query node from in view with id= vid
					HashSet<Integer> cvs_f = cvsHT_f.get(vid);
					// view with id= vid_f contains both covering nodes of from and to
					Query v = mViews.get(vid);
					// iterate entries over each pool
					for (Pool vpl_t : vpls_t) {
						PoolEntry e_t_v = gn2peID(vid, n_t, vpl_t);
						// parent ids of view node id = e_t_v.getQID()
						ArrayList<Integer> pids_t = v.getParentsIDs(e_t_v.getQID());
						ArrayList<Integer> comms = intersect(pids_t, cvs_f);

						for (int c = 0; c < comms.size(); c++) {

							qBwdBits.and(e_t_v.getBwdBits(comms.get(c)));
						}

					}
				}

				if (qBwdBits.isEmpty()) {

					rmvBits.add(n_t);
				}
			}
		}

		qCandBits_t.xor(rmvBits);
	}

	private void chkBwdBits(int from, int to, ArrayList<Integer> vids, RoaringBitmap qCandBits_f,
			RoaringBitmap qCandBits_t, HashMap<Integer, ArrayList<Pool>> vplHT_t,
			HashMap<Integer, HashSet<Integer>> cvsHT_f) {
		// set backward bits for every entry of node to

		RoaringBitmap rmvBits = new RoaringBitmap();

		for (int n_t : qCandBits_t) {

			RoaringBitmap qBwdBits = new RoaringBitmap();
			qBwdBits.or(qCandBits_f);
			for (int vid : vids) {
				// view with id= vid
				// pools of view nodes covering the query node id= to
				ArrayList<Pool> vpls_t = vplHT_t.get(vid);
				// covering view nodes of query node from in view with id= vid
				HashSet<Integer> cvs_f = cvsHT_f.get(vid);
				// view with id= vid_f contains both covering nodes of from and to
				Query v = mViews.get(vid);
				// iterate entries over each pool
				for (Pool vpl_t : vpls_t) {
					PoolEntry e_t_v = gn2peID(vid, n_t, vpl_t);
					// parent ids of view node id = e_t_v.getQID()
					ArrayList<Integer> pids_t = v.getParentsIDs(e_t_v.getQID());
					if (pids_t != null) {
						ArrayList<Integer> comms = intersect(pids_t, cvs_f);

						for (int c = 0; c < comms.size(); c++) {

							qBwdBits.and(e_t_v.getBwdBits(comms.get(c)));
						}
					}

				}
			}

			if (qBwdBits.isEmpty()) {

				rmvBits.add(n_t);
			}

		}

		qCandBits_t.xor(rmvBits);
	}

	private void chkBwdBits(int from, int to, int vid, RoaringBitmap qCandBits_f, RoaringBitmap qCandBits_t,
			HashMap<Integer, ArrayList<Pool>> vplHT_t, HashMap<Integer, HashSet<Integer>> cvsHT_f) {
		// set backward bits for every entry of node to

		RoaringBitmap rmvBits = new RoaringBitmap();
		// view with id= vid
		// pools of view nodes covering the query node id= to
		ArrayList<Pool> vpls_t = vplHT_t.get(vid);
		// covering view nodes of query node from in view with id= vid
		HashSet<Integer> cvs_f = cvsHT_f.get(vid);
		// view with id= vid_f contains both covering nodes of from and to
		Query v = mViews.get(vid);
		for (int n_t : qCandBits_t) {

			RoaringBitmap qBwdBits = new RoaringBitmap();
			qBwdBits.or(qCandBits_f);

			// iterate entries over each pool
			for (Pool vpl_t : vpls_t) {
				PoolEntry e_t_v = gn2peID(vid, n_t, vpl_t);
				// parent ids of view node id = e_t_v.getQID()
				ArrayList<Integer> pids_t = v.getParentsIDs(e_t_v.getQID());
				ArrayList<Integer> comms = intersect(pids_t, cvs_f);

				for (int c = 0; c < comms.size(); c++) {

					qBwdBits.and(e_t_v.getBwdBits(comms.get(c)));
				}

			}

			if (qBwdBits.isEmpty()) {

				rmvBits.add(n_t);
			}

		}

		qCandBits_t.xor(rmvBits);
	}

	private void chkFwdBits2(int from, int to, ArrayList<Integer> vids, RoaringBitmap qCandBits_f,
			RoaringBitmap qCandBits_t, HashMap<Integer, ArrayList<Pool>> vplHT_f,
			HashMap<Integer, HashSet<Integer>> cvsHT_t) {

		RoaringBitmap rmvBits = new RoaringBitmap();

		// set forward bits for every entry of node from

		RoaringBatchIterator it = qCandBits_f.getBatchIterator();
		int[] buffer = new int[16];
		while (it.hasNext()) {
			int batch = it.nextBatch(buffer);
			for (int i = 0; i < batch; i++) {
				int n_f = buffer[i];
				RoaringBitmap qFwdBits = new RoaringBitmap();
				qFwdBits.or(qCandBits_t);
				for (int vid : vids) {
					// pools of view nodes covering the query node id= from
					ArrayList<Pool> vpls_f = vplHT_f.get(vid);
					// covering view nodes of query node to in view with id= vid_f
					HashSet<Integer> cvs_t = cvsHT_t.get(vid);
					// view with id= vid_f contains both covering nodes of from and to
					Query v = mViews.get(vid);
					// iterate entries over each pool
					for (Pool vpl_f : vpls_f) {
						PoolEntry e_f_v = gn2peID(vid, n_f, vpl_f);
						// child ids of view node id = e_f_v.getQID()
						ArrayList<Integer> cids_f = v.getChildrenIDs(e_f_v.getQID());
						ArrayList<Integer> comms = intersect(cids_f, cvs_t);

						for (int c = 0; c < comms.size(); c++) {

							qFwdBits.and(e_f_v.getFwdBits(comms.get(c)));
						}

					}
				}

				if (qFwdBits.isEmpty()) {

					rmvBits.add(n_f);
				}

			}
		}

		qCandBits_f.xor(rmvBits);
	}

	private void chkFwdBits(int from, int to, ArrayList<Integer> vids, RoaringBitmap qCandBits_f,
			RoaringBitmap qCandBits_t, HashMap<Integer, ArrayList<Pool>> vplHT_f,
			HashMap<Integer, HashSet<Integer>> cvsHT_t) {

		RoaringBitmap rmvBits = new RoaringBitmap();

		// set forward bits for every entry of node from
		for (int n_f : qCandBits_f) {
			RoaringBitmap qFwdBits = new RoaringBitmap();
			qFwdBits.or(qCandBits_t);
			for (int vid : vids) {
				// pools of view nodes covering the query node id= from
				ArrayList<Pool> vpls_f = vplHT_f.get(vid);
				// covering view nodes of query node to in view with id= vid_f
				HashSet<Integer> cvs_t = cvsHT_t.get(vid);
				// view with id= vid_f contains both covering nodes of from and to
				Query v = mViews.get(vid);
				// iterate entries over each pool
				for (Pool vpl_f : vpls_f) {
					PoolEntry e_f_v = gn2peID(vid, n_f, vpl_f);
					// child ids of view node id = e_f_v.getQID()
					ArrayList<Integer> cids_f = v.getChildrenIDs(e_f_v.getQID());
					// skip if it is a sink node in view
					if (cids_f != null) {
						ArrayList<Integer> comms = intersect(cids_f, cvs_t);

						for (int c = 0; c < comms.size(); c++) {

							qFwdBits.and(e_f_v.getFwdBits(comms.get(c)));
						}
					}

				}
			}

			if (qFwdBits.isEmpty()) {

				rmvBits.add(n_f);
			}

		}

		qCandBits_f.xor(rmvBits);
	}

	private void chkFwdBits(int from, int to, int vid, RoaringBitmap qCandBits_f, RoaringBitmap qCandBits_t,
			HashMap<Integer, ArrayList<Pool>> vplHT_f, HashMap<Integer, HashSet<Integer>> cvsHT_t) {

		RoaringBitmap rmvBits = new RoaringBitmap();

		// pools of view nodes covering the query node id= from
		ArrayList<Pool> vpls_f = vplHT_f.get(vid);
		// covering view nodes of query node to in view with id= vid_f
		HashSet<Integer> cvs_t = cvsHT_t.get(vid);
		// view with id= vid_f contains both covering nodes of from and to
		Query v = mViews.get(vid);

		// set forward bits for every entry of node from
		for (int n_f : qCandBits_f) {
			RoaringBitmap qFwdBits = new RoaringBitmap();
			qFwdBits.or(qCandBits_t);

			// iterate entries over each pool
			for (Pool vpl_f : vpls_f) {
				PoolEntry e_f_v = gn2peID(vid, n_f, vpl_f);
				// child ids of view node id = e_f_v.getQID()
				ArrayList<Integer> cids_f = v.getChildrenIDs(e_f_v.getQID());
				ArrayList<Integer> comms = intersect(cids_f, cvs_t);

				for (int c = 0; c < comms.size(); c++) {

					qFwdBits.and(e_f_v.getFwdBits(comms.get(c)));
				}

			}

			if (qFwdBits.isEmpty()) {

				rmvBits.add(n_f);
			}

		}

		qCandBits_f.xor(rmvBits);
	}

	// from graph node to pool entry, sorted by ID
	private PoolEntry gn2peID(int qid, int gid, Pool pl) {

		RoaringBitmap bits = pl.getIDBits();

		PoolEntry e = pl.elist().get(bits.rank(gid) - 1);

		return e;

	}

	private ArrayList<Integer> intersect(ArrayList<Integer> l, HashSet<Integer> s) {

		ArrayList<Integer> rs = new ArrayList<Integer>(l.size());

		for (int i : l) {
			if (s.contains(i))
				rs.add(i);

		}

		return rs;
	}

	private ArrayList<Integer> intersect(Set<Integer> s1, Set<Integer> s2) {

		ArrayList<Integer> rs = new ArrayList<Integer>(s1.size());

		for (int i : s1) {
			if (s2.contains(i))
				rs.add(i);

		}

		return rs;
	}

	private boolean pruneOneStepBUP(int qid, boolean[] changed) {

		QNode[] qnodes = mQuery.nodes;
		QNode parent = qnodes[qid];
		if (parent.isSink())
			return false;

		RoaringBitmap candBits = mCandBitsArr[parent.id];
		int card = candBits.getCardinality();
		MatArray mli = mCandLists.get(parent.id);
		ArrayList<QEdge> o_edges = parent.E_O;
		ArrayList<QNode> qnodes_c = new ArrayList<QNode>(o_edges.size()),
				qnodes_d = new ArrayList<QNode>(o_edges.size());

		ArrayList<Integer> qnodes_cv = new ArrayList<Integer>(o_edges.size());

		for (QEdge o_edge : o_edges) {
			if (mEdgeCoverBits.get(o_edge.eid)) {

				qnodes_cv.add(o_edge.to);

			} else {
				int cid = o_edge.to;
				AxisType axis = o_edge.axis;
				QNode child = qnodes[cid];
				if (axis == AxisType.child)
					qnodes_c.add(child);
				else
					qnodes_d.add(child);
			}
		}

		if (qnodes_cv.size() > 0) {

			pruneOneStepBUP_mv(parent.id, qnodes_cv, candBits, changed);
			mli.setList(bits2list(candBits));
		}

		if (qnodes_c.size() > 0) {
			// System.out.println("Before Prune, card = " + candBits.getCardinality());
			pruneOneStepBUP_c(parent, qnodes_c, candBits, changed);
			// System.out.println("After Prune, card = " + candBits.getCardinality());

			mli.setList(bits2list(candBits));
		}

		if (qnodes_d.size() > 0) {

			pruneOneStepBUP_d(parent, qnodes_d, mli.elist(), candBits, changed);

		}

		boolean hasChange = card > candBits.getCardinality() ? true : false;

		if (hasChange)
			changed[qid] = true;
		else
			changed[qid] = false;
		return hasChange;

	}

	private void pruneOneStepBUP_c(QNode parent, ArrayList<QNode> qnodes_c, RoaringBitmap candBits, boolean[] changed) {

		// RoaringBitmap rmvBits = candBits.clone();

		for (QNode child : qnodes_c) {
			if (passNum > 1 && !changed[parent.id] && !changed[child.id]) {
				// System.out.println("Yes pruneOneStepBUP!");

				continue;
			}
			RoaringBitmap union = unionBackAdj(child);
			candBits.and(union);
		}

		// rmvBits.xor(candBits);
		// ArrayList<GraphNode> rmvList = bits2list(rmvBits);
		// candList.removeAll(rmvList);

	}

	private RoaringBitmap unionBackAdj(QNode child) {

		int[] buffer = new int[256];
		RoaringBitmap candBits = mCandBitsArr[child.id];

		RoaringBatchIterator it = candBits.getBatchIterator();
		// RoaringBitmap union = new RoaringBitmap();
		List<RoaringBitmap> orMaps_g = new ArrayList<>();
		while (it.hasNext()) {

			int batch = it.nextBatch(buffer);
			List<RoaringBitmap> orMaps = new ArrayList<>();
			for (int i = 0; i < batch; ++i) {
				GraphNode gn = this.mGraNodes[buffer[i]];
				orMaps.add(gn.adj_bits_id_i);
			}
			orMaps_g.add(FastAggregation.or(orMaps.iterator()));
		}

		RoaringBitmap union = FastAggregation.or(orMaps_g.iterator());

		return union;

	}

	private RoaringBitmap unionFwdAdj(QNode parent) {

		int[] buffer = new int[256];
		RoaringBitmap candBits = mCandBitsArr[parent.id];

		RoaringBatchIterator it = candBits.getBatchIterator();
		// RoaringBitmap union = new RoaringBitmap();
		List<RoaringBitmap> orMaps_g = new ArrayList<>();
		while (it.hasNext()) {

			int batch = it.nextBatch(buffer);
			List<RoaringBitmap> orMaps = new ArrayList<>();
			for (int i = 0; i < batch; ++i) {
				GraphNode gn = this.mGraNodes[buffer[i]];
				orMaps.add(gn.adj_bits_id_o);
			}
			orMaps_g.add(FastAggregation.or(orMaps.iterator()));
		}

		RoaringBitmap union = FastAggregation.or(orMaps_g.iterator());

		return union;

	}

	private void pruneOneStepBUP_d(QNode parent, ArrayList<QNode> qnodes_d, ArrayList<GraphNode> candList_p,
			RoaringBitmap candBits_p, boolean[] changed) {

		ArrayList<GraphNode> rmvList = new ArrayList<GraphNode>();
		RoaringBitmap rmvBits = new RoaringBitmap();

		for (GraphNode gn : candList_p) {
			for (QNode child : qnodes_d) {
				boolean found = false;
				if (passNum > 1 && !changed[parent.id] && !changed[child.id]) {
					// System.out.println("Yes pruneOneStepBUP!");

					continue;
				}

				MatArray mli = mCandLists.get(child.id);
				int counter2 = 0;
				for (GraphNode ni : mli.elist()) {

					if (gn.id == ni.id)
						continue;

					if (mBFL.reach(gn, ni) == 1) {
						found = true;
						break;
					}

				}

				if (!found) {
					rmvList.add(gn);
					rmvBits.add(gn.id);
					break;
				}
			}

		}

		candBits_p.xor(rmvBits);
		candList_p.removeAll(rmvList);

	}

	private boolean pruneTDW(boolean[] changed) {

		boolean hasChange = false;
		// System.out.println("Passnum=" + passNum);
		// System.out.println("Before pruneTDW");
		// printCard();
		for (int qid : nodesTopoList) {
			boolean result = pruneOneStepTDW(qid, changed);
			hasChange = hasChange || result;
		}
		passNum++;
		// System.out.println("After pruneTDW");
		// printCard();
		return hasChange;
	}

	private boolean pruneOneStepTDW(int cid, boolean[] changed) {

		QNode[] qnodes = mQuery.nodes;

		QNode child = qnodes[cid];

		if (child.isSource())
			return false;

		RoaringBitmap candBits = mCandBitsArr[child.id];
		int card = candBits.getCardinality();
		MatArray mli = mCandLists.get(child.id);
		ArrayList<QEdge> i_edges = child.E_I;
		ArrayList<QNode> qnodes_c = new ArrayList<QNode>(i_edges.size()),
				qnodes_d = new ArrayList<QNode>(i_edges.size());

		ArrayList<Integer> qnodes_cv = new ArrayList<Integer>(i_edges.size());

		for (QEdge i_edge : i_edges) {
			int pid = i_edge.from;
			if (mEdgeCoverBits.get(i_edge.eid)) {

				qnodes_cv.add(pid);

			} else {

				AxisType axis = i_edge.axis;
				QNode parent = qnodes[pid];
				if (axis == AxisType.child)
					qnodes_c.add(parent);
				else
					qnodes_d.add(parent);
			}
		}

		if (qnodes_cv.size() > 0) {

			pruneOneStepTDW_mv(child.id, qnodes_cv, candBits, changed);
			mli.setList(bits2list(candBits));
		}

		if (qnodes_c.size() > 0) {

			pruneOneStepTDW_c(child, qnodes_c, candBits, changed);
			mli.setList(bits2list(candBits));
		}

		if (qnodes_d.size() > 0) {

			pruneOneStepTDW_d(child, qnodes_d, mli.elist(), candBits, changed);

		}

		boolean hasChange = card > candBits.getCardinality() ? true : false;
		if (hasChange)
			changed[cid] = true;
		else
			changed[cid] = false;
		return hasChange;

	}

	private void pruneOneStepTDW_c(QNode child, ArrayList<QNode> qnodes_c, RoaringBitmap candBits, boolean[] changed) {

		// RoaringBitmap rmvBits = candBits.clone();

		for (QNode parent : qnodes_c) {
			if (passNum > 1 && !changed[child.id] && !changed[parent.id]) {
				// System.out.println("Yes pruneOneStepBUP!");

				continue;
			}
			RoaringBitmap union = unionFwdAdj(parent);
			candBits.and(union);
		}

		// rmvBits.xor(candBits);
		// ArrayList<GraphNode> rmvList = bits2list(rmvBits);
		// candList.removeAll(rmvList);

	}

	private void pruneOneStepTDW_d(QNode child, ArrayList<QNode> qnodes_d, ArrayList<GraphNode> candList_c,
			RoaringBitmap candBits_c, boolean[] changed) {

		ArrayList<GraphNode> rmvList = new ArrayList<GraphNode>();
		RoaringBitmap rmvBits = new RoaringBitmap();
		for (GraphNode gn : candList_c) {
			for (QNode parent : qnodes_d) {
				boolean found = false;
				if (passNum > 1 && !changed[child.id] && !changed[parent.id]) {
					// System.out.println("Yes pruneOneStepBUP!");

					continue;
				}

				MatArray mli = mCandLists.get(parent.id);
				for (GraphNode par : mli.elist()) {

					if (gn.id == par.id)
						continue;

					if (mBFL.reach(par, gn) == 1) {
						found = true;
						break;
					}

				}

				if (!found) {
					rmvList.add(gn);
					rmvBits.add(gn.id);
					break;
				}
			}

		}

		candBits_c.xor(rmvBits);
		candList_c.removeAll(rmvList);

	}

	private void init() {

		QueryHandler qh = new QueryHandler();
		// nodesOrder = qh.topologyQue(mQuery);
		nodesTopoList = qh.topologyList(mQuery);

		int size = mQuery.V;
		mCandLists = new ArrayList<MatArray>(size);

		mCandBitsArr = new RoaringBitmap[size];

		QNode[] qnodes = mQuery.nodes;
		for (int i = 0; i < qnodes.length; i++) {
			QNode q = qnodes[i];

			ArrayList<GraphNode> invLst;
			if (invLstByQuery)
				invLst = mInvLstsByID.get(q.id);
			else
				invLst = mInvLstsByID.get(q.lb);
			MatArray mlist = new MatArray();
			mlist.addList(invLst);
			mCandLists.add(q.id, mlist);

			if (invLstByQuery)
				mCandBitsArr[q.id] = mBitsByIDArr.get(q.id);
			else
				mCandBitsArr[q.id] = mBitsByIDArr.get(q.lb).clone();
		}

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
