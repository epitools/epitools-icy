/*=========================================================================
 *
 *  Copyright 2013 Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/

package plugins.davhelle.cellgraph.nodes;

import plugins.davhelle.cellgraph.graphs.FrameGraph;

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
