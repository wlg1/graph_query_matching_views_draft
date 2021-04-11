package tupleEnumerator;

import java.util.ArrayList;
import java.util.Arrays;
import org.roaringbitmap.RoaringBitmap;

import dao.PoolEntry;
import global.Consts;
import global.Consts.DirType;
import graph.GraphNode;
import helper.CartesianProduct;
import helper.QueryEvalStat;
import helper.TimeTracker;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import queryPlan.PlanGenerator;

public class EdgeTupleBitsEnumer {

	private double tupleCount = 0;
	private Query query;
	private boolean toContinue = true;
	private int[] tuple; // values are graph node ids.
	private int[] order;
	private int[][] bn; // backward neighbors
	private int[] bn_count;
	private TimeTracker tt;

	RoaringBitmap[] mCandBitsArr;
	GraphNode[] mGraNodes;

	public EdgeTupleBitsEnumer(Query qry, GraphNode[] graNodes, RoaringBitmap[] candBitsArr) {

		query = qry;
		mCandBitsArr = candBitsArr;
		mGraNodes = graNodes;
		tuple = new int[query.V];
		Arrays.fill(tuple, -1);
		tupleCount = 0;
		toContinue = true;
		tt = new TimeTracker();
	}

	public double enumTuples(QueryEvalStat stat) {

		order = PlanGenerator.generateRITOPOQueryPlan(query);
		generateBN(query, order);
		tt.Start();
		// transition(query.V, 0, match, state, stat);
		transition(query.V, 0, stat);
		double enumtm = tt.Stop() / 1000;
		System.out.println("Tuple enumeration time:" + enumtm + " sec.");
		System.out.println("Total enumerated solution tuples:" + tupleCount);

		return tupleCount;
	}

	public double enumTreeTuples(QueryEvalStat stat) {
		tt.Start();
		transition_tree(stat);
		double enumtm = tt.Stop() / 1000;
		System.out.println("Tuple enumeration time:" + enumtm + " sec.");
		System.out.println("Total enumerated solution tuples:" + tupleCount);
		return tupleCount;
	}
	
	public double enumTreeTuples2(QueryEvalStat stat) {
		tt.Start();
		order = PlanGenerator.generateTopoQueryPlan(query);
		int cur_vertex = order[0];
		RoaringBitmap bits = mCandBitsArr[cur_vertex];
		for (int val : bits) {
			if (!toContinue)
				break;
			tuple[cur_vertex] = val;
			transition_tree(query.V, 0, 1, stat);

		}

		//
		double enumtm = tt.Stop() / 1000;
		System.out.println("Tuple enumeration time:" + enumtm + " sec.");
		System.out.println("Total enumerated solution tuples:" + tupleCount);
		return tupleCount;
	}


	private void transition(int max_depth, int depth, QueryEvalStat stat) {

		int cur_vertex = order[depth];

		RoaringBitmap candBits = getCandBits(cur_vertex);
		if (candBits.isEmpty()) {

			return;
		}

		for (int i : candBits) {

			tuple[cur_vertex] = i;
			if (depth == max_depth - 1) {
				//printTuple();
				tupleCount++;
				stat.setNumSolns(tupleCount);
				//if (tupleCount == Consts.OutputLimit)
				//	toContinue = false;

			} else {
				transition(max_depth, depth + 1, stat);
			}

			//if (!toContinue)
			//	return; // break from here

			tuple[cur_vertex] = -1;

		}

	}

	private RoaringBitmap getCandBits(int cur_vertex) {

		RoaringBitmap bits = mCandBitsArr[cur_vertex].clone();
		int num = bn_count[cur_vertex];

		if (num == 0) {

			return bits;
		}

		int[] bns = bn[cur_vertex];

		for (int i = 0; i < num; i++) {
			int bn_vertex = bns[i];
			DirType dir = query.dir(bn_vertex, cur_vertex);
			int fld = tuple[bn_vertex];
			RoaringBitmap curbits;
			if (dir == DirType.FWD) {
				curbits = mGraNodes[fld].adj_bits_id_o;
			} else {
				curbits = mGraNodes[fld].adj_bits_id_i;
			}

			bits.and(curbits);

		}

		return bits;
	}

	private void transition_tree(int max_depth, int depth, int filled, QueryEvalStat stat) {

		if (filled == max_depth) {
			printTuple();
			tupleCount++;
			stat.setNumSolns(tupleCount);
			if (tupleCount == Consts.OutputLimit) {
				toContinue = false;
				return;
			}
		}

		int cur_vertex = order[depth];
		QNode cur_node = query.nodes[cur_vertex];

		if (cur_node.isSink())
			return;

		ArrayList<Integer> children = cur_node.N_O;
		RoaringBitmap rbits = mGraNodes[tuple[cur_vertex]].adj_bits_id_o;
		if (cur_node.N_O_SZ == 1) {
			int c = children.get(0);
			RoaringBitmap bits = mCandBitsArr[c].clone();
			bits.and(rbits);
			for (int i : bits) {
				tuple[c] = i;
				transition_tree(max_depth, depth + 1, filled + cur_node.N_O_SZ, stat);
				if (!toContinue)
					return; // break from here
			}
			tuple[c] = -1;
		}

		else {
			ArrayList<RoaringBitmap> candBitLists = new ArrayList<RoaringBitmap>(cur_node.N_O_SZ);

			for (int i : children) {

				RoaringBitmap bits = mCandBitsArr[i].clone();
				bits.and(rbits);
				candBitLists.add(bits);
			}

			long[] lengths = new long[cur_node.N_O_SZ];
			int i = 0;
			for (RoaringBitmap bits : candBitLists) {
				lengths[i++] = bits.getCardinality();
			}

			for (long[] indices : new CartesianProduct(lengths)) {

				for (i = 0; i < cur_node.N_O_SZ; i++) {
					int val = candBitLists.get(i).select((int) indices[i]);
					tuple[children.get(i)] = val;
				}
				transition_tree(max_depth, depth + 1, filled + cur_node.N_O_SZ, stat);
				if (!toContinue)
					return; // break from here
			}

			for (int c : children) {

				tuple[c] = -1;
			}
		}
	}

	private void printTuple() {

		for (int v : tuple) {

			System.out.print(v + " ");
		}

		System.out.println();
	}

	private int[] getPlan() {

		int[] candidates_count = new int[query.V];

		for (int i = 0; i < query.V; i++) {
			candidates_count[i] = this.mCandBitsArr[i].getCardinality();

		}

		// int[] order = PlanGenerator.generateGQLQueryPlan(query,
		// candidates_count);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		// int[] order = PlanGenerator.generateRIQueryPlan(query);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		// int[] order = PlanGenerator.generateHybQueryPlan(query,
		// candidates_count);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		// int[] order = PlanGenerator.generateTopoQueryPlan(query);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		int[] order = PlanGenerator.generateRITOPOQueryPlan(query);
		PlanGenerator.printSimplifiedQueryPlan(query, order);

		return order;
	}

	// backward neighbor
	private void generateBN(Query query_graph, int[] order) {
		int query_vertices_num = query_graph.V;
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
			ArrayList<Integer> nbrs = query_graph.getNeighborIdList(vertex);
			for (int nbr : nbrs) {
				if (visited_vertices[nbr]) {
					bn[vertex][bn_count[vertex]++] = nbr;
				}
			}

			visited_vertices[vertex] = true;
		}
	}

	////////////////////////////////////////

	private void transition_tree(QueryEvalStat stat) {

		int matched = matched();
		if (matched == query.V) {
			tupleCount++;
			stat.setNumSolns(tupleCount);
			//printTuple();
			if (tupleCount == Consts.OutputLimit) {
				toContinue = false;
				return;
			}

		} else {

			ArrayList<QNode> extQNodes = extendableQNodes();
			if (extQNodes.size() == 1) {
				QNode qn = extQNodes.get(0);
				RoaringBitmap candBits = getTreeCandBits(qn);
				for (int i : candBits) {

					tuple[qn.id] = i;
					transition_tree(stat);
					if (!toContinue)
						return; // break from here
				}

			}

			else {

				ArrayList<RoaringBitmap> candLists = new ArrayList<RoaringBitmap>(query.V);

				for (QNode qn : extQNodes) {
					RoaringBitmap candBits = getTreeCandBits(qn);
					candLists.add(candBits);

				}

				long[] lengths = new long[extQNodes.size()];
				int i = 0;
				for (RoaringBitmap bits : candLists) {
					lengths[i++] = bits.getCardinality();
				}

				for (long[] indices : new CartesianProduct(lengths)) {

					for (i = 0; i < candLists.size(); i++) {
						int val = candLists.get(i).select((int) indices[i]);
						tuple[extQNodes.get(i).id] = val;

					}

					transition_tree(stat);
					if (!toContinue)
						return; // break from here

				}

			}
			clearState(extQNodes);
		}

	}

	private void clearState(ArrayList<QNode> extQNodes) {

		for (QNode qn : extQNodes) {
			tuple[qn.id] = -1;

		}
	}

	private int matched() {

		int matched = 0;
		for (int i : tuple) {

			if (i != -1)
				matched++;
		}

		return matched;
	}

	private RoaringBitmap getTreeCandBits(QNode qn) {

		int num = qn.N_I_SZ;
		int qid = qn.id;
		if (num == 0) {

			return mCandBitsArr[qid];
		}

		QEdge i_edge = qn.E_I.get(0);

		int pid = i_edge.from;

		RoaringBitmap rbits = mGraNodes[tuple[pid]].adj_bits_id_o;
		RoaringBitmap bits = mCandBitsArr[qid].clone();
		bits.and(rbits);

		return bits;
	}

	private ArrayList<QNode> extendableQNodes() {

		ArrayList<QNode> results = new ArrayList<QNode>();
		QNode[] qnodes = query.nodes;
		boolean extendable = true;
		for (QNode qn : qnodes) {
			int id = qn.id;
			QNode q = qnodes[id];
			extendable = true;
			if (tuple[q.id] != -1)
				continue;
			ArrayList<Integer> parents = q.N_I;

			if (q.N_I_SZ > 0)
				for (int p : parents) {
					if (tuple[p] == -1) {
						extendable = false;

					}

				}

			if (extendable)
				results.add(q);

		}

		return results;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
