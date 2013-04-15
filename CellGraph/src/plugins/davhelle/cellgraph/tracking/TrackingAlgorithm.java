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
	
	/**
	 * Find the closest correspondence in time starting
	 * from a given node in the first frame. The search stops when
	 * the time point of the correspondence is in the previous
	 * frame with respect to n or if the next reference is void. 
	 * 
	 * @param n Node with respect to which the time limit is set
	 * @param first Node in first frame
	 * @return Closest node to n in time
	 */
	protected Node getMostRecentCorrespondence(Node n, Node first){
		
		Node last_parent_reference = first;
		Node next_parent_reference = first.getNext();
		int current_node_frame_no = n.getBelongingFrame().getFrameNo();
		
		while(next_parent_reference != null)
			if(next_parent_reference.getBelongingFrame().getFrameNo() < current_node_frame_no){
				last_parent_reference = next_parent_reference;
				next_parent_reference = next_parent_reference.getNext();
			}
			else
				break;
		
		return last_parent_reference;

	}

}
