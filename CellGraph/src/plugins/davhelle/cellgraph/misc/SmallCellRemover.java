/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.misc;

import java.util.ArrayList;
import java.util.Iterator;

import org.jgrapht.graph.DefaultEdge;

import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Component to eliminate all cells that are below a user 
 * defined area threshold			 
 * 
 * @author Davide Heller
 *
 */
public class SmallCellRemover {
	
	final boolean ABSORB_SMALL_CELLS = true;

	private SpatioTemporalGraph stGraph;
	
	public SmallCellRemover(SpatioTemporalGraph stGraph){
		this.stGraph = stGraph;
	}
	
	/**
	 * Method to remove all cells below a given area threshold.
	 * All time points of the spatiotemporal graph are considered.
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

	public int removeCellsOnFrame(int time_point, double threshold) {
		
		ArrayList<Node> small_cell_list = new ArrayList<Node>();
		
		for(Node cell:stGraph.getFrame(time_point).vertexSet())
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
						
						//System.out.println("\t joins with "+neighbor.getCentroid().toText());
						
						//use the getLength method to find the largest intersection
						//area not suited as the intersection is most likely a linear segment
						//TODO check actual object type
						
						//double intersection_area = neighbor_intersection.getArea();
						double intersection_length = neighbor_intersection.getLength();
						
						//System.out.println("\t\t area:"+intersection_area);
						//System.out.println("\t\t length:"+intersection_length);
						
						if( intersection_length > max_length){
							max_length = intersection_length;
							max_length_candidate = neighbor;
						}
					}

					if(max_length_candidate != null){
						
						//System.out.println("\t\t Winner: "+max_length_candidate.getCentroid().toText());
						
						//neighbor gains all neighbors of to_small_cell not yet connected and so do those
						for(Node neighbor: cell.getNeighbors())
							if(neighbor != max_length_candidate && !max_length_candidate.getNeighbors().contains(neighbor)){
								//System.out.println("\t\t\t Adding "+neighbor.getCentroid().toText()+" as neighbor");
								stGraph.getFrame(time_point).addEdge(max_length_candidate, neighbor);
							}


						//max_area_candidate obtains union with to_small_cell as new area
						max_length_candidate.setGeometry(max_length_candidate.getGeometry().union(small_cell_geom));

					}
				}
			
			}
		
		
		
		//TODO update Graph in terms of neighborhoods?
		//TODO fuse lost cell with containing/nearby cell? => need criteria for best fusing candidate
		
		stGraph.getFrame(time_point).removeAllVertices(small_cell_list);
		
		return small_cell_list.size();
	}
	
	
}
