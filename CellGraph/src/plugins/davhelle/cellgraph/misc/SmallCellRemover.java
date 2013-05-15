/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.misc;

import java.util.ArrayList;
import java.util.Iterator;

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
		
		int count = 0;
		
		for(int time_point=0; time_point < stGraph.size(); time_point++){
			ArrayList<Node> small_cell_list = new ArrayList<Node>();
			
			for(Node cell:stGraph.getFrame(time_point).vertexSet())
				if(cell.getGeometry().getArea() < threshold)
					small_cell_list.add(cell);
			
			//TODO update Graph in terms of neighborhoods?
			//TODO fuse lost cell with containing/nearby cell? => need criteria for best fusing candidate
			
			stGraph.getFrame(time_point).removeAllVertices(small_cell_list);
			
			count += small_cell_list.size();
		}
		
		return count;
		
	}
	
	
}
