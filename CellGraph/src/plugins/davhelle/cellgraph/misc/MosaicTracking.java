package plugins.davhelle.cellgraph.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

			System.out.println(time_point);
			
			//initialize
			ArrayList<Node> unassigned_nodes = new ArrayList<Node>();
			Vector<Particle> particles = frames[time_point].getParticles();

			//for every particle update the corresponding NodeType
			for(Particle p: particles){
				Node n = particle2NodeMap.get(p);

				if(time_point > 0){
					
					//Given a list of candidates choose the most likely 
					//corresponding cell in first frame 
					Node most_voted = chooseMostVotedCorrespondence(n.getParentCandidates());
					
					if(most_voted != null){
						//obtain the most recent correspondence wrt n
						Node most_recent = getMostRecentCorrespondence(n, most_voted);
						//assign correspondence TODO get boolean return... (no successive unassigned testing)
						updateCorrespondence(n, most_recent);
					}
					else
						unassigned_nodes.add(n);
					
				}

				//frame of correspondent particle
				int next_frame_idx = time_point;
				boolean is_linked = false;

				//Update all correspondences in time available
				for(int linked_idx: p.next){
					next_frame_idx++;
					if(linked_idx != -1){

						//obtain corresponding particle in future and latter's node
						Particle pNext = frames[next_frame_idx].getParticles().get(linked_idx);
						Node nNext = particle2NodeMap.get(pNext);

						//update correspondent particle (will be overwritten multiple times)
						if(n.getGeometry().buffer(displacement).contains(nNext.getCentroid())){

							nNext.addParentCandidate(n.getFirst());

							//update current particle with the closest correspondence
//							if(!is_linked){
//								updateCorrespondence(nNext, n);
//								is_linked = true;
//								//only influence one cell in the future
//								break;
//							}
						}
					}
				}
			}
			
			//Resolve unassigned nodes (segmentation error or division?)
			if(unassigned_nodes.size() > 0)
				resolveUnassignedNodes(unassigned_nodes);
		}

		this.stGraph.setTracking(true);
	}
	
	private void initializeFirstFrame(){
		//first set trackID of first graph (reference)
		int tracking_id = 0;
		
		for(Node n: stGraph.getFrame(0).vertexSet()){
			n.setTrackID(tracking_id++);
			//alternative:	n.setTrackID(n.hashCode());
			n.setFirst(n);
		}
	}

	private void updateCorrespondence(Node next, Node previous) {

		//update current node and most recent
		next.setTrackID(previous.getTrackID());
		next.setFirst(previous.getFirst());
		next.setPrevious(previous);
		previous.setNext(next);
	
	}
	
	private Node getMostRecentCorrespondence(Node n, Node first){
		//Obtain most recent correspondence

		Node last_parent_reference = first;		
		int current_node_frame_no = n.getBelongingFrame().getFrameNo();
		
		while(last_parent_reference.getNext() != null)
			if(last_parent_reference.getBelongingFrame().getFrameNo() < current_node_frame_no)
				last_parent_reference = last_parent_reference.getNext();
			else
				break;
		
		return last_parent_reference;

	}
	

	private Node chooseMostVotedCorrespondence(List<Node> candidates){

		Node most_voted = null;
		int max_count = 0;

		if(candidates.size() > 0){
			Iterator<Node> candidate_it = candidates.iterator();

			Node voted = candidates.get(0);
			int count = 1;

			while(candidate_it.hasNext())
				if(candidate_it.next() == voted){
					candidate_it.remove();
					count++;
				}

			if(max_count < count){
				most_voted = voted;
				max_count = count;
			}
		}

		return most_voted;
	}
	

	private void resolveUnassignedNodes(List<Node> unassigned_nodes){
		for(Node n:unassigned_nodes){
			//define newNode
			Node newNode = n;

			List<Node> neighbors = n.getNeighbors();

			//quorum sensing List of neighbors to identify newNode's sibling/mother cell
			Map<Node,Integer> qs_array = new HashMap<Node, Integer>();
			//initialize qs_array
			for(Node neighbor: neighbors)
				if(neighbor.getFirst() != null)
					qs_array.put(neighbor.getFirst(), 0);

			//visit all ancestor nodes of neighbors
			for(Node neighbor: neighbors){
				//get back to last reference and update qs_array
				Node ancestor = neighbor.getPrevious();

				//check if any neighbor of the ancestor node is in the qs_list
				if(ancestor != null){

					//for every neighbor of the ancestor
					for(Node ancestorNeighbor: ancestor.getNeighbors()){

						//Check whether ancestor's neighbor has correspondence with newNod's neighbors
						//done using the first field.

						//System.out.println(ancestorNeighbor.getTrackID());
						if(qs_array.containsKey(ancestorNeighbor.getFirst())){
							int count = qs_array.get(ancestorNeighbor.getFirst()).intValue();
							count++;
							qs_array.put(ancestorNeighbor.getFirst(), count);
						}
						else {
							qs_array.put(ancestorNeighbor.getFirst(), 1);
						}
					}
				}
			}

			//print map
			//System.out.println("New node in frame "+i+":"+newNode.getGeometry().toText());
			Node most_likely_parent = null;
			int candidate_sum = 0;
			for(Node candidate: qs_array.keySet()){
				candidate_sum += qs_array.get(candidate);
				//System.out.println(candidate.getTrackID()+": "+qs_array.get(candidate).toString());
				if(most_likely_parent == null)
					most_likely_parent = candidate;
				else
					//check whether the qs score is higher for the candidate (negative comparison result)
					if(qs_array.get(most_likely_parent).compareTo(qs_array.get(candidate)) < 0)
						most_likely_parent = candidate;
			}

			double candidate_average = 0;
			if(qs_array.size() > 0)
				candidate_average = candidate_sum / qs_array.size();

			//modify newNodes correspondence fields
			if(most_likely_parent != null){

				Node last_parent_reference = getMostRecentCorrespondence(newNode, most_likely_parent);
			
				//apply little buffer when testing geometrical correspondence
				if(last_parent_reference.getGeometry().buffer(6).contains(newNode.getCentroid()))
					updateCorrespondence(newNode, last_parent_reference);
				
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
}


