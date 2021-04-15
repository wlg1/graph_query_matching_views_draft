package views;

import java.util.ArrayList;

public class testArraylistRefs {
	
	public void run() {
		ArrayList<ArrayList<String>> mat = new ArrayList<ArrayList<String>>();
		
		for (int i = 0; i < 3; i++) {
			ArrayList<String> X = new ArrayList<String>();
			mat.add(X);
		}
		
		mat.get(1).add("a");
//		X.add("f");  //after loop, X is out of scope so it can't be reached?
		
		ArrayList<ArrayList<Integer>> Y = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < 3; i++) {
			ArrayList<Integer> X = new ArrayList<Integer>();
			X.add(i);
			Y.add(X);
		}
		System.out.println();
	}


	public static void main(String[] args) {
		testArraylistRefs demain = new testArraylistRefs();
		demain.run();
	}
	
}
