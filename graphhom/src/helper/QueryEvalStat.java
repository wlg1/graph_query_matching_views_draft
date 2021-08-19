package helper;

import java.util.ArrayList;

import dao.Pool;
import dao.PoolEntry;
import global.Consts.status_vals;

public class QueryEvalStat {

	public double sizeOfAnsGraph = 0.0, numSolns = 0.0, totNodesBefore = 0.0, totNodesAfter = 0.0, totSolnNodes = 0.0,
			nodesAfterPreFilt = 0.0, nodesAfterVinter = 0.0, sizeOfUncovAnsGraph;
	public double preTime = 0.0, simTime = 0.0, matchTime = 0.0, enumTime,  totTime = 0.0, vInterTime = 0.0, eInterTime = 0.0;
    public status_vals status = status_vals.success;
	public double numPlans = 0.0;
	
	public QueryEvalStat() {
	}

	
	public QueryEvalStat(QueryEvalStat s){
		
		setFields(s.vInterTime, s.preTime, s.simTime, s.eInterTime, s.matchTime, s.enumTime, s.numSolns, s.numPlans, s.sizeOfAnsGraph,
				s.nodesAfterPreFilt, s.nodesAfterVinter, s.sizeOfUncovAnsGraph);
		setTotNodesBefore(s.totNodesBefore);
		setTotNodesAfter(s.totNodesAfter);
	}
	
	public QueryEvalStat(double vt, double pt, double st, double eit, double mt, double et, double solns) {
		setFields(vt, pt, st, eit, mt, et, solns, 0.0, 0.0, 0.0, 0.0, 0.0);
	}
	
//	public QueryEvalStat(double vt, double pt, double lt, double mt, double et, double solns, double pls) {
//	       
//		setFields(vt, pt, lt, mt, et, solns, pls, 0.0, 0.0, 0.0);
//	}
	
	private void setFields(double vt, double pt, double st, double eit, double mt, double et, double solns, double pls, double agsz,
			double napf, double navi, double sou){
		
		vInterTime = vt;
		preTime = pt;
		simTime = st;
		eInterTime = eit;
		matchTime = mt;
		enumTime = et;
		totTime = vt+ mt + et + pt + st;
		numSolns = solns;
		numPlans = pls;
		sizeOfAnsGraph = agsz;
		nodesAfterPreFilt = napf;
		nodesAfterVinter = navi;
		sizeOfUncovAnsGraph = sou;
	}
	
	public void setTotNodesBefore(double n){
		
		totNodesBefore = n;
	}
	
	public void setTotNodesAfter(double n){
		
		totNodesAfter = n;
	}
	
	public void setPreTime(double pt){
		
		preTime = pt;
		totTime+=pt;
	}
	
	
	public void setsimTime(double lt){
		simTime = lt;
		totTime+=lt;
		
	}
	
	public void setvInterTime(double vt){
		vInterTime = vt;
		totTime+=vt;
		
	}
	
	public void seteInterTime(double vt){
		eInterTime = vt;
		totTime+=vt;
		
	}
	
	public void setMatchTime(double mt){
		matchTime = mt;
		totTime+=mt;
		
	}
	
	public void setEnumTime(double et){
		
		enumTime = et;
		totTime+=et;
	}

	public void setNumSolns(double solns){
		
		numSolns = solns;
	}
	
	public void setNumPlans(double pls){
		
		numPlans = pls;
	}
	
	public void setStatus(status_vals s){
		
		status = s;
	
	}

	public void calAnsGraphSize(ArrayList<Pool> poolArr){
	
		for(Pool pl:poolArr){
			sizeOfAnsGraph +=pl.elist().size(); //nodes
			for (PoolEntry e :pl.elist()){
				sizeOfAnsGraph+=e.getNumChildEnties(); //out edges
				//sizeOfAnsGraph+=e.getNumParEnties(); //in edges. dont use b/c redundant w/ outedges
			}
			
		}
		
		
	}
	
	public void calUncovAnsGraphSize(ArrayList<Pool> poolArr){
		
		for(Pool pl:poolArr){
			sizeOfUncovAnsGraph +=pl.elist().size(); //nodes
			for (PoolEntry e :pl.elist()){
				sizeOfUncovAnsGraph+=e.getNumChildEnties(); //out edges
				//sizeOfAnsGraph+=e.getNumParEnties(); //in edges
			}
			
		}
		
		
	}

	public static void main(String[] args) {

	}

}
