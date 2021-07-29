package tupleEnumerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.roaringbitmap.RoaringBitmap;

import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import global.Consts;
import global.Consts.DirType;
import global.Flags;
import graph.GraphNode;
import helper.LimitExceededException;
import helper.QueryEvalStat;
import helper.TimeTracker;
import query.graph.QNode;
import query.graph.Query;
import queryPlan.PlanGenerator;

public class HybTupleEnumer {

	private double tupleCount = 0;
	private Query query;
	private ArrayList<Pool> pool;
	private PoolEntry[] match;
	private Integer[] matchGNpos;
	private int[] order;
	private int[][] bn; // backward neighbors
	private int[] bn_count;
	private TimeTracker tt;
	
	private ArrayList<HashSet<GraphNode>> occ;
	ArrayList<MatArray> mOcc;

	public HybTupleEnumer(Query qry, ArrayList<Pool> pl) {

		query = qry;
		pool = pl; //
		match = new PoolEntry[query.V];
		matchGNpos = new Integer[query.V];
		tupleCount = 0;
		order = // PlanGenerator.generateTopoQueryPlan(query);
				// PlanGenerator.generateRITOPOQueryPlan(query);
				// PlanGenerator.generateRIQueryPlan(query);
				getPlan();

		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		generateBN(query, order);
		tt = new TimeTracker();
		
		occ = new ArrayList<HashSet<GraphNode>>();
		for (int i = 0; i < query.V; i++) {
			occ.add(new HashSet<GraphNode>());
		}
		
	}

	//begins running algo
	public double enumTuples() throws LimitExceededException {
		int[] count = new int[query.V];
		tt.Start();
		// transition(query.V, 0, stat, count);
		transition(query.V, 0);
		double enumtm = tt.Stop() / 1000;
		System.out.println("Tuple enumeration time:" + enumtm + " sec.");
		System.out.println("Total enumerated solution tuples:" + tupleCount);

		return tupleCount;
	}
	
	public double getTupleCount() {

		return tupleCount;
	}
	
	public ArrayList<MatArray> getAnswer() {
	    
	    mOcc = new ArrayList<MatArray>(query.V);
	    for (int i = 0; i < query.V; i++) {
	        HashSet<GraphNode> invSet;
	        invSet = occ.get(i);
	        ArrayList<GraphNode> invLst = new ArrayList<GraphNode>(invSet);
	        Collections.sort(invLst);  //allows determ numSolns for ansgr input
	        
	        MatArray mlist = new MatArray();
	        mlist.addList(invLst);
	        mOcc.add(i, mlist);
	    }
		return mOcc;
	}

	//a recursive step
	private void transition(int max_depth, int depth) throws LimitExceededException {

		int cur_vertex = order[depth]; //query vertex to match

		RoaringBitmap candBits = getCandBits(cur_vertex); //S: graph nodes to use for this query vertex
		if (candBits.isEmpty()) {

			return;
		}

		Pool pl = pool.get(cur_vertex);  //the node set of this query vertex
		ArrayList<PoolEntry> elist = pl.elist();

		//fwd adj intersection
		for (int i : candBits) { //each bit i corresponds to a graph node; order all graph nodes and their pos is i
			//S from getCandBits() uses inters adj list of prev matched neighbors, but NOT inters w/ cur_V nodeset.
			//when get err: Index 25 out of bounds for length 21, do elist.size() == 21... and breakpt
			//if this happens, that means neigh adj list contains gn not in cur_V nodeset
			//soln: when build SG, incl those in nodeset, or rmv them from neigh adj list by intersection w/ nodeset
//			if (elist.size() == 9 && i == 9){ 
//				System.out.println(i);
//			}
			
			
			PoolEntry e = elist.get(i); //the graph node at i's bit
			match[cur_vertex] = e;  //try this graph node as a match to this query vertex
//			matchGNpos[cur_vertex] = e.getValue().L_interval.mStart;
		    
			if (depth == max_depth - 1) {
				tupleCount++;
//				printMatchGNpos();
				
				// add to occ list of each query vertex i
				for (i = 0; i < match.length; i++) { 
					HashSet<GraphNode> occLst = occ.get(i);
					occLst.add(match[i].getValue());
				}

				if (Flags.OUTLIMIT && tupleCount >= Consts.OutputLimit)
					throw new LimitExceededException();

			} else {
				transition(max_depth, depth + 1);
			}

			match[cur_vertex] = null;
			matchGNpos[cur_vertex] = null;

		}

	}

	//don't use this (only diff from above is 3rd arg , QueryEvalStat stat)
	private void transition(int max_depth, int depth, QueryEvalStat stat) throws LimitExceededException {

		int cur_vertex = order[depth];

		RoaringBitmap candBits = getCandBits(cur_vertex);
		if (candBits.isEmpty()) {

			return;
		}

		Pool pl = pool.get(cur_vertex);
		ArrayList<PoolEntry> elist = pl.elist();

		for (int i : candBits) {
			PoolEntry e = elist.get(i);
			match[cur_vertex] = e;
			if (depth == max_depth - 1) {
				tupleCount++;
				// printMatch();
				stat.setNumSolns(tupleCount);

				if (Flags.OUTLIMIT && tupleCount >= Consts.OutputLimit)
					throw new LimitExceededException();

			} else {
				transition(max_depth, depth + 1, stat);
			}

			match[cur_vertex] = null;

		}

	}

	//dont use this
	private void transition(int max_depth, int depth, QueryEvalStat stat, int count[]) throws LimitExceededException {

		int cur_vertex = order[depth];
		QNode qn = query.getNode(cur_vertex);
		RoaringBitmap candBits = getCandBits(cur_vertex);
		if (candBits.isEmpty()) {

			return;
		}

		if (qn.isSink() && !hasFollower(depth)) {

			count[cur_vertex] = candBits.getCardinality();
			if (depth == max_depth - 1) {

				tupleCount += product(count);
				stat.setNumSolns(tupleCount);
				// System.out.println("No. of tuples so far:" + tupleCount);
				if (Flags.OUTLIMIT && tupleCount >= Consts.OutputLimit)
					throw new LimitExceededException();

			} else
				transition(max_depth, depth + 1, stat, count);
		} else {
			count[cur_vertex] = 1;
			Pool pl = pool.get(cur_vertex);
			ArrayList<PoolEntry> elist = pl.elist();

			for (int i : candBits) {
				PoolEntry e = elist.get(i);
				match[cur_vertex] = e;

				if (depth == max_depth - 1) {

					tupleCount += product(count);
					stat.setNumSolns(tupleCount);
					// System.out.println("No. of tuples so far:" + tupleCount);
					if (Flags.OUTLIMIT && tupleCount >= Consts.OutputLimit)
						throw new LimitExceededException();

				} else
					transition(max_depth, depth + 1, stat, count);

				match[cur_vertex] = null;

			}
		}

	}

	//use to get tupleCount. product of int list 'count'
	private double product(int[] count) {

		double rs = 1;

		for (int c : count) {

			rs *= c;
		}

		return rs;
	}

	//in transition(): getCandBits returns S, which are neighbors to previously matched graph node
	private RoaringBitmap getCandBits(int cur_vertex) {

		RoaringBitmap bits = new RoaringBitmap();
		int num = bn_count[cur_vertex]; //number of matched neighbors to cur_vertex?

		if (num == 0) {  //no neighbors b/c it's the first query vertex to match

			for (PoolEntry e : pool.get(cur_vertex).elist()) { //ALL nodes in nodeset of summary graph
				bits.add(e.getPos());

			}
			return bits;
		}

		//prev matches which are neighbors to the curr node set to match
		int[] bns = bn[cur_vertex];

		for (int i = 0; i < num; i++) {  //for each matched neighbor, intersect their adj list to cur_vertex
			int bn_vertex = bns[i]; //ID of matched neighbor
			DirType dir = query.dir(bn_vertex, cur_vertex);
			RoaringBitmap curbits;
			PoolEntry bm = match[bn_vertex];  //matched neighbor as poolentry (contains adj list)
			
			//bits: the final intersected list to return
			//curbits: the list of neigh in loop to intersect w/ bits
			if (dir == DirType.FWD) {

				curbits = bm.mFwdBits.get(cur_vertex); //adj list of matched neighbor
			} else {

				curbits = bm.mBwdBits.get(cur_vertex);
			}

			if (i == 0)
				bits.or(curbits);
			else
				bits.and(curbits);

		}

		return bits;
	}
	
	private void printMatch() {

		for (PoolEntry v : match) {

			System.out.print(v + " ");
		}

		System.out.println();
	}
	
	private void printMatchGNpos() {

		for (Integer v : matchGNpos) {

			System.out.print(v + " ");
		}

		System.out.println();
	}

	//get order of query nodes to match
	private int[] getPlan() {

		int[] order = null;
		if (Flags.ORDER == Consts.OrderType.RI)
			order = PlanGenerator.generateRIQueryPlan(query);
		else {

			int[] candidates_count = new int[query.V];

			for (int i = 0; i < query.V; i++) {
				candidates_count[i] = pool.get(i).elist().size();
			}

			if (Flags.ORDER == Consts.OrderType.GQL) {

				order = PlanGenerator.generateGQLQueryPlan(query, candidates_count);
			} else
				order = PlanGenerator.generateHybQueryPlan(query, candidates_count);

		}

		PlanGenerator.printSimplifiedQueryPlan(query, order);

		// int[] order = PlanGenerator.generateRIQueryPlan(query);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		// int[] order = PlanGenerator.generateHybQueryPlan(query,
		// candidates_count);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		// int[] order = PlanGenerator.generateTopoQueryPlan(query);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		// int[] order = PlanGenerator.generateRITOPOQueryPlan(query);
		// PlanGenerator.printSimplifiedQueryPlan(query, order);

		return order;
	}

	private boolean hasFollower(int i) {

		int cur_vertex = order[i];

		for (int j = i + 1; j < query.V; j++) {

			int next_vertex = order[j];
			if (query.checkEdgeExistence(cur_vertex, next_vertex))
				return true;
		}
		return false;
	}

	// matched neighbors for each vertex
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

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
