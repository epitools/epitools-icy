/**
 * 
 */
package plugins.davhelle.cellgraph.tracking;

/**
 * @author Davide Heller
 *
 */
public enum TrackingFeedback {
	LOST_IN_PREVIOUS_FRAME(-2),
	LOST_IN_NEXT_FRAME(-3),
	LOST_IN_BOTH(-4),
	DIVIDING_IN_NEXT_FRAME(-5),
	BROTHER_CELL_NOT_FOUND(-6);
	
	public final int numeric_code;
	
	private TrackingFeedback(int numeric_code) {
		this.numeric_code = numeric_code;
	}
}
