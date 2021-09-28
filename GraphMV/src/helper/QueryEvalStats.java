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
	public double[] totTimes, totBuildTimes, totEnumTimes, totFltTimes, totSimTimes, totPruneTimes, totPlanTimes;

	public QueryEvalStats() {

		qryEvalStats = (ArrayList<QueryEvalStat>[]) new ArrayList[Flags.REPEATS];
		totTimes = new double[Flags.REPEATS];
		totBuildTimes = new double[Flags.REPEATS];
		totEnumTimes = new double[Flags.REPEATS];
		totFltTimes = new double[Flags.REPEATS];
		totSimTimes = new double[Flags.REPEATS];
		totPruneTimes = new double[Flags.REPEATS];
		totPlanTimes = new double[Flags.REPEATS];
	}

	public QueryEvalStats(String dataFN, String qryFN, String algN) {

		qryEvalStats = (ArrayList<QueryEvalStat>[]) new ArrayList[Flags.REPEATS];
		totTimes = new double[Flags.REPEATS];
		totBuildTimes = new double[Flags.REPEATS];
		totEnumTimes = new double[Flags.REPEATS];
		totFltTimes = new double[Flags.REPEATS];
		totSimTimes = new double[Flags.REPEATS];
		totPruneTimes = new double[Flags.REPEATS];
		totPlanTimes = new double[Flags.REPEATS];
		this.dataFN = dataFN;
		this.qryFN = qryFN;
		this.algN = algN;
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

	public void add(int iter, int qid, double ft, double lt, double mt, double et, double solns) {

		QueryEvalStat qst = new QueryEvalStat(ft, lt, mt, et, solns);
		// add(qst);
		add(iter, qid, qst);
	}

	public void add(int iter, int qid, QueryEvalStat qst) {
		if (qryEvalStats[iter] == null)
			qryEvalStats[iter] = new ArrayList<QueryEvalStat>();

		qryEvalStats[iter].add(qid, qst);

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
		double[] evalTimes, buildTimes, enumTimes, pruneTimes, fltTimes, simTimes, planTimes;
		evalTimes = new double[numQs];
		buildTimes = new double[numQs];
		enumTimes = new double[numQs];
		pruneTimes = new double[numQs];
		fltTimes = new double[numQs];
		simTimes = new double[numQs];
		planTimes = new double[numQs];

		double totNodesInv = 0.0, totNodesSim = 0.0, totNodesFlt = 0.0, totNodesMV = 0.0;
		double totSolns = 0.0;

		opw.append("id" + " " + "status" + " " + "fltTime" + " " + "simTime" + " " + "pruneTime" + " " + "planTime" + " " + "buildTime" + " " + "enumTime"
				+ " " + "totTime" + " " + "totNodesInv" + " " + "totNodesFlt" + " " + "totNodesMV" + " " + "totNodesSim" + " " + "numOfTuples" + " "
				+ "numPlans" + " " +  "sizeOfAG" + "\r\n");

		for (int i = 0; i < Flags.REPEATS; i++) {
			numQs = totQs;
			ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[i];
			for (int q = 0; q < totQs; q++) {
				QueryEvalStat stat = qryEvalStatList.get(q);
				//if (stat.status != Consts.status_vals.success) {
				//	numQs--;

				//} else {
					buildTimes[q] += stat.bldTime;
					fltTimes[q] += stat.fltTime;
					simTimes[q] += stat.simTime;
					pruneTimes[q] += stat.pruneTime;
					planTimes[q] += stat.planTime;
					evalTimes[q] += stat.totTime;
					stat.enumTime = stat.totTime - stat.fltTime - stat.simTime -stat.planTime - stat.bldTime;
					enumTimes[q] += stat.enumTime;
					totTimes[i] += evalTimes[q];
					totFltTimes[i] += fltTimes[q];
					totSimTimes[i] += simTimes[q];
					totBuildTimes[i] += buildTimes[q];
					totEnumTimes[i] += enumTimes[q];
					if (i == 0) {
						totNodesInv += stat.totNodesInv;
						totNodesFlt += stat.totNodesFlt;
						totNodesSim += stat.totNodesSim;
						totNodesMV += stat.totNodesMV;
						totSolns += stat.numSolns;
					}
				//}

				opw.append(q + " " + stat.status + " " + f.format(stat.fltTime) + " " + f.format(stat.simTime) + " " +  f.format(stat.pruneTime) + " " + f.format(stat.planTime) + " "
						+ f.format(stat.bldTime) + " " + f.format(stat.enumTime) + " " + f.format(stat.totTime) + " "
						+ new DecimalFormat(",###").format(stat.totNodesInv) + " "
						+ new DecimalFormat(",###").format(stat.totNodesFlt) + " "
						+ new DecimalFormat(",###").format(stat.totNodesMV) + " "
						+ new DecimalFormat(",###").format(stat.totNodesSim) + " "
						+ new DecimalFormat(",###").format(stat.numSolns) + " "
						+ new DecimalFormat(",###").format(stat.numPlans) + " "
						+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + "\r\n");

			}
		}

		opw.append("Average running time: \r\n");
		opw.append("id" + " " + "status" + " " + "fltTime" + " " + "simTime" + " " + "pruneTime" + " " + "planTime" + " " + "buildTime" + " " + "enumTime"
				+ " " + "totTime" + " " + "totNodesInv" + " " + "totNodesFlt" + " " + "totNodesMV" + " " + "totNodesSim" + " " + "numOfTuples" + " "
				+ "numPlans" + " " +  "sizeOfAG" + "\r\n");
 	    ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[0];
		for (int q = 0; q < totQs; q++) {
			QueryEvalStat stat = qryEvalStatList.get(q);

			opw.append(q + " " + stat.status + " " 
					+ f.format(fltTimes[q] / Flags.REPEATS) + " "
					+ f.format(simTimes[q] / Flags.REPEATS) + " "
					+ f.format(pruneTimes[q] / Flags.REPEATS) + " " + f.format(planTimes[q] / Flags.REPEATS) + " "
					+ f.format(buildTimes[q] / Flags.REPEATS) + " " + f.format(enumTimes[q] / Flags.REPEATS) + " "
					+ f.format(evalTimes[q] / Flags.REPEATS) + " "
					+ new DecimalFormat(",###").format(stat.totNodesInv) + " "
					+ new DecimalFormat(",###").format(stat.totNodesFlt) + " "
					+ new DecimalFormat(",###").format(stat.totNodesMV) + " "
					+ new DecimalFormat(",###").format(stat.totNodesSim) + " "
					+ new DecimalFormat(",###").format(stat.numSolns) + " "
					+ new DecimalFormat(",###").format(stat.numPlans) + " "
					+ new DecimalFormat(",###").format(stat.sizeOfAnsGraph) + "\r\n");

		}

		opw.append("Data loading Time:" + f.format(loadTime) + "\r\n");
		opw.append("Index building Time:" + f.format(bldTime) + "\r\n");

		double avgExeTime = calAvg(totTimes);
		double avgBuildTime = calAvg(totBuildTimes);
		double avgFltTime = calAvg(totFltTimes);
		double avgPruneTime = calAvg(totPruneTimes);
		double avgSimTime = calAvg(totSimTimes);
		double avgPlanTime = calAvg(totPlanTimes);
		double avgEnumTime = calAvg(totEnumTimes);

		opw.append("Average Eval Time per run:" + f.format(avgExeTime) + "\r\n");
		opw.append("Average Eval Time per query:" + f.format(avgExeTime / numQs) + "\r\n");
		opw.append("Average flting Time per run:" + f.format(avgFltTime) + "\r\n");
		opw.append("Average flting Time per query:" + f.format(avgFltTime / numQs) + "\r\n");
		opw.append("Average siming Time per run:" + f.format(avgSimTime) + "\r\n");
		opw.append("Average siming Time per query:" + f.format(avgSimTime / numQs) + "\r\n");
		opw.append("Average pruning Time per run:" + f.format(avgPruneTime) + "\r\n");
		opw.append("Average pruning Time per query:" + f.format(avgPruneTime / numQs) + "\r\n");
		opw.append("Average Plan Time per run:" + f.format(avgPlanTime) + "\r\n");
		opw.append("Average Plan Time per query:" + f.format(avgPlanTime / numQs) + "\r\n");
		opw.append("Average building Time per run:" + f.format(avgBuildTime) + "\r\n");
		opw.append("Average building Time per query:" + f.format(avgBuildTime / numQs) + "\r\n");
		opw.append("Average Enumeration Time per run:" + f.format(avgEnumTime) + "\r\n");
		opw.append("Average Enumeration Time per query:" + f.format(avgEnumTime / numQs) + "\r\n");
		opw.append("Average Nodes inv per query:" + f.format(totNodesInv / numQs) + "\r\n");
		opw.append("Average Nodes flt per query:" + f.format(totNodesFlt / numQs) + "\r\n");
		opw.append("Average Nodes views per query:" + f.format(totNodesMV / numQs) + "\r\n");
		opw.append("Average Nodes sim per query:" + f.format(totNodesSim / numQs) + "\r\n");
		opw.append("Average number of solution tuples per query:" + f.format(totSolns / numQs) + "\r\n");
		opw.append("Max Used Memory:" + f.format(Flags.mt.getMaxUsedMem()) + " MB\r\n");
		opw.append("*****************************************************\r\n");
	
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

		double totNodesBefore = 0.0, totNodesMV = 0.0, totNodesAfter = 0.0;
		double totSolns = 0.0;

		sb.append("id" + " " + "preTime" + " " + "planTime" + " " + "matchTime" + " " + "enumTime" + " " + "totTime"
				+ " " + "numSoln" + " " + "numNodesBefore" + " " + "numNodesAfter" + "\r\n");

		for (int i = 0; i < Flags.REPEATS; i++) {
			ArrayList<QueryEvalStat> qryEvalStatList = qryEvalStats[i];
			for (int q = 0; q < numQs; q++) {
				QueryEvalStat stat = qryEvalStatList.get(q);
				matchTimes[q] += stat.bldTime;
				enumTimes[q] += stat.enumTime;
				preTimes[q] += stat.pruneTime;
				planTimes[q] += stat.planTime;
				evalTimes[q] += stat.totTime;
				totTimes[i] += evalTimes[q];
				totBuildTimes[i] += matchTimes[q];
				totEnumTimes[i] += enumTimes[q];
				if (i == 0) {
					totNodesBefore += stat.totNodesInv;
					totNodesAfter += stat.totNodesSim;
					totSolns += stat.numSolns;
				}

				sb.append(q + " " + f.format(stat.pruneTime) + " " + f.format(stat.planTime) + " "
						+ f.format(stat.bldTime) + " " + f.format(stat.enumTime) + " " + f.format(stat.totTime) + " "
						+ new DecimalFormat(",###").format(stat.numSolns) + " "
						+ new DecimalFormat(",###").format(stat.totNodesInv) + " "
						+ new DecimalFormat(",###").format(stat.totNodesSim) + " " + "\r\n");

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
		double avgMatTime = calAvg(totBuildTimes);
		double avgPreTime = calAvg(totPruneTimes);
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
		sb.append("Average Nodes after using Mat views:" + f.format(totNodesMV / numQs) + "\r\n");
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
