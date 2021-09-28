package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import global.Consts;
import mv.DagMVFltSimMainViewMat;

public class LabelScalabMain {
	
	public LabelScalabMain() {
		
	}
	
	public void run(String dataFileN, String queryFileN, String viewFileN, String allFileN, String prefix) throws Exception {
//		DagHomIEFltSimMain demain = new DagHomIEFltSimMain(dataFileN, queryFileN);  //FLTSIM
//		DagHomIEFltSimMain demain2 = new DagHomIEFltSimMain(dataFileN, queryFileN, false); //FLT
//		DagHomIEMain demain3 = new DagHomIEMain(dataFileN, queryFileN); //SIM
		DagMVFltSimMainViewMat demain4 = new DagMVFltSimMainViewMat(dataFileN, queryFileN, viewFileN); 

//		demain.run();
//		demain2.run();
//		demain3.run();
		demain4.run();
		
//		PrintWriter opw;
//		String outFileN = Consts.OUTDIR + allFileN + ".csv";
//		try {
//			opw = new PrintWriter(new FileOutputStream(outFileN, true));
////			demain.stats.printToFileCombinedHeader(opw);
////			demain.stats.printToFileCombined(opw, "FLTSIM");
//			
//			demain.stats.printToFileCombinedHeaderPartial(opw, viewFileN, partialViewFileN);
//			demain.stats.printToFileCombinedPartial(opw, "FLTSIM");
////			demain2.stats.printToFileCombinedPartial(opw, "FLT");
//			demain3.stats.printToFileCombinedPartial(opw, "SIM");
//			demain4.stats.printToFileCombinedPartial(opw, "View_sim_rmvEmp");
////			demain5.stats.printToFileCombinedPartial(opw, "View_sim");
//			demain6.stats.printToFileCombinedPartial(opw, "View_partial_FLTSIM");
//			demain7.stats.printToFileCombinedPartial(opw, "View_partial_SIM");
//			
//			opw.close();
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
		
	}
	
	public static void main(String[] args) throws Exception {
//		String dataFileN = args[0], queryFileN = args[1], viewFileN = args[2];
//		combinedMain theMain = new combinedMain();
//		theMain.run(dataFileN, queryFileN, viewFileN);
		
		//loop thru files in input list of inputs or inputs in folder
		
		String DGname = "bs";
		String dataFileN = null;
		for (Integer numL =10; numL <=80; numL *=2) {
			dataFileN = DGname +"_lb"+ numL.toString() + "_v1.dag";
			
			String[] splitDataFileName = dataFileN.split("[.]", 0);
			String myDirectoryPath = Consts.INDIR;
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
					  LabelScalabMain theMain = new LabelScalabMain();
					  System.out.println(dataFileN);
					  System.out.println(queryFileN);
					  System.out.println(viewFileN);
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
	
}
