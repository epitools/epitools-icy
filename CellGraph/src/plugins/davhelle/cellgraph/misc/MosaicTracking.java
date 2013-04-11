package plugins.davhelle.cellgraph.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.particleLinking.ParticleLinker;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Track centroids of node types represented
 * in the DevelopmentType spatiotemporal graph
 * 
 * 
 * @author Davide Heller
 *
 */
public class MosaicTracking {

	private SpatioTemporalGraph stGraph;
	private MyFrame[] frames;
	private HashMap<Particle,Node> particle2NodeMap;
	private int frames_number;
	private int linkrange;
	private float displacement;

	/**
	 * Setup tracking structures required for ParticleLinker class
	 * 
	 * @param spatioTemporalGraph Spatio temporal graph to be tracked
	 */
	public MosaicTracking(SpatioTemporalGraph spatioTemporalGraph, int linkrange, float displacement) {
		this.stGraph = spatioTemporalGraph;
		this.frames_number = spatioTemporalGraph.size();
		this.linkrange = linkrange; //TODO tune
		this.displacement = displacement; //TODO tune
		this.frames = new MyFrame[frames_number];
		this.particle2NodeMap = new HashMap<Particle,Node>();

		//convert stGraph into MyFrame array
		particleConversion();

	}

	/**
	 * Run tracking and update tracking IDs of graph nodes (NodeType)
	 */
	public void track(){
		//create Linker
		ParticleLinker linker = new ParticleLinker();

		System.out.println("Initialized ParticleLinker, start tracking..");
		//execute
		linker.linkParticles(frames, frames_number, linkrange, displacement);
		System.out.println("...completed tracking! Linking particles to nodes");

		//update graph structure
		updateGraph();

		System.out.println("...done!");
	}

	/**
	 * Internal method to convert all NodeTypes in each TissueGraph
	 * into Particle objects collected into MyFrame objects. Latter
	 * is required for ParticleLinker class.
	 */
	private void particleConversion(){

		//extract single Graph Frame and convert to MyFrame
		//set of Particles
		Geometry frame_0_union = null;

		for(int time_point=0;time_point<frames_number; time_point++){
			//get graph
			FrameGraph graph_i = stGraph.getFrame(time_point);
			Vector<Particle> particles = new Vector<Particle>();
			int particle_number = graph_i.size();

			//Build the union of frame 0 to discard 
			if(time_point==0){

				Geometry[] output = new Geometry[graph_i.size()];
				Iterator<Node> node_it = graph_i.iterator();
				for(int j=0; j<graph_i.size(); j++){
					output[j] = node_it.next().getGeometry();
				}		

				//Create union of all polygons
				GeometryCollection polygonCollection = new GeometryCollection(output, new GeometryFactory());
				//TODO check if better to add a little buffer
				frame_0_union = polygonCollection.buffer(0);
			}


			//convert graph nodes 
			for(Node n: graph_i.vertexSet()){
				//JTS coordinate
				Geometry node_centroid = n.getCentroid();

				//only add particles of nodes within the frame_0 boundary
				if(frame_0_union.contains(node_centroid)){
					//mark node as tracked but not necessarily resolved
					n.setTrackID(-2);

					Coordinate centroid = node_centroid.getCoordinate();
					//MOSAIC particle
					//centroid.z not available TODO check polygonizer
					//if used tracker won't work
					Particle p = 
							new Particle(
									(float)centroid.x,
									(float)centroid.y,
									(float)0,
									time_point,
									linkrange);
					//update particle vector
					particles.add(p);

					particle2NodeMap.put(p, n);
				}
			}

			//define MOSAIC myFrame
			frames[time_point] = new MyFrame(particles, time_point, linkrange);

		}

	}

	/**
	 * internal method to update all tracking indices
	 * of NodeTypes by using the tracking information
	 * stored in MyFrames Particles after execution. 
	 * Correspondence between particles and graph nodes
	 * is achieved through particle2NodeMap. 
	 */
	private void updateGraph(){
		
		initializeFirstFrame();

		//for every frame extract all particles
		for(int time_point=0;time_point<frames_number; time_point++){
			
			//initialize
			ArrayList<Node> unassigned_nodes = new ArrayList<Node>();
			Vector<Particle> particles = frames[time_point].getParticles();

			//for every particle update the corresponding NodeType
			for(Particle p: particles){
				Node n = particle2NodeMap.get(p);

				if(time_point > 0){
					
					//Given a list of candidates choose the most likely 
					//corresponding cell in first frame and assign the 
					//most recent correspondence to n.
					
					Node most_voted = chooseMostVotedCorrespondence(n.getParentCandidates());
					if(most_voted != null){
						Node most_recent = getMostRecentCorrespondence(n, most_voted);
						updateCorrespondence(n, most_recent);
					}
					else
						unassigned_nodes.add(n);
					
				}

				//frame of correspondent particle
				int next_frame_idx = time_point;
				boolean is_linked = false;

				//Update all future correspondences available (linkrange)
				for(int linked_idx: p.next){
					next_frame_idx++;
					if(linked_idx != -1){

						//obtain corresponding particle in future and latter's node
						Particle pNext = frames[next_frame_idx].getParticles().get(linked_idx);
						Node nNext = particle2NodeMap.get(pNext);
						
//						if(n.getGeometry().contains(
//								new GeometryFactory().createPoint(
//										new Coordinate(280.0,260.0))))
//							System.out.println(n.getTrackID());
						
//						System.out.println(n.getTrackID()+":"+nNext.getGeometry().toText());

						//if correspondence is also geometrically sound add candidate
						if(n.getGeometry().buffer(displacement).contains(nNext.getCentroid())){
							
							nNext.addParentCandidate(n.getFirst());

							//alternative strategy update just one cell in the future
//							if(!is_linked){
//								updateCorrespondence(nNext, n);
//								is_linked = true;
//								//only influence one cell in the future or change if clause to nNext.hasPrevious()? 
//								break;
//							}
						}
//						else
//							System.out.println(nNext.getGeometry().toText());
					}
				}
			}
			
			//Resolve unassigned nodes (segmentation error or division?)
			
			//System.out.print(time_point + ":");
			
			if(unassigned_nodes.size() > 0)
				for(Node unassigned: unassigned_nodes){
					//System.out.print(unassigned.getTrackID()+",");
					resolveUnassignedNodes(unassigned);
				}
			
//			System.out.println();
		}

		
		
		this.stGraph.setTracking(true);
	}
	
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
	private void updateCorrespondence(Node next, Node previous) {
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
	private Node getMostRecentCorrespondence(Node n, Node first){
		
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
	 * Given a number of parent node candidates the most 
	 * voted, i.e. with the highest number of repetitions 
	 * in the list, is identified and returned.
	 * 
	 * @param candidates list of nodes in the first frame
	 * @return most voted node
	 */
	private Node chooseMostVotedCorrespondence(List<Node> candidates){

		Node most_voted = null;
		int max_count = 0;

		while(candidates.size() > 0 && candidates.size() > max_count){
			Iterator<Node> candidate_it = candidates.iterator();

			Node voted = candidate_it.next();
			candidate_it.remove();
			int count = 1;

			while(candidate_it.hasNext()){
				if(candidate_it.next() == voted){
					candidate_it.remove();
					count++;
				}
			}

			if(max_count < count){
				most_voted = voted;
				max_count = count;
			}
		}

		return most_voted;
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
	private void resolveUnassignedNodes(Node unassignedNode){

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

//		double candidate_average = 0;
//		if(qs_map.size() > 0)
//			candidate_average = candidate_sum / qs_map.size();

		//If a parent has been found check whether the most recent correspondence in time is also 
		//geometrically sound with the hypothesis, if yes update the correspondence.
		if(most_likely_parent != null){

			Node last_parent_reference = getMostRecentCorrespondence(unassignedNode, most_likely_parent);

			//apply little buffer when testing geometrical correspondence
			if(last_parent_reference.getGeometry().buffer(6).contains(unassignedNode.getCentroid()))
				updateCorrespondence(unassignedNode, last_parent_reference);

			//less conservative assignment
			//				else{
			//					for(Node candidate: qs_array.keySet()){
			//						if(candidate != null){
			//							if(candidate.getGeometry().buffer(6).contains(newNode.getCentroid())){
			//								System.out.println("\t is contained! (less confident!)");
			//
			//								newNode.setFirst(most_likely_parent);
			//								newNode.setTrackID(most_likely_parent.getTrackID());
			//								newNode.setPrevious(last_parent_reference);
			//
			//								break;
			//							}
			//						}
			//					}
			//				}
		}
	}
}


