package randomGraphGenerator;

import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.util.SupplierUtil;

public class MyRandomGraph {

	public void generate(){
		
		 
		
	        //generateGraphs(graphArray, 11, 50);  
	        //generateGnmRandomGraph(5, 10);
	        generateGnpRandomGraph(6, 0.1);
	}
	
	
	
    private  void generateGnmRandomGraph(int numOfVertex, int numOfEdges){
    	
    	  GraphGenerator<Integer, DefaultEdge, Integer> randomGen = new GnmRandomGraphGenerator<>(numOfVertex, numOfEdges);
    	
    	  //Graph<Integer, DefaultEdge> g = new SimpleDirectedGraph<>(DefaultEdge.class);
    	 // Graph<Integer, DefaultEdge> g = new DirectedAcyclicGraph<>(DefaultEdge.class);
      	
    	  
    	 // randomGen.generateGraph(g,new IntegerVertexFactory(), null);
    	  
    	  
    	  // Graph<Integer, DefaultEdge> g = new SimpleDirectedGraph<>(
          //         SupplierUtil.createIntegerSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER, false);
    	   Graph<Integer, DefaultEdge> g = new DirectedAcyclicGraph<>(
                   SupplierUtil.createIntegerSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER, false);
    	   
    	   randomGen.generateGraph(g);
    	  
    	  
    	 Set<Integer> vertexes = g.vertexSet();
    	 
    	 for(int v:vertexes){
    		 
    		 System.out.println("v" + " " + v);
    	 }
    	 
    	 
    	Set<DefaultEdge> edges = g.edgeSet();
    	for(DefaultEdge e:edges){
    		
    		System.out.println("e" + " " + g.getEdgeSource(e) + " " + g.getEdgeTarget(e));
    	}
    	
    }
    
    private  void generateGnpRandomGraph(int numOfVertex, double prob){
    	
    	
       GraphGenerator<Integer, DefaultEdge, Integer> randomGen =
    	            new GnpRandomGraphGenerator<>(numOfVertex, prob);	
       // cannot guarantee to be dags
  	   Graph<Integer, DefaultEdge> g = new SimpleDirectedGraph<>(
                 SupplierUtil.createIntegerSupplier(), SupplierUtil.DEFAULT_EDGE_SUPPLIER, false);
  	   
  	   randomGen.generateGraph(g);
  	  
  	  
  	 Set<Integer> vertexes = g.vertexSet();
  	 
  	 for(int v:vertexes){
  		 
  		 System.out.println("v" + " " + v);
  	 }
  	 
  	 
  	Set<DefaultEdge> edges = g.edgeSet();
  	for(DefaultEdge e:edges){
  		
  		System.out.println("e" + " " + g.getEdgeSource(e) + " " + g.getEdgeTarget(e));
  	}
  	
  }

	
	public static void main(String[] args) {
	
		MyRandomGraph gragen = new MyRandomGraph();
		gragen.generate();
		

	}

}
