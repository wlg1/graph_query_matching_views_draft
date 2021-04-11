package simfilter;

import java.util.ArrayList;
import java.util.Collections;

import org.roaringbitmap.RoaringBitmap;

import dao.BFLIndex;
import dao.MatArray;
import global.Consts;
import global.Flags;
import global.Consts.AxisType;
import global.Consts.DirType;
import graph.GraphNode;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import queryPlan.PlanGenerator;

public class GraSimGraFilter {

	Query mQuery;
	ArrayList<MatArray> mCandLists;
	BFLIndex mBFL;
	int passNum = 0;
	RoaringBitmap[] mCandBitsArr;
    int[] candidates_count;
	GraphNode[] mGraNodes;

	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	ArrayList<RoaringBitmap> mBitsByIDArr;

	AdjMap[][] mFwdAdjMapList, mBwdAdjMapList;

	public GraSimGraFilter(Query query, GraphNode[] graNodes, ArrayList<ArrayList<GraphNode>> invLstsByID,
			ArrayList<RoaringBitmap> bitsByIDArr, BFLIndex bfl) {

		mQuery = query;
		mBFL = bfl;
		mInvLstsByID = invLstsByID;
		mGraNodes = graNodes;
		mBitsByIDArr = bitsByIDArr;
		init();

	}

	public void prune() {

		QEdge[] edges = mQuery.edges; 
		//QEdge[] edges = getEdges(); 
		passNum = 0;
		boolean hasChange = false;// = backwardCheck(edges);

		do {
			if(Flags.PRUNELIMIT&&passNum>Consts.PruneLimit)
				break;
			hasChange = backwardCheck(edges);
			hasChange = hasChange || forwardCheck(edges);

		} while (hasChange);
		System.out.println("Total passes: " + passNum);
	}

	public ArrayList<MatArray> getCandList() {

		for (int i = 0; i < mQuery.V; i++) {
			QNode q = mQuery.nodes[i];
			ArrayList<GraphNode> list = mCandLists.get(i).elist();
			Collections.sort(list);
			// System.out.println("qid = " + i +" " + " inv= " +
			// this.mInvLstsByID.get(q.lb).size()+ " original bits = " +
			// this.mBitsByIDArr.get(q.lb).getCardinality() + " bits = " +
			// this.mCandBitsArr[i].getCardinality() + " list = " +
			// this.mCandLists.get(i).elist().size());
		}
		return mCandLists;
	}

	private void filterBwd_c(QNode child, RoaringBitmap candBits) {

		RoaringBitmap union = unionBackAdj(child);
		candBits.and(union);

	}

	private void clearAdjMap(ArrayList<GraphNode> srcList, QNode srcNode, DirType dir) {
		ArrayList<Integer> tarList = null;
		AdjMap[] adjMapList = null;
		if (dir == DirType.FWD) {

			tarList = srcNode.N_O;
			adjMapList = mFwdAdjMapList[srcNode.id];
		} else {

			tarList = srcNode.N_I;
			adjMapList = mBwdAdjMapList[srcNode.id];
		}

		if (tarList == null)
			return;

		for (GraphNode n : srcList) {
			AdjMap adjMap = adjMapList[n.pos];
			for (int i : tarList) {
				ArrayList<Integer> vals = adjMap.getValue(i);

				if (vals == null)
					continue;

				for (int id : vals) {
					GraphNode cn = this.mGraNodes[id];
					AdjMap[] adjMapList_c = null;
					if (dir == DirType.BWD) {
						adjMapList_c = mFwdAdjMapList[i];
					} else
						adjMapList_c = mBwdAdjMapList[i];
					AdjMap adjMap_c = adjMapList_c[cn.pos];
					adjMap_c.clear(srcNode.id, n.id);
				}

				adjMap.clear(i);
			}
		}

	}

	private void filterBwd_d(QNode parent, QNode child, ArrayList<GraphNode> candList_p, RoaringBitmap candBits_p) {

		ArrayList<GraphNode> rmvList = new ArrayList<GraphNode>();
		RoaringBitmap rmvBits = new RoaringBitmap();

		AdjMap[] adjMapList_p = mFwdAdjMapList[parent.id];

		for (GraphNode gn : candList_p) {
			AdjMap adjmap_p = adjMapList_p[gn.pos];

			boolean found = false;
			
			if (adjmap_p.getValue(child.id) != null) {
				// bupc++;
				continue;
			}

			AdjMap[] adjMapList_c = mBwdAdjMapList[child.id];
			MatArray mli = mCandLists.get(child.id);
			for (GraphNode ni : mli.elist()) {

				if (gn.id == ni.id)
					continue;
				AdjMap adjmap_c = adjMapList_c[ni.pos];
				if (mBFL.reach(gn, ni) == 1) {
					found = true;
					adjmap_p.addValue(child.id, ni.id);
					adjmap_c.addValue(parent.id, gn.id);
					break;
				}

			}

			if (!found) {
				rmvList.add(gn);
				rmvBits.add(gn.id);
				
			}

		}

		candBits_p.xor(rmvBits);
		candList_p.removeAll(rmvList);
		clearAdjMap(rmvList, parent, DirType.BWD);

	}

	private ArrayList<GraphNode> bits2list(RoaringBitmap bits) {

		ArrayList<GraphNode> list = new ArrayList<GraphNode>();
		for (int i : bits) {

			list.add(mGraNodes[i]);
		}

		return list;

	}

	private boolean backwardCheck(QEdge[] edges) {

		boolean hasChange = false;
		passNum++;
		for (QEdge e : edges) {
			boolean result = backwardCheck(e);
			hasChange = hasChange || result;
		}

		return hasChange;
	}

	private boolean backwardCheck(QEdge e) {

		int from = e.from, to = e.to;
		AxisType axis = e.axis;
		QNode child = mQuery.nodes[to], parent = mQuery.nodes[from];
		RoaringBitmap candBits = mCandBitsArr[from];
		int card = candBits.getCardinality();
		MatArray mli = mCandLists.get(parent.id);
		ArrayList<GraphNode> elist = mli.elist();

		if (axis == AxisType.child){
			filterBwd_c(child, candBits);
			mli.setList(bits2list(candBits));
		} else
			filterBwd_d(parent, child, elist, candBits);

		boolean hasChange = card > candBits.getCardinality() ? true : false;

		return hasChange;
	}

	private boolean forwardCheck(QEdge[] edges) {

		boolean hasChange = false;
		passNum++;
		for (QEdge e : edges) {
			boolean result = forwardCheck(e);
			hasChange = hasChange || result;
		}

		return hasChange;
	}

	private boolean forwardCheck(QEdge e) {

		int from = e.from, to = e.to;
		AxisType axis = e.axis;
		QNode child = mQuery.nodes[to], parent = mQuery.nodes[from];
		RoaringBitmap candBits = mCandBitsArr[child.id];
		int card = candBits.getCardinality();
		MatArray mli = mCandLists.get(child.id);
		ArrayList<GraphNode> elist = mli.elist();
		if (axis == AxisType.child){
			filterFwd_c(parent, candBits);
			mli.setList(bits2list(candBits));
		} else
			filterFwd_d(parent, child, elist, candBits);

		boolean hasChange = card > candBits.getCardinality() ? true : false;

		return hasChange;
	}

	private void filterFwd_c(QNode parent, RoaringBitmap candBits) {

		RoaringBitmap union = unionFwdAdj(parent);
		candBits.and(union);

	}

	private void filterFwd_d(QNode parent, QNode child, ArrayList<GraphNode> candList_c, RoaringBitmap candBits_c) {

		ArrayList<GraphNode> rmvList = new ArrayList<GraphNode>();
		RoaringBitmap rmvBits = new RoaringBitmap();

		AdjMap[] adjMapList_c = mBwdAdjMapList[child.id];

		for (GraphNode gn : candList_c) {
			AdjMap adjmap_c = adjMapList_c[gn.pos];
			
			if (adjmap_c.getValue(parent.id) != null) {
				// tdwc++;
				continue;
			}
			
			boolean found = false;
			AdjMap[] adjMapList_p = mFwdAdjMapList[parent.id];
			MatArray mli = mCandLists.get(parent.id);
			for (GraphNode par : mli.elist()) {

				if (gn.id == par.id)
					continue;
				AdjMap adjmap_p = adjMapList_p[par.pos];
				if (mBFL.reach(par, gn) == 1) {
					found = true;
					adjmap_p.addValue(child.id, gn.id);
					adjmap_c.addValue(parent.id, par.id);

					break;
				}

			}

			if (!found) {
				rmvList.add(gn);
				rmvBits.add(gn.id);
				
			}
		}

		candBits_c.xor(rmvBits);
		candList_c.removeAll(rmvList);
		clearAdjMap(rmvList, child, DirType.FWD);
	}

	//////////////////////////////////////////////


	private RoaringBitmap unionBackAdj(QNode child) {

		RoaringBitmap candBits = mCandBitsArr[child.id];
		RoaringBitmap union = new RoaringBitmap();

		for (int i : candBits) {

			GraphNode gn = this.mGraNodes[i];
			union.or(gn.adj_bits_id_i);

		}

		return union;

	}

	private RoaringBitmap unionFwdAdj(QNode parent) {

		RoaringBitmap candBits = mCandBitsArr[parent.id];
		RoaringBitmap union = new RoaringBitmap();

		for (int i : candBits) {

			GraphNode gn = this.mGraNodes[i];
			union.or(gn.adj_bits_id_o);

		}

		return union;

	}

	private void init() {

		int size = mQuery.V;

		mFwdAdjMapList = new AdjMap[size][];
		mCandLists = new ArrayList<MatArray>(size);
		mBwdAdjMapList = new AdjMap[size][];

		mCandBitsArr = new RoaringBitmap[size];
		QNode[] qnodes = mQuery.nodes;

		for (int i = 0; i < size; i++) {
			QNode q = qnodes[i];
			ArrayList<GraphNode> invLst = mInvLstsByID.get(q.lb);
			AdjMap[] adjMap_f = new AdjMap[invLst.size()];
			mFwdAdjMapList[q.id] = adjMap_f;
			AdjMap[] adjMap_b = new AdjMap[invLst.size()];
			mBwdAdjMapList[q.id] = adjMap_b;

			for (int j = 0; j < invLst.size(); j++) {

				adjMap_f[j] = new AdjMap(mQuery.V);
				adjMap_b[j] = new AdjMap(mQuery.V);

			}

			MatArray mlist = new MatArray();
			mlist.addList(invLst);
			mCandLists.add(q.id, mlist);
			RoaringBitmap t_bits = mBitsByIDArr.get(q.lb);
			mCandBitsArr[q.id] = t_bits.clone();
		}

	}

	private QEdge[] getEdges() {

		//int[] order = PlanGenerator.generateGQLQueryPlan(query, candidates_count);
		//PlanGenerator.printSimplifiedQueryPlan(query, order);

		//int[] order = PlanGenerator.generateRIQueryPlan(query);
		//PlanGenerator.printSimplifiedQueryPlan(query, order);

		//int[] order = PlanGenerator.generateHybQueryPlan(query, candidates_count);
		//PlanGenerator.printSimplifiedQueryPlan(query, order);

		//int[] order = PlanGenerator.generateTopoQueryPlan(query);
		//PlanGenerator.printSimplifiedQueryPlan(query, order);
		 
		 int[] order = PlanGenerator.generateRITOPOQueryPlan(mQuery);
		 PlanGenerator.printSimplifiedQueryPlan(mQuery, order);

		 QEdge[] edges = new QEdge[mQuery.edges.length];
		 int k=0;
			for (int i = 1; i < mQuery.V; ++i) {
				int end_vertex = order[i];
				for (int j = 0; j < i; ++j) {
					int begin_vertex = order[j];
					DirType dir = mQuery.dir(begin_vertex, end_vertex);
					if (dir == DirType.FWD) {
						
						edges[k++] = mQuery.getEdge(begin_vertex, end_vertex);
					}
					else if(dir == DirType.BWD){
						
						edges[k++] = mQuery.getEdge(end_vertex, begin_vertex);
					}
				}
				
			}

		 
		return edges;
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
