/**
 *Entry.java Aug 13, 2009 3:10:12 PM
 *Author: Xiaoying Wu 
 *TODO
 */
package global;

/**
 * 
 */
public class Entry {

	
	public int mStart;
	public int mEnd;
	

	
	public int getStart() {
		return mStart;
	}


	public void setStart(int start) {
		mStart = start;
	}


	public int getEnd() {
		return mEnd;
	}


	public void setEnd(int end) {
		mEnd = end;
	}


	public Entry(int start, int end){
	
		mStart = start;
		mEnd = end;
	}
	
	
	public Entry(){}
	
	 public int compareTo(Object entry) {
			
			if( ! ( entry instanceof Entry ) ){

				throw new ClassCastException("Invalid entry");

				}
			
			if(this.mStart>((Entry)entry).getStart()){
				return 1;
			} else if(this.mStart< ((Entry) entry).getStart() && this.mEnd >((Entry) entry).getEnd())	
			   	return 0;
			else return -1;
			
		} 

	
	public String toString(){
		
		StringBuffer s = new StringBuffer();
		s.append("[");
		s.append("START: "+mStart + ",");
		s.append("END: "+mEnd + "]");
		return s.toString();
	}
	
	public boolean equals(Entry entry){
		if((this.mStart == entry.mStart) && (this.mEnd == entry.mEnd))
			return true;
		return false;
		
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
