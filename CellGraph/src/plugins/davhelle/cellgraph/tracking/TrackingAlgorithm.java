package plugins.davhelle.cellgraph.tracking;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Abstract tracking algorithm to establish the connectivity 
 * between different frames of the movie being analyzed.
 * 
 * @author Davide Heller
 *
 */
public abstract class TrackingAlgorithm {

	protected SpatioTemporalGraph stGraph;
	
	/**
	 * Constructor methods should always initialize the spatio temporal graph
	 * field.
	 * 
	 * @param spatioTemporalGraph
	 */
	public TrackingAlgorithm(SpatioTemporalGraph spatioTemporalGraph) {
		this.stGraph = spatioTemporalGraph;
		initializeFirstFrame();
	}
	
	/**
	 * Similarly to a run() method in the GUI part the method
	 * executes the actual tracking algorithm. Given the 
	 * complexity of certain algorithms this might be very
	 * time consuming.
	 * 
	 */
	public void track(){}
	
	/**
	 * Initialize all nodes of the first frame with successive tracking
	 * IDs and assign a recursive first assignment.
	 */
	private void initializeFirstFrame(){
		//first set trackID of first graph (reference)
		int tracking_id = 0;
		
		for(Node n: stGraph.getFrame(0).vertexSet()){
			n.setTrackID(tracking_id++);
			//alternative:	n.setTrackID(n.hashCode());
			n.setFirst(n);
		}
	}
	
	/**
	 * Link to nodes in a temporal relationship.
	 * 
	 * @param next Node in a successive frame
	 * @param previous Node in a previous frame
	 */
	protected void updateCorrespondence(Node next, Node previous) {
		next.setTrackID(previous.getTrackID());
		next.setFirst(previous.getFirst());
		next.setPrevious(previous);
		
		//only update linkage from previous cell if not set yet
		if(previous.getNext() == null)
			previous.setNext(next);
		
	}

}
