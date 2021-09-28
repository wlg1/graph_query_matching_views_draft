/**
 * 
 */
package dao;

import java.util.ArrayList;
import java.util.HashMap;

import org.roaringbitmap.RoaringBitmap;

/**
 * @author xiaoying
 *
 */
public class Pool {

	ArrayList<PoolEntry> elist;
	RoaringBitmap bitsByID;
	HashMap<Integer, Integer> id2PosMap;
	
	int size;
	
	public Pool(int sz){
		size = sz;
		elist = new ArrayList<PoolEntry>(sz);
		//id2PosMap = new HashMap<Integer, Integer>(sz);
		
	}
	
	public void addEntry(PoolEntry entry){
		
		elist.add(entry);
		//id2PosMap.put(entry.mValue.id, entry.mPos);
	
	}
	
	public void setIDBits(RoaringBitmap bitsByID) {
		
		this.bitsByID = bitsByID;
	}
    
	public RoaringBitmap getIDBits() {
		
	    return bitsByID;	
	}
	
	public ArrayList<PoolEntry> elist(){
		
		return elist;
	}
	
	
	public HashMap<Integer, Integer> getID2PosMap(){
		
		return id2PosMap;
	}
	
	public boolean isEmpty(){
		
		return elist.isEmpty();
	}
	
	
	public void clear(){
		size = 0;
		elist.clear();
		
	}
	
	public int size(){
		
		return size;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
