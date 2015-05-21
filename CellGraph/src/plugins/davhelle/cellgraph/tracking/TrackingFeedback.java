package plugins.davhelle.cellgraph.tracking;

/**
 * Enumeration of Tracking Feedback Types which have corresponding 
 * integer values. Used in the tracking Algorithms for assigning messages
 * to cells when the relative situation is encountered.
 * 
 * @author Davide Heller
 *
 */
public enum TrackingFeedback {
	DEFAULT(-1),
	LOST_IN_PREVIOUS_FRAME(-2),
	LOST_IN_NEXT_FRAME(-3),
	LOST_IN_BOTH(-4),
	DIVIDING_IN_NEXT_FRAME(-5),
	BROTHER_CELL_NOT_FOUND(-6),
	ELIMINATED_IN_NEXT_FRAME(-7),
	BROTHER_CELL_ELIMINATED(-8),
	FALSE_POSITIVE(-9),
	FALSE_NEGATIVE(-10);
	
	public final int numeric_code;
	
	private TrackingFeedback(int numeric_code) {
		this.numeric_code = numeric_code;
	}
}
