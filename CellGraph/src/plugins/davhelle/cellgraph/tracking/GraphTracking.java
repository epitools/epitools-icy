package plugins.davhelle.cellgraph.tracking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.ComparableNode;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Elimination;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.operation.distance.DistanceOp;

/**
 * GraphBased Tracking Algorithm. The main idea of this type of 
 * of algorithms is to find candidates for each cell in the new
 * and in the old frame and match them by a bipartite graph
 * matching algorithm.<br>
 * 
 * Main procedure:<br>
 * <pre>
 * {@code
	public void track(){
		
		//link time points and propagate their information
		for(int time_point = 0; time_point < stGraph.size(); time_point++){
			System.out.println("\n*** Linking frame "+time_point+" ***\n");
			
			if(time_point > 0){
				
				//Build two maps to store the candidates
				Map<Node, List<ComparableNode>> grooms = new HashMap<Node, List<ComparableNode>>();
				Map<Node, List<ComparableNode>> brides = new HashMap<Node, List<ComparableNode>>();;

				//Evaluate the candidates according to a distance criteria
				evaluateCandidates(grooms, brides, time_point);
				
				//Link the candidates according to a matching algorithm
				Map<String, Stack<Node>> unmarried = linkCandidates(grooms,brides);
				
				//Analyze unmarried/unlinked nodes
				analyze_unmarried(unmarried, time_point);
			}
			
			//add candidates to the successive frames
			propagateTimePoint(time_point);	
		}
		
		reviewDivisionsAndEliminations();
		
		reportTrackingResults();
		
		stGraph.setTracking(true);
	}
 * }
 * </pre>
 * 
 * @author Davide Heller
 *
 */
public abstract class GraphTracking extends TrackingAlgorithm {
	
	/**
	 * Holds the number of frames the node information is projected ahead
	 */
	private final int linkrange;

	/**
	 * Distance criteria with which to choose the best candidate
	 */
	private final DistanceCriteria distance_criteria;
	
	/**
	 * Flag whether to detect divisions
	 */
	private final boolean DO_DIVISION_CHECK;
	
	/**
	 * Flag whether to check for track SWAPS
	 */
	private final boolean DO_SWAP_RESC_CHECK;
	
	/**
	 * Flag to Print additional information of the tracking process
	 */
	protected final boolean VERBOSE;
	
	/**
	 * Additional variable to VERBOSE to follow a specific cell's tracking 
	 */
	protected final int follow_ID;

	/**
	 * Holds the proportion of area increase that a dividing cell must have
	 * registered to be acknowledged as division.
	 */
	private final double increase_factor;
	
	/**
	 * First candidate evaluation parameter:
	 * how much does the distance between the two candidates's centroid matter
	*/
	private double lambda1;
	
	/**
	 * Second candidate evaluation parameter:
	 * how much does the normalized area overlap matter
	 */
	private double lambda2;
	
	/**
	 * Division heuristic parameter
	 * sets the percentage of overlap
	 * between the mother and the daughter
	 * cells
	 */
	private double coverage_factor;	
	
	
	/**
	 * Caching for performance increase
	 * on repetitive intersection operation 
	 * on the same geometry.
	 */
	private PreparedGeometryFactory cached_factory;

	
	/**
	 * Initializes Graph based Tracking
	 * 
	 * @param spatioTemporalGraph Spatio-temporal graph to be tracked/linked
	 * @param linkrange the maximum no. of frames the node information is projected ahead
	 */
	/**
	 * @param spatioTemporalGraph
	 * @param linkrange
	 * @param lambda1 weight for the centroid candidate to candidate distance
	 * @param lambda2 weight for the normalized area difference
	 * 
	 */
	public GraphTracking(SpatioTemporalGraph spatioTemporalGraph, int linkrange, double lambda1, double lambda2) {
		super(spatioTemporalGraph, true);
		this.linkrange = linkrange;
		this.distance_criteria = DistanceCriteria.OVERLAP_WITH_MIN_DISTANCE;
		this.DO_DIVISION_CHECK = true;
		this.DO_SWAP_RESC_CHECK = true;
		this.increase_factor = 1.3;
		this.VERBOSE = false;
		this.follow_ID = 109;
		this.lambda1 = lambda1;
		this.lambda2 = lambda2;
		this.coverage_factor = 0.5;
		
		cached_factory = new PreparedGeometryFactory();
	}
	
	@Override
	public void track(){
		
		//link time points and propagate their information
		for(int time_point = 0; time_point < stGraph.size(); time_point++){
			System.out.println("\n*** Linking frame "+time_point+" ***\n");
			
			if(time_point > 0){
				
				//Build two maps to store the candidates
				Map<Node, List<ComparableNode>> grooms = new HashMap<Node, List<ComparableNode>>();
				Map<Node, List<ComparableNode>> brides = new HashMap<Node, List<ComparableNode>>();;

				//Evaluate the candidates according to a distance criteria
				evaluateCandidates(grooms, brides, time_point);
				
				//Link the candidates according to a matching algorithm
				Map<String, Stack<Node>> unmarried = linkCandidates(grooms,brides);
				
				//Analyze unmarried/unlinked nodes
				analyze_unmarried(unmarried, time_point);
			}
			
			//add candidates to the successive frames
			propagateTimePoint(time_point);	
		}
		
		reviewDivisionsAndEliminations();
		
		reportTrackingResults();
		
		stGraph.setTracking(true);
	}

	/**
	 * - Reviews all events classified as a divisions <br>
	 * - Reviews all new entry cells <br>
	 * - Assigns permanent loss labels to permanently lost cells <br>
	 * - Highlights possible Segmentation mistakes <br>
	 */
	private void reviewDivisionsAndEliminations() {
		
		//is cell missing in next frame i.e. TrackingFeedback...
		int elimination_detection_time_limit = stGraph.size() - 1;
		for(int time_point = 0; time_point < elimination_detection_time_limit; time_point++)
		{
			for(Node cell: stGraph.getFrame(time_point).vertexSet())
			{
				if(cell.getErrorTag() == TrackingFeedback.LOST_IN_NEXT_FRAME.numeric_code){
					//if so, is it permanent (depends also on no. of tracked frames)?
					if(!cell.hasNext() && !cell.onBoundary())
					{						
						Elimination cell_elimination = new Elimination(cell);
						System.out.println(cell_elimination.toString());
					}			
				}
			}
		}
		
		//TODO more stable division revision needed:
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
	 * PROPAGATION 
	 * Add candidates to nodes in next [linkrange] time frames
	 * 
	 * @param time_point
	 */
	private void propagateTimePoint(int time_point) {
		
		//TODO Update with STR-tree for spatial indexing
		
		for(Node current: stGraph.getFrame(time_point).vertexSet())
		{	
			PreparedGeometry cached_current = cached_factory.create(current.getGeometry());
			
			//only propagate what has been successfully in current frame.
			if(current.getTrackID() != -1)
			{	
				for(int i=1; i <= linkrange && time_point + i < stGraph.size(); i++)
					for(Node next: stGraph.getFrame(time_point + i).vertexSet()) {
						Geometry next_geometry = next.getGeometry();
						
						if(cached_current.intersects(next_geometry))
						{
							
							Geometry intersection = next_geometry.intersection(current.getGeometry());
							if(intersection.getArea() > 10){
								next.addParentCandidate(current);

								if( VERBOSE && current.getTrackID() == follow_ID)
									System.out.printf("%d propagated to [%.0f,%.0f] @ frame %d",
											follow_ID,
											next.getCentroid().getX(),
											next.getCentroid().getY(),
											time_point+i);
							}
						}
					}								
			}
			
			//also check in case of a division that the brother cell is present
			if(current.hasObservedDivision())
				if(!current.getDivision().isBrotherPresent(current)) //TODO review isBrotherPresent Method input (list would be more logic)
					if(current.getDivision().wasBrotherEliminated(current))
						current.setErrorTag(TrackingFeedback.BROTHER_CELL_ELIMINATED.numeric_code);
					else
						current.setErrorTag(TrackingFeedback.BROTHER_CELL_NOT_FOUND.numeric_code);
		}
		
	}

	/**
	 * Verbose output for tracking results
	 */
	private void reportTrackingResults() {
		System.out.println(
				"\nTracking completed for "+stGraph.size()+" frames:"+
						"\n\t "+stGraph.getFrame(0).size()+" cells in first frame");

		if(DO_DIVISION_CHECK){
			System.out.println("\t "+countDivisions()+" divisions recognized");
			System.out.println("\t "+countEliminations()+" eliminiations recognized");
		}
	}

	/**
	 * @return number of eliminations detected
	 */
	private int countEliminations() {
		int tot_eliminations = 0;

		for(int i=0; i < stGraph.size(); i++)
			tot_eliminations += stGraph.getFrame(i).getEliminationNo();
		return tot_eliminations;
	}

	/**
	 * @return number of divisions detected
	 */
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
	public abstract Map<String, Stack<Node>> linkCandidates(Map<Node, List<ComparableNode>> grooms, Map<Node, List<ComparableNode>> brides);
	
	/**
	 * Rescue mechanism which analyzes lost grooms, i.e. cells 
	 * from previous frames which could not be linked to a cell
	 * in the currently analyzed frame (time_point). 
	 * 
	 * It either recognized a SWAP event, i.e. a cell is assigned
	 * to the wrong cell in a future frame and creates one 
	 * untracked cell as result.
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
				
				if(VERBOSE)
					System.out.printf("**Rescue attempt for: %d\n",last_correspondence.getTrackID());
				
				ArrayList<Node> ancestor_neighbors = new ArrayList<Node>();
				for(Node n: last_correspondence.getNeighbors())
					if(n.getFirst() != null)
						ancestor_neighbors.add(n.getFirst());
					
				//Assumption 1: several alternative scenarios might be available.
				//>Rank every scenario according to multiple criteria (see below)
				ComparableNode bestSWAP = new ComparableNode(null, 0.0);
				Node swapNeighbor = null;
				Node swapUntracked = null;

				Node bestRescNode = null;
				int bestRescVal = 0;
				
				//Check neighbors for untracked cells at time_point
				for(Node neighbor: last_correspondence.getNeighbors())
				{
					//obtain neighbor's last known correspondence
					Node futureN = getMostRecentCorrespondence(time_point + 1, neighbor);

					//if correspondence is in current frame analyze its N.
					if(futureN.getBelongingFrame().getFrameNo() != time_point)
						continue;

					ComparableNode best_untracked = new ComparableNode(null, Double.MAX_VALUE);
					
					for(Node futureNN: futureN.getNeighbors())
					{

						//if neighbor is untracked analyze it for SWAP or RESCUE
						if(unmarried_brides.contains(futureNN)){	

							Node untracked = futureNN;
							
							double neighbor2untracked = DistanceOp.distance(
									neighbor.getCentroid(),
									untracked.getCentroid());
							
							ComparableNode current_untracked = new ComparableNode(untracked, neighbor2untracked);
							if(current_untracked.compareTo(best_untracked) < 0)
								best_untracked = current_untracked;

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
							
							//Check difference in neighborhoods for current neighbor
							Set<Node> old_neighbor_set = new HashSet<Node>(); 
							for(Node node: neighbor.getNeighbors())
								if(node.getFirst() != null)
									old_neighbor_set.add(node.getFirst());

							Set<Node> new_neighbor_set = new HashSet<Node>();
							for(Node node: futureN.getNeighbors())
								if(node.getFirst() != null)
									new_neighbor_set.add(node.getFirst());
							
							old_neighbor_set.retainAll(new_neighbor_set);
							new_neighbor_set.removeAll(old_neighbor_set);
							
//							System.out.printf("\t%d>%d:%d conserved, %d new/lost\n",
//									last_correspondence.getTrackID(),
//									neighbor.getTrackID(),
//									old_neighbor_set.size(),
//									new_neighbor_set.size());
							
							old_neighbor_set.removeAll(ancestor_neighbors);
							
//							System.out.printf("\t%d>%d:%d neighbors left if lost's neighbors pruned\n", 
//									last_correspondence.getTrackID(),
//									neighbor.getTrackID(),
//									old_neighbor_set.size());
							
							if(old_neighbor_set.size() == 0){
								System.out.printf("\t>REVERTING SWAP: [%d,%d] (untracked:%.0f,%.0f):\n",
										last_correspondence.getTrackID(),
										neighbor.getTrackID(),
										best_untracked.getNode().getCentroid().getX(),
										best_untracked.getNode().getCentroid().getY());
								//assign SWAP max priority
								bestSWAP = new ComparableNode(neighbor, Double.MAX_VALUE);
								swapNeighbor = futureN;
								swapUntracked = untracked;
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
	
			//more stringent condition to avoid FP due to segmentation problems on the boundary
//			if(untracked.onBoundary())
//				continue;

			ComparableNode bestBrotherCandidate = new ComparableNode(null, 0.0);
			
			//assumption 1: brother must be direct neighbor
			List<Node> neighbors = untracked.getNeighbors();

			for(Node brotherCandidate: neighbors){
				if(brotherCandidate.hasPrevious()){
					
					if(VERBOSE) System.out.println("Analyzing brother: "+brotherCandidate.getTrackID());

					//assumption 2: mother cell should be the last correspondence of brother cell 
					//              and at preceding frame 
					//TODO weak, what if division happens close by and distorts previous image
					Node motherCandidate = brotherCandidate.getPrevious();
					int mother_frame_no = motherCandidate.getBelongingFrame().getFrameNo();
					int expected_frame_no = time_point - 1;
					if(mother_frame_no != expected_frame_no){
						if(VERBOSE) System.out.println(
								"\t failed for assumption 2: mother cell belongs to frame "+
										mother_frame_no+" instead of "+expected_frame_no);
						
						continue;
					}
					//assumption 3: mother cell cannot divide twice
					if(motherCandidate.hasObservedDivision()){
						if(VERBOSE) System.out.println("\t failed for assumption 3");
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
			
			boolean use_condition_5 = false;
			//assumption 5: mother's area should be bigger than the child's combined area	
			if(use_condition_5){
				double combined_area = brother1.getGeometry().getArea() + brother2.getGeometry().getArea();
				double mother_area = mother.getGeometry().getArea();
				
				if( mother_area < combined_area * increase_factor)
					continue;
			}

			//assumption 6: both children should be covered by at least 60% by the mother cell
			Geometry ib1 = b1.intersection(m);
			Geometry ib2 = b2.intersection(m);
			
			if((ib1.getArea()/b1.getArea()) < coverage_factor){
				if(VERBOSE) System.out.println("Insufficient coverage b1: "+ib1.getArea()/b1.getArea());
				continue;
			}
			
			if((ib2.getArea()/b2.getArea()) < coverage_factor){
				if(VERBOSE) System.out.println("Insufficient coverage b2: "+ib2.getArea()/b2.getArea());
				continue;
			}
			
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
	 * Evaluate all node's candidate of a particular frame. Evaluation
	 * is intended as quantification of the cell center distance between 
	 * a candidate node and the node being considered.
	 * 
	 * Two Map are filled which represent the two classes which are to be matched. 
	 * Since with a give linkrange we connect multiple frames to a particular
	 * frame in the future. Distance are averaged if candidate node share the
	 * same ancestor (first) node.
	 * 
	 * see {@link DistanceCriteria} for more implementations
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
			
			//if no candidates are given add it as "lost bride"
			//could be set more stringent with frame_0_union.buffer(-10.0)
			if(candidates.size() == 0){
				if(frame_0_union.contains(current_cell_center) && !current.onBoundary())
					current_map.put(current, new ArrayList<ComparableNode>());
			}
			else{
				
				while(candidates.size() > 0){

					Iterator<Node> candidate_it = candidates.iterator();

					Node voted = candidate_it.next();
					Node first = voted.getFirst();
					candidate_it.remove();
					
					//Check whether the cell is part of a division, if yes to 
					//avoid that the candidate approach is biased by the mother
					//cell 
					if(voted.hasObservedDivision()){
						Division division = voted.getDivision();
						if(time_point > division.getTimePoint())
							if(division.isMother(voted)){
								//TODO eliminate all mother candidates and use hasOrigin instead of time_point check
								continue;
						}
					}
					
					Point voted_centroid = voted.getCentroid();

					
					//VIABILITY CHECK BASED ON FIRST FRAME GEOMETRY
					//Cell could be either new (division/seg.error), 
					//thus not associated to any first node 
					//TODO more stringent version with frame_0_union.buffer(-10.0)?
					if(first == null){
						if(frame_0_union.contains(voted_centroid) && !voted.onBoundary())
							current_map.put(current, new ArrayList<ComparableNode>());
						continue;
					}
					
					
					//compute a value for the entire first group
					double group_value = Double.MAX_VALUE;
					
					switch(distance_criteria){
					
					case OVERLAP_WITH_MIN_DISTANCE:
						
						if(VERBOSE && voted.getTrackID() == follow_ID)
							System.out.printf("%d to [%.0f,%.0f]:\n",
									voted.getTrackID(),
									current_cell_center.getX(),
									current_cell_center.getY());
						
						double candidate_dist = DistanceOp.distance(
								voted_centroid,
								current_cell_center);
						
						//compute difference in area
						double area_candidate = voted.getGeometry().getArea();
						double area_current = current.getGeometry().getArea();
						
						//compute the intersection between the two cell geometries
						Geometry intersection = current.getGeometry().intersection(voted.getGeometry());
						double normalized_overlap = intersection.getArea() / (area_candidate + area_current);
						double reciprocal_overlap = 1 / normalized_overlap;
						
						//time influence (maximally reduce candidate score by 20%)
						double time_multiplier = 0.5;
						
						//time distance (recent candidates should count more)
						int candidate_frame_no = voted.getBelongingFrame().getFrameNo();
						double time_difference = time_point - candidate_frame_no;
						double time_weight = 1 - (time_multiplier/time_difference);
						
						double weighted_candidateDistance = 
								lambda1 * candidate_dist +
								lambda2 * reciprocal_overlap;
						
						double time_weighted_candidateDistance = weighted_candidateDistance * time_weight;

						if(VERBOSE && voted.getTrackID() == follow_ID)
							System.out.printf("\t%.2f\t%.2f\t[dist:\t%.2f\tarea:\t%.2f\n",
									time_weighted_candidateDistance,
									weighted_candidateDistance,
									candidate_dist,
									reciprocal_overlap);
						
						
						weighted_candidateDistance = time_weighted_candidateDistance;
//							System.out.println(
//								voted.getTrackID()+
//								" to: ["+Math.round(current_cell_center.getX())+
//								","+Math.round(current_cell_center.getY())+"]:\n"+
//								" dist:"+Math.round(candidate_dist) +
//								" area:"+Math.round(1/normalized_overlap)+
//								" (= "+ weighted_candidateDistance + ")");

						
						double min = weighted_candidateDistance;
//						int candidate_no = 1;
//						double candidate_avg = weighted_candidateDistance;

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
								reciprocal_overlap = 1 / normalized_overlap;
								
								//time distance (recent candidates should count more)
								candidate_frame_no = voted.getBelongingFrame().getFrameNo();
								time_difference = time_point - candidate_frame_no;
								time_weight = 1 - (time_multiplier/time_difference);
								
								weighted_candidateDistance = 
										lambda1 * candidate_dist +
										lambda2 * reciprocal_overlap;
								
								time_weighted_candidateDistance = weighted_candidateDistance * time_weight;
								
								if(VERBOSE && voted.getTrackID() == follow_ID)
									System.out.printf("\t%.2f\t%.2f\t[dist:\t%.2f\tarea:\t%.2f\n",
											time_weighted_candidateDistance,
											weighted_candidateDistance,
											candidate_dist,
											reciprocal_overlap);
								
								weighted_candidateDistance = time_weighted_candidateDistance;
			
//									System.out.println(" dist:"+Math.round(candidate_dist) +
//										" area:"+Math.round(1/normalized_overlap) + 
//										" (= "+ weighted_candidateDistance + ")");
								
								
//								candidate_no++;
//								candidate_avg += weighted_candidateDistance;
										
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
