/**
 *Util.java Apr 3, 2009 1:03:43 AM
 *Author: Xiaoying Wu 
 *TODO
 */
package helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 
 */
public class Util {

	
	 /*public static void setBit(byte[] data, int pos, int val) {
	      int posByte = pos/8; 
	      int posBit = pos%8;
	      byte oldByte = data[posByte];
	      oldByte = (byte) (((0xFF7F>>posBit) & oldByte) & 0x00FF);
	      byte newByte = (byte) ((val<<(8-(posBit+1))) | oldByte);
	      data[posByte] = newByte;
	   }

	 
	 public static int getBit(byte[] data, int pos) {
	      int posByte = pos/8; 
	      int posBit = pos%8;
	      byte valByte = data[posByte];
	      int valInt = valByte>>(8-(posBit+1)) & 0x0001;
	      return valInt;
	   }
*/
	
	private  final static int MASK = 0x0007;
	
	private final static int SHIFT = 3;
	
	
	
	
	public static void setBit(byte[] data, int pos) {
	    /*
	     * void set(int i) {        a[i>>SHIFT] |=  (1<<(i & MASK)); }
           void clr(int i) {        a[i>>SHIFT] &= ~(1<<(i & MASK)); }
           int  test(int i){ return a[i>>SHIFT] &   (1<<(i & MASK)); }

	     *   
	     */
		
		  
	     // data[pos>>SHIFT] |= (1<<((pos-1) & MASK));
		data[pos>>SHIFT] |= (1<<(pos & MASK));
	      
	   }
	
	
	 public static int getBit(byte[] data, int pos) {
	     
	      byte valByte = data[pos>>SHIFT];
	      //int valInt = valByte & (1<<((pos-1) & MASK));
	      int valInt = valByte & (1<<(pos & MASK));
	      return valInt;
	   }
	 
	 public static void clrBit(byte[] data, int pos) {
		 			  
		      //data[pos>>SHIFT] &= ~(1<<((pos-1) & MASK));
		 data[pos>>SHIFT] &= ~(1<<(pos & MASK));
		      
	  }
	 
	 public static int getNextPos(byte[] data, int start){
		 int length = data.length;
		 
		 int nextPos = start;
		
		 
		 while (nextPos < length){
			 
			  byte valByte = data[nextPos>>SHIFT];
			  //int valInt = valByte & (1<<((nextPos-1) & MASK)); 
			  int valInt = valByte & (1<<(nextPos & MASK)); 
			 
			  if(valInt == 1)
				  return nextPos;
		 }
		 
		 
		 return -1; // not found
		 
	 }
	 
	 
	 public static int getNextPos(byte[] data, int start, int end){
		 
		 int nextPos = start;
		
		 
		 while (nextPos <= end){
			 
			  byte valByte = data[nextPos>>SHIFT];
			  //int valInt = valByte & (1<<((nextPos-1) & MASK)); 
			  int valInt = valByte & (1<<(nextPos & MASK)); 
			 
			  if(valInt > 0)
				  return nextPos;
			  nextPos ++;
		 }
		 
		 
		 return -1; // not found
		 
	 }
	 
	 public static void printBytes(byte[] data) {
	      System.out.println("");
	      for (int i=data.length-1; i>=0; i--) {
	         System.out.print(byteToBits(data[i])+" ");
	      }
	      System.out.println();
	   }
	 
	   private static String byteToBits(byte b) {
	      StringBuffer buf = new StringBuffer();
	      for (int i=7; i>=0; i--)
	         buf.append((int)(b>>i & 0x0001));
	      return buf.toString();
	   }

	   
	   
	   public static void strToVect(String str, Vector<Integer> vect){
			
		    int from = 0;
		    if (str.charAt(0)== ' ')
		        from =1;
			int pos;
		    
		    
		    while (true) {
		    	pos = str.indexOf(' ', from);
		    	if(pos == -1){
		    		String subStr = str.substring(from);
		    		vect.add(Integer.parseInt(subStr));
		    		break;
		    	}	
		    	String subStr = str.substring(from, pos);
		    	vect.add(Integer.parseInt(subStr));
		        from = pos+1;
		    }
		    
		    
		   
		}
	   
	   
	    /**
	     * Convert the byte array to an int.
	     *
	     * @param b The byte array
	     * @return The integer
	     */
	    public static int byteArrayToInt(byte[] b) {
	        return byteArrayToInt(b, 0);
	    }

	    /**
	     * Convert the byte array to an int starting from the given offset.
	     *
	     * @param b The byte array
	     * @param offset The array offset
	     * @return The integer
	     */
	    public static int byteArrayToInt(byte[] b, int offset) {
	        int value = 0;
	        for (int i = 0; i < 4; i++) {
	            int shift = (4 - 1 - i) * 8;
	            value += (b[i + offset] & 0x000000FF) << shift;
	        }
	        return value;
	    }

	    
	public static final byte[] intToByteArray(int value) {
	        return new byte[] {
	                (byte)(value >>> 24),
	                (byte)(value >>> 16),
	                (byte)(value >>> 8),
	                (byte)value};
	}

	public static final ArrayList<Integer> genRandomNumbers(int num, int range){
		
		 //define ArrayList to hold Integer objects
		  ArrayList<Integer> numbers = new ArrayList<Integer>(range);
		  for(int i = 0; i < range; i++){
		    numbers.add(i);
		  }
		
		  Collections.shuffle(numbers);
		 
		  ArrayList<Integer> randomNumbers = new ArrayList<Integer>(num);
		  
		  for(int i=0; i< num; i++){
			  
			  randomNumbers.add(numbers.get(i));
		  }
		  
		  Collections.sort(randomNumbers);
		  return randomNumbers;
	}
	
	

	public static final ArrayList<Integer> genRandomNumbers(int num, int st, int ed){
		
		 //define ArrayList to hold Integer objects
		  ArrayList<Integer> numbers = new ArrayList<Integer>(ed-st+1);
		  
		  for(int i = st; i <= ed; i++){
		    numbers.add(i);
		  }
		
		  Collections.shuffle(numbers);
		 
		  ArrayList<Integer> randomNumbers = new ArrayList<Integer>(num);
		  
		  for(int i=0; i< num; i++){
			  
			  randomNumbers.add(numbers.get(i));
		  }
		  
		  Collections.sort(randomNumbers);
		  return randomNumbers;
	}
	
	public static final ArrayList<Integer> genRandomNumbers(int range){
		 Random rand =new Random(System.currentTimeMillis());
		 //define ArrayList to hold Integer objects
		  ArrayList<Integer> numbers = new ArrayList<Integer>(range);
		  for(int i = 0; i < range; i++){
		    numbers.add(i);
		  }
		
		  Collections.shuffle(numbers,rand);
		 
		  ArrayList<Integer> randomNumbers = new ArrayList<Integer>(range);
		  
		  for(int i=0; i< range; i++){
			  
			  randomNumbers.add(numbers.get(i));
		  }
		  
		  return randomNumbers;
	}
	
	public static final double getScaledDouble(double val, int scale){
		 
		 BigDecimal	 bd = new BigDecimal(val);
		 double result = ( bd.setScale(scale,BigDecimal.ROUND_HALF_UP)).doubleValue();
		 return result;
		 
	 }
	
	public static final ArrayList<Integer> genKnuth(int num, int range){
		
		ArrayList<Integer> randomNumbers = new ArrayList<Integer>(num);
		Random randomGen  = new Random();
		for(int i =0; i<range; i++){
			
			if((randomGen.nextInt()%(range-i))<num){
				randomNumbers.add(i);
				num--;
			}
		}
		
		return randomNumbers;
	}
	
	private static int minimum(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}
	
	public static int computeLevenshteinDistance(CharSequence str1,
			CharSequence str2) {
		int[][] distance = new int[str1.length() + 1][str2.length() + 1];
 
		for (int i = 0; i <= str1.length(); i++)
			distance[i][0] = i;
		for (int j = 0; j <= str2.length(); j++)
			distance[0][j] = j;
 
		for (int i = 1; i <= str1.length(); i++)
			for (int j = 1; j <= str2.length(); j++)
				distance[i][j] = minimum(
						distance[i - 1][j] + 1,
						distance[i][j - 1] + 1,
						distance[i - 1][j - 1]
								+ ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0
										: 1));
 
		return distance[str1.length()][str2.length()];
	}
	
	
	public static boolean equalVect(ArrayList<String> vect1, ArrayList<String> vect2){
		
		if(vect1.size()!=vect2.size())
			return false;
		
		for(int i=0; i<vect1.size(); i++){
			if(!vect1.get(i).equals(vect2.get(i)))
               return false;
		}
		
		return true;
	}
	
	
    public static boolean existInList(int item, ArrayList<Integer> list){
		
		for(int i=0; i<list.size(); i++){
			int curItem = list.get(i);
			if(curItem == item)
			  return true;
		}
		
		return false;
	}
	
    
    
  public static String genOutFileName(String inFileName){
		
		int lastSlash = inFileName.lastIndexOf(File.separatorChar);

		String outFileShort = new String("");

		if (lastSlash > 0) {
			outFileShort = inFileName.substring(lastSlash + 1);
		} else {
			outFileShort = new String(inFileName);
		}
		if (outFileShort.indexOf('_') > 0) {
			outFileShort = outFileShort.substring(0, outFileShort.indexOf('_'));
		}
		if (outFileShort.indexOf('-') > 0) {
			outFileShort = outFileShort.substring(0, outFileShort.indexOf('-'));
		}
		
		if (outFileShort.indexOf('.') > 0) {
			outFileShort = outFileShort.substring(0, outFileShort.indexOf('.'));
		}
		

		
		return outFileShort;
		
	
	}
  
  
  
  public static int countStrOccurrence(String patternStr, String aStr){
	  
	 
 	    Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(aStr);

	    int count =0;
	    
	    while (matcher.find()) { ++count; }
	    
	    return count;  
  }

  
  public static void combineFiles(String srcDir, String[] srcFileNames, String dstDir, String dstFileName){
	  
	  String theLine = null;
	  try {
		
		 PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(dstDir + File.separator+dstFileName)));
		 for(int i=0; i< srcFileNames.length; i++){
				
			String srcFileName = srcDir + File.separator + srcFileNames[i];   
			FileInputStream srcFIS = new FileInputStream(srcFileName);
			BufferedReader br = new BufferedReader(new InputStreamReader(srcFIS));
			System.out.println(" reading " + srcFileName + " ...");
			while ((theLine = br.readLine()) != null ){
				 out.append(theLine+ "\n");
			}
			
			br.close();
			srcFIS.close();
		 }
		 System.out.println(" done.");
			
		 out.close();
	  
	  
	  } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
  }

public static void combineXMLFiles(String srcDir, String[] srcFileNames, String dstDir, String dstFileName, String headLine, String endLine){
	  
	  String theLine = null;
	  try {
		
		 PrintWriter out = new PrintWriter(new BufferedWriter(
					new FileWriter(dstDir + File.separator+dstFileName)));
		 out.append(headLine+ "\n");
		 for(int i=0; i< srcFileNames.length; i++){
				
			String srcFileName = srcDir + File.separator + srcFileNames[i];   
			FileInputStream srcFIS = new FileInputStream(srcFileName);
			BufferedReader br = new BufferedReader(new InputStreamReader(srcFIS));
			System.out.println(" reading " + srcFileName + " ...");
			br.readLine();
			while ((theLine = br.readLine()) != null ){
				if(theLine.contains(endLine))
					continue;
				 out.append(theLine+ "\n");
			}
			
			br.close();
			srcFIS.close();
		 }
		 
		 out.append(endLine+ "\n");
		 out.close();
		 System.out.println(" done.");
			
		
	  
	  
	  } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
  }

  
  
  public static void promptInput(String message){
		BufferedReader dataIn = new BufferedReader( new
				InputStreamReader(System.in) );
		try{
			System.out.println(message);
			System.out.println("Please Enter Any Key to continue:");
			String temp = dataIn.readLine();
			}catch( IOException e ){
			System.out.println("Error in getting input");
			}
		
	}

  
  

  

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int num = 4, st = 1, ed=17;
		ArrayList<Integer> list = Util.genRandomNumbers(num, st, ed);
		
		for(int i: list){
			
			System.out.print(i + " ");
		}
		
		//System.out.println(Util.genRandomNumbers(num, range));
		
		
	}
	

}
