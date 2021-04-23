package views;

import java.util.ArrayList;
import java.util.HashMap;

import dao.PoolEntry;
import graph.GraphNode;

public class nodeset {
	
	public ArrayList<GraphNode> gnodes;
	public HashMap<GraphNode, HashMap<Integer, ArrayList<GraphNode>>> fwdAdjLists;
	public HashMap<GraphNode, PoolEntry> GNtoPE;
	
	public nodeset() {  //init new so not null when adding in
		gnodes = new ArrayList<GraphNode>() ;
		fwdAdjLists = new HashMap<GraphNode, HashMap<Integer, ArrayList<GraphNode>>>();
		GNtoPE = new HashMap<GraphNode, PoolEntry>();
	}
	
	public void createFwdAL() {
		for (GraphNode gn : gnodes) {
			HashMap<Integer, ArrayList<GraphNode>> edgeHM = new HashMap<Integer, ArrayList<GraphNode>>();
			fwdAdjLists.put(gn, edgeHM);
		}
	}
}
