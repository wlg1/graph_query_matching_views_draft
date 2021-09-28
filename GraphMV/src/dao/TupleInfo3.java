package dao;

import java.util.ArrayList;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class TupleInfo3 {

	private  Map<Integer, Integer> m_info = new LinkedHashMap<Integer, Integer>();
	private  int pos = 0;
	private  ArrayList<Integer> m_idx = new ArrayList<Integer>();
   	
	public TupleInfo3(){}
	
	public TupleInfo3(Map<Integer, Integer> m){
		
		addAll(m);
		pos = m_info.size();
	}
	
	public TupleInfo3(TupleInfo3 s){
		
		addAll(s.getMap());
		pos = m_info.size();
	}
	
	
	
	
	// query node id
	public void addField(int id){
		m_info.put(id, pos++);
		m_idx.add(id);
	  	
	}
	
	
	private void addAll(Map<Integer, Integer> m){
		
		for(int k:m.keySet()){
			
			addField(k);
			
		}
	}
	
	
	public Set<Entry<Integer, Integer>> getEntries(){
		
		Set<Entry<Integer, Integer>> entries = m_info.entrySet();
			
		return entries;
		
	}
	
	
	public Map<Integer, Integer> getMap(){
		
		return m_info;
	}
	
	public void setMap(Map<Integer, Integer> m){
		
		m_info.clear();
		m_idx.clear();
		addAll(m);
		pos = m_info.size();
	}
	
	public static void main(String[] args) {
		

	}

}
