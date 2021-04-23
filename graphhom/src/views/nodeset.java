package views;

import java.util.ArrayList;
import java.util.HashMap;

import graph.GraphNode;

public class nodeset {
	
	public ArrayList<GraphNode> gnodes;
	public HashMap<GraphNode, HashMap<Integer, ArrayList<GraphNode>>> fwdAdjLists;
	
	public nodeset() {
		gnodes = new ArrayList<GraphNode>() ;
		fwdAdjLists = new HashMap<GraphNode, HashMap<Integer, ArrayList<GraphNode>>>();
	}
}
