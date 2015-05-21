package plugins.davhelle.cellgraph.tracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.ComparableNode;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * 
 *  stable-marriage-problem algorithm (Gale–Shapley) source:
 *  http://en.wikipedia.org/wiki/Stable_marriage_problem
 * 
 * @author Davide Heller
 *
 */
public class NearestNeighborTracking extends GraphTracking{
	
	/**
	 * Initializes Neighbor tracking
	 * 
	 * 
	 * @param spatioTemporalGraph Spatio-temporal graph to be tracked/linked
	 * @param linkrange the maximum no. of frames the node information is projected ahead
	 */
	public NearestNeighborTracking(SpatioTemporalGraph spatioTemporalGraph, int linkrange, double lambda1, double lambda2) {
		super(spatioTemporalGraph, linkrange, lambda1, lambda2);
	}

	/**
	 * Linking algorithm based on the stable marriage problem. 
	 * The nodes in the current frame are addressed as "brides"
	 * while the brooms are the candidates from the first frame (except for divisions).
	 * 
	 * @param time_point of frame to be linked
	 * @return returns 2 Stacks containing the unlinked nodes, accessible trough a map interface ("brides", "grooms")
	 */
	public Map<String, Stack<Node>> linkCandidates(Map<Node, List<ComparableNode>> grooms,Map<Node, List<ComparableNode>> brides) {

		//Order the evaluated candidates in ascending manner (smallest distances first) 
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
			
			if(super.VERBOSE && groom.getTrackID() == super.follow_ID){
				
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
		
			//List conversion
			candidate_list = new ArrayList<ComparableNode>(Arrays.asList(candidate_array));
			
			//update (requires change from immutable java.util.Arrays$Arraylist to mutable ArrayList object)
			node_map.put(first, candidate_list);

		}
	}
}