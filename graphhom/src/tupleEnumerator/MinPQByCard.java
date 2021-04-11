package tupleEnumerator;

import java.util.ArrayList;
import java.util.Arrays;

import dao.PoolEntry;
import edu.princeton.cs.algs4.IndexMinPQ;

public class MinPQByCard extends IndexMinPQ<Integer> {

	
	ArrayList<ArrayList<PoolEntry>> candLists;
	

	public MinPQByCard(int maxN) {
		super(maxN);
		candLists = new ArrayList<ArrayList<PoolEntry>>(maxN);
		for(int i =0; i<maxN; i++)
			candLists.add(null);
	}

	public void insert(int i, ArrayList<PoolEntry> l) {
		
		super.insert(i, l.size());
		candLists.set(i, l);
	}

	public QCPair deleteMin() {

		int idx = super.delMin();
		ArrayList<PoolEntry> l = candLists.get(idx);
		candLists.set(idx, null);
		return new QCPair(idx,l);
	}

}
