package headless;

import java.io.File;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.PolygonalCellTileGenerator;
import plugins.davhelle.cellgraph.nodes.Edge;

/**
 * Headless method to identify the length of all edges in the
 * first frame of the input Graph.
 * 
 * @author Davide Heller
 *
 */
public class FindEdgeLength {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String export_folder = String.format("/Users/davide/data/marcm/%d/",0);
		String input_name = "skeletons_wkt/skeleton_000.wkt";
		File input_file = new File(export_folder +input_name);
		
		//Graph generation
		SpatioTemporalGraph stGraph = LoadNeoWtkFiles.loadWktStGraph(1,
				export_folder, input_file);
		
		PolygonalCellTileGenerator.createPolygonalTiles(stGraph);
		
		FrameGraph frame = stGraph.getFrame(0);
		for(Edge e: frame.edgeSet())
			System.out.printf("%.2f\n", frame.getEdgeWeight(e));

	}

}
