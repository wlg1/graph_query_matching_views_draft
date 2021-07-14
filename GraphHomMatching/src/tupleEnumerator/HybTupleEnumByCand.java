package tupleEnumerator;

import java.util.ArrayList;

import dao.Pool;
import dao.PoolEntry;
import global.Consts;
import helper.CartesianProduct;
import helper.QueryEvalStat;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;

public class HybTupleEnumByCand {

	private double tupleCount = 0;
	private Query query;
	private ArrayList<Pool> pool;
	private boolean toContinue = true;

	public HybTupleEnumByCand(Query qry, ArrayList<Pool> pl) {

		query = qry;
		pool = pl;
	}

	public double enumTreeTuples() {

		PoolEntry[] match = new PoolEntry[query.V];
		boolean[] state = new boolean[query.V];
		tupleCount = 0;
		toContinue = true;
		transition_tree(match, state);
		System.out.println("Total enumerated solution tuples:" + tupleCount);
		return tupleCount;
	}

	public double enumTreeTuples(QueryEvalStat stat) {

		PoolEntry[] match = new PoolEntry[query.V];
		boolean[] state = new boolean[query.V];
		tupleCount = 0;
		toContinue = true;
		transition_tree(match, state, stat);
		System.out.println("Total enumerated solution tuples:" + tupleCount);
		return tupleCount;
	}



	public double enumTuples() {

		PoolEntry[] match = new PoolEntry[query.V];
		boolean[] state = new boolean[query.V];
		tupleCount = 0;
		toContinue = true;
		transition(match, state);
		System.out.println("Total enumerated solution tuples:" + tupleCount);
		return tupleCount;
	}

	public double enumTuples(QueryEvalStat stat) {

		PoolEntry[] match = new PoolEntry[query.V];
		boolean[] state = new boolean[query.V];
		tupleCount = 0;
		toContinue = true;
		// transition_comb(match, state, stat);
		MinPQByCard minPQByCard = new MinPQByCard(query.V);
		transition(match, state, minPQByCard, stat);
		System.out.println("Total enumerated solution tuples:" + tupleCount);

		return tupleCount;
	}

	private QCPair getNext(PoolEntry[] match, boolean[] state, MinPQByCard minPQByCard) {

		if(minPQByCard.isEmpty()){
			
			ArrayList<QNode> extQNodes = extendableQNodes(match, state);
			ArrayList<ArrayList<PoolEntry>> candLists = new ArrayList<ArrayList<PoolEntry>>();

			for (QNode qn : extQNodes) {
				ArrayList<PoolEntry> candList = getCandList(qn, match);
				// backtrack if one of the lists is empty
				if (candList.isEmpty()) {
					// System.out.println("Empty!");
					clear(candLists);
					return null;
				}
				candLists.add(candList);
				minPQByCard.insert(qn.id, candList);
			}

			
		}
		
		
		return minPQByCard.deleteMin();

	}

	private void transition(PoolEntry[] match, boolean[] state, MinPQByCard minPQByCard, QueryEvalStat stat) {

		int matched = matched(state);
		
		if (tupleCount >= Consts.OutputLimit) { toContinue = false; return; }
		 
		if (matched == query.V) {
			tupleCount++;
			stat.setNumSolns(tupleCount);
			// printMatch(match);

		}

		else {

			QCPair qc = getNext(match, state, minPQByCard); 
				
			if(qc==null)
				return;
			ArrayList<PoolEntry> candList = qc.c;
			state[qc.i] = true;

			for (PoolEntry e : candList) {

				match[qc.i] = e;
				transition(match, state, minPQByCard, stat);
				if (!toContinue)
				 return; // break from here

			}

			match[qc.i] = null;
			state[qc.i] = false;

		}

	}

	private void transition(PoolEntry[] match, boolean[] state) {

		int matched = matched(state);
		if (tupleCount >= Consts.OutputLimit) { toContinue = false; return; }
		
		if (matched == query.V) {
			tupleCount++;
			// printMatch(match);

		}

		else {

			ArrayList<QNode> extQNodes = extendableQNodes(match, state);
			ArrayList<ArrayList<PoolEntry>> candLists = new ArrayList<ArrayList<PoolEntry>>();

			for (QNode qn : extQNodes) {
				ArrayList<PoolEntry> candList = getCandList(qn, match);
				// backtrack if one of the lists is empty
				if (candList.isEmpty()) {
					clear(candLists);
					return;
				}
				candLists.add(candList);

			}

			int idx = chooseByCandSize(extQNodes, candLists);

			QNode qn = extQNodes.get(idx);
			ArrayList<PoolEntry> candList = candLists.get(idx);

			state[qn.id] = true;
			for (PoolEntry e : candList) {

				match[qn.id] = e;
				transition(match, state);
				if (!toContinue)
					return; // break from here

			}
			match[qn.id] = null;
			state[qn.id] = false;

		}

	}

	private void transition_tree(PoolEntry[] match, boolean[] state, QueryEvalStat stat) {

		int matched = matched(state);
		if (tupleCount >= 1000) {
			toContinue = false;
			return;
		}

		if (matched == query.V) {
			tupleCount++;
			stat.setNumSolns(tupleCount);
			// printMatch(match);

		} else {

			ArrayList<QNode> extQNodes = extendableQNodes(match, state);
 		    if (extQNodes.size() == 1) {
				QNode qn = extQNodes.get(0);
				ArrayList<PoolEntry> candList = getTreeCandList(qn, match);
				if (candList.isEmpty()) {
					return;
				}
				state[qn.id] = true;
				for (PoolEntry e : candList) {

					match[qn.id] = e;
					transition_tree(match, state, stat);
					if (!toContinue)
						return; // break from here
				}

			}

			else {

				ArrayList<ArrayList<PoolEntry>> candLists = new ArrayList<ArrayList<PoolEntry>>();

				for (QNode qn : extQNodes) {
					ArrayList<PoolEntry> candList = getTreeCandList(qn, match);
					// backtrack if one of the lists is empty
					if (candList.isEmpty()) {
						clear(candLists);
						return;
					}
					candLists.add(candList);

				}

				long[] lengths = new long[extQNodes.size()];
				int i = 0;
				for (ArrayList<PoolEntry> list : candLists) {
					lengths[i++] = list.size();
				}

				setState(state, extQNodes);
				for (long[] indices : new CartesianProduct(lengths)) {

					for (i = 0; i < candLists.size(); i++) {
						PoolEntry e = candLists.get(i).get((int) indices[i]);
						match[e.getQID()] = e;
					}

					transition_tree(match, state, stat);
					if (!toContinue)
						return; // break from here

				}

			}
			clearState(match, state, extQNodes);
		}

	}

	private void transition_comb(PoolEntry[] match, boolean[] state, QueryEvalStat stat) {

		int matched = matched(state);
		if (tupleCount >= 1000) {
			toContinue = false;
			return;
		}

		if (matched == query.V) {
			tupleCount++;
			stat.setNumSolns(tupleCount);
			// printMatch(match);

		} else {

			ArrayList<QNode> extQNodes = extendableQNodes(match, state);

			if (extQNodes.size() == 1) {
				QNode qn = extQNodes.get(0);
				ArrayList<PoolEntry> candList = getCandList(qn, match);
				if (candList.isEmpty()) {
					return;
				}
				state[qn.id] = true;
				for (PoolEntry e : candList) {

					match[qn.id] = e;
					transition_comb(match, state, stat);
					if (!toContinue)
						return; // break from here
				}

			}

			else {

				ArrayList<ArrayList<PoolEntry>> candLists = new ArrayList<ArrayList<PoolEntry>>();

				for (QNode qn : extQNodes) {
					ArrayList<PoolEntry> candList = getCandList(qn, match);
					// backtrack if one of the lists is empty
					if (candList.isEmpty()) {
						clear(candLists);
						return;
					}
					candLists.add(candList);

				}

				long[] lengths = new long[extQNodes.size()];
				int i = 0;
				for (ArrayList<PoolEntry> list : candLists) {
					lengths[i++] = list.size();
				}

				setState(state, extQNodes);
				for (long[] indices : new CartesianProduct(lengths)) {

					for (i = 0; i < candLists.size(); i++) {
						PoolEntry e = candLists.get(i).get((int) indices[i]);
						match[e.getQID()] = e;
					}

					transition_comb(match, state, stat);
					if (!toContinue)
						return; // break from here

				}

			}
			clearState(match, state, extQNodes);
		}

	}

	private boolean transition_comb(PoolEntry[] match, boolean[] state) {

		int matched = matched(state);
		if (tupleCount >= 1000)
			return false;

		if (matched == query.V) {
			tupleCount++;

			// printMatch(match);

		} else {

			ArrayList<QNode> extQNodes = extendableQNodes(match, state);

			if (extQNodes.size() == 1) {
				QNode qn = extQNodes.get(0);
				ArrayList<PoolEntry> candList = getCandList(qn, match);
				if (candList.isEmpty()) {
					return false;
				}

				state[qn.id] = true;
				for (PoolEntry e : candList) {

					match[qn.id] = e;

					boolean toContinue = transition_comb(match, state);
					if (!toContinue)
						return false; // break from here

				}

			} else {

				ArrayList<ArrayList<PoolEntry>> candLists = new ArrayList<ArrayList<PoolEntry>>();

				for (QNode qn : extQNodes) {
					ArrayList<PoolEntry> candList = getCandList(qn, match);
					// backtrack if one of the lists is empty
					if (candList.isEmpty()) {
						clear(candLists);
						return false;
					}
					candLists.add(candList);

				}

				long[] lengths = new long[extQNodes.size()];
				int i = 0;
				for (ArrayList<PoolEntry> list : candLists) {
					lengths[i++] = list.size();
				}

				setState(state, extQNodes);
				for (long[] indices : new CartesianProduct(lengths)) {

					for (i = 0; i < candLists.size(); i++) {
						PoolEntry e = candLists.get(i).get((int) indices[i]);
						match[e.getQID()] = e;
						state[e.getQID()] = true;
					}

					boolean toContinue = transition_comb(match, state);
					if (!toContinue)
						return false; // break from here
				}

			}

			clearState(match, state, extQNodes);
		}

		return true;

	}

	private boolean transition_tree(PoolEntry[] match, boolean[] state) {

		int matched = matched(state);
		if (tupleCount >= Consts.OutputLimit)
			return false;

		if (matched == query.V) {
			tupleCount++;

			// printMatch(match);

		} else {

			ArrayList<QNode> extQNodes = extendableQNodes(match, state);

			if (extQNodes.size() == 1) {
				QNode qn = extQNodes.get(0);
				ArrayList<PoolEntry> candList = getTreeCandList(qn, match);
				if (candList.isEmpty()) {
					return false;
				}

				state[qn.id] = true;
				for (PoolEntry e : candList) {

					match[qn.id] = e;

					boolean toContinue = transition_tree(match, state);
					if (!toContinue)
						return false; // break from here

				}

			} else {

				ArrayList<ArrayList<PoolEntry>> candLists = new ArrayList<ArrayList<PoolEntry>>();

				for (QNode qn : extQNodes) {
					ArrayList<PoolEntry> candList = getTreeCandList(qn, match);
					// backtrack if one of the lists is empty
					if (candList.isEmpty()) {
						clear(candLists);
						return false;
					}
					candLists.add(candList);

				}

				long[] lengths = new long[extQNodes.size()];
				int i = 0;
				for (ArrayList<PoolEntry> list : candLists) {
					lengths[i++] = list.size();
				}

				setState(state, extQNodes);
				for (long[] indices : new CartesianProduct(lengths)) {

					for (i = 0; i < candLists.size(); i++) {
						PoolEntry e = candLists.get(i).get((int) indices[i]);
						match[e.getQID()] = e;
						state[e.getQID()] = true;
					}

					boolean toContinue = transition_comb(match, state);
					if (!toContinue)
						return false; // break from here
				}

			}

			clearState(match, state, extQNodes);
		}

		return true;

	}

	private void clearState(PoolEntry[] match, boolean[] state, ArrayList<QNode> extQNodes) {

		for (QNode qn : extQNodes) {
			match[qn.id] = null;
			state[qn.id] = false;
		}
	}

	private void setState(boolean[] state, ArrayList<QNode> extQNodes) {

		for (QNode qn : extQNodes) {
			state[qn.id] = true;
		}
	}

	private void printMatch(PoolEntry[] match) {

		for (PoolEntry v : match) {

			System.out.print(v + " ");
		}

		System.out.println();
	}

	private void clear(ArrayList<ArrayList<PoolEntry>> candLists) {

		for (ArrayList<PoolEntry> list : candLists)

			list.clear();
	}

	private ArrayList<PoolEntry> getCandList(QNode qn, PoolEntry[] match) {

		int num = qn.N_I_SZ;

		if (num == 0) {

			return pool.get(qn.id).elist();
		}

		ArrayList<QEdge> i_edges = qn.E_I;
		int qid = qn.id;

		ArrayList<ArrayList<PoolEntry>> matLists = new ArrayList<ArrayList<PoolEntry>>(num);
		for (QEdge i_edge : i_edges) {

			int pid = i_edge.from;
			PoolEntry pm = match[pid];
			ArrayList<PoolEntry> matList = pm.mFwdEntries.get(qid);
			matLists.add(matList);

		}

		ArrayList<PoolEntry> results = merge(matLists, num);

		return results;
	}

	private ArrayList<PoolEntry> getTreeCandList(QNode qn, PoolEntry[] match) {

		int num = qn.N_I_SZ;

		if (num == 0) {

			return pool.get(qn.id).elist();
		}

		QEdge i_edge = qn.E_I.get(0);
		int qid = qn.id;

		int pid = i_edge.from;
		PoolEntry pm = match[pid];
		ArrayList<PoolEntry> matList = pm.mFwdEntries.get(qid);

		return matList;
	}

	private ArrayList<PoolEntry> merge(ArrayList<ArrayList<PoolEntry>> matLists, int num) {
		// each list is sorted in ascending order of mStart
		ArrayList<PoolEntry> results = new ArrayList<PoolEntry>();
		int[] idx = new int[num];
		int[] len = new int[num];

		for (int i = 0; i < num; i++) {

			idx[i] = 0;
			len[i] = matLists.get(i).size();

		}

		while (!eof(idx, len, num)) {

			PoolEntry minEntry = minEntry(matLists, idx, num);
			PoolEntry maxEntry = maxEntry(matLists, idx, num);
			if (minEntry.compareTo(maxEntry) == 0) {

				results.add(minEntry);
			}

			advance(minEntry, matLists, idx, len, num);
		}

		return results;
	}

	private void advance(PoolEntry minEntry, ArrayList<ArrayList<PoolEntry>> matLists, int[] idx, int[] len, int num) {

		for (int i = 0; i < num; i++) {
			ArrayList<PoolEntry> matList = matLists.get(i);
			if (idx[i] < len[i] && minEntry.compareTo(matList.get(idx[i])) == 0) {

				idx[i]++;
			}

		}

	}

	private PoolEntry minEntry(ArrayList<ArrayList<PoolEntry>> matLists, int[] idx, int num) {

		PoolEntry minEntry = matLists.get(0).get(idx[0]);

		for (int i = 1; i < num; i++) {

			PoolEntry entry = matLists.get(i).get(idx[i]);
			if (minEntry.compareTo(entry) > 0)
				minEntry = entry;

		}

		return minEntry;
	}

	private PoolEntry maxEntry(ArrayList<ArrayList<PoolEntry>> matLists, int[] idx, int num) {

		PoolEntry maxEntry = matLists.get(0).get(idx[0]);

		for (int i = 1; i < num; i++) {

			PoolEntry entry = matLists.get(i).get(idx[i]);
			if (maxEntry.compareTo(entry) < 0)
				maxEntry = entry;

		}

		return maxEntry;
	}

	private boolean eof(int[] idx, int[] len, int num) {

		for (int i = 0; i < num; i++) {

			if (idx[i] == len[i])
				return true;
		}

		return false;
	}

	private int matched(boolean[] state) {

		int matched = 0;
		for (boolean s : state) {

			if (s)
				matched++;
		}

		return matched;
	}

	private ArrayList<QNode> extendableQNodes(PoolEntry[] match, boolean[] state) {

		ArrayList<QNode> results = new ArrayList<QNode>();
		QNode[] qnodes = query.nodes;
		boolean extendable = true;
		for (QNode qn : qnodes) {
			int id = qn.id;
			QNode q = qnodes[id];
			extendable = true;
			if (state[q.id])
				continue;
			ArrayList<Integer> parents = q.N_I;

			if (q.N_I_SZ > 0)
				for (int p : parents) {
					if (!state[p]) {
						extendable = false;

					}

				}

			if (extendable)
				results.add(q);

		}

		return results;
	}

	private int chooseByCandSize(ArrayList<QNode> qnodes, ArrayList<ArrayList<PoolEntry>> candLists) {

		int min = Integer.MAX_VALUE;

		int idx = 0;

		for (int i = 0; i < qnodes.size(); i++) {

			QNode q = qnodes.get(i);
			ArrayList<PoolEntry> candList = candLists.get(i);
			if (min > candList.size()) {

				min = candList.size();
				idx = i;
			}
		}

		return idx;

	}

	public static void main(String[] args) {
		

	}

}
