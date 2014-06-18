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
		  //File test_file = new File("/Users/davide/tmp/T1/test/test_t0000.tif");
		  File test_file = new File("/Users/davide/tmp/T1/test2/test2_t0000.tif");
		  int no_of_test_files = 3;
		  
		  SpatioTemporalGraph stGraph = 
				  new SpatioTemporalGraphGenerator(
						  GraphType.TISSUE_EVOLUTION,
						  test_file, 
						  no_of_test_files).getStGraph();
		  
		 assert stGraph.size() == no_of_test_files: "wrong frame no";
		 
		 TrackingAlgorithm tracking_method = new NearestNeighborTracking(stGraph, 1, 5.0,1.0);
		 tracking_method.track();
		 
		 for(int i=0; i < no_of_test_files; i++){
			 System.out.printf("Anlalyzing frame %d\n", i);
			 FrameGraph frame = stGraph.getFrame(i);
			for(Node n: frame.vertexSet()){
				 System.out.printf("Cell %d - ",n.getTrackID());
				 //new constructor which populates the frame with the correct weights too
				 //test whether cellGraph still works after change of Graph Type
				 PolygonalCellTile tile = new PolygonalCellTile(n,frame);
				 System.out.printf("\tFound %d intersection/s\n",tile.getTileIntersectionNo());
				 
			}
		 }
		 
		 FrameGraph first_frame = stGraph.getFrame(0);
		 HashSet<Node> tested_nodes = new HashSet<Node>();
		 for(Node n: first_frame.vertexSet()){
			 for(Node neighbor: n.getNeighbors()){
				 if(!tested_nodes.contains(neighbor)){
					 System.out.printf("Starting with edge between %s:",
							 PolygonalCellTile.getCellPairKey(n, neighbor));
					 
				 }
			 }
			 tested_nodes.add(n);
		 }
					 
					 
					 
	}

}
