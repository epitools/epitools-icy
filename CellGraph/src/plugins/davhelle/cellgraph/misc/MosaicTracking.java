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
		
		//extract single frame and convert to MyFrame
		//set of Particles
		Geometry frame_0_union = null;
		
		for(int i=0;i<frames_number; i++){
			//get graph
			FrameGraph graph_i = stGraph.getFrame(i);
			Vector<Particle> particles = new Vector<Particle>();
			int particle_number = graph_i.size();
			
			//Build the union of frame 0 to discard 
			if(i==0){

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
									i,
									linkrange);
					//update particle vector
					particles.add(p);

					particle2NodeMap.put(p, n);
				}
			}
			
			//define MOSAIC myFrame
			frames[i] = new MyFrame(particles, i, linkrange);
			
		}
		
	}
	
	/**
	 * internal method to update all tracking indices
	 * of NodeTypes by using the tracking information
	 * stored in MyFrames Particles. Correspondence
	 * is achieved through particle2NodeMap. 
	 */
	private void updateGraph(){
		//for simple case linkage=1
		
		//first set trackID of first graph (reference)
		int tracking_id = 0;
		for(Node n: stGraph.getFrame(0).vertexSet()){
			n.setTrackID(tracking_id++);
			n.setFirst(n);
		}
		//alternative	n.setTrackID(n.hashCode());	

		//for every frame extract all particles
		for(int i=0;i<frames_number-1; i++){
			Vector<Particle> particles = 
					frames[i].getParticles();
			
			//for every particle update the corresponding NodeType
			for(Particle p: particles){
				Node n = particle2NodeMap.get(p);
				
				//check whether the node is a new node/not tracked
				//Is it a segmentation error or division? quorum sensing to assign parent
				if(i > 0 && i < stGraph.size()){
					if(n.getPrevious() == null){
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
						
						
						
						//conservative condition
						//the most_likely_parent must be a neighbor
						
						
						
						//modify newNodes correspondence fields
						if(most_likely_parent != null){
							System.out.println("Division candidate in frame "+i+" with id "+
									most_likely_parent.getTrackID()+ "(" + 
									qs_array.get(most_likely_parent).toString() + " vs " + candidate_average + ")" + 									
									" @ "+
									newNode.getGeometry().toText());
							

							Node last_known = most_likely_parent;
							//strategy to set sibling and parent
							while(last_known.getNext() != null){
								last_known = last_known.getNext();
							}

							
							Node last_parent_reference = null;				
							//identify whether last_known is sibling(same frame) or the parent(previous frame)
							if(last_known.getBelongingFrame() == newNode.getBelongingFrame())
								last_parent_reference = last_known.getPrevious();
							else
								last_parent_reference = last_known;
							
							if(last_parent_reference.getGeometry().contains(newNode.getCentroid())){
								System.out.println("\t is contained!");
							
								newNode.setFirst(most_likely_parent);
								newNode.setTrackID(most_likely_parent.getTrackID());
								newNode.setPrevious(last_parent_reference);
							}
							
						}
							
					}
				}
				
				//frame of correspondent particle
				int next_frame_idx = i;
				boolean is_linked = false;
				
				//Update all correspondences in time available
				for(int linked_idx: p.next){
					next_frame_idx++;
					if(linked_idx != -1){
						
						//obtain corresponding particle in future and latter's node
						Particle pNext = frames[next_frame_idx].getParticles().get(linked_idx);
						Node nNext = particle2NodeMap.get(pNext);
						
						//update correspondent particle (will be overwritten multiple times)
						nNext.setTrackID(n.getTrackID());
						nNext.setFirst(n.getFirst());
						nNext.setPrevious(n);
						
						//update current particle with the closest correspondence
						if(!is_linked){
							n.setNext(nNext);
							is_linked = true;
							//break;
						}
					}
				}
			}			
		}
		
		this.stGraph.setTracking(true);
	}

}
