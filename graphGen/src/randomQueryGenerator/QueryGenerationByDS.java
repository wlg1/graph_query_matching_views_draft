package randomQueryGenerator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public class QueryGenerationByDS {

	
	ArrayList<Query> qryList;
	
	int[] dsPer = {4};
		//{0,2,4,6,8,10};
	
	
	public void run(String qryFN){
		
		String fn = qryFN.substring(0, qryFN.lastIndexOf('.'));
		String suffix = qryFN.substring(qryFN.lastIndexOf('.'));
		qryList = readQueries(qryFN);
		
		for(int ds:dsPer){
			
			String qryFN_n = new String(fn+"_m"+ds+suffix);
			genQueries(qryFN_n, ds, qryList);
			
		}
	}
	
	
	private void genQueries(String fn, int ds, ArrayList<Query> qlist){
		
		try {
			PrintWriter pw = new PrintWriter(new FileOutputStream(fn));
			int qid = 0;
			for(Query qry:qlist){
				int num = (int) Math.round(qry.V*(double)ds/10);
				ArrayList<Integer> randList = genRandomNumbers(num);
				write(qid++, qry, randList, pw);
			}
			
	       pw.close();
		
		
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}
		
		
	}
	
	private void write(int qid, Query qry, ArrayList<Integer> randList, PrintWriter pw) {
		pw.println("q" + qid);
		// output nodes
		QNode[] nodes = qry.nodes;

		for(int i:randList){
			
			nodes[i].axis = 1;
		}
		
		for (QNode n : nodes) {

			pw.println("v" + " " + n.id + " " + n.lb);
		}

		pw.flush();

		// output edges

		for (QNode n : nodes) {
			if (n.N_O_SZ > 0)
				for (QNode w : n.N_O) {
					pw.println("e" + " " + n.id + " " + w.id + " " + w.axis);

				}
		}
		pw.flush();
	}
	
	
	private ArrayList<Query> readQueries(String qryFN){
		
		ArrayList<Query> queries = new ArrayList<Query>();
		
		QueryParser queryParser = new QueryParser(qryFN);
		Query qry = null;
		while ((qry = queryParser.readNextQuery()) != null) {
			
			queries.add(qry);
		}
		
		return queries;
	}
	
	
	
	private  ArrayList<Integer> genRandomNumbers(int range){
		
		 //define ArrayList to hold Integer objects
		  ArrayList<Integer> numbers = new ArrayList<Integer>(range);
		  for(int i = 0; i < range; i++){
		    numbers.add(i);
		  }
		
		  Collections.shuffle(numbers);
		 
		  ArrayList<Integer> randomNumbers = new ArrayList<Integer>(range);
		  
		  for(int i=0; i< range; i++){
			  
			  randomNumbers.add(numbers.get(i));
		  }
		  
		  return randomNumbers;
	}
	
	
	public static void main(String[] args) {
	

		QueryGenerationByDS qgen = new QueryGenerationByDS();
		qgen.run(".//data//acmpc100_n.qry");
		
	}

}
