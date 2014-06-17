/**
 * 
 */
package headless;

import java.io.File;

import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.painters.PolygonConverterPainter;

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
		  File test_file = new File("/Users/davide/tmp/T1/test2/test2_t0000.tif");
		  int no_of_test_files = 3;
		  
		  SpatioTemporalGraph stGraph = 
				  new SpatioTemporalGraphGenerator(
						  GraphType.TISSUE_EVOLUTION,
						  test_file, 
						  no_of_test_files).getStGraph();
		  
		 assert stGraph.size() == no_of_test_files: "wrong frame no";
		 
		 PolygonConverterPainter polygonal_tile = new PolygonConverterPainter(stGraph);

		 int tile_number = polygonal_tile.getTileNumber();
		 
		 assert tile_number == 2: String.format("Tile numbers not correct: %d",tile_number);
		 
		 System.out.println(String.format("Successfully identified %d tiles",tile_number));

	}

}
