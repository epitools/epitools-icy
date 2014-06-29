/**
 * 
 */
package headless;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.misc.SmallCellRemover;
import plugins.davhelle.cellgraph.misc.T1Transition;
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
public class DetectT1Transitions {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//Input files
		File test_file = new File("/Users/davide/tmp/T1/test/test_t0000.tif");
		//File test_file = new File("/Users/davide/tmp/T1/test2/test2_t0000.tif");
		int no_of_test_files = 10;

		SpatioTemporalGraph stGraph = StGraphUtils.createDefaultGraph(test_file,
				no_of_test_files);

		HashMap<Node, PolygonalCellTile> cell_tiles = StGraphUtils.createPolygonalTiles(stGraph);

		//analysis functions
//		reportIncidenceOfMitoticPlane(stGraph, cell_tiles); //works only with "/Users/davide/tmp/T1/test/test_t0000.tif" !!
//		reportEdgeEvolution(stGraph);
		
		HashMap<Integer, T1Transition> transition_map = new HashMap<Integer, T1Transition>();
		
		//Detect T1 transitions
		//1 - start - Find cells that changed their neighborhood
		int current_time_point = 1;
		FrameGraph frame = stGraph.getFrame(current_time_point);
		for(Node n: frame.vertexSet()){
			if(n.hasPrevious()){
				Node p = n.getPrevious();
				if(frame.degreeOf(n) > p.getBelongingFrame().degreeOf(p)){
					//1 - stop - detected neighbor gain
					System.out.printf("%d gained a side\n",n.getTrackID());
					
					checkDivisionExplanation(cell_tiles,
							current_time_point, n);
					
					//If unresolved hypothesize a neighbor rearrangement 
					//   -> new object & forward/backward analysis
					//a. who is the new neighbor
					FrameGraph previous_frame = p.getBelongingFrame();
					Node gained_neighbor = null;
					for(Node neighbor: n.getNeighbors()){
						if(previous_frame.containsEdge(p, neighbor.getPrevious()))
							System.out.printf("Edge %s was maintained\n",PolygonalCellTile.getCellPairKey(p, neighbor));
						else{
							//TODO test reversibility of edge source/target in undirected Graph
							System.out.printf("Edge %s is new\n",PolygonalCellTile.getCellPairKey(p, neighbor));
							gained_neighbor = neighbor;
						}
					}
					
					int[] winner_tuple = {n.getTrackID(),gained_neighbor.getTrackID()};
					Arrays.sort(winner_tuple);
					
					if(winner_tuple[0] == -1)
						continue;
					
					int transition_key = Arrays.hashCode(winner_tuple);
					
					if(transition_map.containsKey(transition_key)){
						//Transition is already being recorded -> add smth if necessary
						System.out.printf("Transition %s already detected\n",transition_map.get(transition_key).toString());
						continue;
					}
					
					
					T1Transition transition = new T1Transition(frame);
					transition.addWinner(n);
					transition.addWinner(gained_neighbor);
					
					//substitute with cell_tiles edge geometry
					transition.setGained_edge(frame.getEdge(n, gained_neighbor));
					
					transition_map.put(transition_key, transition);
					
					//Find losers
					Geometry gained_side = cell_tiles.get(n).getTileEdge(gained_neighbor);
					for(Node neighbor: n.getNeighbors())
						if(neighbor != gained_neighbor)
							if(gained_side.intersects(neighbor.getGeometry()))
								transition.addLooser(neighbor);
					
					//seek into the future
					assert transition.sanity_check(): "Something is wrong with the detected transition";
					
					Node next = n;
					while(next.hasNext()){
						FrameGraph current_frame = next.getBelongingFrame();
						if(transition.isActive(current_frame))
							System.out.printf("Transition %s exists in frame %d\n",transition.toString(),current_frame.getFrameNo());
						else
							System.out.printf("Transition %s does NOT exist in frame %d\n",transition.toString(),current_frame.getFrameNo());
						next = next.getNext();
					}
					
					//TODO: Can you answer the binary question if each edge is present or 
					//not for all frames? 
					//=> How many stable edges are there?
					//=> What is the minimal length for a stable edge?
					//=> What to include in the T1Transition object?
					//=> Tentative visualization? (using the geometry included in the object..)
					// 	 => a bit like for the correction helper (overlay stuff from the previous frame)
					
				}
			}
		}
		
		//Cluster transitions according to their length
		
	}

	private static void checkDivisionExplanation(
			HashMap<Node, PolygonalCellTile> cell_tiles,
			int current_time_point, Node n) {
		
		for(Node neighbor: n.getNeighbors()){
			//2 - start - Check for close by Division that might explain the change
			if(neighbor.hasObservedDivision()){
				Division d = neighbor.getDivision();
				if(d.getTimePoint() == current_time_point){
					//Does the mitotic plane touch n
					Node child1 = d.getChild1();
					Node child2 = d.getChild2();

					PolygonalCellTile daughter_cell_tile = cell_tiles.get(child1);
					Geometry mitotic_plane = daughter_cell_tile.getTileEdge(child2);

					if(n.getGeometry().intersects(mitotic_plane)){
						//2 - stop - the neighbor gain can be explained through the division
						System.out.printf("%d gained a side due to %d dividing into %d and %d\n",
								n.getTrackID(),
								d.getMother(),
								d.getChild1(),
								d.getChild2());
					}
				}
			}
		}
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
