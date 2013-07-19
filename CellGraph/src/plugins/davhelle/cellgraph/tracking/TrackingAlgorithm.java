/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.tracking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.BorderCells;
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
	protected Geometry frame_0_union;
	protected int tracking_id;
	
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
	 * IDs and assign a recursive first assignment. And build the geometrical
	 * object representing all tracked cells in the first frame.
	 */
	private void initializeFirstFrame(){
		
		//first set trackID and geometry of first graph (reference)
		tracking_id = 0;
		
		//Now process the first frame and record it's new geometry
		FrameGraph first_frame = stGraph.getFrame(0);
		Geometry[] output = new Geometry[first_frame.size()];
		
		//iterate trought all nodes and initialize fields and obtain geometry
		//alternative:	n.setTrackID(n.hashCode());
		
		Iterator<Node> node_it = first_frame.vertexSet().iterator();
		while(node_it.hasNext()){
			Node n = node_it.next();
			
			//initialize fields
			n.setTrackID(tracking_id);
			n.setFirst(n);
			
			//extract geometry
			output[tracking_id] = n.getGeometry();
			
			tracking_id++;
		}

		//Create union of all polygons, TODO check if little buffer should be added
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
	
	
	/**
	 * Method to classify nodes which have not been assigned 
	 * to a Node in a previous time frame (previous field..). 
	 * This can either be due to a segmentation/tracking mistake or
	 * due to a division event. By quorum sensing the neighborhood
	 * of the lost nodes the heuristic identifies the most
	 * likely correspondence in a previous time frame.
	 * 
	 * More in detail for every neighbor the heuristic identifies
	 * a correspondence at a previous time point and uses that
	 * neighborhood as candidates for the most likely ancestor. 
	 * Sets are used to avoid nearby cell divisions to bias the voting.
	 * 
	 * @param unassignedNode Node with an empty previous field
	 */
	protected Node rescueCandidate(Node unassignedNode){
		
		boolean is_division = false;

		List<Node> neighbors = unassignedNode.getNeighbors();
		Set<Node> unique_neighbors = new HashSet<Node>(neighbors);

		//Create quorum sensing map to identify most likely correspondence
		Map<Node,Integer> qs_map = new HashMap<Node, Integer>();
		
		//initialize qs_map
		for(Node neighbor: neighbors)
			if(neighbor.getFirst() != null)
				qs_map.put(neighbor.getFirst(), 0);
			
		//Neighborhood quorum sensing
		for(Node neighbor: unique_neighbors){
			
			Node ancestor = neighbor.getPrevious();
			if(ancestor != null){
				
				Set<Node> unique_ancestor_neighbors = new HashSet<Node>();
				
				//for unique neighbors of the ancestor
				for(Node ancestorNeighbor: ancestor.getNeighbors())
					if(ancestorNeighbor.getFirst() != null)
						unique_ancestor_neighbors.add(ancestorNeighbor.getFirst());
				
				//Use the set elements as candidates for the qs voting
				for(Node candidate: unique_ancestor_neighbors)
					if(qs_map.containsKey(candidate)){
						int count = qs_map.get(candidate).intValue();
						count++;
						qs_map.put(candidate.getFirst(), count); //ev. ++count
					}
					else {
						if(candidate.getFirst() != null)
							qs_map.put(candidate.getFirst(), 1);
					}
			}
		}

		//Find most voted candidate, i.e. the most likely parent
		Node most_likely_parent = null;
		
		//TODO might use candidate sum for improving heuristic
		int candidate_sum = 0;
		//System.out.println(unassignedNode.getGeometry().toText());
		for(Node candidate: qs_map.keySet()){
			candidate_sum += qs_map.get(candidate);
			//System.out.println("\t"+candidate.getTrackID()+":"+qs_map.get(candidate).toString());
			if(most_likely_parent == null)
				most_likely_parent = candidate;
			else
				//check whether the qs score is higher for the candidate (negative comparison result)
				if(qs_map.get(most_likely_parent).compareTo(qs_map.get(candidate)) < 0)
					most_likely_parent = candidate;
		}

		//System.out.println("before:"+most_likely_parent.getTrackID());
		
		//check if equally good candidates exist and if yes compare the distances to the unassigned node
		for(Node candidate: qs_map.keySet()){
			if(candidate != most_likely_parent)
				if(qs_map.get(most_likely_parent).compareTo(qs_map.get(candidate)) == 0){
					double distanceMostLikely = unassignedNode.getCentroid().distance(most_likely_parent.getCentroid());
					double distanceAlternative= unassignedNode.getCentroid().distance(candidate.getCentroid());
					//equally good solution exist, choose the one that is closer
					if(distanceAlternative < distanceMostLikely)
						most_likely_parent = candidate;
				}
		}
		
		//System.out.println("after:"+most_likely_parent.getTrackID());
		
//		double candidate_average = 0;
//		if(qs_map.size() > 0)
//			candidate_average = candidate_sum / qs_map.size();
		
		return most_likely_parent;

		//If a parent has been found check whether the most recent correspondence in time is also 
		//geometrically sound with the hypothesis, if yes update the correspondence.
		
//		if(most_likely_parent != null){
//
//			Node last_parent_reference = getMostRecentCorrespondence(unassignedNode, most_likely_parent);
//
//			//apply little buffer when testing geometrical correspondence
//			if(last_parent_reference.getGeometry().buffer(6).contains(unassignedNode.getCentroid())){
//				System.out.println(last_parent_reference.getTrackID()+" -> "+unassignedNode.getGeometry().toText());
//				is_division = true;
//			}
//
//			//less conservative assignment
//			//				else{
//			//					for(Node candidate: qs_array.keySet()){
//			//						if(candidate != null){
//			//							if(candidate.getGeometry().buffer(6).contains(newNode.getCentroid())){
//			//								System.out.println("\t is contained! (less confident!)");
//			//
//			//								newNode.setFirst(most_likely_parent);
//			//								newNode.setTrackID(most_likely_parent.getTrackID());
//			//								newNode.setPrevious(last_parent_reference);
//			//
//			//								break;
//			//							}
//			//						}
//			//					}
//			//				}
//		}
//		
//		return is_division;
	}

}
