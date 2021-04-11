package queryGeneratorByTemplate;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import queryParser.QEdge;
import queryParser.QNode;
import queryParser.Query;
import queryParser.QueryParser;

public class GraphFlowQryGen {

	private String mInFN, mOutFN;
	private PrintWriter opw;
	QueryParser queryParser;

	public GraphFlowQryGen(String inF, String outF) {

		mInFN = inF;
		mOutFN = outF;
		queryParser = new QueryParser(mInFN);
		try {
			opw = new PrintWriter(new FileOutputStream(mOutFN, false));
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}
	}

	public void run(boolean haslable) {

		Query query = null;
		QNode[] nodes;
		QEdge[] edges;
		int qid = 0;
		while ((query = queryParser.readNextQuery()) != null) {

			nodes = query.nodes;
			edges = query.edges;
			opw.println("Q" + qid++);
			// "(a:9)->(b:9), (a:9)->(c:15), (b:9)->(c:15), (c:15)->(d:0)"
			for (int i = 0; i < edges.length; i++) {
				QEdge e = edges[i];
				int fid = e.from;
				int tid = e.to;
				String flb = nodes[fid].lb, tlb = nodes[tid].lb;
				if (haslable)
					opw.append("(" + fid + ":" + flb + ")->" + "(" + tid + ":" + tlb + ")");
				else
					opw.append("(" + fid + ")->" + "(" + tid + ")");
				if (i < edges.length - 1) {
					opw.append(", ");

				} else
					opw.append("\r\n");

			}

			opw.flush();

		}
		opw.close();
	}

	public static void main(String[] args) {

		GraphFlowQryGen gfg = new GraphFlowQryGen
		//("E:\\experiments\\GraHomMat\\input\\inst_lb1_acyc_c.qry", "E:\\experiments\\GraHomMat\\input\\graphflow\\acyc_lb1.qry");
		//("E:\\experiments\\GraHomMat\\input\\inst_lb5_cyc_c.qry", "E:\\experiments\\GraHomMat\\input\\graphflow\\cyc_lb5.qry");
				//("E:\\experiments\\GraHomMat\\input\\inst_lb5_acyc_c.qry", "E:\\experiments\\GraHomMat\\input\\graphflow\\acyc_lb5.qry");
				//("E:\\experiments\\GraHomMat\\input\\inst_lb10_cyc_c.qry", "E:\\experiments\\GraHomMat\\input\\graphflow\\cyc_lb10.qry");
				//("E:\\experiments\\GraHomMat\\input\\inst_lb3_acyc_c.qry", "E:\\experiments\\GraHomMat\\input\\graphflow\\acyc_lb3.qry");
				("E:\\experiments\\GraHomMat\\input\\scale_lb20.qry", "E:\\experiments\\GraHomMat\\input\\graphflow\\scale_lb20.qry");
		
				 //("E:\\experiments\\GraHomMat\\input\\inst_lb3_cyc_c.qry","E:\\experiments\\GraHomMat\\input\\graphflow\\cyc_lb3.qry");
		 //("E:\\experiments\\GraHomMat\\input\\hprd_c.qry", "E:\\experiments\\GraHomMat\\input\\graphflow\\hprd.qry");
		// ("E:\\experiments\\GraHomMat\\input\\human_c.qry","E:\\experiments\\GraHomMat\\input\\graphflow\\human.qry");
	//("E:\\experiments\\GraHomMat\\input\\yeast_c.qry", "E:\\experiments\\GraHomMat\\input\\graphflow\\yeast.qry");
		
		gfg.run(true); // false for lb=1
	}

}
