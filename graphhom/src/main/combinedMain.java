package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import global.Consts;

public class combinedMain {
	
	public combinedMain() {
		
	}
	
	public void run(String dataFileN, String queryFileN, String viewFileN, String allFileN, String prefix) throws Exception {
		String partialViewFileN =  prefix + "_partial.vw";
		
//		ViewAnsGrMain2 demain = new ViewAnsGrMain2(dataFileN, queryFileN, viewFileN, true, true);
//		ViewAnsGrMain2 demain5 = new ViewAnsGrMain2(dataFileN, queryFileN, viewFileN, true, false);
		ViewAnsGrMain2 demain6 = new ViewAnsGrMain2(dataFileN, queryFileN, viewFileN, false, true);
		ViewAnsGrMain2 demain7 = new ViewAnsGrMain2(dataFileN, queryFileN, viewFileN, false, false);
		DagHomIEFltSimMain demain2 = new DagHomIEFltSimMain(dataFileN, queryFileN, true);
		DagHomIEFltSimMain demain3 = new DagHomIEFltSimMain(dataFileN, queryFileN, false);
		DagHomIEMain demain4 = new DagHomIEMain(dataFileN, queryFileN);
		PartialViewAnsGrMain demain8 = new PartialViewAnsGrMain(dataFileN, queryFileN, partialViewFileN, 
				false, true, true);

//		demain.run();
		demain2.run();
//		demain3.run();
//		demain4.run();
//		demain5.run();
		demain6.run();
//		demain7.run();
		demain8.run();
		
		PrintWriter opw;
		String outFileN = Consts.OUTDIR + allFileN + ".csv";

		try {
			opw = new PrintWriter(new FileOutputStream(outFileN, true));
			demain2.stats.printToFileCombinedHeader(opw);
//			demain.stats.printToFileCombined(opw, "View_ans_rmvEmp");
//			demain5.stats.printToFileCombined(opw, "View_ans");
			demain6.stats.printToFileCombined(opw, "View_sim_rmvEmp");
//			demain7.stats.printToFileCombined(opw, "View_sim");
			demain8.stats.printToFileCombined(opw, "View_partial");
			demain2.stats.printToFileCombined(opw, "FLTSIM");
//			demain3.stats.printToFileCombined(opw, "FLT");
//			demain4.stats.printToFileCombined(opw, "SIM");
			
			
			opw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	public static void main(String[] args) throws Exception {
//		String dataFileN = args[0], queryFileN = args[1], viewFileN = args[2];
//		combinedMain theMain = new combinedMain();
//		theMain.run(dataFileN, queryFileN, viewFileN);
		
		//loop thru files in input list of inputs or inputs in folder
		String dataFileN = "Email_lb20.dag";
		String[] splitDataFileName = dataFileN.split("[.]", 0);
		String myDirectoryPath = "D:\\Documents\\_prog\\prog_cust\\eclipse-workspace\\graph_expr\\input_files";
		  File dir = new File(myDirectoryPath);
		  File[] directoryListing = dir.listFiles();
		  if (directoryListing != null) {
		    for (File child : directoryListing) {
		    	String[] splitFileName = child.getName().split("[.]", 0);
		    	String prefix = splitFileName[0];
		    	String ext = splitFileName[1];
		    	if (ext.equals("qry")) {
		    		String queryFileN = splitFileName[0] + ".qry";
		    		String viewFileN = splitFileName[0] + ".vw";
		    		String allFileN = splitDataFileName[0] + "_" + splitFileName[0] + "_ALL";
		    		combinedMain theMain = new combinedMain();
					theMain.run(dataFileN, queryFileN, viewFileN, allFileN, prefix);
		    	}
		    }
		  } else {
		    // Handle the case where dir is not really a directory.
		    // Checking dir.isDirectory() above would not be sufficient
		    // to avoid race conditions with another process that deletes
		    // directories.
		  }


	}
	
}
