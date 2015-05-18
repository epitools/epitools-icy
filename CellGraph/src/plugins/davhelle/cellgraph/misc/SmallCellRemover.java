package plugins.davhelle.cellgraph.misc;

import java.util.ArrayList;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Component to eliminate all cells that are below a user 
 * defined area threshold			 
 * 
 * @author Davide Heller
 *
 */
public class SmallCellRemover {
	
	/**
	 * Flag for absorb action. The eliminated cells area is allocated to the biggest intersecting neighbor
	 */
	final boolean ABSORB_SMALL_CELLS = true;

	private SpatioTemporalGraph stGraph;
	
	public SmallCellRemover(SpatioTemporalGraph stGraph){
		this.stGraph = stGraph;
	}
	
	/**
	 * Method to remove all cells below a given area threshold.
	 * All time points of the spatio-temporal graph are considered.
	 * 
	 * @param threshold the area threshold
	 * @return number of removed cells
	 */
	public int removeCellsBelow(double threshold){
		
		int count_of_removed_cells = 0;
		
		for(int time_point=0; time_point < stGraph.size(); time_point++){
			int no_of_removed_cells_in_frame_i = removeCellsOnFrame(time_point, threshold);
			
			count_of_removed_cells += no_of_removed_cells_in_frame_i;
		}
		
		return count_of_removed_cells;
		
	}

	/**
	 * Removes all cells below a given threshold in the frame corresponding to the 
	 * temporal index given as input
	 * 
	 * @param time_point number of the frame to be processed
	 * @param threshold area threshold below which cells are eliminated.
	 * @return
	 */
	public int removeCellsOnFrame(int time_point, double threshold) {
		
		ArrayList<Node> small_cell_list = new ArrayList<Node>();
		
		FrameGraph frame = stGraph.getFrame(time_point);
		for(Node cell:frame.vertexSet())
			if(cell.getGeometry().getArea() < threshold){
				small_cell_list.add(cell);
				
				/*
				 * The following section enables the 
				 * SmallCellRemover to reassign the 
				 * space of the too small cell to 
				 * the cell which the longest intersection
				 */
				if(ABSORB_SMALL_CELLS){
					
					//System.out.println(cell.getCentroid().toText());

					//find out the longest intersection with neighboring cells
					double max_length = Double.MIN_VALUE;
					Node max_length_candidate = null;
					Geometry small_cell_geom = cell.getGeometry();
					for(Node neighbor: cell.getNeighbors()){
						Geometry neighbor_intersection = small_cell_geom.intersection(neighbor.getGeometry());
						
						//use the getLength method to find the largest intersection
						double intersection_length = neighbor_intersection.getLength();
						
						if( intersection_length > max_length){
							max_length = intersection_length;
							max_length_candidate = neighbor;
						}
					}

					if(max_length_candidate != null){
						
						//neighbor gains all neighbors of to_small_cell not yet connected and so do those
						for(Node neighbor: cell.getNeighbors())
							if(neighbor != max_length_candidate && !max_length_candidate.getNeighbors().contains(neighbor)){
								Edge newEdge = frame.addEdge(max_length_candidate, neighbor);
								newEdge.setFrame(frame);
							}

						//max_area_candidate obtains union with to_small_cell as new area
						max_length_candidate.setGeometry(max_length_candidate.getGeometry().union(small_cell_geom));

					}
				}
			
			}
		
		frame.removeAllVertices(small_cell_list);
		
		return small_cell_list.size();
	}
	
	
}
