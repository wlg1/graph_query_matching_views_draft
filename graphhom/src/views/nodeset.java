package views;

import java.util.HashMap;

import org.roaringbitmap.RoaringBitmap;

import dao.PoolEntry;

public class nodeset {
	
	public RoaringBitmap gnodesBits;
	public HashMap<Integer, HashMap<Integer, RoaringBitmap>> fwdAdjLists;
	public HashMap<Integer, PoolEntry> GNtoPE;
//	public boolean hasNodes = true;
	
	public nodeset() {  //init new so not null when adding in
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
	
//	public void clear(){
//		gnodesBits.clear();
//		fwdAdjLists.clear();
//		GNtoPE.clear();
//	}
}
