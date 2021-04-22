package answerGraph;

import java.util.ArrayList;
import org.roaringbitmap.RoaringBitmap;

import dao.BFLIndex;
import dao.MatArray;
import dao.Pool;
import dao.PoolEntry;
import global.Consts.AxisType;
import graph.GraphNode;
import query.graph.QEdge;
import query.graph.QNode;
import query.graph.Query;
import queryPlan.PlanGenerator;

//only add edge to nodes if covering edge has it

public class HybAnsGraphBuilderViews {

	Query mQuery;
	ArrayList<Pool> mPool;
	BFLIndex mBFL;
	ArrayList<MatArray> mCandLists;
	
	public HybAnsGraphBuilderViews(Query query, BFLIndex bfl,
			ArrayList<MatArray> candLsts) {

		mQuery = query;
		mBFL = bfl;
		mCandLists = candLsts;
	}

	public ArrayList<Pool> run() {

		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V]; //each nodeset has its own bitmap of GN in it
		initPool(tBitsIdxArr);

		for(QEdge edge: mQuery.edges){
			
			linkOneStep(edge,tBitsIdxArr);
		}
		
		return mPool;

	}
	
	
	// for tree/dag
	public ArrayList<Pool> runBUP(){
		
		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V];
		initPool(tBitsIdxArr);
		
		int[] order = PlanGenerator.generateTopoQueryPlan(mQuery);
		
		for(int i=mQuery.V-1; i>=0; i--){
			QNode q = mQuery.getNode(order[i]);
		    ArrayList<QEdge> edges = q.E_I;
		    if(edges!=null)
		    for(QEdge edge: edges){
				
				linkOneStep(edge,tBitsIdxArr);
			}
		}
		
		return mPool;
	}

	public ArrayList<Pool> runTDW(){
		
		RoaringBitmap[] tBitsIdxArr = new RoaringBitmap[mQuery.V];
		initPool(tBitsIdxArr);
		
		int[] order = PlanGenerator.generateTopoQueryPlan(mQuery);
		
		for(int i=0; i<mQuery.V; i++){
			QNode q = mQuery.getNode(order[i]);
		    ArrayList<QEdge> edges = q.E_I;
		    if(edges!=null)
		    for(QEdge edge: edges){
				
				linkOneStep(edge,tBitsIdxArr);
			}
		}
		
		return mPool;
	}

	
	private void initPool(RoaringBitmap[] tBitsIdxArr) {
		
		//first get all graph nodes of nodesets of covering views
		//these are less of these than the candidate nodes that come from mCandLists.get(i)

		mPool = new ArrayList<Pool>(mQuery.V);
		QNode[] qnodes = mQuery.nodes;
		for (int i = 0; i < mQuery.V; i++) {
			Pool qAct = new Pool();
			mPool.add(qAct);
			MatArray mli = mCandLists.get(i);
			ArrayList<GraphNode> elist = mli.elist();
			QNode qn = qnodes[i];
			RoaringBitmap t_bits = new RoaringBitmap();
			tBitsIdxArr[i] = t_bits;
			int pos = 0; 
			for (GraphNode n : elist) { 
				PoolEntry actEntry = new PoolEntry(pos++, qn, n);
				qAct.addEntry(actEntry);
				t_bits.add(actEntry.getValue().L_interval.mStart);
			}

		}

	}

	private void linkOneStep(QEdge edge, RoaringBitmap[] tBitsIdxArr) {

		int from = edge.from, to = edge.to;
		AxisType axis = edge.axis;
		Pool pl_f = mPool.get(from), pl_t = mPool.get(to);
		
		//given covering view edges, get all graph nodes in the covering edge's nodesets
		//view adj lists: key is head node, value is list of tail nodes. if tail in list, add it
		//view ONLY has graph nodes, so must still create poolentries

		//for every e_f, poolentry in nodeset that's head to query edge
		//tBitsIdxArr[to] is bitmap of nodeset that's tail to query edge 
		//pl_t.elist() is poolentries of nodeset that's tail to query edge 
		for (PoolEntry e_f : pl_f.elist()) {
			if (axis == AxisType.child) 
				linkOneStep(e_f, tBitsIdxArr[to], pl_t.elist());
			else { //check path reachability b/w graphnodes
				
				GraphNode n_f = e_f.getValue();
				for (PoolEntry e_t : pl_t.elist()) {

					GraphNode n_t = e_t.getValue();
					if (n_f.id == n_t.id)
						continue;
					if (n_f.L_interval.mEnd < n_t.L_interval.mStart)
						break;
					if (mBFL.reach(n_f, n_t) == 1) {

						e_f.addChild(e_t);
						e_t.addParent(e_f);

					}

				}
			}

		}

		
	}
	
	
	private void linkOneStep(int from, int to, AxisType axis, RoaringBitmap[] tBitsIdxArr) {
		
		Pool pl_f = mPool.get(from), pl_t = mPool.get(to);

		
		for (PoolEntry e_f : pl_f.elist()) {
			if (axis == AxisType.child) 
				linkOneStep(e_f, tBitsIdxArr[to], pl_t.elist());
			else {
				
				GraphNode n_f = e_f.getValue();
				for (PoolEntry e_t : pl_t.elist()) {

					GraphNode n_t = e_t.getValue();
					if (n_f.id == n_t.id)
						continue;
					if (n_f.L_interval.mEnd < n_t.L_interval.mStart)
						break;
					if (mBFL.reach(n_f, n_t) == 1) {

						e_f.addChild(e_t);
						e_t.addParent(e_f);

					}

				}
			}

		}

		
	}

	private boolean linkOneStep(PoolEntry r, RoaringBitmap t_bits, ArrayList<PoolEntry> list) {

		GraphNode s = r.getValue();
		
		//intersect bitmaps of nodes neighboring head and nodes in tail nodeset 
		RoaringBitmap rs_and = RoaringBitmap.and(s.adj_bits_o, t_bits); 

		if (rs_and.isEmpty())
			return false; //there are no poss edges from head to this nodeset

		for (int ti : rs_and) { //create edges (in bwd and fwd adj lists) b/w head and tail poolentries
			PoolEntry e = list.get(t_bits.rank(ti) - 1);
			r.addChild(e);
			e.addParent(r);
		}

		return true;

	}

	public static void main(String[] args) {

	}

}
