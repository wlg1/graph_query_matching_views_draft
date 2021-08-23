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
		String partialViewFileN =  prefix + "_partial_v1.vw";
		
		DagHomIEFltSimMain demain = new DagHomIEFltSimMain(dataFileN, queryFileN, true);  //FLTSIM
//		DagHomIEFltSimMain demain2 = new DagHomIEFltSimMain(dataFileN, queryFileN, false); //FLT
//		DagHomIEMain demain3 = new DagHomIEMain(dataFileN, queryFileN); //SIM
//		ViewAnsGrMain2 demain4 = new ViewAnsGrMain2(dataFileN, queryFileN, viewFileN, false, true); //rmvEmpty
//		ViewAnsGrMain2 demain5 = new ViewAnsGrMain2(dataFileN, queryFileN, viewFileN, false, false);
		PartialViewAnsGrMainUNCOVprefilt demain6 = new PartialViewAnsGrMainUNCOVprefilt(dataFileN, queryFileN, partialViewFileN, 
				false, true, true); // FLTSIM
		PartialViewAnsGrMainUNCOVprefilt demain7 = new PartialViewAnsGrMainUNCOVprefilt(dataFileN, queryFileN, partialViewFileN, 
				false, true, false); // SIM

		demain.run();
//		demain2.run();
//		demain3.run();
//		demain4.run();
//		demain5.run();
		demain6.run();
		demain7.run();
		
		PrintWriter opw;
		String outFileN = Consts.OUTDIR + allFileN + ".csv";

		try {
			opw = new PrintWriter(new FileOutputStream(outFileN, true));
//			demain.stats.printToFileCombinedHeader(opw);
//			demain.stats.printToFileCombined(opw, "FLTSIM");
			
			demain.stats.printToFileCombinedHeaderPartial(opw, viewFileN, partialViewFileN);
			demain.stats.printToFileCombinedPartial(opw, "FLTSIM");
//			demain2.stats.printToFileCombined(opw, "FLT");
//			demain3.stats.printToFileCombined(opw, "SIM");
//			demain4.stats.printToFileCombined(opw, "View_sim_rmvEmp");
//			demain5.stats.printToFileCombined(opw, "View_sim");
			demain6.stats.printToFileCombinedPartial(opw, "View_partial_FLTSIM");
			demain7.stats.printToFileCombinedPartial(opw, "View_partial_SIM");
			
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
