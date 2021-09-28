/**
 * 
 */
package dao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.roaringbitmap.RoaringBitmap;

import graph.GraphNode;
import query.graph.QNode;

/**
 * @author xiaoying
 *
 */
public class PoolEntry implements Comparable<PoolEntry> {

	int mPos; // position in the pool
	QNode mQNode; // the corresponding query node it matches
	GraphNode mValue;
	public HashMap<Integer, RoaringBitmap> mFwdBits, mBwdBits;

	double numChildren = 0; // total number of child entries
	double numParents = 0; // total number of parent entries

	double size = 0; // total number of solution tuples for the subquery rooted at mQNode

	boolean counted = false;

	public PoolEntry(QNode q, GraphNode val) {
		mQNode = q;
		mValue = val;
		initFBEntries();
	}

	public PoolEntry(int pos, QNode q, GraphNode val) {
		mPos = pos;
		mQNode = q;
		mValue = val;
		initFBEntries();
	}

	public PoolEntry(int pos, QNode q, GraphNode val, ArrayList<PoolEntry> ves, ArrayList<HashSet<Integer>> vns) {
		mPos = pos;
		mQNode = q;
		mValue = val;
		initFBEntries();
	}

	public boolean isSink() {

		return mQNode.N_O_SZ == 0;
	}

	public int getPos() {

		return mPos;
	}

	public int getQID() {

		return mQNode.id;
	}

	public QNode getQNode() {

		return mQNode;
	}

	public RoaringBitmap getFwdBits(int qid) {

		return mFwdBits.get(qid);
	}

	public RoaringBitmap getBwdBits(int qid) {

		return mBwdBits.get(qid);
	}

	public void addChild(PoolEntry c) {

		RoaringBitmap bits = mFwdBits.get(c.getQID());
		// bits.add(c.mPos);
		bits.add(c.mValue.id);
		//numChildren++;
	}

	public void addParent(PoolEntry c) {

		RoaringBitmap bits = mBwdBits.get(c.getQID());
		// bits.add(c.mPos);
		bits.add(c.mValue.id);
		//numParents++;
	}

	public GraphNode getValue() {

		return mValue;
	}

	public double getNumChildEnties() {

		if (!counted) {

			calNumChildEnties();
			counted = true;
		}

		return numChildren;
	}

	public double getNumParEnties() {

		return numParents;
	}

	private void calNumChildEnties() {
		if (mFwdBits != null) {
			Set<Integer> keys = mFwdBits.keySet();

			for (int k : keys) {

				RoaringBitmap bits = mFwdBits.get(k);
				numChildren += bits.getCardinality();
			}
		}

	}

	@Override
	public int compareTo(PoolEntry other) {
		int rs = this.mValue.L_interval.mStart - other.mValue.L_interval.mStart;

		return rs;
	}

	public static Comparator<PoolEntry> NodeIDComparator = new Comparator<PoolEntry>() {

		@Override
		public int compare(PoolEntry n1, PoolEntry n2) {

			return n1.mValue.id - n2.mValue.id;

		}

	};

	public String toString() {

		StringBuilder s = new StringBuilder();
		toString(this, s);

		return s.toString();
	}

	private void toString(PoolEntry e, StringBuilder s) {

		s.append(e.mValue.id);
		s.append(" ");

	}

	private void initFBEntries() {

		if (mQNode.N_O_SZ > 0) {
			int sz = mQNode.N_O_SZ;
			mFwdBits = new HashMap<Integer, RoaringBitmap>(sz);
			for (int cid : mQNode.N_O) {

				RoaringBitmap bits = new RoaringBitmap();
				mFwdBits.put(cid, bits);
			}

		}

		if (mQNode.N_I_SZ > 0) {
			int sz = mQNode.N_I_SZ;
			mBwdBits = new HashMap<Integer, RoaringBitmap>(sz);
			for (int pid : mQNode.N_I) {

				RoaringBitmap bits = new RoaringBitmap();
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
