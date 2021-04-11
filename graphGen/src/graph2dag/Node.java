package graph2dag;

import java.util.ArrayList;


public class Node {

	public int id;
	public String lb;
   
	public int N_O_SZ = 0;
	public ArrayList<Integer> N_O;
	public int[] L_out;
	public Color color = Color.white;

	public enum Color {
		/** not yet seen */
		white,
		/** processing, in dfs stack */
		grey,
		/** already processed */
		black;
	}

	public static void main(String[] args) {

	}

}
