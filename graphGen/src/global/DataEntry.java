/**
 *DataEntry.java Aug 11, 2009 10:28:43 AM
 *Author: Xiaoying Wu 
 *TODO
 */
package global;


/**
 * 
 */
public class DataEntry extends Entry{


	public int mLevel;
	public int mPos;
	
	public int getLevel() {
		return mLevel;
	}


	public void setLevel(int level) {
		mLevel = level;
	}

	public int getPos() {
		return mPos;
	}


	public void setPos(int pos) {
		mPos = pos;
	}
	
	public DataEntry(int start, int end, int level){
	
		super(start,end);
		mLevel = level;
		mPos = 0; // by default, it is not used by XBTree
	}
	
	
	public DataEntry(int pos, int start, int end, int level){
		
		super(start,end);
		mLevel = level;
		mPos = pos;
	}
	
	
	public DataEntry(){}
	
	public String toString(){
		
		StringBuffer s = new StringBuffer();
		s.append("[");
		s.append("POS: "+mPos + ",");
		s.append("START: "+mStart + ",");
		s.append("END: "+mEnd + ",");
		s.append("LEVEL: "+mLevel + "]\n");
		return s.toString();
	}
	
	public boolean equals(DataEntry entry){
		if((this.mStart == entry.mStart) && (this.mEnd == entry.mEnd)
		 &&(this.mLevel == entry.mLevel))
			return true;
		return false;
		
	}
	
    public boolean lessThan(DataEntry entry){
    	
    	return this.mStart<entry.mStart;
    	
    }
	
	
	public void print(){
		
		System.out.print(this.toString());
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
