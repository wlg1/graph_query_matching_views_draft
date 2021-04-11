package queryGeneratorByTemplate;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;

import edu.cornell.lassp.houle.RngPack.RandomElement;
import edu.cornell.lassp.houle.RngPack.RandomSeedable;
import edu.cornell.lassp.houle.RngPack.Ranmar;

public class QueryGenerator {

	private String mInFileName, mOutFileName;
	private int numlbs;
	private LineNumberReader m_in = null;
	private PrintWriter opw;
	private boolean first = true;
	private final int readAheadLimit = 10000;

	public QueryGenerator(String inFN, String outFN, int numlbs) {

		mInFileName = inFN;
		mOutFileName = outFN;
		this.numlbs = numlbs;

		try {
			m_in = new LineNumberReader(new FileReader(mInFileName));
			opw = new PrintWriter(new FileOutputStream(mOutFileName, false));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public QueryGenerator(String inFN, String outFN) {

		mInFileName = inFN;
		mOutFileName = outFN;
	

		try {
			m_in = new LineNumberReader(new FileReader(mInFileName));
			opw = new PrintWriter(new FileOutputStream(mOutFileName, false));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	public void run() {

		String line = null;
		String[] buf;
		long seed = RandomSeedable.ClockSeed();
		RandomElement e = new Ranmar(seed);

		while (true) {
			if (first) {
				line = readNextLine();
				first = false;
				opw.println(line);
			}
			// get total number of nodes
			int[] VE = getVandE();
			int V = VE[0];
			int E = VE[1];
			if (V == 0)
				break;
			while ((line = readNextLine()) != null) {
				buf = line.split("\\s+");
				if (line.charAt(0) == 'v') {
					int index = Integer.parseInt(buf[1]);
					int lb = e.choose(1, numlbs);
					opw.println("v" + " " + index + " " + lb);
				} else

					opw.println(line);

			}
			opw.flush();

		}

		opw.close();
	}
	
	
	public void run(int lowbound) {

		String line = null;
		String[] buf;
		long seed = RandomSeedable.ClockSeed();
		RandomElement e = new Ranmar(seed);

		while (true) {
			if (first) {
				line = readNextLine();
				first = false;
				opw.println(line);
			}
			// get total number of nodes
			int[] VE = getVandE();
			int V = VE[0];
			int E = VE[1];
			if (V == 0)
				break;
			while ((line = readNextLine()) != null) {
				buf = line.split("\\s+");
				if (line.charAt(0) == 'v') {
					int index = Integer.parseInt(buf[1]);
					int lb = e.choose(lowbound, numlbs);
					opw.println("v" + " " + index + " " + lb);
				} else

					opw.println(line);

			}
			opw.flush();

		}

		opw.close();
	}
	public void genHybrid(int axis) {

		String line = null;
		String[] buf;
		long seed = RandomSeedable.ClockSeed();
		RandomElement e = new Ranmar(seed);

		while (true) {
			if (first) {
				line = readNextLine();
				first = false;
				opw.println(line);
			}
			// get total number of nodes
			int[] VE = getVandE();
			int V = VE[0];
			int E = VE[1];
			if (V == 0)
				break;
			while ((line = readNextLine()) != null) {
				buf = line.split("\\s+");
				
				if(line.charAt(0) == 'e'){
					int v = Integer.parseInt(buf[1]);
					int w = Integer.parseInt(buf[2]);
					int a = 0;
					if(axis==1)
						a=1;
					else if(axis==2&& e.coin()){
						a=1;
					}
						
					opw.println("e" + " " + v + " " + w + " " + a);
				}
				else

					opw.println(line);

			}
			opw.flush();

		}

		opw.close();
	}

	private String readNextLine() {
		String line = null;

		try {
			// read a non-emtpy line
			while ((line = m_in.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0)
					break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return line;
	}

	private int[] getVandE() {
		int[] rs = new int[2];

		String line = null;
		try {
			m_in.mark(readAheadLimit);
			while ((line = readNextLine()) != null) {

				if (line.charAt(0) == 'v')
					rs[0]++;
				else if (line.charAt(0) == 'e')
					rs[1]++;
				else
					break;

			}
			m_in.reset();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return rs;
	}

	public static void main(String[] args) {
		// for datasets of sigmod20, lowbounds =0, others lowbounds = 1;
        int lowbounds = 1;
        
		QueryGenerator qgen = new QueryGenerator
				("D:\\Documents\\_prog\\prog_cust\\eclipse-workspace\\graphGen\\src\\templates-full\\templates_cyc_c.txt", "D:\\Documents\\_prog\\prog_cust\\eclipse-workspace\\graph_expr\\input_files\\inst_lb1_cyc_c.qry", lowbounds+20);
//				("D:\\Documents\\_prog\\prog_cust\\eclipse-workspace\\graphGen\\src\\templates-full\\templates_tree_m.txt", "D:\\Documents\\_prog\\prog_cust\\eclipse-workspace\\graphGen\\src\\output\\inst_lb20_tree_m.qry", lowbounds+20);
				//("E:\\experiments\\DagOnGraph\\input\\templates.txt", "E:\\experiments\\DagOnGraph\\input\\instances_lb200.qry", 200);
		        //("E:\\experiments\\DagOnGraph\\input\\templates.txt", "E:\\experiments\\DagOnGraph\\input\\instances_lb1.qry", 1);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_cyc_d.qry", 1);
		//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_cyc_c.qry", 1);
		//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_cyc_m.qry", 1);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_cyc_d_s0.qry", lowbounds+20);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_cyc_c_s0.qry", lowbounds+20);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_cyc_m_s0.qry", lowbounds+20);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_cyc_d_s0.qry", lowbounds+20);
		
		        //("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_acyc_d_s0.qry", lowbounds+20);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_acyc_c_s0.qry", lowbounds+20);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_acyc_m_s0.qry", lowbounds+20);
			
				//("E:\\experiments\\GraHomMat\\input\\templates_path_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_path_d.qry", lowbounds+20);
		//("E:\\experiments\\GraHomMat\\input\\templates_path_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_path_c.qry", lowbounds+20);
		//("E:\\experiments\\GraHomMat\\input\\templates_path_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_path_m.qry", lowbounds+20;
		        //("E:\\experiments\\GraHomMat\\input\\templates-full\\templates_path_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_path_d.qry", lowbounds+20);
				//("E:\\experiments\\GraHomMat\\input\\templates-full\\\\templates_path_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_path_c.qry", lowbounds+20);
				//("E:\\experiments\\GraHomMat\\input\\templates-full\\templates_path_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_path_m.qry", lowbounds+20);

		//("E:\\experiments\\GraHomMat\\input\\templates_tree_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_tree_d.qry", 1);
		//("E:\\experiments\\GraHomMat\\input\\templates_tree_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_tree_c.qry", 1);
		//("E:\\experiments\\GraHomMat\\input\\templates_tree_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_tree_m.qry", 1);
		//("E:\\experiments\\GraHomMat\\input\\templates-full\\templates_tree_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_tree_d.qry", lowbounds+20);
		//("E:\\experiments\\GraHomMat\\input\\templates-full\\templates_tree_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_tree_c.qry", lowbounds+20);
		//("E:\\experiments\\GraHomMat\\input\\templates-full\\templates_tree_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_tree_m.qry", lowbounds+20);

		//("E:\\experiments\\GraHomMat\\input\\templates_dag_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_dag_d.qry", 1);
		//("E:\\experiments\\GraHomMat\\input\\templates_dag_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_dag_c.qry", 1);
		//("E:\\experiments\\GraHomMat\\input\\templates_dag_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb1_dag_m.qry", 1);
		//("E:\\experiments\\GraHomMat\\input\\templates_dag_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_dag_d.qry", 20);
		//("E:\\experiments\\GraHomMat\\input\\templates_dag_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_dag_c.qry", 20);
		//("E:\\experiments\\GraHomMat\\input\\templates_dag_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_dag_m.qry", 20);
	
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_acyc_d.qry", 21);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_acyc_c.qry", 21);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb20_acyc_m.qry", 21);
		//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb10_acyc_d.qry", 11);
		//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb10_acyc_c.qry", 11);
		//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb10_acyc_m.qry", 11);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb10_acyc_d.qry", 11);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb10_acyc_c.qry", 11);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb10_acyc_m.qry", 11);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb5_cyc_d.qry", 6);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb5_cyc_c.qry", 6);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb5_cyc_m.qry", 6);

				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb30_acyc_d.qry", lowbounds+30);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb30_acyc_c.qry", lowbounds+30);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb30_acyc_m.qry", lowbounds+30);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb30_cyc_d.qry", lowbounds+30);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb30_cyc_c.qry", lowbounds+30);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb30_cyc_m.qry", lowbounds+30);

				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb50_acyc_d.qry", 50);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb50_acyc_c.qry", 50);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb50_acyc_m.qry", 50);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb50_cyc_d.qry", 50);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb50_cyc_c.qry", 50);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb50_cyc_m.qry", 50);

				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb100_acyc_d.qry", 100);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb100_acyc_c.qry", 100);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb100_acyc_m.qry", 100);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb100_cyc_d.qry", 100);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb100_cyc_c.qry", 100);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb100_cyc_m.qry", 100);

				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb200_acyc_d.qry", 200);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb200_acyc_c.qry", 200);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb200_acyc_m.qry", 200);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb200_cyc_d.qry", 200);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb200_cyc_c.qry", 200);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb200_cyc_m.qry", 200);

				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb5_acyc_d.qry", 5);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb5_acyc_c.qry", 5);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb5_acyc_m.qry", 5);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb5_cyc_d.qry", 5);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb5_cyc_c.qry", 5);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb5_cyc_m.qry", 5);
				
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb3_acyc_d.qry", 4);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb3_acyc_c.qry", 4);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb3_acyc_m.qry", 4);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb3_cyc_d.qry", 4);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb3_cyc_c.qry", 4);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb3_cyc_m.qry", 4);

				
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb25_acyc_d.qry", lowbounds+25);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb25_acyc_c.qry", lowbounds+25);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb25_acyc_m.qry", lowbounds+25);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb25_cyc_d.qry", lowbounds+25);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb25_cyc_c.qry", lowbounds+25);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb25_cyc_m.qry", lowbounds+25);
				
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb15_acyc_d_s0.qry", lowbounds+15);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb15_acyc_c_s0.qry", lowbounds+15);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb15_acyc_m_s0.qry", lowbounds+15);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb15_cyc_d_s0.qry", lowbounds+15);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb15_cyc_c_s0.qry", lowbounds+15);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb15_cyc_m_s0.qry", lowbounds+15);
		
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb7708_acyc_d.qry", 7708);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb7708_acyc_c.qry", 7708);
				//("E:\\experiments\\GraHomMat\\input\\templates_acyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb7708_acyc_m.qry", 7708);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_d.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb7708_cyc_d.qry", 7708);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_c.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb7708_cyc_c.qry", 7708);
				//("E:\\experiments\\GraHomMat\\input\\templates_cyc_m.txt", "E:\\experiments\\GraHomMat\\input\\inst_lb7708_cyc_m.qry", 7708);
				//qgen.run();
				qgen.run(lowbounds); 
				//("E:\\experiments\\GraHomMat\\input\\yeast_dense_8.graph", "E:\\experiments\\GraHomMat\\input\\yeast_dense_8_d.qry");
		//("E:\\experiments\\GraHomMat\\input\\hprd_query.graph", "E:\\experiments\\GraHomMat\\input\\hprd_m.qry");
		//("E:\\experiments\\GraHomMat\\input\\hu_sparse_20_5.graph", "E:\\experiments\\GraHomMat\\input\\hu_sparse_20_5_m.qry");
		//qgen.genHybrid(2);
		
	}

}
