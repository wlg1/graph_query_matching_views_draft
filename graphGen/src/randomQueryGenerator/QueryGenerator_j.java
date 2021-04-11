package randomQueryGenerator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class QueryGenerator_j {

	Digraph m_graph;
	GraphNode[] m_gNodes;
	GraphNode m_root;
	HashMap<Integer, String> i2lMap;

	/// for zipf's distribution; a pool of zipf ///
	/// dist given theta and the number of elements ///

	double m_theta;
	ZipfSet m_zipfSet;
	int m_currentLevel;

	GraphNode m_currentElement = null;

	/// output ///
	PrintWriter m_fileOut = null;

	/// random number generator ///
	Random rand = new Random();
	Random rand2 = new Random();

	int m_qid = 0;

	public QueryGenerator_j(String inFN, double theta) {

		DigraphLoader loader = new DigraphLoader(inFN);
		loader.loadVE();
		m_graph = loader.getGraph();
		m_gNodes = m_graph.nodes;
		m_root = m_graph.root;
		i2lMap = loader.getI2LMap();
		m_theta = theta;
		m_zipfSet = new ZipfSet();
		for (GraphNode node : m_gNodes) {
			node.zipf = m_zipfSet.getZipf(m_theta, node.N_O_SZ);
		}
		m_root.zipf = m_zipfSet.getZipf(m_theta, m_root.N_O_SZ);
	}

	public void generateQueries(int numQ, int maxDepth, double dSlash, int noNestedPaths, double nestedPathProbs,
			int levelPathNesting, String outFilename) {
		System.out.println("Printing " + numQ + " queries to "+outFilename+" ...");
	

		try {
			QNode rootq = null;
			PrintWriter pw = new PrintWriter(new FileOutputStream(outFilename));

			for (int i = 0; i < numQ; i++) { // generate m_queries one by one
				m_qid = 0;
				Zipf z = m_root.zipf;
				int childIndex = z.probe();
				GraphNode startElement = m_gNodes[m_root.N_O.get(childIndex)];
				rootq = generateOneQuery(maxDepth, 0, dSlash, noNestedPaths, nestedPathProbs, levelPathNesting,
						startElement);
				if (rootq == null)
					i--; // it may fail
				else {
					System.out.println("No. " + i + ":");
					System.out.println(rootq);
					pw.println("No. " + i + ":");
					pw.println(rootq);
				}

			}
			  pw.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		}

	}

	public QNode generateOneQuery(int maxDepth, int currentLevel, double dSlash, int noNestedPaths,
			double nestedPathProbs, int levelPathNesting, GraphNode startElement) {

		double coin;
		int level = currentLevel; // can not be larger than depth
		GraphNode element = startElement;
		QNode rootq = null, qnode = null;

		Zipf z;

		boolean inDoubleSlashMode = false;
		int skipLevels = 0;

		ArrayList<GraphNode> elementList = new ArrayList<GraphNode>();
		ArrayList<QNode> stepList = new ArrayList<QNode>();
		int axis = 0;
		// generate the main branch
		while (level < maxDepth) {

			if (element.N_O_SZ > 0) {

				z = element.zipf;
				int childIndex1 = z.probe();
				// childIndex1 is for the main branch
				/// generate the operator, m_name test and predicates for the
				// current level ///
				if (inDoubleSlashMode && skipLevels > 0) {
					// skipping phase
					skipLevels--;

				} else { // inDoubleSlashmode == false or
					// it is true && skiplevel = 0

					if (inDoubleSlashMode == false && rand.nextDouble() < dSlash) {

						inDoubleSlashMode = true;
						coin = rand.nextDouble();
						skipLevels = (int) Math.floor(coin * (maxDepth - level));
					}

					if (skipLevels > 0) {
						skipLevels--;
						axis = 1;
					} else {

						qnode = new QNode();
						qnode.id = m_qid++;
						qnode.axis = axis;
						qnode.lb = i2lMap.get(element.lb);
						// System.out.println("generateOneQuery qid= " +
						// qnode.id);
						elementList.add(element);
						if (stepList.isEmpty())
							rootq = qnode;
						else
							stepList.get(stepList.size() - 1).addChild(qnode);
						stepList.add(qnode);
						axis = 0; // rest axis
						inDoubleSlashMode = false;

					} // end if skipLeves == 0

				} // end of else (inDoubleSlashMode && skipLevels > 0)

				m_currentLevel = level;
				m_currentElement = element;
				element = m_gNodes[element.N_O.get(childIndex1)];
				level++;
			} // end if element.getSizeOfChildren() > 0

			else {
				// this element doesn't have children
				/// choose "//" controlled by dslash and inDoubleSlashMode ///

				if (inDoubleSlashMode == false) {
					// coin = rand.nextDouble();
					// System.out.println(coin);
					if (rand.nextDouble() < dSlash) {
						axis = 1;
						inDoubleSlashMode = true;
					}
				}
				qnode = new QNode();
				qnode.id = m_qid++;
				// System.out.println("generateOneQuery qid= " + qnode.id);
				qnode.axis = axis;
				qnode.lb = i2lMap.get(element.lb);
				elementList.add(element);
				if (stepList.isEmpty())
					rootq = qnode;
				else
					stepList.get(stepList.size() - 1).addChild(qnode);
				stepList.add(qnode);
				m_currentLevel = level;
				m_currentElement = element;
				break;
			}
		}

		// generate side branches

		int size = elementList.size();
		int npLevel;
		int chooseSize = Math.min(size, maxDepth - 1);
		int maxTry = 50, tries = 0;
		QNode branch = null;
		int childIndex;
		GraphNode nextElement = null, nextElement2 = null;

		for (int i = 0; i < noNestedPaths && tries < maxTry; i++, tries++) {
			npLevel = (int) Math.floor(rand.nextDouble() * chooseSize);
			element = elementList.get(npLevel);
			if (npLevel < size - 1)
				nextElement = elementList.get(npLevel + 1);
			else
				nextElement = null;
			if (element.N_O_SZ > 1) {
				z = element.zipf;
				do {
					childIndex = z.probe();
					nextElement2 = m_gNodes[element.N_O.get(childIndex)];
				} while (nextElement != null && nextElement.id == nextElement2.id);

				branch = generateRelativePath(maxDepth, npLevel + 1, dSlash, nestedPathProbs, levelPathNesting - 1,
						nextElement2);

				if (branch != null) {

					qnode = stepList.get(npLevel);
					qnode.addChild(branch);
					// System.out.println(" qnode.id=" + qnode.id + "
					// branch.id=" + branch.id);
				} else {
					i--;

				}

			} else {
				i--;

			}

		}

		if (tries == maxTry)
			return null;

		return rootq;
	}

	public QNode generateRelativePath(int maxDepth, int currentLevel, double dSlash, double nestedPathProbs,
			int levelPathNesting, GraphNode startElement) {

		double coin;
		int level = currentLevel; // can not be larger than depth
		GraphNode element = startElement;
		QNode rootq = null, parq = null, qnode = null;
		// QNode qnode = new QNode();
		// qnode.id = m_qid++;
		// System.out.println("generateRelativePath qid= " + qnode.id);
		Zipf z;

		boolean inDoubleSlashMode = false;
		int skipLevels = 0;
		int axis = 0;
		// generate the main branch
		while (level < maxDepth) {

			if (element.N_O_SZ > 0) {

				z = element.zipf;
				int childIndex1 = z.probe();
				// childIndex1 is for the main branch
				int childIndex2; // childIndex2 is for a nested path in a
									// predicate

				if (inDoubleSlashMode && skipLevels > 0) {
					// skipping phase
					skipLevels--;

				} else { // inDoubleSlashmode == false or
					// it is true && skiplevel = 0

					if (inDoubleSlashMode == false && level > currentLevel && rand.nextDouble() < dSlash) {
						inDoubleSlashMode = true;

						coin = rand.nextDouble();
						skipLevels = (int) Math.floor(coin * (maxDepth - level));
					}

					if (skipLevels > 0) {
						skipLevels--;
						axis = 1;
					} else {
						inDoubleSlashMode = false;

						qnode = new QNode();
						qnode.id = m_qid++;
						// System.out.println("generateOneQuery qid= " +
						// qnode.id);
						qnode.axis = axis;
						qnode.lb = i2lMap.get(element.lb);
						axis = 0;
						if (rootq == null)
							rootq = qnode;
						if (parq != null)
							parq.addChild(qnode);
						parq = qnode;

						/// create a nested path ///
						coin = rand.nextDouble();
						if (levelPathNesting > 0 && coin < nestedPathProbs && level < maxDepth - 1
								&& element.N_O_SZ > 1) {
							// at least a nested path will be generated
							QNode branch = null;
							do {
								childIndex2 = z.probe();
							} while (childIndex2 == childIndex1);

							branch = generateRelativePath(maxDepth, level + 1, dSlash, nestedPathProbs,
									levelPathNesting - 1, m_gNodes[element.N_O.get(childIndex2)]);

							if (branch != null)
								qnode.addChild(branch);
							else
								m_qid--;
						}

					} // end if skipLeves == 0

				} // end of else (inDoubleSlashMode && skipLevels > 0)

				m_currentLevel = level;
				m_currentElement = element;
				element = m_gNodes[element.N_O.get(childIndex1)];
				level++;

			} // end if element.getSizeOfChildren() > 0

			else {
				// this element doesn't have children
				/// choose "//" controlled by dslash and inDoubleSlashMode ///

				if (inDoubleSlashMode == false && level > currentLevel) {
					// coin = rand.nextDouble();
					// System.out.println(coin);
					if (rand.nextDouble() < dSlash) {
						axis = 1;
						inDoubleSlashMode = true;
					}
				}

				// if (level > currentLevel)
				// qnode.axis = 1;
				qnode = new QNode();
				qnode.id = m_qid++;
				qnode.lb = i2lMap.get(element.lb);
				qnode.axis = axis;
				if (rootq == null)
					rootq = qnode;
				if (parq != null)
					parq.addChild(qnode);
				m_currentLevel = level;
				m_currentElement = element;
				break;
			}
		}

		return rootq;
	}

	public static void main(String[] args) {

		QueryGenerator_j qg = new QueryGenerator_j(".//data//citeseer_sub_lb10.lg", 0.8);
				//(".//data//citeseerx-lb1k.lg", 0.8);
				//(".\\data\\xm05e.dag", 0.8);
		//(".\\data\\agrocyc-lb10.lg", 0.8);

		int numQ = 5;
		int maxDepth = 5;
		double dSlash = 0.9;
		int noNestedPaths = 3;
		double nestedPathProb = 0.8;
		int levelPathNesting = maxDepth;
		String outFilename =".//data//citeseer_sub_rand_q.txt";
				//".\\data\\agrocyc_rand_q.txt";
				//".\\data\\xm05e_rand_q.txt";
		qg.generateQueries(numQ, maxDepth, dSlash, noNestedPaths, nestedPathProb, levelPathNesting,outFilename);

	}

}
