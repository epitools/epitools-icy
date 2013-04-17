package plugins.davhelle.cellgraph.tracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.ComparableNode;
import plugins.davhelle.cellgraph.nodes.Node;

public class NearestNeighborTracking extends TrackingAlgorithm{
	
	private int linkrange;

	public NearestNeighborTracking(SpatioTemporalGraph spatioTemporalGraph, int linkrange) {
		super(spatioTemporalGraph);
		this.linkrange = linkrange;
	}
	
	@Override
	public void track(){
		
		//for every frame extract all particles
		for(int time_point=0;time_point<stGraph.size(); time_point++){
			System.out.println("Linking frame "+time_point);
			

			/* 
			 * Given the mutually unique relationship between the first frame
			 * and the current frame. All not assigned nodes can be 
			 * classified as either:
			 * + lost or delaminated (first frame only) 
			 * + segmentation errors
			 * + divisions (current frame only).
			 * 
			 * First node candidates are computed at each time point to 
			 * solve the marriage problem for the current frame.
			*/
			
			//Build two maps to store the evaluated candidates (evaluation <> distance from center)
			Map<Node, List<ComparableNode>> first_map = new HashMap<Node, List<ComparableNode>>();
			Map<Node, List<ComparableNode>> current_map = new HashMap<Node, List<ComparableNode>>();;

			//Update both parties
			for(Node current: stGraph.getFrame(time_point).vertexSet()){
				
				//reference towards which distances are computed in evaluation
				Point current_cell_center = current.getCentroid();
				
				//based on prior candidate voting decide node based on closest neighbor
				if(time_point > 0){
					
					List<Node> candidates = current.getParentCandidates();
					
					while(candidates.size() > 0){
						
						Iterator<Node> candidate_it = candidates.iterator();

						Node voted = candidate_it.next();
						Node first = voted.getFirst(); //TODO Null check?
						candidate_it.remove();
						
						if(first == null)
							continue;
						
						int count = 1;
						double sum = DistanceOp.distance(
								voted.getCentroid(),
								current_cell_center);

						while(candidate_it.hasNext()){
							voted = candidate_it.next();
							if( voted.getFirst() == first){
								candidate_it.remove();
								sum +=  DistanceOp.distance(
										voted.getCentroid(),
										current_cell_center);
								count++;
							}
						}

						double avg = sum / count;
						
						if(!first_map.containsKey(first))
							first_map.put(first, new ArrayList<ComparableNode>());
						
						ComparableNode candidate_distance = new ComparableNode(current,avg);
						first_map.get(first).add(candidate_distance);
						
						if(!current_map.containsKey(current))
							current_map.put(current, new ArrayList<ComparableNode>());
						
						current_map.get(current).add(new ComparableNode(first, avg));
					}
				}
			}


			Map<Node, ArrayList<Node>> grooms = new HashMap<Node, ArrayList<Node>>();
			Stack<Node> unmarried_grooms = new Stack<Node>();

			for(Node first: first_map.keySet()){
				List<ComparableNode> grooms_list = first_map.get(first);
				int grooms_size = grooms_list.size();
				ComparableNode[] grooms_array = grooms_list.toArray(new ComparableNode[grooms_size]);
				//ascending sort
				Arrays.sort(grooms_array);
//				System.out.println(first.getTrackID()+":"+Arrays.toString(grooms_array));
				

				ArrayList<Node> ordered_grooms = new ArrayList<Node>();
				for(int i=0; i<grooms_size; i++)
					ordered_grooms.add(grooms_array[i].getNode());

				grooms.put(first, ordered_grooms);
				unmarried_grooms.push(first);

			}

			Map<Node, Node[]> brides = new HashMap<Node, Node[]>();

			for(Node first: current_map.keySet()){
				List<ComparableNode> grooms_list = current_map.get(first);
				int grooms_size = grooms_list.size();
				ComparableNode[] grooms_array = grooms_list.toArray(new ComparableNode[grooms_size]);
				//ascending sort
				Arrays.sort(grooms_array);

				Node[] ordered_grooms = new Node[grooms_size];
				for(int i=0; i<grooms_size; i++)
					ordered_grooms[i] = grooms_array[i].getNode();

				brides.put(first, ordered_grooms);
			}

			//					System.out.println("\tOrdered Candidates");

			Map<Node, Node> marriage = new HashMap<Node,Node>();

			ArrayList<Node> unmarried_brides = new ArrayList<Node>(brides.keySet());
			ArrayList<Node>	nochoice_grooms = new ArrayList<Node>();

			while(!unmarried_grooms.empty()){
				//take groom candidate from stack and try to assign
				Node groom = unmarried_grooms.pop();

				//get preference list of groom 
				Iterator<Node> bride_it = grooms.get(groom).iterator();
				boolean not_married = true;

				//loop util groom has preferences and is not married
				while(bride_it.hasNext() && not_married){

					//get bride candidate and mark her as visited
					Node bride = bride_it.next();
					bride_it.remove();

					//check if wanted bride is married at all
					if(!marriage.containsKey(bride)){
						if(!marriage.containsValue(groom)){
							marriage.put(bride, groom);	
							unmarried_brides.remove(bride);
							not_married = false;
						}
					}

					//if already married see if this is better fit
					else{
						Node old_groom = marriage.get(bride);
						Node[] preffered_grooms = brides.get(bride);
						for(int pref_idx = 0; pref_idx < preffered_grooms.length; pref_idx++ )
							if(preffered_grooms[pref_idx] == old_groom)
								//current husband has better rating
								break;
							else 
								if(preffered_grooms[pref_idx] == groom){
									//new husband has better rating!
									if(!marriage.containsValue(groom)){
										unmarried_grooms.push(old_groom);
										marriage.put(bride, groom);
										not_married = false;
										break;
									}
								}
					}
				}

				//if groom has no more bride candidates eliminate from list
				if(grooms.get(groom).isEmpty())
					nochoice_grooms.add(groom);

			}

			//finally update node correspondences
			for(Node bride: marriage.keySet()){
				Node groom = marriage.get(bride);
				updateCorrespondence(bride, getMostRecentCorrespondence(bride, groom));
			}
		


			//Compute candidates for successive nodes in [linkrange] time frames (one and only assignment x node x frame)

			for(Node current: stGraph.getFrame(time_point).vertexSet())

				for(int i=1; i <= linkrange && time_point + i < stGraph.size(); i++)

					for(Node next: stGraph.getFrame(time_point + i).vertexSet())

						if(next.getGeometry().contains(current.getCentroid()))

							next.addParentCandidate(current);



			//+	division object
			//+ clean up code
			//+ document & divide into methods, rename variables. 

		}
	}
}

