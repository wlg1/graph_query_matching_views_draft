package main;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import global.Consts;
import helper.QueryEvalStats;

public class combinedMain {
	
	QueryEvalStats stats1;
	QueryEvalStats stats2;
	
	public combinedMain() {
		
	}
	
	public void run(String dataFileN, String queryFileN, String viewFileN) throws Exception {
		//loop thru files in input list of inputs or inputs in folder
		ViewAnsGrMain2 demain = new ViewAnsGrMain2(dataFileN, queryFileN, viewFileN, true, true);
//		ViewAnsGrMain2 demain = new ViewAnsGrMain2(dataFileN, queryFileN, viewFileN, true, false);
//		ViewAnsGrMain2 demain = new ViewAnsGrMain2(dataFileN, queryFileN, viewFileN, false, true);
//		ViewAnsGrMain2 demain = new ViewAnsGrMain2(dataFileN, queryFileN, viewFileN, false, false);
		DagHomIEFltSimMain demain2 = new DagHomIEFltSimMain(dataFileN, queryFileN, true);
		DagHomIEFltSimMain demain3 = new DagHomIEFltSimMain(dataFileN, queryFileN, false);
		DagHomIEMain demain4 = new DagHomIEMain(dataFileN, queryFileN);

		demain.run();
		demain2.run();
		demain3.run();
		demain4.run();
		
		stats1 = demain.stats;
		stats2 = demain2.stats;
		
		//output to combined csv
		writeStatsToCSV();
	}
	
	public static void main(String[] args) throws Exception {
		String dataFileN = args[0], queryFileN = args[1], viewFileN = args[2];
		combinedMain theMain = new combinedMain();
		theMain.run(dataFileN, queryFileN, viewFileN);

	}
	
	private void writeStatsToCSV() {
		PrintWriter opw;
		String outFileN = Consts.OUTDIR + "combined.csv";

		try {
			opw = new PrintWriter(new FileOutputStream(outFileN, true));
			stats1.printToFileCombined(opw);
			stats2.printToFileCombined(opw);
			opw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
