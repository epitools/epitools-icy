package plugins.davhelle.cellgraph.tracking;

/**
 * Distance method names to evaluate the candidate list
 */

public enum DistanceCriteria{
	MINIMAL_DISTANCE, 
	AVERAGE_DISTANCE, 
	AREA_OVERLAP,	
	//TODO Add weighted distance? decreasing in time, e.g. 1*(t-1) + 0.8*(t-2)....
	AREA_DIFF_WITH_MIN_DISTANCE, 
	WEIGHTED_MIN_DISTANCE, 
	OVERLAP_WITH_MIN_DISTANCE
}