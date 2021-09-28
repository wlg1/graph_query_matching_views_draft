/**
 *CoverResult.java May 13, 2009 3:41:47 PM
 *Author: Xiaoying Wu 
 * given a view and a query, for each query node, find all the view nodes 
 * of the view covering that query node.  
 */

package mv.queryCovering;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 
 */
public class CoverResult {

	
	protected int m_viewId, m_qSize; // m_qSize denotes the number of query nodes of the query
	protected BitSet m_childBits;// bitmap for the query nodes with child axis
	protected BitSet m_coverBits; // bitmap for the covered query nodes
	// key: query node m_id, value: a list of view node ids
	protected HashMap<Integer, HashSet<Integer>> m_cover;
	
	// key: query edge id, value: a list of view edge ids
	protected HashMap<Integer, HashSet<Integer>> m_edgecover;
	
	protected BitSet m_edgeCoverBits; //bitmap for the covered query edges
	
	public CoverResult(int viewId, int qSize){
		
		m_viewId = viewId;
	    m_qSize = qSize;
	    m_coverBits = new BitSet(qSize);
	    m_edgeCoverBits = new BitSet();
	    m_childBits =new BitSet(qSize);
		m_cover = new HashMap<Integer, HashSet<Integer>>();
		m_edgecover = new HashMap<Integer, HashSet<Integer>>();
	}
	
	
	public int getViewId(){
		
		return m_viewId;
	}
	
	public HashSet<Integer> getVNodeList(int qNodeId){
		 return m_cover.get(qNodeId);
		
	}
	
	public HashSet<Integer> getVEdgeList(int qNodeId){
		 return m_edgecover.get(qNodeId);
		
	}
	
	public String getVNodesStr(int qNodeId){
		
		Set<Integer> ids = m_cover.get(qNodeId);
		
		String idStr = new String(""); 
		if(ids!=null) 
		idStr = ids.toString();
		
		return idStr;
	}
	

	public void addCoverEdge(int qEdgeId, int vEdgeId) {
		
		HashSet<Integer> ecover = m_edgecover.get(qEdgeId);
		if(ecover == null) {
			ecover = new HashSet<Integer>();
			m_edgecover.put(qEdgeId, ecover);
			m_edgeCoverBits.set(qEdgeId);
		}
		ecover.add(vEdgeId);
	}
	
     public void addCoverNode(int qNodeId, int vNodeId, boolean childCovered){
		
		HashSet<Integer> cover;
		if(m_cover.containsKey(qNodeId)){
			
			cover = m_cover.get(qNodeId);
			
		} else{
			
			cover = new HashSet<Integer>(5);
			m_cover.put(qNodeId, cover);
			m_coverBits.set(qNodeId);
		}
		
		cover.add(vNodeId);
		
		if(childCovered)
			m_childBits.set(qNodeId);
	}
	
	

	// check if the query is covered by this view
	public boolean isCovered(){
		
		if(m_coverBits.cardinality() == m_qSize)
			return true;
	
		return false;
		
	}
	
	public BitSet getCoverBits(){
		
		return m_coverBits;
	}
		
	public BitSet getEdgeCoverBits(){
		
		return m_edgeCoverBits;
	}
	
	public BitSet getChildBits(){
		
		return m_childBits;
	}
	
	public HashSet<Integer> getCover(int qId){
		
		if(m_cover.containsKey(qId))
			return m_cover.get(qId);
		return null;
	}
	
	
	public String toString() {

		StringBuffer s = new StringBuffer();
		
		Iterator<Integer> iter = m_cover.keySet().iterator();
		
		s.append(" Query nodes covered by view " + m_viewId + " :\n");
		while(iter.hasNext()){
			int id = iter.next();
			HashSet<Integer> cover = m_cover.get(id);
			s.append("Query node m_id " + id + "\'s cover: " + cover.toString() + "\n");
		}
		
		return s.toString();
	}
	
	
	public void print(){
		
		Iterator<Integer> iter = m_cover.keySet().iterator();
		
        System.out.println(" Query nodes covered by view " + m_viewId + " :\n");
		while(iter.hasNext()){
			int id = iter.next();
			HashSet<Integer> cover = m_cover.get(id);
			 System.out.println("Query node m_id " + id + "\'s cover: " + cover.toString() + "\n");
		}
		
		System.out.println("\n");
	}
	
	
	public void printToFile(PrintWriter out) {
		
	   Iterator<Integer> iter = m_cover.keySet().iterator();
		
       out.println("Query nodes covered by view " + m_viewId + " :\n");
		while(iter.hasNext()){
			int id = iter.next();
			HashSet<Integer> cover = m_cover.get(id);
			out.println("Query node m_id " + id + "\'s cover: " + cover.toString() + "\n");
		}
		
		out.println("\n");
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

	}

}
