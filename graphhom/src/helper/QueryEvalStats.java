package helper;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import global.Flags;

public class QueryEvalStats {

	public String dataFN, qryFN, algN;
	public int V, E, numLbs;
	public double loadTime = 0.0, bldTime = 0.0;
	public double evalTime;
	public double avgQSize = 0.0;
	// public double totmem = 0.0;
	ArrayList<QueryEvalStat>[] qryEvalStats;
	public double[] totTimes, totMatchTimes, totEnumTimes, totPreTimes, totPlanTimes, totvInterTimes;
	ArrayList<QueryEvalStat> viewEvalStatList;

	public QueryEvalStats() {

		qryEvalStats = (ArrayList<QueryEvalStat>[]) new ArrayList[Flags.REPEATS];
		totTimes = new double[Flags.REPEATS];
		totMatchTimes = new double[Flags.REPEATS];
		totEnumTimes = new double[Flags.REPEATS];
		totPreTimes = new double[Flags.REPEATS];
		totPlanTimes = new double[Flags.REPEATS];
		totvInterTimes = new double[Flags.REPEATS];
	}

	public QueryEvalStats(String dataFN, String qryFN, String algN) {

		qryEvalStats = (ArrayList<QueryEvalStat>[]) new ArrayList[Flags.REPEATS];
		totTimes = new double[Flags.REPEATS];
		totMatchTimes = new double[Flags.REPEATS];
		totEnumTimes = new double[Flags.REPEATS];
		totPreTimes = new double[Flags.REPEATS];
		totPlanTimes = new double[Flags.REPEATS];
		totvInterTimes = new double[Flags.REPEATS];
		this.dataFN = dataFN;
		this.qryFN = qryFN;
		this.algN = algN;
		viewEvalStatList = new ArrayList<QueryEvalStat>();
	}

	public void setGraphStat(int V, int E, int lbs) {

		this.V = V;
		this.E = E;
		numLbs = lbs;

	}

	public void setQryStat(double size) {

		avgQSize = size;
	}

	public void setLoadTime(double lt) {

		loadTime = lt;

	}

	public void setBuildTime(double bt) {

		bldTime = bt;
	}

	public void add(int iter, int qid, double vt, double pt, double st, double eit, double mt, double et, double solns) {

		QueryEvalStat qst = new QueryEvalStat(vt, pt, st, eit, mt, et, solns);
		// add(qst);
		add(iter, qid, qst);
	}

	public void add(int iter, int qid, QueryEvalStat qst) {
		if (qryEvalStats[iter] == null)
			qryEvalStats[iter] = new ArrayList<QueryEvalStat>();

		qryEvalStats[iter].add(qid, qst);

	}
	
	public void addView(QueryEvalStat vstat) {
		viewEvalStatList.add(vstat);

	}

	public void printToFile(PrintWriter opw) {
		opw.append("*****************************************************\r\n");
		opw.append("Dataset:" + dataFN + "\r\n");
		opw.append("V:" + V + " " + "E:" + E + " " + "lbs:" + numLbs + "\r\n");
		opw.append("Queryset:" + qryFN + "\r\n");
		opw.append("Algorithm:" + algN + "\r\n");
		
		DecimalFormat f = new DecimalFormat("##.0000");

		int totQs = qryEvalStats[0].size();
		int numQs = totQs;
		double[] evalTimes, matchTimes, enumTimes, preTimes, simTimes;
		evalTimes = new double[numQs];
		matchTimes = new double[numQs];
		enumTimes = new double[numQs];
		preTimes = new double[numQs];
		simTimes = new double[numQs];

		double totNodesBefore = 0.0, totNodesAfter = 0.0;
		double totSolns = 0.0;

		opw.append("id" + " " + "status" + " " + "preTime" + " " + "planTime" + " " + "matchTime" + " " + "enumTime"
				+ " " + "totTime" + " " + "numNodesBefore" + " " + "numNodesAfter" + " " + "numSoln" + " " + "numPlans" + " "
				+ "sizeOfAG"
				+ "\r\n");

		for (int i = 0; i < Flags.REPEATS; i++) {
			numQs = totQs;
			ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[i];
			for (int q = 0; q < totQs; q++) {
				QueryEvalStat stat = qryEvalStatList.get(q);
				//if (stat.status != Consts.status_vals.success) {
				//	numQs--;

				//} else {
					matchTimes[q] += stat.matchTime;
					enumTimes[q] += stat.enumTime;
					preTimes[q] += stat.preTime;
					simTimes[q] += stat.simTime;
					evalTimes[q] += stat.totTime;
					totTimes[i] += evalTimes[q];
					totMatchTimes[i] += matchTimes[q];
					totEnumTimes[i] += enumTimes[q];
					if (i == 0) {
						totNodesBefore += stat.totNodesBefore;
						totNodesAfter += stat.totNodesAfter;
						totSolns += stat.numSolns;
					}
				//}

				opw.append(q + " " + stat.status + " " + f.format(stat.preTime) + " " + f.format(stat.simTime) + " "
						+ f.format(stat.matchTime) + " " + f.format(stat.enumTime) + " " + f.format(stat.totTime) + " "
						+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
						+ new DecimalFormat(",###").format(stat.totNodesAfter) + " "
						+ new DecimalFormat(",###").format(stat.numSolns) + " "
						+ new DecimalFormat(",###").format(stat.numPlans) + " "
						+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + "\r\n");

			}
		}

		opw.append("Average running time: \r\n");
		opw.append("id" + " " + "status" + " " + "preTime" + " " + "planTime" + " " + "matchTime" + " " + "enumTime"
				+ " " + "totTime" + " " + "totNodesBefore" + " " + "totNodesAfter" + " " + "numOfTuples" + " "
				+ "numPlans" + " " +  "sizeOfAG" + "\r\n");
		ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[0];
		for (int q = 0; q < totQs; q++) {
			QueryEvalStat stat = qryEvalStatList.get(q);

			opw.append(q + " " + stat.status + " " 
					+ f.format(preTimes[q] / Flags.REPEATS) + " " + f.format(simTimes[q] / Flags.REPEATS) + " "
					+ f.format(matchTimes[q] / Flags.REPEATS) + " " + f.format(enumTimes[q] / Flags.REPEATS) + " "
					+ f.format(evalTimes[q] / Flags.REPEATS) + " "
					+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
					+ new DecimalFormat(",###").format(stat.totNodesAfter) + " "
					+ new DecimalFormat(",###").format(stat.numSolns) + " "
					+ new DecimalFormat(",###").format(stat.numPlans) + " "
					+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + "\r\n");

		}

		opw.append("Data loading Time:" + f.format(loadTime) + "\r\n");
		opw.append("Index building Time:" + f.format(bldTime) + "\r\n");

		double avgExeTime = calAvg(totTimes);
		double avgMatTime = calAvg(totMatchTimes);
		double avgPreTime = calAvg(totPreTimes);
		double avgPlanTime = calAvg(totPlanTimes);
		double avgEnumTime = calAvg(totEnumTimes);

		opw.append("Average Eval Time per run:" + f.format(avgExeTime) + "\r\n");
		opw.append("Average Eval Time per query:" + f.format(avgExeTime / numQs) + "\r\n");
		opw.append("Average pruning Time per run:" + f.format(avgPreTime) + "\r\n");
		opw.append("Average pruning Time per query:" + f.format(avgPreTime / numQs) + "\r\n");
		opw.append("Average Plan Time per run:" + f.format(avgPlanTime) + "\r\n");
		opw.append("Average Plan Time per query:" + f.format(avgPlanTime / numQs) + "\r\n");
		opw.append("Average Matching Time per run:" + f.format(avgMatTime) + "\r\n");
		opw.append("Average Matching Time per query:" + f.format(avgMatTime / numQs) + "\r\n");
		opw.append("Average Enumeration Time per run:" + f.format(avgEnumTime) + "\r\n");
		opw.append("Average Enumeration Time per query:" + f.format(avgEnumTime / numQs) + "\r\n");
		opw.append("Average Nodes before per query:" + f.format(totNodesBefore / numQs) + "\r\n");
		opw.append("Average Nodes after per query:" + f.format(totNodesAfter / numQs) + "\r\n");
		opw.append("Average number of solution tuples per query:" + f.format(totSolns / numQs) + "\r\n");
		opw.append("Max Used Memory:" + f.format(Flags.mt.getMaxUsedMem()) + " MB\r\n");
		opw.append("*****************************************************\r\n");
	
	}
	
	public void printToFileNoPlan(PrintWriter opw) {
		opw.append("*****************************************************\r\n");
		opw.append("Dataset:" + dataFN + "\r\n");
		opw.append("V:" + V + " " + "E:" + E + " " + "lbs:" + numLbs + "\r\n");
		opw.append("Queryset:" + qryFN + "\r\n");
		opw.append("Algorithm:" + algN + "\r\n");
		
		DecimalFormat f = new DecimalFormat("##.00");

		int totQs = qryEvalStats[0].size();
		int numQs = totQs;
		double[] evalTimes, matchTimes, enumTimes, preTimes;
		evalTimes = new double[numQs];
		matchTimes = new double[numQs];
		enumTimes = new double[numQs];
		preTimes = new double[numQs];

		double totNodesBefore = 0.0, totNodesAfter = 0.0;
		double totSolns = 0.0;

		opw.append("id" + " " + "status" + " " + "preTime" + " " + "matchTime" + " " + "enumTime"
				+ " " + "totTime" + " " + "numNodesBefore" + " " + "numNodesAfter" + " " + "numSoln" + " "
				+ "sizeOfAG"
				+ "\r\n");

		for (int i = 0; i < Flags.REPEATS; i++) {
			numQs = totQs;
			ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[i];
			for (int q = 0; q < totQs; q++) {
				QueryEvalStat stat = qryEvalStatList.get(q);
				//if (stat.status != Consts.status_vals.success) {
				//	numQs--;

				//} else {
					matchTimes[q] += stat.matchTime;
					enumTimes[q] += stat.enumTime;
					preTimes[q] += stat.preTime;
					evalTimes[q] += stat.totTime;
					totTimes[i] += evalTimes[q];
					totMatchTimes[i] += matchTimes[q];
					totEnumTimes[i] += enumTimes[q];
					if (i == 0) {
						totNodesBefore += stat.totNodesBefore;
						totNodesAfter += stat.totNodesAfter;
						totSolns += stat.numSolns;
					}
				//}

				opw.append(q + " " + stat.status + " " + f.format(stat.preTime) + " "
						+ f.format(stat.matchTime) + " " + f.format(stat.enumTime) + " " + f.format(stat.totTime) + " "
						+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
						+ new DecimalFormat(",###").format(stat.totNodesAfter) + " "
						+ new DecimalFormat(",###").format(stat.numSolns) + " "
						+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + "\r\n");

			}
		}

		opw.append("Average running time: \r\n");
		opw.append("id" + " " + "status" + " " + "preTime" + " " + "matchTime" + " " + "enumTime"
				+ " " + "totTime" + " " + "totNodesBefore" + " " + "totNodesAfter" + " " + "numOfTuples" + " "
				+  "sizeOfAG" + "\r\n");
		ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[0];
		for (int q = 0; q < totQs; q++) {
			QueryEvalStat stat = qryEvalStatList.get(q);

			opw.append(q + " " + stat.status + " " 
					+ f.format(preTimes[q] / Flags.REPEATS) + " "
					+ f.format(matchTimes[q] / Flags.REPEATS) + " " + f.format(enumTimes[q] / Flags.REPEATS) + " "
					+ f.format(evalTimes[q] / Flags.REPEATS) + " "
					+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
					+ new DecimalFormat(",###").format(stat.totNodesAfter) + " "
					+ new DecimalFormat(",###").format(stat.numSolns) + " "
					+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + "\r\n");

		}

		opw.append("Data loading Time:" + f.format(loadTime) + "\r\n");
		opw.append("Index building Time:" + f.format(bldTime) + "\r\n");

		double avgExeTime = calAvg(totTimes);
		double avgMatTime = calAvg(totMatchTimes);
		double avgPreTime = calAvg(totPreTimes);
		double avgEnumTime = calAvg(totEnumTimes);

		opw.append("Average Eval Time per run:" + f.format(avgExeTime) + "\r\n");
		opw.append("Average Eval Time per query:" + f.format(avgExeTime / numQs) + "\r\n");
		opw.append("Average pruning Time per run:" + f.format(avgPreTime) + "\r\n");
		opw.append("Average pruning Time per query:" + f.format(avgPreTime / numQs) + "\r\n");
		opw.append("Average Matching Time per run:" + f.format(avgMatTime) + "\r\n");
		opw.append("Average Matching Time per query:" + f.format(avgMatTime / numQs) + "\r\n");
		opw.append("Average Enumeration Time per run:" + f.format(avgEnumTime) + "\r\n");
		opw.append("Average Enumeration Time per query:" + f.format(avgEnumTime / numQs) + "\r\n");
		opw.append("Average Nodes before per query:" + f.format(totNodesBefore / numQs) + "\r\n");
		opw.append("Average Nodes after per query:" + f.format(totNodesAfter / numQs) + "\r\n");
		opw.append("Average number of solution tuples per query:" + f.format(totSolns / numQs) + "\r\n");
		opw.append("Max Used Memory:" + f.format(Flags.mt.getMaxUsedMem()) + " MB\r\n");
		opw.append("*****************************************************\r\n");
	
	}
	
	public void printToFileViews(PrintWriter opw) {
		opw.append("*****************************************************\r\n");
		opw.append("Dataset:" + dataFN + "\r\n");
		opw.append("V:" + V + " " + "E:" + E + " " + "lbs:" + numLbs + "\r\n");
		opw.append("Queryset:" + qryFN + "\r\n");
		opw.append("Algorithm:" + algN + "\r\n");
		
		DecimalFormat f = new DecimalFormat("##.0000");

		int totQs = qryEvalStats[0].size();
		int numQs = totQs;
		double[] evalTimes, matchTimes, enumTimes, preTimes;
		evalTimes = new double[numQs];
		matchTimes = new double[numQs];
		enumTimes = new double[numQs];
		preTimes = new double[numQs];

		double totNodesBefore = 0.0, totNodesAfter = 0.0;
		double totSolns = 0.0;
		
		opw.append("*****************************************************\r\n");
		opw.append("View Build Times: \r\n");
		opw.append("id" + " " + "status" + " " + "preTime" + " " + "matchTime" + " "
				+ "totTime" + " " + "numNodesBefore" + " " + "numNodesAfter" + " "
				+ "sizeOfAG"
				+ "\r\n");
		int totVs = viewEvalStatList.size();
		for (int q = 0; q < totVs; q++) {
			QueryEvalStat stat = viewEvalStatList.get(q);
			opw.append(q + " " + stat.status + " " + f.format(stat.preTime) + " "
					+ f.format(stat.matchTime) + " " + f.format(stat.totTime) + " "
					+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
					+ new DecimalFormat(",###").format(stat.totNodesAfter) + " "
					+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + "\r\n");
		}
		
		opw.append("*****************************************************\r\n");

		opw.append("id" + " " + "status" + " " + "preTime" + " " + "matchTime" + " " + "enumTime"
				+ " " + "totTime" + " " + "numNodesBefore" + " " + "numNodesAfter" + " " + "numSoln" + " "
				+ "sizeOfAG"
				+ "\r\n");

		for (int i = 0; i < Flags.REPEATS; i++) {
			numQs = totQs;
			ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[i];
			for (int q = 0; q < totQs; q++) {
				QueryEvalStat stat = qryEvalStatList.get(q);
				//if (stat.status != Consts.status_vals.success) {
				//	numQs--;

				//} else {
					matchTimes[q] += stat.matchTime;
					enumTimes[q] += stat.enumTime;
					preTimes[q] += stat.preTime;
					evalTimes[q] += stat.totTime;
					totTimes[i] += evalTimes[q];
					totMatchTimes[i] += matchTimes[q];
					totEnumTimes[i] += enumTimes[q];
					if (i == 0) {
						totNodesBefore += stat.totNodesBefore;
						totNodesAfter += stat.totNodesAfter;
						totSolns += stat.numSolns;
					}
				//}

				opw.append(q + " " + stat.status + " " + f.format(stat.preTime) + " "
						+ f.format(stat.matchTime) + " " + f.format(stat.enumTime) + " " + f.format(stat.totTime) + " "
						+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
						+ new DecimalFormat(",###").format(stat.totNodesAfter) + " "
						+ new DecimalFormat(",###").format(stat.numSolns) + " "
						+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + "\r\n");

			}
		}

		opw.append("Average running time: \r\n");
		opw.append("id" + " " + "status" + " " + "preTime" + " " + "matchTime" + " " + "enumTime"
				+ " " + "totTime" + " " + "totNodesBefore" + " " + "totNodesAfter" + " " + "numOfTuples" + " "
				+  "sizeOfAG" + "\r\n");
		ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[0];
		for (int q = 0; q < totQs; q++) {
			QueryEvalStat stat = qryEvalStatList.get(q);

			opw.append(q + " " + stat.status + " " 
					+ f.format(preTimes[q] / Flags.REPEATS) + " "
					+ f.format(matchTimes[q] / Flags.REPEATS) + " " + f.format(enumTimes[q] / Flags.REPEATS) + " "
					+ f.format(evalTimes[q] / Flags.REPEATS) + " "
					+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
					+ new DecimalFormat(",###").format(stat.totNodesAfter) + " "
					+ new DecimalFormat(",###").format(stat.numSolns) + " "
					+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + "\r\n");

		}

		opw.append("Data loading Time:" + f.format(loadTime) + "\r\n");
		opw.append("Index building Time:" + f.format(bldTime) + "\r\n");

		double avgExeTime = calAvg(totTimes);
		double avgMatTime = calAvg(totMatchTimes);
		double avgPreTime = calAvg(totPreTimes);
		double avgEnumTime = calAvg(totEnumTimes);

		opw.append("Average Eval Time per run:" + f.format(avgExeTime) + "\r\n");
		opw.append("Average Eval Time per query:" + f.format(avgExeTime / numQs) + "\r\n");
		opw.append("Average pruning Time per run:" + f.format(avgPreTime) + "\r\n");
		opw.append("Average pruning Time per query:" + f.format(avgPreTime / numQs) + "\r\n");
		opw.append("Average Matching Time per run:" + f.format(avgMatTime) + "\r\n");
		opw.append("Average Matching Time per query:" + f.format(avgMatTime / numQs) + "\r\n");
		opw.append("Average Enumeration Time per run:" + f.format(avgEnumTime) + "\r\n");
		opw.append("Average Enumeration Time per query:" + f.format(avgEnumTime / numQs) + "\r\n");
		opw.append("Average Nodes before per query:" + f.format(totNodesBefore / numQs) + "\r\n");
		opw.append("Average Nodes after per query:" + f.format(totNodesAfter / numQs) + "\r\n");
		opw.append("Average number of solution tuples per query:" + f.format(totSolns / numQs) + "\r\n");
		opw.append("Max Used Memory:" + f.format(Flags.mt.getMaxUsedMem()) + " MB\r\n");
		opw.append("*****************************************************\r\n");
	
	}
	
	public void printToFilePartialViews(PrintWriter opw) {
		opw.append("*****************************************************\r\n");
		opw.append("Dataset:" + dataFN + "\r\n");
		opw.append("V:" + V + " " + "E:" + E + " " + "lbs:" + numLbs + "\r\n");
		opw.append("Queryset:" + qryFN + "\r\n");
		opw.append("Algorithm:" + algN + "\r\n");
		
		DecimalFormat f = new DecimalFormat("##.0000");

		int totQs = qryEvalStats[0].size();
		int numQs = totQs;
		double[] evalTimes, matchTimes, enumTimes, preTimes, vInterTimes, simTimes, eInterTimes;
		evalTimes = new double[numQs];
		matchTimes = new double[numQs];
		enumTimes = new double[numQs];
		preTimes = new double[numQs];
		vInterTimes = new double[numQs];
		simTimes = new double[numQs];
		eInterTimes = new double[numQs];

		double totNodesBefore = 0.0, totNodesAfter = 0.0;
		double totSolns = 0.0;
		
		opw.append("*****************************************************\r\n");
		opw.append("View Build Times: \r\n");
		opw.append("id" + " " + "status" + " " + "filtTime" + " " + "matchTime" + " "
				+ "totTime" + " " + "nodesSumInvL" + " " + "nodesSG" + " "
				+ "sizeOfSG"
				+ "\r\n");
		int totVs = viewEvalStatList.size();
		for (int q = 0; q < totVs; q++) {
			QueryEvalStat stat = viewEvalStatList.get(q);
			opw.append(q + " " + stat.status + " " + f.format(stat.preTime) + " "
					+ f.format(stat.matchTime) + " " + f.format(stat.totTime) + " "
					+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
					+ new DecimalFormat(",###").format(stat.totNodesAfter) + " "
					+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + "\r\n");
		}
		
		opw.append("*****************************************************\r\n");

		opw.append("id" + " " + "status" + " " + "vInterTime" + " " + "preFTime" + " " + "simFTime" + " " + "SGbuildTime"
				+ " " + "eInterTime" + " " + "joinTime"
				+ " " + "totTime" + " " + "nodesSumInvL" + " " + "nodesAfterPreFilt" + " " + "nodesAfterVinter" + " " 
				+ "nodesSG" + " " + "numSoln" + " " +  "sizeOfSG" + " " + "sizeUncovSG" + "\r\n");

		for (int i = 0; i < Flags.REPEATS; i++) {
			numQs = totQs;
			ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[i];
			for (int q = 0; q < totQs; q++) {
				QueryEvalStat stat = qryEvalStatList.get(q);
				//if (stat.status != Consts.status_vals.success) {
				//	numQs--;

				//} else {
					matchTimes[q] += stat.matchTime;
					enumTimes[q] += stat.enumTime;
					preTimes[q] += stat.preTime;
					evalTimes[q] += stat.totTime;
					vInterTimes[q] += stat.vInterTime;
					eInterTimes[q] += stat.eInterTime;
					simTimes[q] += stat.simTime;
					
					totTimes[i] += evalTimes[q];  // for use at very end, not to calc avg
					totMatchTimes[i] += matchTimes[q]; // for use at very end, not to calc avg
					totEnumTimes[i] += enumTimes[q]; // for use at very end, not to calc avg
					if (i == 0) {
						totNodesBefore += stat.totNodesBefore;
						totNodesAfter += stat.totNodesAfter;
						totSolns += stat.numSolns;
					}
				//}

				opw.append(q + " " + stat.status + " " + f.format(stat.vInterTime) + " " + f.format(stat.preTime) + " "
						+ f.format(stat.simTime) + " " + f.format(stat.matchTime) + " " 
						+ f.format(stat.eInterTime) + " " + f.format(stat.enumTime) + " " + f.format(stat.totTime) + " "
						+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
						+ new DecimalFormat(",###").format(stat.nodesAfterPreFilt) + " "
						+ new DecimalFormat(",###").format(stat.nodesAfterVinter) + " "
						+ new DecimalFormat(",###").format(stat.totNodesAfter) + " "
						+ new DecimalFormat(",###").format(stat.numSolns) + " "
						+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + " "
						+ new DecimalFormat(",###").format(stat.sizeOfUncovAnsGraph) + "\r\n");

			}
		}

		opw.append("Average running time: \r\n");
		opw.append("id" + " " + "status" + " " + "vInterTime" + " " + "preFTime" + " " + "simFTime" + " " + "SGbuildTime"
				+ " " + "eInterTime" + " " + "joinTime"
				+ " " + "totTime" + " " + "nodesSumInvL" + " " + "nodesAfterPreFilt" + " " + "nodesAfterVinter" + " " 
				+ "nodesSG" + " " + "numSoln" + " " +  "sizeOfSG" + " " + "sizeUncovSG" + "\r\n");
		ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[0];
		for (int q = 0; q < totQs; q++) {
			QueryEvalStat stat = qryEvalStatList.get(q);

			opw.append(q + " " + stat.status + " " 
					+ f.format(vInterTimes[q] / Flags.REPEATS) + " "
					+ f.format(preTimes[q] / Flags.REPEATS) + " "
					+ f.format(simTimes[q] / Flags.REPEATS) + " " + f.format(matchTimes[q] / Flags.REPEATS) + " "
					+ f.format(eInterTimes[q] / Flags.REPEATS) + " " + f.format(enumTimes[q] / Flags.REPEATS) + " "
					+ f.format(evalTimes[q] / Flags.REPEATS) + " "
					+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
					+ new DecimalFormat(",###").format(stat.nodesAfterPreFilt) + " "
					+ new DecimalFormat(",###").format(stat.nodesAfterVinter) + " "
					+ new DecimalFormat(",###").format(stat.totNodesAfter) + " "
					+ new DecimalFormat(",###").format(stat.numSolns) + " "
					+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + " "
					+ new DecimalFormat(",###").format(stat.sizeOfUncovAnsGraph) + "\r\n");

		}

		opw.append("Data loading Time:" + f.format(loadTime) + "\r\n");
		opw.append("Index building Time:" + f.format(bldTime) + "\r\n");

		double avgExeTime = calAvg(totTimes);
		double avgMatTime = calAvg(totMatchTimes);
		double avgPreTime = calAvg(totPreTimes);
		double avgEnumTime = calAvg(totEnumTimes);

		opw.append("Average Eval Time per run:" + f.format(avgExeTime) + "\r\n");
		opw.append("Average Eval Time per query:" + f.format(avgExeTime / numQs) + "\r\n");
		opw.append("Average pruning Time per run:" + f.format(avgPreTime) + "\r\n");
		opw.append("Average pruning Time per query:" + f.format(avgPreTime / numQs) + "\r\n");
		opw.append("Average Matching Time per run:" + f.format(avgMatTime) + "\r\n");
		opw.append("Average Matching Time per query:" + f.format(avgMatTime / numQs) + "\r\n");
		opw.append("Average Enumeration Time per run:" + f.format(avgEnumTime) + "\r\n");
		opw.append("Average Enumeration Time per query:" + f.format(avgEnumTime / numQs) + "\r\n");
		opw.append("Average Nodes before per query:" + f.format(totNodesBefore / numQs) + "\r\n");
		opw.append("Average Nodes after per query:" + f.format(totNodesAfter / numQs) + "\r\n");
		opw.append("Average number of solution tuples per query:" + f.format(totSolns / numQs) + "\r\n");
		opw.append("Max Used Memory:" + f.format(Flags.mt.getMaxUsedMem()) + " MB\r\n");
		opw.append("*****************************************************\r\n");
	
	}
	
	public void printToFileCombinedHeader(PrintWriter opw) {
		opw.append("*****************************************************\r\n");
		opw.append("Dataset:" + dataFN + "\r\n");
		opw.append("V:" + V + " " + "E:" + E + " " + "lbs:" + numLbs + "\r\n");
		opw.append("Queryset:" + qryFN + "\r\n");
		opw.append("Algorithm:" + algN + "\r\n");
		
		opw.append("Average running time: \r\n");
		opw.append("Algo" + " " + "id" + " " + "status" + " " + "preFiltTime" + " " + "SGbuildTime" + " " + "MIjoinTime"
				+ " " + "totTime" + " " + "#SumInvL" + " " + "#NodesSG" + " " + "numSoln" + " "
				+  "sizeOfSG" + " " + "#edgesSG" + "\r\n");
	}
	
	public void printToFileCombinedHeaderPartial(PrintWriter opw, String viewFileN, String partialViewFileN) {
		opw.append("*****************************************************\r\n");
		opw.append("Dataset:  " + dataFN + "\r\n");
		opw.append("V:" + V + " " + "E:" + E + " " + "lbs:" + numLbs + "\r\n");
		opw.append("Queryset:  " + qryFN + "\r\n");
		opw.append("View_set:  " + viewFileN + "\r\n");
		opw.append("Partial_View_set:  " + partialViewFileN + "\r\n");
		opw.append("Num_runs:  " + Flags.REPEATS + "\r\n");
		
		opw.append("Average running time: \r\n");
		opw.append("Algo" + " " + "status" + " " + "vInterTime" + " " + "preFTime" + " " + "simFTime" + " " + "SGbuildTime"
				+ " " + "eInterTime" + " " + "noJoinTotTime" + " " + "joinTime" 
				+ " " + "totTime" + " " + "nodesSumInvL" + " " + "nodesAfterPreFilt" + " " + "nodesAfterVinter" + " " 
				+ "nodesSG" + " " + "numSoln" + " " +  "sizeOfSG" + " " + "numE.SG" + " " + "sizeUncovSG" 
				+ "\r\n");
	}
	
	public void printToFileCombined(PrintWriter opw, String algo) {
		DecimalFormat f = new DecimalFormat("##.0000");

		int totQs = qryEvalStats[0].size();
		int numQs = totQs;
		double[] evalTimes, matchTimes, enumTimes, preTimes;
		evalTimes = new double[numQs];
		matchTimes = new double[numQs];
		enumTimes = new double[numQs];
		preTimes = new double[numQs];

		double totNodesBefore = 0.0, totNodesAfter = 0.0;
		double totSolns = 0.0;

		for (int i = 0; i < Flags.REPEATS; i++) {
			numQs = totQs;
			ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[i];
			for (int q = 0; q < totQs; q++) {
				QueryEvalStat stat = qryEvalStatList.get(q);
				matchTimes[q] += stat.matchTime;
				enumTimes[q] += stat.enumTime;
				preTimes[q] += stat.preTime;
				evalTimes[q] += stat.totTime;
				totTimes[i] += evalTimes[q];
				totMatchTimes[i] += matchTimes[q];
				totEnumTimes[i] += enumTimes[q];
				if (i == 0) {
					totNodesBefore += stat.totNodesBefore;
					totNodesAfter += stat.totNodesAfter;
					totSolns += stat.numSolns;
				}

			}
		}

		ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[0];
		for (int q = 0; q < totQs; q++) {
			QueryEvalStat stat = qryEvalStatList.get(q);
			double numE = stat.sizeOfAnsGraph - stat.totNodesAfter;
			opw.append(algo + " " + q + " " + stat.status + " " 
					+ f.format(preTimes[q] / Flags.REPEATS) + " "
					+ f.format(matchTimes[q] / Flags.REPEATS) + " " + f.format(enumTimes[q] / Flags.REPEATS) + " "
					+ f.format(evalTimes[q] / Flags.REPEATS) + " "
					+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
					+ new DecimalFormat(",###").format(stat.totNodesAfter) + " "
					+ new DecimalFormat(",###").format(stat.numSolns) + " "
					+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + " "
					+ new DecimalFormat(",###").format(numE) + "\r\n");

		}

	
	}
	
	public void printToFileCombinedPartial(PrintWriter opw, String algo) {
		DecimalFormat f = new DecimalFormat("##.0000");

        int totQs = qryEvalStats[0].size();
        int numQs = totQs;
        double[] evalTimes, matchTimes, enumTimes, preTimes, vInterTimes, simTimes, eInterTimes;
        evalTimes = new double[numQs];
        matchTimes = new double[numQs];
        enumTimes = new double[numQs];
        preTimes = new double[numQs];
        vInterTimes = new double[numQs];
        simTimes = new double[numQs];
        eInterTimes = new double[numQs];


		for (int i = 0; i < Flags.REPEATS; i++) {
			numQs = totQs;
			ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[i];
			for (int q = 0; q < totQs; q++) {
				QueryEvalStat stat = qryEvalStatList.get(q);
				matchTimes[q] += stat.matchTime;
				enumTimes[q] += stat.enumTime;
				preTimes[q] += stat.preTime;
				evalTimes[q] += stat.totTime;
                vInterTimes[q] += stat.vInterTime;
                eInterTimes[q] += stat.eInterTime;
                simTimes[q] += stat.simTime;

			}
		}

		ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[0];
		for (int q = 0; q < totQs; q++) {
			QueryEvalStat stat = qryEvalStatList.get(q);
			double numE = stat.sizeOfAnsGraph - stat.totNodesAfter;
			String noJoinTotTime = f.format(vInterTimes[q] / Flags.REPEATS + preTimes[q] / Flags.REPEATS
					+ simTimes[q] / Flags.REPEATS + matchTimes[q] / Flags.REPEATS 
					+ eInterTimes[q] / Flags.REPEATS);
			opw.append(algo + " " + stat.status + " " 
                    + f.format(vInterTimes[q] / Flags.REPEATS) + " "
                    + f.format(preTimes[q] / Flags.REPEATS) + " "
                    + f.format(simTimes[q] / Flags.REPEATS) + " " + f.format(matchTimes[q] / Flags.REPEATS) + " "
                    + f.format(eInterTimes[q] / Flags.REPEATS) + " " + noJoinTotTime + " "
                    + f.format(enumTimes[q] / Flags.REPEATS) + " "
                    + f.format(evalTimes[q] / Flags.REPEATS) + " "
                    + new DecimalFormat(",###").format(stat.totNodesBefore) + " "
                    + new DecimalFormat(",###").format(stat.nodesAfterPreFilt) + " "
                    + new DecimalFormat(",###").format(stat.nodesAfterVinter) + " "
                    + new DecimalFormat(",###").format(stat.totNodesAfter) + " "
                    + new DecimalFormat(",###").format(stat.numSolns) + " "
                    + new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + " "
					+ new DecimalFormat(",###").format(numE) + " "
                    + new DecimalFormat(",###").format(stat.sizeOfUncovAnsGraph) + "\r\n");

		}

	
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append("Dataset:" + dataFN + "\r\n");
		sb.append("V:" + V + " " + "E:" + E + " " + "lbs:" + numLbs + "\r\n");
		sb.append("Queryset:" + qryFN + "\r\n");
		sb.append("Algorithm:" + algN + "\r\n");
		DecimalFormat f = new DecimalFormat("##.00");
		int numQs = qryEvalStats[0].size();
		double[] evalTimes, matchTimes, enumTimes, preTimes, planTimes;
		evalTimes = new double[numQs];
		matchTimes = new double[numQs];
		enumTimes = new double[numQs];
		preTimes = new double[numQs];
		planTimes = new double[numQs];

		double totNodesBefore = 0.0, totNodesAfter = 0.0;
		double totSolns = 0.0;

		sb.append("id" + " " + "preTime" + " " + "planTime" + " " + "matchTime" + " " + "enumTime" + " " + "totTime"
				+ " " + "numSoln" + " " + "numNodesBefore" + " " + "numNodesAfter" + "\r\n");

		for (int i = 0; i < Flags.REPEATS; i++) {
			ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[i];
			for (int q = 0; q < numQs; q++) {
				QueryEvalStat stat = qryEvalStatList.get(q);
				matchTimes[q] += stat.matchTime;
				enumTimes[q] += stat.enumTime;
				preTimes[q] += stat.preTime;
				planTimes[q] += stat.simTime;
				evalTimes[q] += stat.totTime;
				totTimes[i] += evalTimes[q];
				totMatchTimes[i] += matchTimes[q];
				totEnumTimes[i] += enumTimes[q];
				if (i == 0) {
					totNodesBefore += stat.totNodesBefore;
					totNodesAfter += stat.totNodesAfter;
					totSolns += stat.numSolns;
				}

				sb.append(q + " " + f.format(stat.preTime) + " " + f.format(stat.simTime) + " "
						+ f.format(stat.matchTime) + " " + f.format(stat.enumTime) + " " + f.format(stat.totTime) + " "
						+ new DecimalFormat(",###").format(stat.numSolns) + " "
						+ new DecimalFormat(",###").format(stat.totNodesBefore) + " "
						+ new DecimalFormat(",###").format(stat.totNodesAfter) + " " + "\r\n");

			}
		}

		sb.append("Average running time: \r\n");
		for (int q = 0; q < numQs; q++) {

			sb.append("[q" + " " + q + " " + "preTime" + " " + f.format(preTimes[q] / Flags.REPEATS) + " " + "planTime"
					+ " " + f.format(planTimes[q] / Flags.REPEATS) + " " + "matchTime" + " "
					+ f.format(matchTimes[q] / Flags.REPEATS) + " " + "enumTime" + " "
					+ f.format(enumTimes[q] / Flags.REPEATS) + " " + "totTime" + " "
					+ f.format(evalTimes[q] / Flags.REPEATS) + "]\r\n");
		}

		sb.append("Data loading Time:" + f.format(loadTime) + "\r\n");
		sb.append("Index building Time:" + f.format(bldTime) + "\r\n");
		double avgExeTime = calAvg(totTimes);
		double avgMatTime = calAvg(totMatchTimes);
		double avgPreTime = calAvg(totPreTimes);
		double avgPlanTime = calAvg(totPlanTimes);
		double avgEnumTime = calAvg(totEnumTimes);

		sb.append("Average Eval Time per run:" + f.format(avgExeTime) + "\r\n");
		sb.append("Average Eval Time per query:" + f.format(avgExeTime / numQs) + "\r\n");
		sb.append("Average pruning Time per run:" + f.format(avgPreTime) + "\r\n");
		sb.append("Average pruning Time per query:" + f.format(avgPreTime / numQs) + "\r\n");
		sb.append("Average Plan Time per run:" + f.format(avgPlanTime) + "\r\n");
		sb.append("Average Plan Time per query:" + f.format(avgPlanTime / numQs) + "\r\n");
		sb.append("Average Matching Time per run:" + f.format(avgMatTime) + "\r\n");
		sb.append("Average Matching Time per query:" + f.format(avgMatTime / numQs) + "\r\n");
		sb.append("Average Enumeration Time per run:" + f.format(avgEnumTime) + "\r\n");
		sb.append("Average Enumeration Time per query:" + f.format(avgEnumTime / numQs) + "\r\n");
		sb.append("Average Nodes before per query:" + f.format(totNodesBefore / numQs) + "\r\n");
		sb.append("Average Nodes after per query:" + f.format(totNodesAfter / numQs) + "\r\n");
		sb.append("Average number of solution tuples per query:" + f.format(totSolns / numQs) + "\r\n");
		sb.append("Max Used Memory:" + f.format(Flags.mt.getMaxUsedMem()) + " MB\r\n");

		return sb.toString();
	}

	private double calAvg(double[] statArr) {

		double sum = 0L;
		for (int i = 0; i < Flags.REPEATS; i++) {
			double curr = statArr[i];
			sum += curr;

		}

		return sum / Flags.REPEATS;
	}

	public static void main(String[] args) {

	}

}
