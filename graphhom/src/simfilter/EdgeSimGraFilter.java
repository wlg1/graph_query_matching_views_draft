package simfilter;

import java.util.ArrayList;
import java.util.Collections;

import org.roaringbitmap.RoaringBitmap;

import dao.BFLIndex;
import dao.MatArray;
import global.Consts;
import global.Flags;
import global.Consts.DirType;
import graph.GraphNode;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import queryPlan.PlanGenerator;

public class EdgeSimGraFilter {

	Query mQuery;
	BFLIndex mBFL;
	int passNum = 0;
	RoaringBitmap[] mCandBitsArr;
	GraphNode[] mGraNodes;
	ArrayList<MatArray> mCandLists;
	ArrayList<ArrayList<GraphNode>> mInvLstsByID;
	ArrayList<RoaringBitmap> mBitsByIDArr;

	AdjMap[][] mFwdAdjMapList, mBwdAdjMapList;
	boolean invLstByQuery = false;

	public EdgeSimGraFilter(Query query, GraphNode[] graNodes, ArrayList<ArrayList<GraphNode>> invLstsByID,
			ArrayList<RoaringBitmap> bitsByIDArr) {

		mQuery = query;
		mInvLstsByID = invLstsByID;
		mGraNodes = graNodes;
		mBitsByIDArr = bitsByIDArr;
		init();

	}
	
	public EdgeSimGraFilter(Query query, GraphNode[] graNodes, ArrayList<ArrayList<GraphNode>> invLstsByID,
			ArrayList<RoaringBitmap> bitsByIDArr, boolean invLstByQuery) {

		mQuery = query;
		mInvLstsByID = invLstsByID;
		mGraNodes = graNodes;
		mBitsByIDArr = bitsByIDArr;
		this.invLstByQuery = invLstByQuery;
		init();

	}


	public void prune() {

		QEdge[] edges = mQuery.edges;
		// QEdge[] edges = getEdges();
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

	
	public RoaringBitmap[] getCandBits(){
		
		return mCandBitsArr;
	}
	

	private void filterBwd_c(QNode child, RoaringBitmap candBits) {

		RoaringBitmap union = unionBackAdj(child);
		candBits.and(union);

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
		QNode child = mQuery.nodes[to], parent = mQuery.nodes[from];
		RoaringBitmap candBits = mCandBitsArr[from];
		int card = candBits.getCardinality();
	
		filterBwd_c(child, candBits);
	
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
		QNode child = mQuery.nodes[to], parent = mQuery.nodes[from];
		RoaringBitmap candBits = mCandBitsArr[child.id];
		int card = candBits.getCardinality();
		filterFwd_c(parent, candBits);
	
		boolean hasChange = card > candBits.getCardinality() ? true : false;

		return hasChange;
	}

	private void filterFwd_c(QNode parent, RoaringBitmap candBits) {

		RoaringBitmap union = unionFwdAdj(parent);
		candBits.and(union);

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
	
	
	private void genCandList() {

		QNode[] qnodes = mQuery.nodes;
		mCandLists = new ArrayList<MatArray>(mQuery.V);
		
		for (int i = 0; i < qnodes.length; i++) {
			QNode q = qnodes[i];
			MatArray mlist = new MatArray();
			RoaringBitmap candBits = mCandBitsArr[q.id];
			ArrayList<GraphNode> list = bits2list(candBits);
			Collections.sort(list);
			mlist.addList(list);
			mCandLists.add(q.id, mlist);
		}
		
	}
	
	
	public ArrayList<MatArray> getCandList() {

		genCandList();
		return mCandLists;
	}
	
	private ArrayList<GraphNode> bits2list(RoaringBitmap bits) {

		ArrayList<GraphNode> list = new ArrayList<GraphNode>();
		for (int i : bits) {

			list.add(mGraNodes[i]);
		}

		return list;

	}

	private void init() {

		int size = mQuery.V;

		mFwdAdjMapList = new AdjMap[size][];
		mBwdAdjMapList = new AdjMap[size][];

		mCandBitsArr = new RoaringBitmap[size];
		QNode[] qnodes = mQuery.nodes;

		for (int i = 0; i < size; i++) {
			QNode q = qnodes[i];
			ArrayList<GraphNode> invLst;
			if(invLstByQuery)
			 invLst = mInvLstsByID.get(q.id);
			else
			 invLst = mInvLstsByID.get(q.lb);	
			AdjMap[] adjMap_f = new AdjMap[invLst.size()];
			mFwdAdjMapList[q.id] = adjMap_f;
			AdjMap[] adjMap_b = new AdjMap[invLst.size()];
			mBwdAdjMapList[q.id] = adjMap_b;

			for (int j = 0; j < invLst.size(); j++) {

				adjMap_f[j] = new AdjMap(mQuery.V);
				adjMap_b[j] = new AdjMap(mQuery.V);

			}

			if(invLstByQuery)
				mCandBitsArr[q.id] = mBitsByIDArr.get(q.id);
			else
				mCandBitsArr[q.id] = mBitsByIDArr.get(q.lb).clone();
		}

	}

	private QEdge[] getEdges() {

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

		int[] order = PlanGenerator.generateRITOPOQueryPlan(mQuery);
		PlanGenerator.printSimplifiedQueryPlan(mQuery, order);

		QEdge[] edges = new QEdge[mQuery.edges.length];
		int k = 0;
		for (int i = 1; i < mQuery.V; ++i) {
			int end_vertex = order[i];
			for (int j = 0; j < i; ++j) {
				int begin_vertex = order[j];
				DirType dir = mQuery.dir(begin_vertex, end_vertex);
				if (dir == DirType.FWD) {

					edges[k++] = mQuery.getEdge(begin_vertex, end_vertex);
				} else if (dir == DirType.BWD) {

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
