package plugins.davhelle.cellgraph.nodes;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.tracking.TrackingFeedback;

/**
 * Class representing a Elimination event.<br>
 * 
 * In a practical scenario this describes a cell which is excluded
 * from a tissue and will therefore be absent from all future frames.
 * 
 * @author Davide Heller
 *
 */
public class Elimination {

	/**
	 * Time point at which the cell was last seen
	 */
	private int time_point;
	/**
	 * Cell that is eliminated (reference at the last time point)
	 */
	private Node cell;

	/**
	 * Describes the elimination event
	 * 
	 * @param eliminated_cell last reference to the cell that is eliminated
	 */
	public Elimination(Node eliminated_cell){
		this.cell = eliminated_cell;
		
		FrameGraph frame_of_last_occurrence = cell.getBelongingFrame();
		this.time_point = frame_of_last_occurrence.getFrameNo();
		
		frame_of_last_occurrence.addElimination(this);	
		cell.setElimination(this);
		cell.setErrorTag(TrackingFeedback.ELIMINATED_IN_NEXT_FRAME.numeric_code);
		
		//add elimination to all the preceding cells if tracking is available
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
	 * @return the time_point in which the cell is last observed
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
