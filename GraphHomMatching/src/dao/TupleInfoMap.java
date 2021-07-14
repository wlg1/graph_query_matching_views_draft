package dao;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/*
 * schema information of the tuples
 * 
 */
		
		
public class TupleInfoMap {
    
	// key: qid, value:tuple id
	private Map<Integer, Integer> m_info = new LinkedHashMap<Integer, Integer>();
	
	private int pos = 0;

	public TupleInfoMap() {

		m_info = new LinkedHashMap<Integer, Integer>();

	}

	public TupleInfoMap(Map<Integer, Integer> m) {
		m_info = new LinkedHashMap<Integer, Integer>(m);
		pos = m_info.size();
	}

	public TupleInfoMap(TupleInfoMap s) {
		m_info = new LinkedHashMap<Integer, Integer>(s.getMap());
		pos = m_info.size();
	}

	// query node id
	public void addField(int id) {
		
		if (!m_info.containsKey(id))
			m_info.put(id, pos++);

	}

	public Set<Entry<Integer, Integer>> getEntries() {

		Set<Entry<Integer, Integer>> entries = m_info.entrySet();

		return entries;

	}

	public int getLength(){
		
	    return pos;	
	}
	
	public Map<Integer, Integer> getMap() {

		return m_info;
	}

	public void setMap(Map<Integer, Integer> m) {

		m_info.clear();
		m_info.putAll(m);
		pos = m_info.size();
	}

	public static void main(String[] args) {

	}

}
