package mv.answerGraph;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.roaringbitmap.RoaringBatchIterator;
import org.roaringbitmap.RoaringBitmap;

import dao.BFLIndex;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import global.Consts.AxisType;
import graph.GraphNode;
import mv.queryCovering.CoverResult;
import mv.viewManager.ViewManager;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;

public class HybAGMVBuilder {

	Query mQuery;

	ArrayList<Pool> mPool;
	GraphNode[] mGraNodes;
	BFLIndex mBFL;
	ArrayList<MatArray> mCandListByST;
	ArrayList<RoaringBitmap> mBitsByIDArr;
	RoaringBitmap[] tBitsSTArr;

	ArrayList<Query> mViews;
	ArrayList<ArrayList<Pool>> mViewPools;
	ArrayList<CoverResult> mCovers;

	BitSet mEdgeCoverBits;

	// indexed by query node id, hashtable is keyed by view id, values are the set
	// of view nodes covering the query id.
	ArrayList<HashMap<Integer, HashSet<Integer>>> mCoverViewNodes;
	ArrayList<HashMap<Integer, ArrayList<Pool>>> mViewNodePools;

	public HybAGMVBuilder(Query query, GraphNode[] graNodes, BFLIndex bfl, ArrayList<MatArray> candLsts,
			ArrayList<RoaringBitmap> bitsByIDArr, ArrayList<Query> views, ArrayList<ArrayList<Pool>> vpls,
			ArrayList<CoverResult> cvs, ArrayList<HashMap<Integer, ArrayList<Pool>>> viewNodePools,
			ArrayList<HashMap<Integer, HashSet<Integer>>> coverViewNodes) {

		mQuery = query;
		mBFL = bfl;
		mGraNodes = graNodes;
		mCandListByST = candLsts;
		mBitsByIDArr = bitsByIDArr;
		mViewPools = vpls;
		mCovers = cvs;
		mViews = views;
		mCoverViewNodes = coverViewNodes;
		mViewNodePools = viewNodePools;
		tBitsSTArr = new RoaringBitmap[mQuery.V];
		mEdgeCoverBits = ViewManager.getCoveredEdges(mQuery, mCovers);

	}

	public void run() {

		initPool();

		for (QEdge edge : mQuery.edges) {
			if (mEdgeCoverBits.get(edge.eid))
				linkOneStepMV(edge);
			else {
				System.out.println("Edge " + edge + " is not covered.");
				linkOneStep(edge);
			}

		}
	}

	public ArrayList<Pool> getAnsGraph() {
		for (int i = 0; i < mQuery.V; i++) {

			Pool pl = mPool.get(i);
			Collections.sort(pl.elist(), PoolEntry.NodeIDComparator);
			RoaringBitmap bits = mBitsByIDArr.get(i);
			pl.setIDBits(bits);
		}
		return mPool;
	}

	private void linkOneStep(QEdge edge) {

		int from = edge.from, to = edge.to;
		AxisType axis = edge.axis;
		Pool pl_f = mPool.get(from), pl_t = mPool.get(to);

		for (PoolEntry e_f : pl_f.elist()) {
			if (axis == AxisType.child)
				linkOneStep(e_f, tBitsSTArr[to], pl_t.elist());
			else {

				GraphNode n_f = e_f.getValue();
				for (PoolEntry e_t : pl_t.elist()) {

					GraphNode n_t = e_t.getValue();
					if (n_f.id == n_t.id)
						continue;
					if (n_f.L_interval.mEnd < n_t.L_interval.mStart)
						break;
					if (mBFL.reach(n_f, n_t) == 1) {

						e_f.addChild(e_t);
						e_t.addParent(e_f);

					}

				}
			}

		}

	}

	private void linkOneStepMV(QEdge edge) {

		int from = edge.from, to = edge.to;
		RoaringBitmap qEntryBits_f = mBitsByIDArr.get(from), qEntryBits_t = mBitsByIDArr.get(to);

		HashMap<Integer, ArrayList<Pool>> vplHT_f = mViewNodePools.get(from), vplHT_t = mViewNodePools.get(to);
		HashMap<Integer, HashSet<Integer>> cvsHT_f = mCoverViewNodes.get(from), cvsHT_t = mCoverViewNodes.get(to);

		Set<Integer> vids_f = cvsHT_f.keySet(), vids_t = cvsHT_t.keySet();
		ArrayList<Integer> vids = intersect(vids_f, vids_t);
		setFowardBits(from, to, vids, qEntryBits_f, qEntryBits_t, vplHT_f, cvsHT_t);
		setBackwardBits(from, to, vids, qEntryBits_f, qEntryBits_t, vplHT_t, cvsHT_f);

	}

	private boolean linkOneStep(PoolEntry r, RoaringBitmap t_bits, ArrayList<PoolEntry> list) {

		GraphNode s = r.getValue();

		RoaringBitmap rs_and = RoaringBitmap.and(s.adj_bits_o, t_bits);

		if (rs_and.isEmpty())
			return false;

		RoaringBatchIterator it = rs_and.getBatchIterator();
		int[] buffer = new int[256];

		while (it.hasNext()) {

			int batch = it.nextBatch(buffer);
			for (int i = 0; i < batch; ++i) {
				// elements in both bits and list are ordered by start values.
				PoolEntry e = list.get(t_bits.rank(buffer[i]) - 1);
				r.addChild(e);
				e.addParent(r);

			}

		}

		return true;

	}

	private void initPool() {

		mPool = new ArrayList<Pool>(mQuery.V);
		QNode[] qnodes = mQuery.nodes;
		for (int i = 0; i < mQuery.V; i++) {

			MatArray mli = mCandListByST.get(i);
			ArrayList<GraphNode> elist = mli.elist();
			QNode qn = qnodes[i];
			RoaringBitmap t_bits = new RoaringBitmap();
			Pool qAct = new Pool(elist.size());
			mPool.add(qAct);
			tBitsSTArr[i] = t_bits;
			int pos = 0;
			for (GraphNode n : elist) {
				PoolEntry actEntry = new PoolEntry(pos++, qn, n);
				qAct.addEntry(actEntry);
				// bits are ordered by graph node start value.
				t_bits.add(actEntry.getValue().L_interval.mStart);
				// t_bits.add(actEntry.getValue().id);
			}
		}
	}

	// from graph node to pool entry, sorted by ST
	private PoolEntry gn2peST(int qid, int gid, Pool pl) {

		GraphNode gn = mGraNodes[gid];

		RoaringBitmap bits = this.tBitsSTArr[qid];

		PoolEntry e = pl.elist().get(bits.rank(gn.L_interval.mStart) - 1);

		return e;

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

	private void setBackwardBits2(int from, int to, ArrayList<Integer> vids, RoaringBitmap qEntryBits_f,
			RoaringBitmap qEntryBits_t, HashMap<Integer, ArrayList<Pool>> vplHT_t,
			HashMap<Integer, HashSet<Integer>> cvsHT_f) {
		// set backward bits for every entry of node to

		Pool pl_t = mPool.get(to);
		RoaringBatchIterator it = qEntryBits_t.getBatchIterator();
		int[] buffer = new int[16];
		while (it.hasNext()) {
			int batch = it.nextBatch(buffer);
			for (int i = 0; i < batch; i++) {
				int n_t = buffer[i];
				PoolEntry e_t = gn2peST(to, n_t, pl_t);
				RoaringBitmap qBwdBits = e_t.getBwdBits(from);
				qBwdBits.or(qEntryBits_f);
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
						// qBwdBits.or(e_t_v.getBwdBits(comms.get(0)));

						for (int c = 0; c < comms.size(); c++) {

							qBwdBits.and(e_t_v.getBwdBits(comms.get(c)));
						}
						// qBwdBits.and(qEntryBits_f);

					}

				}

			}
		}
	}

	private void setBackwardBits(int from, int to, ArrayList<Integer> vids, RoaringBitmap qEntryBits_f,
			RoaringBitmap qEntryBits_t, HashMap<Integer, ArrayList<Pool>> vplHT_t,
			HashMap<Integer, HashSet<Integer>> cvsHT_f) {
		// set backward bits for every entry of node to

		Pool pl_t = mPool.get(to);

		for (int n_t : qEntryBits_t) {
			PoolEntry e_t = gn2peST(to, n_t, pl_t);
			RoaringBitmap qBwdBits = e_t.getBwdBits(from);
			qBwdBits.or(qEntryBits_f);
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
						// qBwdBits.or(e_t_v.getBwdBits(comms.get(0)));

						for (int c = 0; c < comms.size(); c++) {

							qBwdBits.and(e_t_v.getBwdBits(comms.get(c)));
						}
						// qBwdBits.and(qEntryBits_f);
					}
				}

			}

		}

	}

	private void setFowardBits(int from, int to, ArrayList<Integer> vids, RoaringBitmap qEntryBits_f,
			RoaringBitmap qEntryBits_t, HashMap<Integer, ArrayList<Pool>> vplHT_f,
			HashMap<Integer, HashSet<Integer>> cvsHT_t) {

		// set forward bits for every entry of node from
		Pool pl_f = mPool.get(from);

		for (int n_f : qEntryBits_f) {
			PoolEntry e_f = gn2peST(from, n_f, pl_f);
			RoaringBitmap qFwdBits = e_f.getFwdBits(to);
			qFwdBits.or(qEntryBits_t);
			for (int vid : vids) {

				// view with id= vid_f
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

					if (cids_f != null) {
						ArrayList<Integer> comms = intersect(cids_f, cvs_t);
						// qFwdBits.or(e_f_v.getFwdBits(comms.get(0)));

						for (int c = 0; c < comms.size(); c++) {

							qFwdBits.and(e_f_v.getFwdBits(comms.get(c)));
						}
						// qFwdBits.and(qEntryBits_t);

					}
				}

			}

		}
	}

	private void setFowardBits2(int from, int to, ArrayList<Integer> vids, RoaringBitmap qEntryBits_f,
			RoaringBitmap qEntryBits_t, HashMap<Integer, ArrayList<Pool>> vplHT_f,
			HashMap<Integer, HashSet<Integer>> cvsHT_t) {

		// set forward bits for every entry of node from
		Pool pl_f = mPool.get(from);
		RoaringBatchIterator it = qEntryBits_f.getBatchIterator();
		int[] buffer = new int[16];
		while (it.hasNext()) {
			int batch = it.nextBatch(buffer);
			for (int i = 0; i < batch; i++) {
				int n_f = buffer[i];
				PoolEntry e_f = gn2peST(from, n_f, pl_f);
				RoaringBitmap qFwdBits = e_f.getFwdBits(to);
				qFwdBits.or(qEntryBits_t);
				for (int vid : vids) {

					// view with id= vid_f
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
						// qFwdBits.or(e_f_v.getFwdBits(comms.get(0)));

						for (int c = 0; c < comms.size(); c++) {

							qFwdBits.and(e_f_v.getFwdBits(comms.get(c)));
						}
						// qFwdBits.and(qEntryBits_t);

					}

				}

			}
		}

	}

	public static void main(String[] args) {

	}

}
