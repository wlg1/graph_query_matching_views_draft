package views;

import java.util.ArrayList;
import java.util.HashMap;

import org.roaringbitmap.RoaringBitmap;

import dao.PoolEntry;
import graph.GraphNode;

public class nodeset {
	
	public ArrayList<GraphNode> gnodes;
//	public RoaringBitmap gnodes;
	
	public HashMap<Integer, HashMap<Integer, ArrayList<GraphNode>>> fwdAdjLists;
//	public HashMap<Integer, HashMap<Integer, RoaringBitmap>> fwdAdjLists;
	public HashMap<Integer, PoolEntry> GNtoPE;
	
	public nodeset() {  //init new so not null when adding in
		gnodes = new ArrayList<GraphNode>() ;
//		RoaringBitmap gnodes = new RoaringBitmap();
		
//		fwdAdjLists = new HashMap<Integer, HashMap<Integer, RoaringBitmap>>();
		fwdAdjLists = new HashMap<Integer, HashMap<Integer, ArrayList<GraphNode>>>();
		GNtoPE = new HashMap<Integer, PoolEntry>();
	}
	
	public void createFwdAL() {
		for (GraphNode gn : gnodes) {
			HashMap<Integer, ArrayList<GraphNode>> edgeHM = new HashMap<Integer, ArrayList<GraphNode>>();
//			HashMap<Integer, RoaringBitmap> edgeHM = new HashMap<Integer, RoaringBitmap>();
			fwdAdjLists.put(gn.pos, edgeHM);
		}
	}
}
