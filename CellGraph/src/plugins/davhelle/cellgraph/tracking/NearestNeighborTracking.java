/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.tracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.ComparableNode;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Elimination;
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
, AREA_DIFF_WITH_MIN_DISTANCE, WEIGHTED_MIN_DISTANCE, OVERLAP_WITH_MIN_DISTANCE
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
	private final boolean DO_SWAP_RESC_CHECK;
	private final boolean VERBOSE;
	private final int follow_ID;

	/**
	 * Holds the proportion of area increase that a dividing cell must have
	 * registered to be acknowledged as division.
	 * @see #increase_factor
	 */
	private final double increase_factor;
	
	/**
	 * Candidate evaluation parameters
	 * @see #lambda1
	 * @see #lambda2
	*/
	private double lambda1;
	private double lambda2;
	
	
	/**
	 * Initializes Neighbor tracking
	 * 
	 * 
	 * @param spatioTemporalGraph Spatio-temporal graph to be tracked/linked
	 * @param linkrange the maximum no. of frames the node information is projected ahead
	 */
	public NearestNeighborTracking(SpatioTemporalGraph spatioTemporalGraph, int linkrange, double lambda1, double lambda2) {
		super(spatioTemporalGraph);
		this.linkrange = linkrange;
		this.group_criteria = DistanceCriteria.OVERLAP_WITH_MIN_DISTANCE;
		this.DO_DIVISION_CHECK = true;
		this.DO_SWAP_RESC_CHECK = true;
		this.increase_factor = 1.3;
		this.VERBOSE = false;
		this.follow_ID = 14;
		this.lambda1 = lambda1;
		this.lambda2 = lambda2;
	}
	
	@Override
	public void track(){
		
		//link time points and propagate their information
		for(int time_point = 0; time_point < stGraph.size(); time_point++){
			System.out.println("Linking frame "+time_point);
			
			if(time_point > 0){
				Map<String, Stack<Node>> unmarried = linkTimePoint(time_point);
				
				//analyze unmarried/unlinked nodes
				analyze_unmarried(unmarried, time_point);
			}
			
			propagateTimePoint(time_point);	
		}
		
		reviewDivisionsAndEliminations();
		
		reportTrackingResults();
	}

	/**
	 * - review all events classified as a divisions 
	 * - review all new entry cells
	 * - assign permanent loss labels to permanently lost cells
	 * - Highlight possible Segmentation mistakes
	 */
	private void reviewDivisionsAndEliminations() {
		
		//is cell missing in next frame i.e. TrackingFeedback...
		int elimination_detection_time_limit = stGraph.size() - 1;
		for(int time_point = 0; time_point < elimination_detection_time_limit; time_point++)
		{
			for(Node cell: stGraph.getFrame(time_point).vertexSet())
			{
				if(cell.getErrorTag() == TrackingFeedback.LOST_IN_NEXT_FRAME.numeric_code)
					//if so, is it permanent (depends also on no. of tracked frames)?
					if(!cell.hasNext())
					{						
						Elimination cell_elimination = new Elimination(cell);
						System.out.println(cell_elimination.toString());
						
						//temporary solution to visualize all eliminated cells
						//TODO transform to overlay
						//recursiveTAG(cell, TrackingFeedback.ELIMINATED_IN_NEXT_FRAME);
					}			
			}
		}
		
		//Fixes needed:
		//False Division event out of a segmentation creates a false elimination as well
		//Test against it, revert division if possible.
		
		//Division morphology caused eliminations. Limited frame knowledge. Further assumptions needed
		//in case of limited time point availability (e.g. start or end of movie)
		
		//		if yes, mark as likely elimination
		// 		if not, mark as seg.error
		
		//is cell dividing?
		//	permanent?
		//		brother cell?...
		//		segmentation mistakes? Revert division?
		
		//New cell
		// 	permanent/missed division/segmentation mistake
		//	add division.. 
		
	}

	/**
	 * Function to tag all preceding cells in the lineage with the 
	 * same tag.
	 * 
	 * @param cell latest time point of the lineage to be tagged
	 * @param tag tag to be applied
	 */
	private void recursiveTAG(Node cell, TrackingFeedback tag) {
		if(cell == null)
			return;
		else
		{
			cell.setErrorTag(tag.numeric_code);
			recursiveTAG(cell.getPrevious(),tag);
		}
	}

	/**
	 * PROPAGATION 
	 * Add candidates to nodes in next [linkrange] time frames
	 * 
	 * @param time_point
	 */
	private void propagateTimePoint(int time_point) {

		//TODO [performance enhancement] use neighborhood to find candidates in next frame:
		//find out which node the previous neighbor candidated for and use it
		//as starting node in the next graph to find all other correspondences
		//idea: keep last of previously propagated nodes or iterate on a neighbor basis
		//      e.g. and spreading towards a certain k-neighborhood (risk?)
		
		for(Node current: stGraph.getFrame(time_point).vertexSet())
		{	
			//only propagate what has been successfully in current frame.
			if(current.getTrackID() != -1)
			{	
				for(int i=1; i <= linkrange && time_point + i < stGraph.size(); i++)
					for(Node next: stGraph.getFrame(time_point + i).vertexSet())
						
						if(next.getGeometry().intersects(current.getGeometry()))
						{
							if(next.getGeometry().intersection(current.getGeometry()).getArea() > 10){
							next.addParentCandidate(current);
							
							if( VERBOSE && current.getTrackID() == follow_ID)
								System.out.println(follow_ID + " propagated to [" +
										Math.round(next.getCentroid().getX()) + 
										"," +
										Math.round(next.getCentroid().getY()) + "] @ frame "+(time_point+i));
							}
						}								
			}
			//also check in case of a division that the brother cell is present
			if(current.hasObservedDivision())
				if(!current.getDivision().isBrotherPresent(current)) //TODO review isBrotherPresent Method input (list would be more logic)
					current.setErrorTag(TrackingFeedback.BROTHER_CELL_NOT_FOUND.numeric_code);
			
		}
	}

	private void reportTrackingResults() {
		System.out.println(
				"\nTracking completed for "+stGraph.size()+" frames:"+
						"\n\t "+stGraph.getFrame(0).size()+" cells in first frame");

		if(DO_DIVISION_CHECK){
			System.out.println("\t "+countDivisions()+" divisions recognized");
			System.out.println("\t "+countEliminations()+" eliminiations recognized");
		}
	}

	private int countEliminations() {
		int tot_eliminations = 0;

		for(int i=0; i < stGraph.size(); i++)
			tot_eliminations += stGraph.getFrame(i).getEliminationNo();
		return tot_eliminations;
	}

	private int countDivisions() {
		int tot_divisions = 0;

		for(int i=0; i < stGraph.size(); i++)
			tot_divisions += stGraph.getFrame(i).getDivisionNo();
		return tot_divisions;
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
			
			if(VERBOSE && groom.getTrackID() == follow_ID){
				
				System.out.println("Prefered brides of "+ follow_ID +" are:");
				
				for(ComparableNode b: grooms.get(groom)){
					Node next = b.getNode();
					System.out.println(
							"[" + Math.round(next.getCentroid().getX()) + 
							"," + Math.round(next.getCentroid().getY()) +
							"] : "+ b.getValue());
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
//					if(old_groom.getTrackID() == 286 && time_point == 27)
//						System.out.println("Alarm! Your bride might get stolen!");
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
	 * Rescue mechanism which analyzes lost grooms, i.e. cells 
	 * from previous frames which could not be linked to a cell
	 * in the currently analyzed frame (time_point). 
	 * 
	 * It either recognized a SWAP event, i.e. a cell is assigned
	 * to the wrong cell in a future frame and creates two 
	 * untracked cells as result.
	 * 
	 * Or a normal lost due to excessive movement. The latter event
	 * is reviewed by neighborhood consistency.
	 * 
	 * @param unmarried_brides non assigned nodes from the currently resolved frame
	 * @param unmarried_grooms non assigned nodes from previous frames
	 * @param time_point currently resolved time point no / frame no.
	 *   
	 */
	private void groom_rescue(Stack<Node> unmarried_brides, Stack<Node> unmarried_grooms, int time_point){

		groomLoop:
			while(!unmarried_grooms.empty()){

				//get the last correspondence of the groop
				//NOTICE: this could be at any time point previous to the current
				Node last_correspondence = getMostRecentCorrespondence(time_point, unmarried_grooms.pop());

				//skip if node is on boundary
				if(last_correspondence.onBoundary())
					continue;
				
				ArrayList<Node> ancestor_neighbors = new ArrayList<Node>();
				for(Node n: last_correspondence.getNeighbors())
					if(n.getFirst() != null)
						ancestor_neighbors.add(n.getFirst());
					
				//Assumption 1: several alternative might be available
				ComparableNode bestSWAP = new ComparableNode(null, 0.0);
				Node swapNeighbor = null;
				Node swapUntracked = null;

				Node bestRescNode = null;
				int bestRescVal = 0;

				//Check neighbors for untracked cells at time_point
				for(Node neighbor: last_correspondence.getNeighbors()){

					//obtain neighbor's last known correspondence
					Node futureN = getMostRecentCorrespondence(time_point + 1, neighbor);

					//if correspondence is in current frame analyze its N.
					if(futureN.getBelongingFrame().getFrameNo() != time_point)
						continue;

					for(Node futureNN: futureN.getNeighbors()){

						//if neighbor is untracked analyze it for SWAP or RESCUE
						if(unmarried_brides.contains(futureNN)){	

							Node untracked = futureNN;
							
							double neighbor2untracked = DistanceOp.distance(
									neighbor.getCentroid(),
									untracked.getCentroid());

							double lost2untracked = DistanceOp.distance(
									last_correspondence.getCentroid(),
									untracked.getCentroid());

							double neighbor2future = DistanceOp.distance(
									neighbor.getCentroid(),
									futureN.getCentroid());

							double lost2future = DistanceOp.distance(
									last_correspondence.getCentroid(),
									futureN.getCentroid());


							//check for assignment swap assuming that the following conditions should hold
							
							//assumption 1: the previous neighbor cell center is closer to the untracked cell
							//				than to the currently assigned cell correspondence
							
							boolean swap_condition_1 = neighbor2future > neighbor2untracked;
							//assumption 2: the lost cell is closer to the currently neighbor assigned correspondence
							//				rather than to the untracked cell
							boolean swap_condition_2 = lost2future < lost2untracked;

							if(swap_condition_1 && swap_condition_2){
								
								System.out.println("SWAP candidate: "+neighbor.getTrackID()+"<>"+last_correspondence.getTrackID());

								//check if current swap better than previous
								//check fit neighbor <> untracked
								Geometry nIu = neighbor.getGeometry().intersection(untracked.getGeometry());
								double nIuFIT = nIu.getArea() / neighbor.getGeometry().getArea();

								//check fit lost <> neighbor's correspondence
								Geometry lIc = last_correspondence.getGeometry().intersection(futureN.getGeometry());
								double lIcFIT = lIc.getArea() / last_correspondence.getGeometry().getArea();

								//create comparable Node with the sum of fits
								ComparableNode candidate = new ComparableNode(neighbor, nIuFIT + lIcFIT);

								//compare solutions and keep best
								if(candidate.compareTo(bestSWAP) > 0){
									bestSWAP = candidate;
									swapNeighbor = futureN;
									swapUntracked = untracked;
								}
								

							}
							else{

								//check for normal lost and rank according to intersection
								//of neighbors between lost and untracked
								
								int shared_neighbors = 0;
								
								for(Node n: untracked.getNeighbors()){
									if(n.getFirst() == null)
										continue;
									
									if(ancestor_neighbors.contains(n.getFirst()))
										shared_neighbors++;
									
									if(n.hasObservedDivision()){
										Division d = n.getDivision();
										
										if(d.isMother(n))
											continue;
										else
											if(ancestor_neighbors.contains(d.getMother().getFirst()))
												shared_neighbors++;
									}
								}
								
								System.out.println("RESC candidate:"+last_correspondence.getTrackID()+">"+neighbor.getTrackID()+"("+shared_neighbors+")");

								//safety check: at least 4 neighbors should be shared
								if(shared_neighbors > 3 && bestRescVal < shared_neighbors){
									bestRescVal = shared_neighbors;
									bestRescNode = untracked; 
								}
							}
						}
					}
				}

				//Swap wins over normal lost
				if(bestSWAP.getNode() != null){

					Node neighbor = bestSWAP.getNode();

					forceCorrespondence(swapNeighbor, last_correspondence);
					forceCorrespondence(swapUntracked, neighbor);

					//update TrackingFeedback with field NOTHING_TO_REPORT(-1)
					swapUntracked.setErrorTag(-1);

					unmarried_brides.remove(swapUntracked);

					System.out.println("  Reverted likely SWAP between "+
							last_correspondence.getTrackID()+" and "+neighbor.getTrackID());

					continue groomLoop;

				}

				if(bestRescNode != null){

					Node rescued = bestRescNode;

					updateCorrespondence(rescued, last_correspondence);
					unmarried_brides.remove(rescued);

					System.out.println("   Rescued "+
							last_correspondence.getTrackID());

					continue;
				}


				//End point, cell could not be rescued and is tagged as lost
				last_correspondence.setErrorTag(TrackingFeedback.LOST_IN_NEXT_FRAME.numeric_code);
			}

	}
	
	/**
	 * Division recognition based on the following assumption:
	 * 
	 * 1: brother cell must be direct neighbor
	 * 2: mother cell should be the last correspondence of brother cell and at preceding frame
	 * 3: mother cell cannot divide twice
	 * 4: mother cell should share the biggest intersection with the untracked cell
	 * [currently off] 5: mother's area should be bigger than the child's combined area 
	 * 6: both children should be covered by at least 60% by the mother cell
	 * 
	 * @param unmarried_brides
	 * @param unmarried_grooms
	 * @param time_point
	 */
	private void division_recognition(Stack<Node> unmarried_brides, Stack<Node> unmarried_grooms, int time_point){
		
		
		while(!unmarried_brides.empty()){
			
			//find likely brother & mother
			Node untracked = unmarried_brides.pop();
	
//			if(untracked.onBoundary())
//				continue;

			ComparableNode bestBrotherCandidate = new ComparableNode(null, 0.0);
			
			//assumption 1: brother must be direct neighbor
			List<Node> neighbors = untracked.getNeighbors();

			for(Node brotherCandidate: neighbors){
				if(brotherCandidate.hasPrevious()){
					
//					System.out.println("Analyzing brother: "+brotherCandidate.getTrackID());

					//assumption 2: mother cell should be the last correspondence of brother cell 
					//              and at preceding frame 
					//TODO weak, what if division happens close by and distorts previous image
					Node motherCandidate = brotherCandidate.getPrevious();
					int mother_frame_no = motherCandidate.getBelongingFrame().getFrameNo();
					int expected_frame_no = time_point - 1;
					if(mother_frame_no != expected_frame_no){
//						System.out.println(
//								"\t failed for assumption 2: mother cell belongs to frame "+
//										mother_frame_no+" instead of "+expected_frame_no);
						
						continue;
					}
					//assumption 3: mother cell cannot divide twice
					if(motherCandidate.hasObservedDivision()){
//						System.out.println("\t failed for assumption 3");
						continue;
					}

					//assumption 4: mother cell should share the biggest intersection with the untracked cell
					Geometry motherIntersection = untracked.getGeometry().intersection(
							motherCandidate.getGeometry());
					
					ComparableNode candidate = new ComparableNode(brotherCandidate, motherIntersection.getArea());
					
					if(candidate.compareTo(bestBrotherCandidate) > 0)
						bestBrotherCandidate = candidate;

				}
			}
					

			//no brother found
			if(bestBrotherCandidate.getNode() == null)
				continue;
			
			Node brother1 = untracked;
			Node brother2 = bestBrotherCandidate.getNode();
			Node mother = brother2.getPrevious();
			
			//Geometry acronyms
			//TODO extend use downwards
			Geometry b1 = brother1.getGeometry();
			Geometry b2 = brother2.getGeometry();
			Geometry m = mother.getGeometry();
			
//			//assumption 5: mother's area should be bigger than the child's combined area	
//			double combined_area = brother1.getGeometry().getArea() + brother2.getGeometry().getArea();
//			double mother_area = mother.getGeometry().getArea();
//			
//			if( mother_area < combined_area * 0.8)
//				continue;

			//assumption 6: both children should be covered by at least 60% by the mother cell
			Geometry ib1 = b1.intersection(m);
			Geometry ib2 = b2.intersection(m);
			
			final double coverage_factor = 0.6;
			
			
			if((ib1.getArea()/b1.getArea()) < coverage_factor)
				continue;
			
			if((ib2.getArea()/b2.getArea()) < coverage_factor)
				continue;
			
			//TODO 6 could be extended with a maximial factor too. The two area
			//overlaps should be coherent. This could avoid recognizing segmentation errors
			//as divisions
			
			//assumption 7: the cell center of the mother should be within 
			//				the rectangle formed by the children's cell center
			//				coordinates, i.e. rectangle[ccB1x,ccB1y, ccB2x,ccB2y].contains(ccM)

			// maybe do also with dot products,
			// i.e. same direction but higher abs.val
			// dot(B1>B2; B1>M) and dot(B2>B1; B2>M) , REQUIRES NORM!
			
			// or enought checking x, y intervals!
			// between xB1 and xB2 or between yB1 and yB2 ? 
			// if(b1.getCentroid().getX() )
			
			
			//mark as successful division
			Division division = new Division(mother , brother1, brother2, tracking_id);
			tracking_id = tracking_id + 2;
			
			System.out.println(division.toString());
		
		}
		
	
	}

	/**
	 * Analyze nodes that could not be associated because of a 
	 * a faulty tracking (artificial or seg. based)
	 * or a division event
	 * 
	 * @param unmarried Nodes that have not been associated in the linking process
	 * @param time_point Time point being analyzed
	 */
	private void analyze_unmarried(Map<String, Stack<Node>> unmarried, int time_point) {
		
		//obtain separate unmarried stacks
		Stack<Node> unmarried_brides = unmarried.get("brides");
		Stack<Node> unmarried_grooms = unmarried.get("grooms");
		
		System.out.println(" uB:"+ unmarried_brides.size());
		System.out.println(" uG:"+ unmarried_grooms.size());
		
		//Rescue of lost grooms due to excessive movement or swaps
		if(DO_SWAP_RESC_CHECK)
			groom_rescue(unmarried_brides, unmarried_grooms, time_point);
	
		//Check for divisions
		if(DO_DIVISION_CHECK)
			division_recognition(unmarried_brides, unmarried_grooms, time_point);
		
	}

	/**
	 * Method to order the candidates based on the distance computed
	 * by the evaluation method. Given the ComparableNode object the
	 * standard Arrays.sort method can be used.
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
						
					case AREA_DIFF_WITH_MIN_DISTANCE:
						
						candidate_dist = DistanceOp.distance(
								voted_centroid,
								current_cell_center);
						
						//compute difference in area
						double area_candidate = voted.getGeometry().getArea();
						double area_current = current.getGeometry().getArea();
						
						double area_difference = Math.abs(area_candidate - area_current);
						double normalized_area_diff = area_difference/area_candidate;
						
						//alternative
						
						//compute the intersection between the two cell geometries
						intersection = current.getGeometry().intersection(voted.getGeometry());
						double normalized_overlap = intersection.getArea() / area_candidate;
	
						//final score
						
						
						System.out.println(
								voted.getTrackID()+
								" dist:"+candidate_dist+
								" to: ["+Math.round(current_cell_center.getX())+
								","+Math.round(current_cell_center.getY())+"]");
						
						System.out.println(
								voted.getTrackID()+" area:"+
										"\n\t"+area_candidate+
										"\n\t"+area_current+
										"\n\t"+normalized_area_diff+
										"\n\t"+1/normalized_overlap);
						
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
						
					case OVERLAP_WITH_MIN_DISTANCE:
						
						candidate_dist = DistanceOp.distance(
								voted_centroid,
								current_cell_center);
						
						//compute difference in area
						area_candidate = voted.getGeometry().getArea();
						area_current = current.getGeometry().getArea();
						
						//compute the intersection between the two cell geometries
						intersection = current.getGeometry().intersection(voted.getGeometry());
						normalized_overlap = intersection.getArea() / (area_candidate + area_current);
						
						weighted_candidateDistance = 
								lambda1 * candidate_dist +
								lambda2 * (1 / normalized_overlap);

						
						if(VERBOSE && voted.getTrackID() == follow_ID)
							System.out.println(
								voted.getTrackID()+
								" to: ["+Math.round(current_cell_center.getX())+
								","+Math.round(current_cell_center.getY())+"]:\n"+
								" dist:"+Math.round(candidate_dist) +
								" area:"+Math.round(1/normalized_overlap)+
								" (= "+ weighted_candidateDistance + ")");

						
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
								area_current = current.getGeometry().getArea();
								
								//compute the intersection between the two cell geometries
								intersection = current.getGeometry().intersection(voted.getGeometry());
								normalized_overlap = intersection.getArea() / (area_candidate + area_current);
								
								weighted_candidateDistance = 
										lambda1 * candidate_dist +
										lambda2 * (1 / normalized_overlap);
								
								if(VERBOSE && voted.getTrackID() == follow_ID)
									System.out.println(" dist:"+Math.round(candidate_dist) +
										" area:"+Math.round(1/normalized_overlap) + 
										" (= "+ weighted_candidateDistance + ")");
										
								if(min > weighted_candidateDistance)
									min = weighted_candidateDistance;

							}
						}

						group_value = min;
						
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

