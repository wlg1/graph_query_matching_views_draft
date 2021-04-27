package views;

import java.util.ArrayList;
import java.util.HashMap;

import org.roaringbitmap.RoaringBitmap;

import dao.PoolEntry;
import graph.GraphNode;

public class nodeset {
	
//	public ArrayList<GraphNode> gnodes;
	public RoaringBitmap gnodesBits;
	
	public HashMap<Integer, HashMap<Integer, RoaringBitmap>> fwdAdjLists;
	public HashMap<Integer, PoolEntry> GNtoPE;
	
	public nodeset() {  //init new so not null when adding in
//		gnodes = new ArrayList<GraphNode>() ;
		gnodesBits = new RoaringBitmap();
		
		fwdAdjLists = new HashMap<Integer, HashMap<Integer, RoaringBitmap>>();
		GNtoPE = new HashMap<Integer, PoolEntry>();
	}
	
	public void createFwdAL() {
		for (int gn : gnodesBits) {
			HashMap<Integer, RoaringBitmap> edgeHM = new HashMap<Integer, RoaringBitmap>();
			fwdAdjLists.put(gn, edgeHM);
		}
	}
}
