/**
 * 
 */
package headless;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.misc.SmallCellRemover;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.painters.PolygonConverterPainter;
import plugins.davhelle.cellgraph.tracking.HungarianTracking;
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
		
		//Input files
		File test_file = new File("/Users/davide/tmp/T1/test/test_t0000.tif");
		//File test_file = new File("/Users/davide/tmp/T1/test2/test2_t0000.tif");
		int no_of_test_files = 40;

		SpatioTemporalGraph stGraph = createSpatioTemporalGraph(test_file,
				no_of_test_files);

		HashMap<Node, PolygonalCellTile> cell_tiles = createPolygonalTiles(
				no_of_test_files, stGraph);

		//Follow division
		reportIncidenceOfMitoticPlane(stGraph, cell_tiles);

		//reportEdgeEvolution(stGraph);
	}

	private static void reportIncidenceOfMitoticPlane(
			SpatioTemporalGraph stGraph,
			HashMap<Node, PolygonalCellTile> cell_tiles) {
		Division division_node_11 = null;
		for(Node n: stGraph.getFrame(0).vertexSet()){
			if(n.hasObservedDivision())
				System.out.printf("Found dividing cell: %d (%.0f,%.0f) in frame %d\n",
						n.getTrackID(),
						n.getCentroid().getX(),
						n.getCentroid().getY(),
						n.getDivision().getTimePoint());
				
			if(n.getTrackID() == 11){
				assert n.hasObservedDivision(): "Wrong cell specified";
				division_node_11 = n.getDivision();
				assert division_node_11.getTimePoint() == 26: String.format("Wrong time point: %d",division_node_11.getTimePoint());
			}
		}


		Node child1 = division_node_11.getChild1();
		Node child2 = division_node_11.getChild2();

		PolygonalCellTile daughter_cell_tile = cell_tiles.get(child1);
		Geometry mitotic_plane = daughter_cell_tile.getTileEdge(child2);

		//on which cells does the mitotic plane end?
		for(Node neighbor: child1.getNeighbors()){
			Geometry neighboring_geometry = neighbor.getGeometry();
			if(mitotic_plane.intersects(neighboring_geometry) && neighbor != child2	){
				System.out.printf("Found intersection to neighbor: %d (%.0f,%.0f)\n",
						neighbor.getTrackID(),neighbor.getCentroid().getX(),neighbor.getCentroid().getY());
			}
		}
	}

	private static HashMap<Node, PolygonalCellTile> createPolygonalTiles(
			int no_of_test_files, SpatioTemporalGraph stGraph) {
		System.out.println("Identifying the tiles..");
		HashMap<Node,PolygonalCellTile> cell_tiles = new HashMap<Node, PolygonalCellTile>();
		for(int i=0; i < no_of_test_files; i++){
			FrameGraph frame = stGraph.getFrame(i);
			for(Node n: frame.vertexSet()){
				PolygonalCellTile tile = new PolygonalCellTile(n);
				cell_tiles.put(n, tile);
			}
		}
		return cell_tiles;
	}

	private static SpatioTemporalGraph createSpatioTemporalGraph(
			File test_file, int no_of_test_files) {
		System.out.println("Creating graph..");
		SpatioTemporalGraph stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						test_file, 
						no_of_test_files).getStGraph();

		assert stGraph.size() == no_of_test_files: "wrong frame no";

		System.out.println("Identifying the border..");
		new BorderCells(stGraph).markOnly();
		
		System.out.println("Removing small cells..");
		new SmallCellRemover(stGraph).removeCellsBelow(10.0);

		System.out.println("Tracking cells..");
		new HungarianTracking(stGraph, 5, 5.0,1.0).track();
		return stGraph;
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
					System.out.printf("Edge evolution for %s (%.0f,%.0f):" +
							"\n\tframe %d:\t%.2f\n",
							PolygonalCellTile.getCellPairKey(n, neighbor),
							n.getCentroid().getX(),n.getCentroid().getY(),
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
