/**
 * 
 */
package headless;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.painters.PolygonConverterPainter;
import plugins.davhelle.cellgraph.tracking.NearestNeighborTracking;
import plugins.davhelle.cellgraph.tracking.TrackingAlgorithm;

/**
 * 
 * This class starts CellGraph in headless Mode and
 * aims to analyze the T1 Transitions
 * @author Davide Heller
 *
 */
public class T1Transitions {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		  File test_file = new File("/Users/davide/tmp/T1/test/test_t0000.tif");
		  //File test_file = new File("/Users/davide/tmp/T1/test2/test2_t0000.tif");
		  int no_of_test_files = 40;
		  
		  SpatioTemporalGraph stGraph = 
				  new SpatioTemporalGraphGenerator(
						  GraphType.TISSUE_EVOLUTION,
						  test_file, 
						  no_of_test_files).getStGraph();
		  
		 assert stGraph.size() == no_of_test_files: "wrong frame no";
		 
		 TrackingAlgorithm tracking_method = 
				 new NearestNeighborTracking(stGraph, 5, 5.0,1.0);
		 tracking_method.track();
		 
		 for(int i=0; i < no_of_test_files; i++){
			 System.out.printf("Anlalyzing frame %d\n", i);
			 FrameGraph frame = stGraph.getFrame(i);
			for(Node n: frame.vertexSet()){
				 PolygonalCellTile tile = new PolygonalCellTile(n);
			}
		 }
		 
		 reportEdgeEvolution(stGraph);
	}

	/**
	 * Follow every edge for every available time point and
	 * report it's lengths
	 * 
	 * @param stGraph
	 */
	private static void reportEdgeEvolution(SpatioTemporalGraph stGraph) {
		FrameGraph first_frame = stGraph.getFrame(0);
		 HashSet<Node> tested_nodes = new HashSet<Node>();
		 for(Node n: first_frame.vertexSet()){
			 for(Node neighbor: n.getNeighbors()){
				 if(!tested_nodes.contains(neighbor)){
					 System.out.printf("Edge evolution for %s:\n\tframe %d:\t%.2f\n",
							 PolygonalCellTile.getCellPairKey(n, neighbor),
							 first_frame.getFrameNo(),
							 first_frame.getEdgeWeight(
									 first_frame.getEdge(n, neighbor)));
					 
					 //Follow edge if it is projected in the future
					 Node next = n;
					 while(next.hasNext()){
						 next = next.getNext();
						 FrameGraph current_frame = next.getBelongingFrame();
						 
						 //Search if the same neighborhood connection exists
						 for(Node next_neighbor: next.getNeighbors()){
							 if(next_neighbor.getFirst() == neighbor)
								 System.out.printf("\tframe %d:\t%.2f\n",
										 current_frame.getFrameNo(),
										 current_frame.getEdgeWeight(
												 current_frame.getEdge(next, next_neighbor)));
						 }
					 }
				 }
			 }
			 tested_nodes.add(n);
		 }
	}
}
