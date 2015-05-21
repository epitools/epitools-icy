package plugins.davhelle.cellgraph.tracking;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Abstract tracking algorithm to establish the connectivity 
 * between different frames of the movie being analyzed.
 * 
 * @author Davide Heller
 *
 */
public abstract class TrackingAlgorithm {

	protected SpatioTemporalGraph stGraph;
	protected Geometry frame_0_union;
	protected int tracking_id;
	
	/**
	 * Constructor methods should always initialize the spatio temporal graph
	 * field.
	 * 
	 * @param spatioTemporalGraph
	 */
	public TrackingAlgorithm(SpatioTemporalGraph spatioTemporalGraph,boolean do_id_initialization) {
		this.stGraph = spatioTemporalGraph;
		if(do_id_initialization)
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
	 * IDs and assign a recursive first assignment. And build the geometrical
	 * object representing all tracked cells in the first frame.
	 */
	private void initializeFirstFrame(){
		
		//first set trackID and geometry of first graph (reference)
		tracking_id = 0;
		
		//Now process the first frame and record it's new geometry
		FrameGraph first_frame = stGraph.getFrame(0);
		Geometry[] output = new Geometry[first_frame.size()];
		
		//iterate through all nodes and initialize fields and obtain geometry
		//alternative:	n.setTrackID(n.hashCode());
		
		for(Node n: first_frame.vertexSet()){
			
			//initialize fields
			n.setTrackID(tracking_id);
			n.setFirst(n);
			
			//extract geometry
			output[tracking_id] = n.getGeometry();
			
			tracking_id++;
		}

		//Create union of all polygons to find the boundary
		GeometryCollection polygonCollection = new GeometryCollection(output, new GeometryFactory());
		frame_0_union = polygonCollection.buffer(0);
		
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
		
		//propagate division
		if(previous.hasObservedDivision())
			next.setDivision(previous.getDivision());
		
		//only update linkage from previous cell if not set yet
		if(previous.getNext() == null){
			
			previous.setNext(next);
			
			int previous_frame_no = previous.getBelongingFrame().getFrameNo();
			int next_frame_no = next.getBelongingFrame().getFrameNo();
			
			//check if correspondence is in the immediately successive frame
			if(next_frame_no - previous_frame_no > 1)
				previous.setErrorTag(-3);
			
		}
		
	}
	
	/**
	 * Link nodes and force update of previous node even if already
	 * assigned.
	 * 
	 * @param next
	 * @param previous
	 */
	protected void forceCorrespondence(Node next, Node previous){
		next.setTrackID(previous.getTrackID());
		next.setFirst(previous.getFirst());
		next.setPrevious(previous);
		
		//propagate division
		if(previous.hasObservedDivision())
			next.setDivision(previous.getDivision());
			
		previous.setNext(next);

		int previous_frame_no = previous.getBelongingFrame().getFrameNo();
		int next_frame_no = next.getBelongingFrame().getFrameNo();

		//check if correspondence is in the immediately successive frame
		if(next_frame_no - previous_frame_no > 1)
			previous.setErrorTag(-3);
			
		
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
	
	/**
	 * Find the closest correspondence in time starting
	 * from a given node in the first frame. The search stops when
	 * the time point of the correspondence is in the previous
	 * frame of the given time point. 
	 * 
	 * @param time_point frame no with respect to which the most recent node is found
	 * @param first Node in first frame
	 * @return Closest node to n in time
	 */
	protected Node getMostRecentCorrespondence(int time_point, Node first){
		
		Node last_parent_reference = first;
		Node next_parent_reference = first.getNext();
		int current_node_frame_no = time_point;
		
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
