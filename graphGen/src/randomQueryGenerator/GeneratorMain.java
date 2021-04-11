package randomQueryGenerator;

public class GeneratorMain {

	QueryGenerator qryGen;

	public GeneratorMain(String inFN) {

		qryGen = new QueryGenerator(inFN, 0.8);
	}

	public void runOneSetting(String outFN) {

		int numQ = 1000;
		double nestedPathProb = 0.6;

		int maxDepth = 10;
		double dSlash = 0;
		int noNestedPaths = 3;
		int levelPathNesting = maxDepth;

		qryGen.generateQueries(numQ, maxDepth, dSlash, noNestedPaths, nestedPathProb, levelPathNesting, outFN);

		QueryMinimizor qmini = new QueryMinimizor();
		qmini.minimize(outFN);
	}

	public void run(String outFN) {

		int numQ = 20;
		int[] maxDepthList = 
			//{ 6, 8, 10, 12, 14, 16 }; // for citeseerx
		{6,7,8,9,10}; // for acm
		double[] dSlashList =
			//{0}; // for pc only
		//	{ 1.0 };
		// {0.5, 0.6, 0.8, 1.0};
		//	 {0.7, 0.8, 0.9, 1.0};
		 {0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
		int[] noNestedPathsList = { 1, 2, 3 };
		double nestedPathProb = 0.6;

		int maxDepth = 5;
		double dSlash = 0.6;
		int noNestedPaths = 3;
		int levelPathNesting = maxDepth;

		for (int md = 0; md < maxDepthList.length; md++) {
			maxDepth = maxDepthList[md];
			levelPathNesting = maxDepth;
			for (int ds = 0; ds < dSlashList.length; ds++) {
				dSlash = dSlashList[ds];

				for (int np = 0; np < noNestedPathsList.length; np++) {
					noNestedPaths = noNestedPathsList[np];

					qryGen.generateQueries(numQ, maxDepth, dSlash, noNestedPaths, nestedPathProb, levelPathNesting,
							outFN);
				}
			}

		}

		QueryMinimizor qmini = new QueryMinimizor();
		qmini.minimize(outFN);
	}

	public static void main(String[] args) {

		GeneratorMain gmain = new GeneratorMain
				//(".//data//randv3a2t5k.dag");
		//(".//data//randv5a2t5k.dag");
		//(".//data//randv7a2t5k.dag");
		//(".//data//randv10a2t5k.dag");
				//(".//data//randv15a2t5k.dag");
		//(".//data//randv15a2t3k.dag");
		//(".//data//randv3a2t1k.dag");
		// (".//data//cite_lb7k.lg");
		// (".//data//cite_lb5k.lg");
		// (".//data//cite_lb3k.lg");
		// (".//data//cite_lb8k.lg");
		// (".//data//cite_lb6k.lg");
		// (".//data//cite_lb4k.lg");
		// (".//data//cite_lb2k.lg");		
		// (".//data//cite_lb10k.lg");
		// (".//data//cite_lb1k.lg");
		// (".//data//uniprot22m_lb100.lg");
		// (".//data//uniprot22m_lb1k.lg");
		// (".//data//outputacm.dag");
		 //  (".//data//dblpcitation.dag");	
		(".//data//citation-network2.dag");	
		gmain.run
		//(".//data//randv3a2t5k.qry");
		//(".//data//randv5a2t5k.qry");
		//(".//data//randv7a2t5k.qry");
		//(".//data//randv10a2t5k.qry");
		//(".//data//randv15a2t5k.qry");
		 //(".//data//randv15a2t5k.qry");
		//(".//data//cite_lb7k.qry");
		// (".//data//cite_lb5k.qry");
		// (".//data//cite_lb3k.qry");
		// (".//data//cite_lb10k.qry");
		//(".//data//cite_lb8k.qry");
		// (".//data//cite_lb6k.qry");
		// (".//data//cite_lb4k.qry");
		// (".//data//cite_lb2k.qry");
		// (".//data//cite_lb1k.qry");
		// (".//data//uniprot22m_lb1k.qry");
		// (".//data//acm.qry");
		// (".//data//acmpc.qry");
		 //  (".//data//dblpMoreD.qry");	
		 (".//data//cite_nw2.qry");	
		
		//gmain.runOneSetting
		//(".//data//randv15a2t5k.qry");
		//(".//data//randv15a2t3k.qry");
		//(".//data//randv3a2t1k.qry");
		//(".//data//acmpc.qry");
	}

}
