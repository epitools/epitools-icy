/**
 * 
 */
package headless;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.PolygonalCellTileGenerator;
import plugins.davhelle.cellgraph.nodes.Edge;

/**
 * @author Davide Heller
 *
 */
public class FindEdgeLength {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SpatioTemporalGraph stGraph = LoadNeoWtkFiles.loadStGraph(2, 1);
		
		PolygonalCellTileGenerator.createPolygonalTiles(stGraph);
		
		FrameGraph frame = stGraph.getFrame(0);
		for(Edge e: frame.edgeSet())
			System.out.printf("%.2f\n", frame.getEdgeWeight(e));

	}

}
