package mv.queryCovering;

import java.util.ArrayList;
import java.util.Arrays;
import global.Consts;
import global.Consts.AxisType;
import global.Consts.DirType;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import query.graph.QueryHandler;
import query.graph.QueryParser;
import queryPlan.PlanGenerator;

public class QueryCoverHandler {

	private Query mQuery, mView;
	private int[] order;
	private int[][] bn; // backward neighbors of view
	private int[] bn_count;
	private ArrayList<ArrayList<QNode>> invLists; // inverted qnode list per view node

	private AxisType[][] tc_view, tc_query;
	private ArrayList<QNode> match;
	private ArrayList<ArrayList<QNode>> matchList;
	private CoverResult cover;

	public QueryCoverHandler(Query v, Query q) {

		mQuery = q;
		mView = v;
		order = PlanGenerator.generateRIQueryPlan(mView);
		QueryHandler qh = new QueryHandler();
		tc_view = qh.transitiveClosure(mView);
		tc_query = qh.transitiveClosure(mQuery);
		match = new ArrayList<QNode>(mView.V);
		for (int i = 0; i < mView.V; i++)
			match.add(null);
		matchList = new ArrayList<ArrayList<QNode>>();

	}

	public void run() {

		genV2QHomos();
		printMatchList();
		genCover();
		// cover.print();
	}

	public CoverResult getCover() {

		return cover;

	}

	private void printMatchList() {

		System.out.println("Total # of matches:" + matchList.size());

		for (ArrayList<QNode> match : matchList) {

			for (int i = 0; i < match.size(); i++) {
				QNode qm = match.get(i);
				System.out.print(i + ":" + qm.id + " ");
			}

			System.out.println("-----------------");
		}

	}

	private void genV2QHomos() {

		generateBN();
		genCandQNList();
		transition(mView.V, 0);

	}

	private void genCover() {

		cover = new CoverResult(mView.Qid, mQuery.V);
		for (ArrayList<QNode> match : matchList) {
			for (int i = 0; i < mView.V; i++) {
				QNode qm = match.get(i);
				cover.addCoverNode(qm.id, i, false);
			}

			for (QEdge e : mView.edges) {

				int f_v = e.from, t_v = e.to;
				int f_q = match.get(f_v).id, t_q = match.get(t_v).id;
				QEdge qe = mQuery.getEdge(f_q, t_q);
				if (qe != null) {
					cover.addCoverEdge(qe.eid, e.eid);
				}

			}

		}

	}

	private void transition(int max_depth, int depth) {

		int cur_vertex = order[depth];

		ArrayList<QNode> candList = getCandList(cur_vertex);
		if (candList.isEmpty()) {

			return;
		}

		for (QNode qn : candList) {

			match.set(cur_vertex, qn);

			if (depth == max_depth - 1) {

				addMatch();

			} else {
				transition(max_depth, depth + 1);
			}

			match.set(cur_vertex, null);
		}

	}

	private void addMatch() {
		ArrayList<QNode> copy = new ArrayList<QNode>(mView.V);
		for (int i = 0; i < mView.V; i++) {
			copy.add(match.get(i));

		}

		matchList.add(copy);
	}

	private ArrayList<QNode> getCandList(int cur_vertex) {

		int num = bn_count[cur_vertex];
		if (num == 0) {

			return invLists.get(cur_vertex);

		}

		ArrayList<ArrayList<QNode>> candLists = new ArrayList<ArrayList<QNode>>(num);
		int[] bns = bn[cur_vertex];

		for (int i = 0; i < num; i++) {
			int bn_vertex = bns[i];
			ArrayList<QNode> candList = getCandList(bn_vertex, cur_vertex);
			candLists.add(candList);
		}

		ArrayList<QNode> candList = join(candLists, num);

		return candList;
	}

	private ArrayList<QNode> getCandList(int bn_vertex, int cur_vertex) {

		DirType dir = mView.dir(bn_vertex, cur_vertex);
		AxisType axis_v = AxisType.none;

		if (dir == DirType.FWD) {

			axis_v = mView.getEdge(bn_vertex, cur_vertex).axis;

		} else if (dir == DirType.BWD) {

			axis_v = mView.getEdge(cur_vertex, bn_vertex).axis;

		}

		ArrayList<QNode> cur_invList = invLists.get(cur_vertex);

		ArrayList<QNode> candList = new ArrayList<QNode>(cur_invList.size());
		for (QNode qn : cur_invList) {

			QNode bm = match.get(bn_vertex);
			AxisType axis_q = AxisType.none;
			if (dir == DirType.FWD) {

				axis_q = tc_query[bm.id][qn.id];

			} else if (dir == DirType.BWD) {

				axis_q = tc_query[qn.id][bm.id];

			}

			if (axis_q == AxisType.none)
				continue;
			// if ((axis_v == AxisType.child && axis_q == AxisType.child) || (axis_v ==
			// AxisType.descendant))
			if ((axis_v == AxisType.child && axis_q == AxisType.child)
					|| (axis_v == AxisType.descendant && axis_q == AxisType.descendant))

				candList.add(qn);

		}

		return candList;
	}

	private ArrayList<QNode> join(ArrayList<ArrayList<QNode>> candLists, int num) {

		ArrayList<QNode> result = new ArrayList<QNode>();

		int[] len = new int[num], idx = new int[num];

		for (int i = 0; i < num; i++)
			len[i] = candLists.get(i).size();

		while (!end(len, idx, num)) {

			int minIdx = getMinIdx(candLists, idx, num);
			int maxIdx = getMaxIdx(candLists, idx, num);

			if (minIdx == maxIdx) {
				QNode val = candLists.get(minIdx).get(idx[minIdx]);
				result.add(val);
				advance(idx, num);

			} else
				idx[minIdx]++;
		}

		return result;
	}

	private void advance(int[] idx, int num) {

		for (int i = 0; i < num; i++) {

			idx[i]++;
		}
	}

	private int getMinIdx(ArrayList<ArrayList<QNode>> candLists, int[] idx, int num) {
		int min = 0;

		QNode val = candLists.get(0).get(idx[0]);
		QNode minval = val;
		for (int i = 1; i < num; i++) {

			val = candLists.get(i).get(idx[i]);
			if (minval.id > val.id) {

				minval = val;
				min = i;
			}

		}

		return min;
	}

	private int getMaxIdx(ArrayList<ArrayList<QNode>> candLists, int[] idx, int num) {
		int max = 0;

		QNode val = candLists.get(0).get(idx[0]);
		QNode maxval = val;
		for (int i = 1; i < num; i++) {

			val = candLists.get(i).get(idx[i]);
			if (maxval.id < val.id) {

				maxval = val;
				max = i;
			}

		}

		return max;
	}

	private boolean end(int[] len, int[] idx, int num) {

		for (int i = 0; i < num; i++) {

			if (idx[i] == len[i])
				return true;
		}

		return false;
	}

	// inverted query node list for each view node

	private void genCandQNList() {

		invLists = new ArrayList<ArrayList<QNode>>(mView.V);

		for (QNode vn : mView.nodes) {

			ArrayList<QNode> invList = new ArrayList<QNode>(mQuery.V);

			for (QNode qn : mQuery.nodes) {

				if (qn.lb == vn.lb)
					invList.add(qn);

			}

			invLists.add(invList);
		}

	}

	// backward neighbor of view
	private void generateBN() {
		int query_vertices_num = mView.V;
		bn_count = new int[query_vertices_num];
		Arrays.fill(bn_count, 0);
		bn = new int[query_vertices_num][];
		for (int i = 0; i < query_vertices_num; ++i) {
			bn[i] = new int[query_vertices_num];
		}
		boolean[] visited_vertices = new boolean[query_vertices_num];
		Arrays.fill(visited_vertices, false);
		visited_vertices[order[0]] = true;
		for (int i = 1; i < query_vertices_num; ++i) {
			int vertex = order[i];
			ArrayList<Integer> nbrs = mView.getNeighborIdList(vertex);
			for (int nbr : nbrs) {
				if (visited_vertices[nbr]) {
					bn[vertex][bn_count[vertex]++] = nbr;
				}
			}

			visited_vertices[vertex] = true;
		}

	}

	public static void main(String[] args) {

		String queryFN = args[0]; // the query file
		String queryFileN = Consts.INDIR + queryFN;
		ArrayList<Query> queries = new ArrayList<Query>();
		QueryParser queryParser = new QueryParser(queryFileN);
		Query query = null, view = null;

		while ((query = queryParser.readNextQuery()) != null) {

			System.out.println(query);
			queries.add(query);
		}

		query = queries.get(0);

		ArrayList<CoverResult> covers = new ArrayList<CoverResult>();

		for (int i = 1, vid = 0; i < queries.size(); i++, vid++) {
			view = queries.get(i);
			view.Qid = vid;
			QueryCoverHandler qch = new QueryCoverHandler(view, query);
			qch.run();
			covers.add(qch.getCover());
		}

		for (CoverResult cover : covers)
			cover.print();

	}

}
