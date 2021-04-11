package randomQueryGenerator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public class RandQuerySelector {


	ArrayList<Query> qryList, qryList_n;
	
	
	public RandQuerySelector(){
		
		
	}
	
	public void run(String qryFN, int num){
		
		String fn = qryFN.substring(0, qryFN.lastIndexOf('.'));
		String suffix = qryFN.substring(qryFN.lastIndexOf('.'));
		String qryFN_n = new String(fn+"_n"+suffix);
		
		qryList = readQueries(qryFN);
		qryList_n = readQueries(qryFN_n);
		
		ArrayList<Integer> randList = genRandomNumbers(num);
		
		String seleFN = new String(fn+num+suffix);
		String seleFN_n = new String(fn+num+"_n" + suffix);
		writeToFile(seleFN,qryList, randList);
		writeToFile(seleFN_n,qryList_n, randList);
	}
	
	
	private void writeToFile(String fn, ArrayList<Query> qlist, ArrayList<Integer> rlist){
		
		try {
			
			PrintWriter pw = new PrintWriter(new FileOutputStream(fn));
			
			for(int i=0; i<rlist.size(); i++){
				
				int qid = rlist.get(i);
				Query qry = qlist.get(qid);
				write(i, qry, pw);
			}
			
			pw.close();
			
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		}
		
		
	}
	
	private void write(int qid, Query qry, PrintWriter pw) {
		pw.println("q" + qid);
		// output nodes
		QNode[] nodes = qry.nodes;

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
		
		RandQuerySelector rqs = new RandQuerySelector();
		rqs.run
		//(".//data//randv3a2t5k.qry",10);
		//(".//data//randv5a2t5k.qry",10);
		//(".//data//randv7a2t5k.qry",10);
		//(".//data//randv10a2t5k.qry",10);
		//(".//data//randv15a2t5k.qry",10);
		//(".//data//randv15a2t3k.qry",10);
		//(".//data//randv3a2t1k.qry", 10);
		//(".//data//cite_lb7k.qry",10);
		//(".//data//cite_lb5k.qry",10);
		//(".//data//cite_lb3k.qry",10);
		//(".//data//cite_lb10k.qry", 10);
		//(".//data//cite_lb8k.qry",10);
		//(".//data//cite_lb6k.qry",10);
		//(".//data//cite_lb4k.qry",10);
		//(".//data//cite_lb2k.qry", 10);
		//(".//data//cite_lb1k.qry", 10);
		//(".//data//acm.qry", 100);
		//(".//data//acmpc.qry", 100);
		//(".//data//acm.qry", 100);
		(".//data//dblp100.qry", 10);	
		//(".//data//dblpMoreD.qry", 10);	
		
	}

}
