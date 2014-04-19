/*=========================================================================
 *
 *  (C) Copyright (2012-2014) Basler Group, IMLS, UZH
 *  
 *  All rights reserved.
 *	
 *  author:	Davide Heller
 *  email:	davide.heller@imls.uzh.ch
 *  
 *=========================================================================*/

package plugins.davhelle.cellgraph.nodes;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.tracking.TrackingFeedback;

public class Elimination {

	/**
	 * Time point at which the cell was last seen
	 * and cell reference
	 */
	private int time_point;
	private Node cell;
	

	public Elimination(Node eliminated_cell){
		this.cell = eliminated_cell;
		
		FrameGraph frame_of_last_occurrence = cell.getBelongingFrame();
		this.time_point = frame_of_last_occurrence.getFrameNo();
		
		frame_of_last_occurrence.addElimination(this);	
		cell.setElimination(this);
		cell.setErrorTag(TrackingFeedback.ELIMINATED_IN_NEXT_FRAME.numeric_code);
		
		//add elimination to all the preceding cells in the 
		//tracked lineage
		Node ancestor = cell.getPrevious();
		while(ancestor != null){
			ancestor.setElimination(this);
			ancestor = ancestor.getPrevious();
		}
	}
	
	/**
	 * @return the last occurrence of the cell
	 */
	public Node getCell() {
		return cell;
	}
	
	/**
	 * @return the time_point
	 */
	public int getTimePoint() {
		return time_point;
	}
	
	public String toString(){
		return "\tElimination in frame "+time_point+
				": "+cell.getTrackID()+" [" +
				Math.round(cell.getCentroid().getX()) + 
				"," +
				Math.round(cell.getCentroid().getY()) + "]";
	}
}
