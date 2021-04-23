package views;

import java.util.ArrayList;
import java.util.HashMap;

import graph.GraphNode;

public class nodeset {
	
	public ArrayList<GraphNode> gnodes;
	public HashMap<GraphNode, HashMap<Integer, ArrayList<GraphNode>>> fwdAdjLists;
	
	public nodeset() {
		gnodes = (ArrayList<GraphNode>) null;
		fwdAdjLists = (HashMap<GraphNode, HashMap<Integer, ArrayList<GraphNode>>>) null;
	}
}
