package randomQueryGenerator;

import java.util.ArrayList;


public class QuerySimulation {

	Query mTP1, mTP2;
	boolean[][] mH;
	
	int sz1, sz2;
	
	
	public QuerySimulation(Query tp1, Query tp2){
		
		mTP1 = tp1;
		mTP2 = tp2;
		sz1 = tp1.V;
		sz2 = tp2.V;
	}
	
	
	public QuerySimulation(Query tp){
		
		mTP1= mTP2 = tp;
		sz1 = sz2 = tp.V;
	}
	
	public boolean[][] findEndomo() {

		computeSimulation();

		return mH;
	}
	
	public boolean computeSimulation() {
		
		mH = new boolean[sz1][sz2];
		boolean[][] mC = new boolean[sz1][sz2];
		
		if (!bottomup_traversal(mC)) {

			return false;
		}

		topdown_traversal(mC);

		return true;
	}
	
	public boolean[][] getSimulation() {

		return mH;
	}
	
	private void topdown_traversal(boolean[][] mC) {

		boolean[][] mA = new boolean[sz1][sz2];

		for (int j = 0; j < sz2; j++) {
		
			for (int i = 0; i < sz1; i++) {
				
				mH[i][j] = mC[i][j] && sat_parent(i, j, mH, mA);

				mA[i][j] = mH[i][j] || satAncSubMatch(i, j, mA);

			}

		}

	}

	private boolean satAncSubMatch(int n1, int n2, boolean[][] mA) {

		if(n2 ==0)
			return false;
		
		QNode p2 = mTP2.getNode(n2).N_I.get(0);
		if (!mA[n1][p2.id])
			return false;

		return true;
	}

	
	private boolean sat_parent(int n1, int n2, boolean[][] mP, boolean[][] mA) {

		if (n1 == 0)
			return true;
		if (n1 != 0 && n2 == 0)
			return false;
		QNode q1 = mTP1.getNode(n1), q2 = mTP2.getNode(n2);
		QNode p1 = q1.N_I.get(0), p2 = q2.N_I.get(0);
        int axis1 = q1.axis, axis2 = q2.axis;
		
        if(axis1 == 0 && axis2 == 0 && mP[p1.id][p2.id])
        	return true;
        if(axis1 == 1 && mA[p1.id][p2.id])
			return true;

		return false;
	}

	private boolean bottomup_traversal(boolean[][] mC) {

		boolean[][] mD = new boolean[sz1][sz2];
		for (int j = sz2 - 1; j >= 0; j--) {
			for (int i = sz1 - 1; i >= 0; i--) {
				if (mTP1.getNode(i).lb.equals(mTP2.getNode(j).lb)
						&& sat_Children(i, j, mC, mD))
					mC[i][j] = true;
				mD[i][j] = mC[i][j] || satDescSubMatch(i, j, mD);
			}
		}

		return mD[0][0];

	}

	
	private boolean sat_Children(int n1, int n2, boolean[][] mC, boolean[][] mD) {

		if(mTP1.getNode(n1).N_O_SZ ==0)
			return true;
		ArrayList<QNode> children1 = mTP1.getNode(n1).N_O, children2 =  mTP2.getNode(n2).N_O;
		
		if(mTP2.getNode(n2).N_O_SZ ==0)
			return false;
		for(int i=0; i<children1.size(); i++){
			QNode child1 = children1.get(i);
			int ax1 = child1.axis;
			boolean hasMatch = false;
			
			for (int j=0; j<children2.size(); j++) {
				QNode child2 = children2.get(j);
				int ax2 = child2.axis;
				if(ax1 == 0 && ax2 == 0 && mC[child1.id][child2.id]) {// child
					hasMatch = true;
				}
				else if(ax1 == 1 && mD[child1.id][child2.id]){
					//descendant
					hasMatch = true;
				}
				
				if(hasMatch)
					break;
			}
			
			if (!hasMatch)
				return false;
		}
		
	
		return true;
	}
	
	
	private boolean satDescSubMatch(int n1, int n2, boolean[][] mD) {

		if(mTP2.getNode(n2).N_O_SZ ==0)
			return false;
		
		ArrayList<QNode> qChildren = mTP2.getNode(n2).N_O; 
		for (QNode qChild : qChildren) {
			if (mD[n1][qChild.id])
				return true;
		}

		return false;

	}

	
	public static void main(String[] args) {
	

	}

}
