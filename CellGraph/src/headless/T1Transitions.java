/**
 * 
 */
package headless;

import java.io.File;

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
			 for(Node n: stGraph.getFrame(i).vertexSet()){
				 System.out.printf("Cell %d - ",n.getTrackID());
				 PolygonalCellTile tile = new PolygonalCellTile(n);
				 System.out.printf("\tFound %d intersection/s\n",tile.getTileIntersectionNo());
			 }
		 }
	}

}
