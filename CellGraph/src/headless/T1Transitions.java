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
		 
		 for(Node n: stGraph.getFrame(0).vertexSet())
			 new PolygonalCellTile(n);
	}

}
