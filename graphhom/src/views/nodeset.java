package views;

import java.util.ArrayList;
import java.util.HashMap;

import graph.GraphNode;

public class nodeset {
	
	public HashMap<GraphNode, HashMap<Integer, ArrayList<GraphNode>>> bwdAdjLists;
	public HashMap<GraphNode, HashMap<Integer, ArrayList<GraphNode>>> fwdAdjLists;
	
	public nodeset() {
		
	}
}
