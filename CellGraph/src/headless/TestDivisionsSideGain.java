/**
 * 
 */
package headless;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Here we test whether a cell that will divide obtains an extra side before
 * dividing. Furthermore we want to identify whether the gain is due to a
 * nearby division event (and incident mitotic plane) or due to a T1 transition.
 * 
 * @author Davide Heller
 *
 */
public class TestDivisionsSideGain {

	public static void main(String[] args) {
		
		SpatioTemporalGraph stGraph = LoadNeoWtkFiles.loadNeo(0);
		
		FrameGraph frame0 = stGraph.getFrame(0);
		
		HashMap<Node, ArrayList<Node>> dividing_node_ddns = new HashMap<Node, ArrayList<Node>>();
		int division_no = 0;
		int gain_of_side = 0;
		int two_gaining_cells = 0;
		for(Node cell: frame0.vertexSet()){
			
			if(cell.onBoundary())
				continue;
			
			if(cell.hasObservedDivision()){
				
				Division division = cell.getDivision();
				int division_frame_no = division.getTimePoint();
				division_no++;
				
				for(Node n: cell.getNeighbors()){
					
					if(n.hasObservedDivision()){
						
						//we are looking at a direct dividing neighbor
						
						if(division_frame_no > n.getDivision().getTimePoint()){
							
							if(!dividing_node_ddns.containsKey(cell))
								dividing_node_ddns.put(cell, new ArrayList<Node>());
							
							dividing_node_ddns.get(cell).add(n);
							
						}
					}
				}
				
				int time_point_minus_5 = division_frame_no - 5;
				if(time_point_minus_5 > 0)
				{
					FrameGraph frame_minus5 = stGraph.getFrame(time_point_minus_5);
					Node future_cell = frame_minus5.getNode(cell.getTrackID());

					if(future_cell != null){
						int poly_class_difference = frame_minus5.degreeOf(future_cell) - frame0.degreeOf(cell);
						if(poly_class_difference > 0)
							gain_of_side++;
						
						//System.out.printf("%d\t%d\n",cell.getTrackID(),poly_class_difference);
					}
				}
				
				//test to what kind of cell the mitotic plane is adding a neighbor.
				HashSet<Node> s1 = new HashSet<Node>(division.getChild1().getNeighbors());
				HashSet<Node> s2 = new HashSet<Node>(division.getChild2().getNeighbors());
				
				//intersect
				s1.retainAll(s2);
				int intersecting_neighbor_no = s1.size();
				
				if(intersecting_neighbor_no > 0){
					two_gaining_cells++;
					System.out.printf("%d\t%d\n",cell.getTrackID(),intersecting_neighbor_no);
				}
				
			}
		}
		
		System.out.printf("Found %d/%d divisions with ddns\n",dividing_node_ddns.size(),division_no);
		System.out.printf("Found %d/%d divisions two gaining neighbors\n",two_gaining_cells,division_no);

		
		 int gain_of_side_with_ddn = 0;
		 cell_loop:
		 for(Node cell: dividing_node_ddns.keySet())
		 {
			 for(Node ddn: dividing_node_ddns.get(cell))
			 {
				 int difference = cell.getDivision().getTimePoint() - ddn.getDivision().getTimePoint();
				 if(difference > 5){
					Division division = ddn.getDivision();
				 	int division_time_point = division.getTimePoint();
				 	FrameGraph frame_of_division = stGraph.getFrame(division_time_point);
				 	FrameGraph frame_before_division = stGraph.getFrame(division_time_point - 1);
				 	
				 	Node future_cell = frame_before_division.getNode(cell.getTrackID());
				 	
				 	if(frame_before_division.containsEdge(division.getMother(), future_cell))
				 	{
				 		future_cell = frame_of_division.getNode(cell.getTrackID());
				 		if(frame_of_division.containsEdge(future_cell, division.getChild1()) &&
				 				frame_of_division.containsEdge(future_cell,division.getChild2()))
				 		{
				 			gain_of_side_with_ddn++;
				 			//continue cell_loop;
				 		}
				 				
				 	}
				 }	 
			 }
		 }
		 
		 System.out.printf("Found %d/%d side gains with ddns\n",gain_of_side_with_ddn,gain_of_side);
		 //percentage of cells that have a side gain due to a nearby cell division
	}

}
