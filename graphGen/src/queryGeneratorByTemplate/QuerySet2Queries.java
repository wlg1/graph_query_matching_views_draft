package queryGeneratorByTemplate;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import graphStats.Consts;
import queryParser.QEdge;
import queryParser.QNode;
import queryParser.Query;
import queryParser.QueryParser;

public class QuerySet2Queries {

	String querySetFileN;
	QueryParser queryParser;

	public QuerySet2Queries(String queryFN) {

		querySetFileN = Consts.INDIR + queryFN;
		queryParser = new QueryParser(querySetFileN);

	}

	public void run() {
		String prefix = querySetFileN.substring(0, querySetFileN.indexOf("."));

		Query query = null;
		int count = 0;

		while ((query = queryParser.readNextQuery()) != null) {
			String outFN = prefix + "_q" + (count++) + ".graph";
			try {
				PrintWriter opw = new PrintWriter(new FileOutputStream(outFN, false));
				printToFile(query, opw);
                opw.close();
			} catch (FileNotFoundException e) {

				e.printStackTrace();
			}

		}
	}

	private void printToFile(Query query, PrintWriter opw) {

		int V = query.V, E = query.E;
		opw.println("t " + V + " " + E);
		for (QNode n : query.nodes) {
			opw.println("v" + " " + n.id + " " + n.lb + " " + query.degree(n.id));

		}

		for (QEdge e : query.edges) {

			opw.println("e" + " " + e.from + " " + e.to + " " + 0);
		}
	}

	public static void main(String[] args) {

		QuerySet2Queries qsq = new QuerySet2Queries
				//("inst_lb20_cyc_c.qry");
		        //  ("inst_lb20_acyc_c.qry");
				//("scale_lb20.qry");
				("scale_lb15_c.qry");
		qsq.run();
	
	}

}
