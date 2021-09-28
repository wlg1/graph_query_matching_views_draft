/**
 * 
 */
package dao;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;

import graph.GraphNode;
import query.graph.QNode;

/**
 * @author xiaoying
 *
 */
public class PoolEntry2 implements Comparable<PoolEntry2> {

	int mPos;  // position in the pool
	QNode mQNode; // the corresponding query node it matches
	GraphNode mValue;
	public HashMap<Integer, ArrayList<PoolEntry2>> mFwdEntries, mBwdEntries;
	public HashMap<Integer, BitSet> mFwdBits, mBwdBits;
	
    double numChildren = 0; // total number of child entries
    double numParents = 0; // total number of parent entries
    
	double size = 0; // total number of solution tuples for the subquery rooted
						// at mQNode

	public PoolEntry2(QNode q, GraphNode val) {
		mQNode = q;
		mValue = val;
		initFBEntries();
	}

	public PoolEntry2(int pos, QNode q, GraphNode val) {
		mPos = pos; 
		mQNode = q;
		mValue = val;
		initFBEntries();
	}

	
	public boolean isSink() {

		return mQNode.N_O_SZ == 0;
	}

	public int getPos(){
		
		return mPos;
	}
	
	public int getQID() {

		return mQNode.id;
	}

	public QNode getQNode() {

		return mQNode;
	}

	public ArrayList<PoolEntry2> getFwdEntries(int qid) {

		return mFwdEntries.get(qid);
	}
	
	
	public BitSet getFwdBits(int qid) {

		return mFwdBits.get(qid);
	}
	
	public BitSet getBwdBits(int qid) {

		return mBwdBits.get(qid);
	}

	public ArrayList<PoolEntry2> getBwdEntries(int qid) {

		return mBwdEntries.get(qid);
	}

	
	public void addChild(PoolEntry2 c) {

		ArrayList<PoolEntry2> subs = mFwdEntries.get(c.getQID());
		subs.add(c);
		BitSet bits = mFwdBits.get(c.getQID());
		bits.set(c.mPos);
		numChildren++;
	}
	
	public void addParent(PoolEntry2 c) {

		ArrayList<PoolEntry2> subs = mBwdEntries.get(c.getQID());
		subs.add(c);
		BitSet bits = mBwdBits.get(c.getQID());
		bits.set(c.mPos);
		numParents++;
	}


	public GraphNode getValue() {

		return mValue;
	}
	
	public double getNumChildEnties(){
		
		return numChildren;
	}
	
	public double getNumParEnties(){
		
		return numParents;
	}

	public double size() {
		if (size == 0)
			getTotSolns(this);

		return size;
	}

	private void getTotSolns(PoolEntry2 e) {

		if (e.isSink()) {
			e.size = 1;
			return;
		}

		QNode qn = e.mQNode;
		double tot = 1;
		for (int c : qn.N_O) {
			ArrayList<PoolEntry2> elist = e.mFwdEntries.get(c);

			double tot_c = 0;
			for (int i = 0; i < elist.size(); i++) {
				PoolEntry2 sub = elist.get(i);
				//getTotSolns(sub);
				tot_c += sub.size();
			}
			tot *= tot_c;

		}
		e.size = tot;

	}

	@Override
	public int compareTo(PoolEntry2 other) {
		int rs = this.mValue.L_interval.mStart - other.mValue.L_interval.mStart;

		return rs;
	}

    public static Comparator<PoolEntry2> NodeIDComparator = new Comparator<PoolEntry2>(){

		@Override
		public int compare(PoolEntry2 n1, PoolEntry2 n2) {
			
			return n1.mValue.id - n2.mValue.id;
			
		}
    	
    	
    };
	
	public String toString() {

		StringBuilder s = new StringBuilder();
		toString(this, s);

		return s.toString();
	}

	private void toStringNested(PoolEntry2 e, StringBuilder s) {

		if (e.isSink()) {

			s.append(e.mValue.id);
			return;

		}

		s.append(e.mValue.id);

		QNode qn = e.mQNode;
		for (int c : qn.N_O) {
			s.append("{");
			ArrayList<PoolEntry2> elist = e.mFwdEntries.get(c);

			for (int i = 0; i < elist.size(); i++) {
				PoolEntry2 sub = elist.get(i);
				toStringNested(sub, s);
				if (i < elist.size() - 1)
					s.append(",");
			}
			s.append("}");
		}
	}

	private void toString(PoolEntry2 e, StringBuilder s) {

		s.append(e.mValue.id);
		s.append(" ");
	
	}
	
	private void initFBEntries() {

		if (mQNode.N_O_SZ > 0) {
			int sz = mQNode.N_O_SZ;
			mFwdEntries = new HashMap<Integer, ArrayList<PoolEntry2>>(sz);
			mFwdBits = new HashMap<Integer, BitSet>(sz);
			for (int cid : mQNode.N_O) {

				ArrayList<PoolEntry2> subs = new ArrayList<PoolEntry2>(1);
				mFwdEntries.put(cid, subs);
				BitSet bits = new BitSet();
				mFwdBits.put(cid, bits);
			}
			
		}

		
		if (mQNode.N_I_SZ > 0) {
			int sz = mQNode.N_I_SZ;
			mBwdEntries = new HashMap<Integer, ArrayList<PoolEntry2>>(sz);
			mBwdBits = new HashMap<Integer, BitSet>(sz);
			for (int pid : mQNode.N_I) {

				ArrayList<PoolEntry2> subs = new ArrayList<PoolEntry2>(1);
				mBwdEntries.put(pid, subs);
				BitSet bits = new BitSet();
				mBwdBits.put(pid, bits);
			}
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

	}

}
