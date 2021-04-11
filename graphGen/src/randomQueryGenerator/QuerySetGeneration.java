package randomQueryGenerator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

public class QuerySetGeneration {

	ArrayList<Query> queries;

	String queryFN, statFN;
	int[] queryNum;

	public QuerySetGeneration(String queryFN, String statFN, int[] queryNum) {

		this.queryFN = queryFN;
		this.statFN = statFN;
		this.queryNum = queryNum;
	}

	public QuerySetGeneration(String queryFN, String statFN) {

		this.queryFN = queryFN;
		this.statFN = statFN;

	}

	public void run() {
		readQueries();
		int rounds = queryNum.length;
		String suffix = queryFN.substring(queryFN.lastIndexOf('.'));
		String fn = queryFN.substring(0, queryFN.lastIndexOf('.'));

		try {
			BufferedReader in = new BufferedReader(new FileReader(statFN));
			for (int i = 0; i < rounds; i++) {

				int num = queryNum[i];
				String outFN = new String(fn + i + suffix);
				PrintWriter pw = new PrintWriter(new FileOutputStream(outFN));
				String line = null;
				int ID = 0;
				if ((line = readNextLine(in)) != null) {
					String[] buf = line.split("\\s+");
					if (buf.length < num)
						num = buf.length;
					for (int j = 0; j < num; j++) {

						int qid = Integer.parseInt(buf[j]);
						Query qry = queries.get(qid);
						write(ID++, qry, pw);

					}

				}

				pw.close();
			}
		} catch (FileNotFoundException e1) {

			e1.printStackTrace();
		}

	}

	public void run(int numOfQrys) {
		readQueries();
		String suffix = queryFN.substring(queryFN.lastIndexOf('.'));
		String fn = queryFN.substring(0, queryFN.lastIndexOf('.'));

		try {
			BufferedReader in = new BufferedReader(new FileReader(statFN));

			String line = null;
			int i = 0;
			while ((line = readNextLine(in)) != null) {
				int ID = 0;
				String outFN = new String(fn + i + suffix);
				PrintWriter pw = new PrintWriter(new FileOutputStream(outFN));
				i++;
				String[] buf = line.split("\\s+");
				if (buf.length < numOfQrys)
					numOfQrys = buf.length;
				ArrayList<Integer> qlist = genRandomNumbers(numOfQrys, buf.length);
				for (int j = 0; j < numOfQrys; j++) {

					int qid = Integer.parseInt(buf[qlist.get(j)]);
					Query qry = queries.get(qid);
					write(ID++, qry, pw);

				}

				pw.close();

			}

		} catch (FileNotFoundException e1) {

			e1.printStackTrace();
		}

	}

	private void write(int qid, Query qry, PrintWriter pw) {
		pw.println("q" + qid);
		// output nodes
		QNode[] nodes = qry.nodes;

		for (QNode n : nodes) {

			pw.println("v" + " " + n.id + " " + n.lb);
		}

		pw.flush();

		// output edges

		for (QNode n : nodes) {
			if (n.N_O_SZ > 0)
				for (QNode w : n.N_O) {
					pw.println("e" + " " + n.id + " " + w.id + " " + w.axis);

				}
		}
		pw.flush();
	}

	private void readQueries() {

		queries = new ArrayList<Query>();
		QueryParser queryParser = new QueryParser(queryFN);

		Query qry_o = null;
		while ((qry_o = queryParser.readNextQuery()) != null) {

			queries.add(qry_o);
		}

	}

	private String readNextLine(BufferedReader in) {
		String line = null;

		try {
			// read a non-emtpy line
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0)
					break;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return line;
	}

	private static final ArrayList<Integer> genRandomNumbers(int num, int range) {

		// define ArrayList to hold Integer objects
		ArrayList<Integer> numbers = new ArrayList<Integer>(range);
		for (int i = 0; i < range; i++) {
			numbers.add(i);
		}

		Collections.shuffle(numbers);

		ArrayList<Integer> randomNumbers = new ArrayList<Integer>(num);

		for (int i = 0; i < num; i++) {

			randomNumbers.add(numbers.get(i));
		}

		Collections.sort(randomNumbers);
		return randomNumbers;
	}

	public static void main(String[] args) {
		//int[] qnum = new int[] { 500, 150, 50, 20, 100 };

		//QuerySetGeneration qsg = new QuerySetGeneration(".//data//cita_nw2_n.qry", ".//data//citanw2qstats.txt", qnum);
		QuerySetGeneration qsg = new QuerySetGeneration(".//data//cita_nw2_n.qry", ".//data//citanw2qstats_n.txt");

		qsg.run(10);

	}

}
