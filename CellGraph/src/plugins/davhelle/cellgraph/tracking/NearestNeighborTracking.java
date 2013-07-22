/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.tracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.ComparableNode;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Nearest Neighbor tracking implements a tracking strategy
 * that associates nodes by cell center correspondence
 * minimization. Moreover when multiple frames are linked
 * i.e. linkrange > 1. A stable marriage solving algorithm
 * is used to identify the perfect matching between the
 * candidates and the node.
 * 
 * ErrorsTags of nodes are set to highlight likely 
 * segmentation errors or suggest a division/delamination event.
 * 
 *  stable-marriage-problem algorithm (Gale–Shapley) source:
 *  http://en.wikipedia.org/wiki/Stable_marriage_problem
 * 
 * @author Davide Heller
 *
 */
public class NearestNeighborTracking extends TrackingAlgorithm{
	
	/**
	 * Distance method names to choose from a candidate list
	 */
	private enum DistanceCriteria{
		MINIMAL_DISTANCE, AVERAGE_DISTANCE, AREA_OVERLAP	
		//TODO Add weighted distance? decreasing in time, e.g. 1*(t-1) + 0.8*(t-2)....
, MIN_DISTANCE_WITH_AREA_DIFF, WEIGHTED_MIN_DISTANCE
	}

	/**
	 * Holds the number of frames the node information is projected ahead
	 * @see #linkrange
	 */
	private final int linkrange;

	/**
	 * Holds the kind of distance criteria with which to choose the best parent candidate
	 * @see #group_criteria
	 */
	private final DistanceCriteria group_criteria;
	private final boolean DO_DIVISION_CHECK;

	/**
	 * Holds the proportion of area increase that a dividing cell must have
	 * registered to be acknowledged as division.
	 * @see #increase_factor
	 */
	private final double increase_factor;
	
	/**
	 * Initializes Neighbor tracking
	 * 
	 * 
	 * @param spatioTemporalGraph Spatio-temporal graph to be tracked/linked
	 * @param linkrange the maximum no. of frames the node information is projected ahead
	 */
	public NearestNeighborTracking(SpatioTemporalGraph spatioTemporalGraph, int linkrange) {
		super(spatioTemporalGraph);
		this.linkrange = linkrange;
		this.group_criteria = DistanceCriteria.MIN_DISTANCE_WITH_AREA_DIFF;
		this.DO_DIVISION_CHECK = false;
		this.increase_factor = 1.5;
	}
	
	@Override
	public void track(){
		
		//for every frame extract all particles
		for(int time_point=0;time_point<stGraph.size(); time_point++){
			System.out.println("Linking frame "+time_point);
			
			//link only from the second time point on
			if(time_point > 0){
				
				//link the current time point
				Map<String, Stack<Node>> unmarried = linkTimePoint(time_point);
				
				//analyze unmarried nodes
				analyze_unmarried(unmarried, time_point);
				
			}
			
			//Compute candidates for successive nodes in [linkrange] time frames
			for(Node current: stGraph.getFrame(time_point).vertexSet()){
				//don't propagate what has not been recognized.
				if(current.getTrackID() != -1)
					for(int i=1; i <= linkrange && time_point + i < stGraph.size(); i++)
						for(Node next: stGraph.getFrame(time_point + i).vertexSet())
							if(next.getGeometry().intersects(current.getGeometry()))
							{
								next.addParentCandidate(current);
								if(current.getTrackID() == 302)
									System.out.println("302 propagated to [" + Math.round(next.getCentroid().getX()) + 
									"," + Math.round(next.getCentroid().getY()) + "] @ frame "+(time_point+i));
							}
								
			
				//also check in case of a division that the brother cell is present
				if(current.hasObservedDivision())
					if(!current.getDivision().isBrotherPresent(current)) //TODO review isBrotherPresent Method input (list would be more logic)
						current.setErrorTag(TrackingFeedback.BROTHER_CELL_NOT_FOUND.numeric_code);
			
			}	
		}			
	}

	/**
	 * Linking algorithm based on the stable marriage problem. 
	 * The nodes in the current frame are addressed as "brides"
	 * while the brooms are the candidates from the first frame (except for divisions).
	 * 
	 * @param time_point of frame to be linked
	 * @return returns 2 Stacks containing the unlinked nodes, accessible trough a map interface ("brides", "grooms")
	 */
	private Map<String, Stack<Node>> linkTimePoint(int time_point) {
		
		//Build two maps to store the evaluated candidates (evaluation <> distance from center)
		Map<Node, List<ComparableNode>> grooms = new HashMap<Node, List<ComparableNode>>();
		Map<Node, List<ComparableNode>> brides = new HashMap<Node, List<ComparableNode>>();;

		//Evaluate the distances of the candidates of each node in current time frame
		evaluateCandidates(grooms, brides, time_point);

		//Order the evaluated candidates in ascending mannor (smallest distances first) 
		orderCandidates(grooms);
		orderCandidates(brides);

		//Initialize data structures
		Map<Node, Node> marriage = new HashMap<Node,Node>();

		Stack<Node> unmarried_grooms = new Stack<Node>();
		Stack<Node> unmarried_brides = new Stack<Node>();
		Stack<Node>	nochoice_grooms = new Stack<Node>();

		//Fill candidates
		unmarried_grooms.addAll(grooms.keySet());
		unmarried_brides.addAll(brides.keySet());

		//Stable marriage problem (Gale–Shapley algorithm)
		while(!unmarried_grooms.empty()){
			//take groom candidate from stack and try to assign
			Node groom = unmarried_grooms.pop();

			int follow_id = 286;
			int follow_t = 27;
			
			if(groom.getTrackID() == follow_id && time_point == follow_t){
				System.out.print("Prefered brides of "+ follow_id +"are:");
				for(ComparableNode b: grooms.get(groom)){
					Node next = b.getNode();
					System.out.println("[" + Math.round(next.getCentroid().getX()) + 
									"," + Math.round(next.getCentroid().getY()) + "]");
				}
				
				System.out.println();
				
			}
			
			//get preference list of groom 
			Iterator<ComparableNode> bride_it = grooms.get(groom).iterator();
			boolean married = false;

			//loop util groom has preferences and is not married
			while(bride_it.hasNext() && !married){

				//get bride candidate and mark her as visited
				Node bride = bride_it.next().getNode();
				bride_it.remove();

				//check if wanted bride is married at all
				if(!marriage.containsKey(bride)){
					unmarried_brides.remove(bride);

					marriage.put(bride, groom);
					married = true;
				}

				//if already married see if current groom is better fit
				else{

					Node old_groom = marriage.get(bride);
					if(old_groom.getTrackID() == 286 && time_point == 27)
						System.out.println("Alarm! Your bride might get stolen!");
					Iterator<ComparableNode> grooms_it = brides.get(bride).iterator();

					//cycle preferences (ascending order, best first)
					while(grooms_it.hasNext()){
						Node preffered_groom = grooms_it.next().getNode();

						//current husband has better rating
						if(preffered_groom == old_groom)
							break;

						//new husband has better rating!
						if(preffered_groom == groom){
							unmarried_grooms.push(old_groom);

							marriage.put(bride, groom);
							married = true;
							break;
						}
					}
				}
			}

			//if groom has no more bride candidates eliminate from list
			if(!marriage.containsValue(groom) && grooms.get(groom).isEmpty())
				nochoice_grooms.push(groom);

		}

		//finally update node correspondences
		for(Node bride: marriage.keySet()){
			Node groom = marriage.get(bride);
			updateCorrespondence(bride, getMostRecentCorrespondence(bride, groom));
		}
		
		
		Map<String, Stack<Node>> unmarried = new HashMap<String, Stack<Node>>();
		
		unmarried.put("brides", unmarried_brides);
		unmarried.put("grooms", nochoice_grooms);
		
		return unmarried;
	}

	/**
	 * Analyze nodes that could not be associated because of a division event or a segmentation error.	 * 
	 * 
	 * @param unmarried Nodes that have not been associated in the linking process
	 * @param time_point Time point being analyzed
	 */
	private void analyze_unmarried(Map<String, Stack<Node>> unmarried, int time_point) {
		
		//obtain separate unmarried stacks
		Stack<Node> unmarried_brides = unmarried.get("brides");
		Stack<Node> unmarried_grooms = unmarried.get("grooms");
		
		//analyze unmarried brides, i.e. current nodes without previously linked node
		System.out.println(" uB:"+ unmarried_brides.size());
		while(!unmarried_brides.empty()){
			
			Node lost = unmarried_brides.pop();
	
			if(lost.onBoundary())
				continue;
			
			//Make a rescue attempt on lost node
			//i.e. determine whether a division or
			//major image shift could have happened.
			Node rescued = rescueCandidate(lost);
			
			
			if(rescued != null){
				
//				System.out.println("Attempting rescue with:"+rescued.getTrackID());
				//check whether linking missed candidate due to excessive movement
				//TODO add conditional for division case, mother node should
				//not be further searched after division happended..
				Node lastCorrespondence = getMostRecentCorrespondence(time_point, rescued);
				
				if(unmarried_grooms.contains(rescued)){
					updateCorrespondence(lost, lastCorrespondence);
					unmarried_grooms.remove(rescued);
					
					//visual feedback
					System.out.println("\tRescue of "+rescued.getTrackID());
					//lost_previous.add(lost);
				}
				else{
					if(DO_DIVISION_CHECK){
					//check whether lost node is result of a division process
					//by looking if the center was inside the rescue node at 
					//the previous time point
					//TODO decide whether to add .buffer(6).
					if(lastCorrespondence.getGeometry().contains(lost.getCentroid())){
						
						System.out.println("\tDivision recognition started..");
						
						//initialize division assumption
						Node mother = rescued;
						Node brother = null;
						boolean has_brother = false;
						
						
						//get brother cell by checking neighbors
						for(Node neighbor: lost.getNeighbors())
							if(neighbor.getFirst() == mother 
							&& lastCorrespondence.getGeometry().contains(neighbor.getCentroid())){
								has_brother = true;
								brother = neighbor;
							}
						
						//check for the presence of an area increase
						boolean has_area_increase = false;
						int pastFrameNo = 5;
						
						double original_area = mother.getGeometry().getArea();
						double area_threshold = original_area*increase_factor;
						Node past_mother = lastCorrespondence;
						
						while(past_mother != null && pastFrameNo > 0){
							if(past_mother.getGeometry().getArea() > area_threshold){
								has_area_increase = true;
								break;
							}
							else
								past_mother = past_mother.getPrevious();
								
						}
						
						//verify division conditions
						if(has_brother && has_area_increase){							
							//create division object
							Division division = new Division(lastCorrespondence, lost, brother, tracking_id);
							tracking_id = tracking_id + 2;
							System.out.println(division.toString());
	
						}
						else{
							lost.setErrorTag(TrackingFeedback.LOST_IN_PREVIOUS_FRAME.numeric_code);
							System.out.println(
									"\t@User: failed division - "+rescued.getTrackID() + 
									"@[" + Math.round(rescued.getCentroid().getX()) + 
									"," + Math.round(rescued.getCentroid().getY()) + "]");
						}
					}
						
						
					}
					else{
						lost.setErrorTag(TrackingFeedback.LOST_IN_PREVIOUS_FRAME.numeric_code);
//						System.out.println("\t@User: outside previous geometry - "+rescued.getTrackID() + 
//								"@[" + Math.round(rescued.getCentroid().getX()) + 
//								"," + Math.round(rescued.getCentroid().getY()) + "]");
					}
				}	
			}				
			else{
				lost.setErrorTag(TrackingFeedback.LOST_IN_PREVIOUS_FRAME.numeric_code);
//				System.out.println("\t@User: no rescue candidate");
			}
		}
		
		//analyze unmarried grooms, i.e. first nodes not been associated to current frame
		System.out.println(" uG:"+ unmarried_grooms.size());
		while(!unmarried_grooms.empty()){
			Node last_correspondence = getMostRecentCorrespondence(time_point, unmarried_grooms.pop());
			
			//ignore if on border
			if(last_correspondence.onBoundary())
				continue;
			else{
				//look through neighborhood and see whether an untracked
				//particle occurs at t+1 in their neighborhood
				for(Node neighbor: last_correspondence.getNeighbors()){
					if(neighbor.hasNext()){
						Node futureN = neighbor.getNext();
						//check intersection with the non-assigned brides
						for(Node futureNN: futureN.getNeighbors())
							if(unmarried_brides.contains(futureNN))
								//check whether solution can be found. \
								System.out.println(
										"Cell "+last_correspondence.getTrackID()+
										" was lost in Frame "+last_correspondence.getBelongingFrame().getFrameNo()+
										"\n possible misassignmnet might occurred as"+
										"\n Cell "+futureN.getTrackID()+"(from "+neighbor.getTrackID()+") has untracked neighbor");
						
						
					}
				}
				
			}
			

			
			//if an untracked cells occurs, check whether the two share
			//a common neighbor. And check if the candidates list of the latter
			//contains either the lost groom or a groom that was assigned to 
			//a neighbor.
			
			
			
			
			if(last_correspondence.getErrorTag() == TrackingFeedback.LOST_IN_PREVIOUS_FRAME.numeric_code)
				last_correspondence.setErrorTag(TrackingFeedback.LOST_IN_BOTH.numeric_code);
			else
				last_correspondence.setErrorTag(TrackingFeedback.LOST_IN_NEXT_FRAME.numeric_code);
		}
	
	}

	/**
	 * Method to order the candidates based on the distance computed
	 * by the evaluation method. Given the ComparableNode object the
	 * inhouse Arrays.sort method can be used.
	 * 
	 * @param node_map Map with node's list to be ordered
	 */
	private void orderCandidates(
			Map<Node, List<ComparableNode>> node_map) {

		for(Node first: node_map.keySet()){
			
			//retrieve
			List<ComparableNode> candidate_list = node_map.get(first);
			int candidate_no = candidate_list.size();
			
			//ascending sort
			ComparableNode[] candidate_array = candidate_list.toArray(new ComparableNode[candidate_no]);
			Arrays.sort(candidate_array);
//			System.out.print(first.getTrackID()+":"+Arrays.toString(candidate_array));
		
			
			//List conversion
			candidate_list = new ArrayList<ComparableNode>(Arrays.asList(candidate_array));
			
			//update (requires change from immutable java.util.Arrays$Arraylist to mutable ArrayList object)
			node_map.put(first, candidate_list);

		}
		
//		System.out.println();

	}

	/**
	 * Evaluate all node's candidate of a particular frame. Evaluation
	 * is intended as quantification of the cell center distance between 
	 * a candidate node and the node being considered.
	 * 
	 * Two Map are filled which represent the two classes which are to be matched. 
	 * Since with a give linkrange we connect multiple frames to a particular
	 * frame in the future. Distance are averaged if candidate node share the
	 * same ancestor (first) node.
	 * 
	 * @param first_map correspondence from first to current frame
	 * @param current_map correspondence from current to first frame
	 * @param time_point time point of the current frame being considered
	 */
	private void evaluateCandidates(Map<Node, List<ComparableNode>> first_map,
			Map<Node, List<ComparableNode>> current_map, int time_point) {
		
		//visit all nodes of the current frame
		for(Node current: stGraph.getFrame(time_point).vertexSet()){
			
			//initialize reference towards which distances are computed
			Point current_cell_center = current.getCentroid();
			
			//given ancestor candidates compute mean distances
			//based on individual nodes linking to the same first() node.
			
			List<Node> candidates = current.getParentCandidates();
			
			//if no candidates are given add it as "lost bride", TODO .buffer(-10.0)?
			if(candidates.size() == 0){
				if(frame_0_union.contains(current_cell_center) && !current.onBoundary())
					current_map.put(current, new ArrayList<ComparableNode>());
			}
			else{
				
				double current_area = current.getGeometry().getArea();
				
				while(candidates.size() > 0){

					Iterator<Node> candidate_it = candidates.iterator();

					Node voted = candidate_it.next();
					Node first = voted.getFirst();
					candidate_it.remove();
					
					//Check whether the cell is part of a division, if yes to 
					//avoid that the candidate approach is biased by the mother
					//cell 
					//TODO rethink naming -> hasDivision (what if child node divide twice...)
					if(voted.hasObservedDivision()){
						Division division = voted.getDivision();
						if(time_point > division.getTimePoint())
							if(division.isMother(voted)){
								//TODO might want to eliminate all mother cells at once
								//from the candidate list
								continue;
						}
					}
					
					Point voted_centroid = voted.getCentroid();

					
					//VIABILITY CHECK BASED ON FIRST FRAME GEOMETRY
					//Cell could be either new (division/seg.error), 
					//thus not associated to any first node //TODO .buffer(-10.0)?
					if(first == null){
						if(frame_0_union.contains(voted_centroid) && !voted.onBoundary())
							current_map.put(current, new ArrayList<ComparableNode>());
						continue;
					}
					
					
					//compute a value for the entire first group
					double group_value = Double.MAX_VALUE;
					
					switch(group_criteria){
					
					case AVERAGE_DISTANCE:
						//compute the average distance out of all
						//distances with same node (group)
						int count = 1;
						double sum = DistanceOp.distance(
								voted_centroid,
								current_cell_center);

						while(candidate_it.hasNext()){
							voted = candidate_it.next();
							if( voted.getFirst() == first){
								candidate_it.remove();
								voted_centroid = voted.getCentroid();
								sum +=  DistanceOp.distance(
										voted_centroid,
										current_cell_center);
								count++;
							}
						}

						double avg = sum / count;
						group_value = avg;
						break;
						
					case MINIMAL_DISTANCE:
						//compute the minimal distance out of all
						//distances with same node (group)
						double min = DistanceOp.distance(
								voted_centroid,
								current_cell_center);

						while(candidate_it.hasNext()){
							voted = candidate_it.next();
							if( voted.getFirst() == first){
								candidate_it.remove();
								voted_centroid = voted.getCentroid();
								double candidate_dist = DistanceOp.distance(
										voted_centroid,
										current_cell_center);
								if(min > candidate_dist)
									min = candidate_dist;

							}
						}

						group_value = min;
						break;
						
					case AREA_OVERLAP:	
						
						//compute the intersection between the two cell geometries
						Geometry intersection = current.getGeometry().intersection(voted.getGeometry());

						//Use the ratio of the intersection area and current's area as metric	
						double overlap_ratio = intersection.getArea() / current_area;
						//or alternatively divided by the sum of the two cell areas
						//double overlap_ratio = intersection.getArea() / (current_area + voted.getGeometry().getArea());
						//adding the candidate's area should reduce the bias of a large cell to affect
						//the ratio. 
						
						//init best_ratio
						double best_ratio = overlap_ratio;
						
						while(candidate_it.hasNext()){
							
							//count only candidates with the same first cell
							voted = candidate_it.next();
							if( voted.getFirst() == first){
								candidate_it.remove();
								//compute the intersection between the two cell geometries
								intersection = current.getGeometry().intersection(voted.getGeometry());

								//Use the ratio of the intersection area and current's area as metric	
								overlap_ratio = intersection.getArea() / current_area;
								//or alternatively divided by the sum of the two cell areas
								//double overlap_ratio = intersection.getArea() / (current_area + voted.getGeometry().getArea());
								//adding the candidate's area should reduce the bias of a large cell to affect
								//the ratio. 
								
								//choose the biggest overlap ration
								if(overlap_ratio > best_ratio)
									best_ratio = overlap_ratio;
							
							}
						}
						
						//end by assigning the reverse value as group value, ordered by minimum
						group_value = 1 - best_ratio;
							
						break;
						
					case WEIGHTED_MIN_DISTANCE:
						//compute the minimal distance out of all
						//distances with same node (group)
						
						double candidate_dist = DistanceOp.distance(
								voted_centroid,
								current_cell_center);
						
						//compute the intersection between the two cell geometries
						intersection = current.getGeometry().intersection(voted.getGeometry());
						overlap_ratio = intersection.getArea() / current_area;
					
						double weighted_candidateDistance = candidate_dist * (1 - overlap_ratio);
						
						min = weighted_candidateDistance;

						while(candidate_it.hasNext()){
							
							voted = candidate_it.next();
							if( voted.getFirst() == first){
								candidate_it.remove();
								voted_centroid = voted.getCentroid();
								
								candidate_dist = DistanceOp.distance(
										voted_centroid,
										current_cell_center);
								
								//compute the intersection between the two cell geometries
								intersection = current.getGeometry().intersection(voted.getGeometry());
								overlap_ratio = intersection.getArea() / current_area;
							
								weighted_candidateDistance = candidate_dist * (1 - overlap_ratio);
							
								
							if(min > weighted_candidateDistance)
									min = weighted_candidateDistance;

							}
						}

						group_value = min;
						
						break;
						
					case MIN_DISTANCE_WITH_AREA_DIFF:
						
						candidate_dist = DistanceOp.distance(
								voted_centroid,
								current_cell_center);
						
						//compute difference in area
						double area_candidate = voted.getGeometry().getArea();
						double area_current = current.getGeometry().getArea();
						
						double area_difference = Math.abs(area_candidate - area_current);
						double normalized_area_diff = area_difference/area_candidate;
						
						//weighting parameters
						final double lambda1 = 1;
						final double lambda2 = 1;
	
						//final score
						
						
//						System.out.println(voted.getTrackID()+" dist:"+candidate_dist);
//						System.out.println(
//								voted.getTrackID()+" area:"+
//										"\n\t"+area_candidate+
//										"\n\t"+area_current+
//										"\n\t"+normalized_area_diff);
						
						weighted_candidateDistance = 
								lambda1 * candidate_dist +
								lambda2 * normalized_area_diff;
						
						
						min = weighted_candidateDistance;

						while(candidate_it.hasNext()){
							
							voted = candidate_it.next();
							if( voted.getFirst() == first){
								candidate_it.remove();
								voted_centroid = voted.getCentroid();
								
								candidate_dist = DistanceOp.distance(
										voted_centroid,
										current_cell_center);
								
								//compute difference in area
								area_candidate = voted.getGeometry().getArea();
								area_current = voted.getGeometry().getArea();
								
								area_difference = Math.abs(area_candidate - area_current);
								normalized_area_diff = area_difference/area_candidate;
			
								//final score
								
								
//								System.out.println(voted.getTrackID()+" dist:"+candidate_dist);
//								System.out.println(voted.getTrackID()+" area:"+normalized_area_diff);
//								
								weighted_candidateDistance = 
										lambda1 * candidate_dist +
										lambda2 * normalized_area_diff;
										
								if(min > weighted_candidateDistance)
									min = weighted_candidateDistance;

							}
						}

						group_value = min;
						
						//use the area overlap ratio to weight the distance from the intersection to current's cell center
						break;
						
					}

					//assign candidate to both maps with the respective distance

					//first -> current
					if(!first_map.containsKey(first))
						first_map.put(first, new ArrayList<ComparableNode>());

					ComparableNode candidate_distance = new ComparableNode(current,group_value);
					first_map.get(first).add(candidate_distance);

					//current -> first
					if(!current_map.containsKey(current))
						current_map.put(current, new ArrayList<ComparableNode>());

					current_map.get(current).add(new ComparableNode(first, group_value));

					//the two maps will be later matched by solving an abstracted
					//stable marriage problem
				}
			}
		}	
	}
}

