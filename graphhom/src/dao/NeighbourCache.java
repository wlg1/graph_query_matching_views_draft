package dao;

import java.util.Hashtable;

import org.roaringbitmap.RoaringBitmap;

public class NeighbourCache {

	Hashtable<Integer, RoaringBitmap> ht;
	Hashtable<Integer, String> strHT;
	
	public NeighbourCache(){
		
		ht = new Hashtable<Integer, RoaringBitmap>();
		strHT = new Hashtable<Integer, String>();
	}
	
	public void insert(int k, String v){
	
		
		strHT.put(k, v);
	}
	
	public void insert(int k, RoaringBitmap v){
		if(ht.containsKey(k))
			System.out.println("has before!");
		
		ht.put(k, v);
	}
	
	public RoaringBitmap getValue(int k){
		
		return ht.get(k);
	}
	
	public String getStrValue(int k){
		
		return strHT.get(k);
	}
	
	public static void main(String[] args) {
	
	}

}
