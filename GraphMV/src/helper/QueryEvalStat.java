package helper;

import java.util.ArrayList;

import dao.Pool;
import dao.PoolEntry;
import global.Consts.status_vals;

public class QueryEvalStat {

	public double sizeOfAnsGraph = 0.0, numSolns = 0.0, totNodesInv = 0.0, totNodesFlt = 0.0, totNodesSim = 0.0, totNodesMV = 0.0, totSolnNodes = 0.0;
	public double fltTime =0.0, simTime=0.0, pruneTime = 0.0, planTime = 0.0, bldTime = 0.0, enumTime = 0.0,  totTime = 0.0;
    public status_vals status = status_vals.success;
	public double numPlans = 0.0;
	
	public QueryEvalStat() {
	}

	
	public QueryEvalStat(QueryEvalStat s){
		
		setFields(s.fltTime, s.simTime, s.planTime, s.bldTime, s.enumTime, s.numSolns, s.numPlans, s.sizeOfAnsGraph);
		setTotNodesInv(s.totNodesInv);
		setTotNodesFlt(s.totNodesFlt);
		setTotNodesMV(s.totNodesMV);
		setTotNodesSim(s.totNodesSim);
	}
	
	public QueryEvalStat(double ft, double lt, double bt, double et, double solns) {
		setFields(ft, 0, lt, bt, et, solns, 0.0, 0.0);
	}
	
	public QueryEvalStat(double ft, double st, double lt, double bt, double et, double solns, double pls) {
	       
		setFields(ft, st, lt, bt, et, solns, pls, 0.0);
	}
	
	private void setFields(double ft, double st, double lt, double bt, double et, double solns, double pls, double agsz){
		
		fltTime = ft;
		simTime = st;
		pruneTime = ft+st;
		planTime = lt;
		bldTime = bt;
		enumTime = et;
		totTime = bt + et + ft + lt + st;
		numSolns = solns;
		numPlans = pls;
		sizeOfAnsGraph = agsz;
	}
	
	public void setTotNodesInv(double n){
		
		totNodesInv = n;
	}
	
    public void setTotNodesFlt(double n){
		
		totNodesFlt = n;
	}
	
	public void setTotNodesSim(double n){
		
		totNodesSim = n;
	}
	
	public void setTotNodesMV(double n){
		
		totNodesMV = n;
	}
	
	public void setFltTime(double ft){
		
		fltTime = ft;
		pruneTime+=ft;
		totTime+=ft;
	}
	
    public void setSimTime(double st){
		
		simTime = st;
		pruneTime+=st;
		totTime+=st;
	}
	
	public void setPlanTime(double lt){
		planTime = lt;
		totTime+=lt;
		
	}
	
	public void setBuildTime(double bt){
		bldTime = bt;
		totTime+=bt;
		
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
				//sizeOfAnsGraph+=e.getNumParEnties(); //in edges
			}
			
		}
		
		
	}

	public static void main(String[] args) {

	}

}
