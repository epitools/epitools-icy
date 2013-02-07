package plugins.davhelle.cellgraph;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableUndirectedGraph;


/**
 * CellCorner represents a cell corner in a cell tissue retrieved by
 * an image segmentation. It is also used as Node for the relative Graph
 * which is mend to represent the entire tissue segmentation. Belonging
 * cells are found by traversing the graph through the cycle methods.
 * 
 * @author Davide Heller
 * @version prototype 0.1
 *
 */
public class CellCorner{
	
	/**
	 * Class fields
	 * vtk_idx	Index of the cell corner in the original vtk polydata structure
	 * coor		coordinates of the cell corner (open for both 2D or 3D development)
	 * cell_hashes	contains the cells the corner belongs to after cycle search	
	 */
	private int vtk_idx;
	private double[] coor;
	private ArrayList<Integer> cell_hashes; 
	
	
	/**
	 * Constructor with void cell_hashes list
	 * 
	 * @param vtk_index
	 * @param coordinates
	 */
	public CellCorner(int vtk_index, double[] coordinates){
		vtk_idx = vtk_index;
		coor = coordinates;
		cell_hashes = new ArrayList<Integer>();
	}
	
	
	/**
	 * Add a belonging cell to the CellCorner
	 * @param hash
	 */
	public void add_cell_hash(int hash){
		if(!cell_hashes.contains(hash))
			cell_hashes.add(hash);
	}
	
	/**
	 * Obtain the list of cells the CellCorner currently belongs to
	 * @return belonging cells
	 */
	public ArrayList<Integer> get_cell_hashes(){
		//int[] cell_array = new int[cell_hashes.size()];
		//for(int i=0; i<cell_hashes.size(); i++)
		//	cell_array[i] = cell_hashes[i];
		return cell_hashes;
	}
	
	/**
	 * Retrieve coordinates of the CellCorner (Dimension independent)
	 * @return coordinates
	 */
	public double[] getPosition(){
		return coor;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override 
	public String toString(){
		return "V"+vtk_idx;
	}
	
	/**
	 * Obtain index of the CellCorner
	 * @return index
	 */
	public int getIdx(){
		return vtk_idx;
	}
	
	/**
	 * Obtain z coordinate from the Cell coordinate
	 * @return z component or 0 if not specified
	 */
	public double getZ(){
		if(coor.length == 3)
			return coor[2];
		else
			return 0;
	}
	
	public int getX(){
		return (int)coor[0];
	}
	
	public int getY(){
		return (int)coor[1];
	}
	
	/**
	 * Obtain a Point2D representation of the CellCorner(2D)
	 * @return java.awt.Point2D of CellCorner  
	 */
	public Point2D getPoint2d(){
		return new Point2D.Double(coor[0], coor[1]);
	}
	
	/**
	 * Check equality by comparing CellCorner's coordinates
	 * @param other CellCorner to compare with
	 * @return TRUE/FALSE according to identity
	 */
	public boolean equals(CellCorner other){
		//usage of java.utils.Arrays.equals would be better
		return (this.coor == other.coor);
	}
	
	
	/**
	 * Faster comparison by just looking to vtk_index identity
	 * @param other	CellCorner to compare with
	 * @return
	 */
	public boolean cmp_idx(CellCorner other){
		return this.vtk_idx == other.vtk_idx;
	}
	
	
	/**
	 * Computes the angle between 3 CellConers in the specified order
	 * this<-center->destination, the arrows(->) indicate the vectors
	 * which will be used to compute the dot-product, i.e. the angle.
	 *
	 * Note that this method currently only considers the x,y components!
	 * 
	 * @param center CellCorner at which the angle is measured
	 * @param destination CellCorner to which the the angle extends
	 * @return Angle between this<-center->destination
	 *
	 */
	public double compute_angle(CellCorner center, CellCorner destination){
		return Geometry.computeAngle2D(center.getPosition(), this.getPosition(), destination.getPosition());
	}
	
	//TODO make it z compatible -> criteria might not work as expected if real z coordinate is given
	/**
	 * Computes the cross product of the vectors joining the three CellCorners
	 * the two vectors are specified as follows: center->this & center->destination
	 * @param center CellCorner from which the vectors start
	 * @param destination CellCorner destination for second vector
	 * @return vector[center->this] x vector[center->destination]
	 */
	public double compute_crossProductZ(CellCorner center, CellCorner destination){
		return Geometry.computeCPz(center.getPosition(), this.getPosition(), destination.getPosition());
	}
	
	
	/**
	 * Searches for the belonging cells of the calling CellCorner according to the following
	 * heuristic defined in cycle search recursive subroutine<br>

			
						Hash tag is based on the ordered
	 *   polygon list since 

	 * REMARK: This prototype heuristic might not be very efficient nor the best approach.
	 * 			literature might suggest other ways, also on non geometric basis (graph theory)
	 * 			keywords: elementary cycles, atomic cycles, simplex search
	 * 
	 * @param g	JgraphT graph data structure TODO change to more general type
	 * @param cell_map	Map to store the found cell/polygons that belong toh
	 */
	public void compute_cycles(ListenableUndirectedGraph<CellCorner, DefaultEdge> g, Map<Integer, ArrayList<Integer>> cell_map){
		if(g.containsVertex(this)){

			
			//step 1: start at a node and get its neighbors
			
			//System.out.println("Identifying cells of Cell-Corner "+this.toString());
			
			for(CellCorner neighbor: this.getNeighbors(g)){
				
				//TODO might consider to change this to an ArrayList
				//step 2:Create a List in which the cell's corner will be saved
				LinkedList<CellCorner> polygon = new LinkedList<CellCorner>();

				//step 3:Add the starting corner to the polygon
				polygon.add(this);
				
				//step 4:Add the remaining corners by traveling on it's neighbors
				neighbor.cycle_search(g, polygon);
				
				//step 4b: decide whether the polygon is closed by checking if an edge exist between first and last vertex
				if(!g.containsEdge(polygon.peekFirst(), polygon.peekLast()) || polygon.size() < 3)
					//System.out.print("NO "); //or discard it simply by
					continue; //TODO should be improved in order to avoid wasteful search
					
				
				//step 5:Save the cycle in an appropriate form, so that retrieval is efficient for future searches
				ArrayList<Integer> cycle_ids = new ArrayList<Integer>();

					//idea might be to just store the indices
				for(CellCorner cycle_element: polygon){
					cycle_ids.add(cycle_element.getIdx());
				}
				
				//Not needed anymore but useful to see the difference of hash creation
				//int cycle_hash_id = cycle_ids.hashCode();
				
				//Sorting the polygon list and then doing the hashCode allows same cycles to be recognized
				ArrayList<Integer> ord_cycle_ids = new ArrayList<Integer>(cycle_ids);				
				Collections.sort(ord_cycle_ids);
				
				int ord_cycle_hash_id = ord_cycle_ids.hashCode();
				
				for(CellCorner cycle_element: polygon){
					cycle_element.add_cell_hash(ord_cycle_hash_id);
				}
				
				//insert found cycle into map
				if(!cell_map.containsKey(ord_cycle_hash_id))
					//do not input ord_cycle_ids otherwise polygon order gets lost
					cell_map.put(ord_cycle_hash_id, cycle_ids);
				
				//output info about cycle
				//System.out.println(this.toString()+"["+this.coor[0]+" "+this.coor[1]+"]:Cycle "+ord_cycle_hash_id+" found: "+polygon.toString());	

			}
		}
	}
	
	
	/**
	 * Subroutine to deliver all CellCorner Neighbors of the calling CellCorner. Might be implemented
	 * as field in future releases to speed up the code. 
	 * 
	 * @param g graph on which to seek for the neighbors
	 * @return list of CellCorner neighbors, void list if none
	 */
	public CellCorner[] getNeighbors(ListenableUndirectedGraph<CellCorner, DefaultEdge> g){
		int v_degree = g.degreeOf(this);
		CellCorner[] my_neighbors = new CellCorner[v_degree];
		
		Set<DefaultEdge> edge_set = g.edgesOf(this);
		Iterator<DefaultEdge> edge_it = edge_set.iterator();
		
		for(int i=0; i<v_degree; i++){
			
			DefaultEdge next_e = edge_it.next();
			
			CellCorner next_v = g.getEdgeTarget(next_e);

			//select opposite vertex if next_v is current vertex (this)
			if(next_v.cmp_idx(this))
				next_v = g.getEdgeSource(next_e);
			
			my_neighbors[i] = next_v;
			
		}
		
		return my_neighbors;
		
	}
	
	
	
	/**
	 * Recursive subroutine to search for the connected cells of a CellCorner.
	 *   returns the entire polygon/cell [list of CellCorners] as result of a complete recursion
	 *   order is the list is stringent to rebuilt the polygon. The search is based on the following
	 *   heuristic
	 *   
	        "Go always left" - 
			Choose the next CellCorner that has positive a positive cross product with respect
			to the CellCorner you come from and also has the smallest angle with respect to 
			the same CellCorners as above. Do this until you complete the circle.
	 *   
	 *  REMARK: This prototype heuristic might not be very efficient nor the best approach.
	 * 			literature might suggest other ways, also on non geometric basis (graph theory)
	 * 			keywords: elementary cycles, atomic cycles, simplex search
	 * 
	 * @param g graph to search on
	 * @param p	current polygon(cell), depending on the recursion level it might not be complete
	 */
	public void cycle_search(ListenableUndirectedGraph<CellCorner, DefaultEdge> g, LinkedList<CellCorner> p){
		
		//System.out.println("Visiting "+this.toString());
		
		//TODO remove tests for exception handling
		if(!g.containsVertex(this)){
			System.out.println(this.toString()+" does not exist in g");
			return;
		}
		
		if(p.size() == 0){
			System.out.println("Polygon list is void! This should not happen!");
			return;
		}
		
		//TODO fix this exception, how to teach the algorithm that it's an outer cell?
		//by increasing the threshold bigger wholes are found, producing meaningless objects though
		if(p.size() > 15){
			//System.out.println("System looped or outside"+p.toString());
			return;
		}
		
		//System.out.println("Looking at cycles of:"+p.peekFirst().toString());
		//System.out.println(this);
		
		if(p.size() != 1 && g.containsEdge(p.peekFirst(), this)){
			//test for subcycles (leave out first and last, i.e. obviously connected)
			//alternatively one might think of a solution with sets intersection..
			if(p.size() > 2){
				List<CellCorner> sub_list = p.subList(1, p.size()-2);
				Iterator<CellCorner> sub_it = sub_list.iterator();
				while(sub_it.hasNext())
					if(g.containsEdge(sub_it.next(), this))
						return;
			}
			
			//cycle complete -> polygon found
			//System.out.println("I found the polygon!"+this.toString()+" concludes the cycle");
			p.add(this);
			return;
		}
		else{
			
			//check how many edges the vertex has
			int v_degree = g.degreeOf(this);
			Set<DefaultEdge> edge_set;
			Iterator<DefaultEdge> edge_it;
			DefaultEdge next_e;
			CellCorner next_v;
			CellCorner last_v = p.peekLast();
			
			//according to the degree we search the neighbors
			switch (v_degree){
			
			case 0:
				//This should never happen
				System.out.println("Fatal error: edgeless vertex has been choosen!");
				return;
			
			case 1:
				//should have been resolved beforehand by graph improvement
				p.add(this);
				//System.out.println("Ended in degree 1 vertex");
				return;
				
			case 2:
				//junction vertex obtain next vertex and step forward with recursion
				edge_set = g.edgesOf(this);
				
				//get next node
				edge_it = edge_set.iterator();
				
				next_e = edge_it.next();
				
				//skip vertex you're coming from
				if(g.getEdgeSource(next_e).cmp_idx(last_v) || g.getEdgeTarget(next_e).cmp_idx(last_v) )
					next_e = edge_it.next();
				
				//retrieve 				
				next_v = g.getEdgeTarget(next_e);

				//select opposite vertex if next_v is current vertex (this)
				if(next_v.cmp_idx(this))
					next_v = g.getEdgeSource(next_e);
				
				//add current vertex to polygon
				p.add(this);
				
				//proceed in recursion
				next_v.cycle_search(g, p);				
				return;
				
			default:
				//vertex has more than 1 edge
				edge_set = g.edgesOf(this);

				//get the iterator over all other outgoing edges
				edge_it = edge_set.iterator();
				
				//Prepare object for best corner readout
				double smallest_angle = 2*Math.PI;		
				CellCorner v_min_angle = null;
				
				//Cycle undiscovered Edges				
				while(edge_it.hasNext()){
					
					next_e = edge_it.next();
					
					//skip vertex you're coming from
					if(g.getEdgeSource(next_e).cmp_idx(last_v) || g.getEdgeTarget(next_e).cmp_idx(last_v) )
						continue;
					
					//retrieve 				
					next_v = g.getEdgeTarget(next_e);

					//select opposite vertex if next_v is current vertex (this)
					if(next_v.cmp_idx(this))
						next_v = g.getEdgeSource(next_e);
					
					//if the the node is present in the polygon list -> subcycle: SHOULD NOT HAPPEN
					if(p.contains(next_v))
						return;
						
					boolean is_left_turn = true;
					double cpz = 0;
					double angle = 0;
					
					if(is_left_turn){
						//do cross product to understand on which side the next vertex is wrt last one
						cpz = last_v.compute_crossProductZ(this,  next_v);
						//compute angle towards next vertex wrt last vertex
						angle = last_v.compute_angle(this, next_v);
					}
					else{
						cpz = next_v.compute_crossProductZ(this, last_v);
						angle = next_v.compute_angle(this, last_v);
					}
					
					
					//TODO refine special condition of 90 and 180 degree situation, 
					//at the moment this setting appears to work being able to close cycles
					//that contain the considered edge but not allowing other searches to 
					//make an unsure decision. UNABLE TO RECOGNIZE CELLS WITH MORE THAN ONE 90/180 CASE
//					if(angle == Math.PI/2){
//						System.out.println("90 degree angle occurred:"+angle+"\n\t"+last_v.getPoint2d()+"\n\t"+this.getPoint2d()+"\n\t"+next_v.getPoint2d());
//						p.add(this);
//						return;
//					}
						
					
					if(cpz == 0){
						//System.out.println("180 degree angle occurred:"+angle+"\n\t"+last_v.getPoint2d()+"\n\t"+this.getPoint2d()+"\n\t"+next_v.getPoint2d());
						p.add(this);
						return;
					}
									
					
					//cross product decides side(l/r), dot product the angle size
//					if(this.toString().endsWith("V4")){
//						System.out.println(angle);
//					}
					
					if(cpz < 0){
						//System.out.print(" - X");
						//right hand side angle defined as negative
						//angle += Math.PI;
						//better to invert as if we would record the angle from the other side
						angle = 2*Math.PI - angle;
					}
					
//					if(this.toString().endsWith("V4")){
//						System.out.println("Changed to:"+angle);
//					}
					
					//Choose the smallest left-side-angle
					if(angle < smallest_angle){
						v_min_angle = next_v;
						smallest_angle = angle;
					}
					
				}
				
				//add current vertex to polygon
				p.add(this);
				
				//Safety check that recurrence is executed on proper CellCorner
				if(g.containsVertex(v_min_angle))
					v_min_angle.cycle_search(g, p);
				else
					System.out.println("Error:"+v_min_angle.toString()+"neighbour of"+this.toString()+" is not part of the graph");
				
				return;					
					
			}
								
		}

	}
	
	
	/**
	 * Test the graph creation with the JgraphT library and the successive graph traversal
	 * 
	 * @param args	not used
	 */
	public static void main(String [] args){
		
		//Create a undirected graph with the JGraphT library
		ListenableUndirectedGraph<CellCorner, DefaultEdge> g = new ListenableUndirectedGraph<CellCorner, DefaultEdge>(DefaultEdge.class);
		
		//Custom Graph for testing
		//			    V4
		//		        |
		//			V6--V2--V5
	    //		  / |   |   |
		//		V8	V7--V1--V3
		
		
		//create CellCorners -> add third void coordinate and try out math method
		CellCorner v1 = new CellCorner(1,new double[]{0.0,0.0,0.0});
		CellCorner v2 = new CellCorner(2,new double[]{0.0,1.0,0.0});
		CellCorner v3 = new CellCorner(3,new double[]{2.0,0.0,0.0});
		CellCorner v4 = new CellCorner(4,new double[]{0.0,3.0,0.0});
		CellCorner v5 = new CellCorner(5,new double[]{1.0,1.5,0.0});
		CellCorner v6 = new CellCorner(6,new double[]{-1.0,1.5,0.0});
		CellCorner v7 = new CellCorner(7,new double[]{-1.5,-1.5,0.0});
		CellCorner v8 = new CellCorner(8,new double[]{-2.5,-3.5,0.0});
		
		
		//insert CellCorners
		g.addVertex(v1);
		g.addVertex(v2);
		g.addVertex(v3);
		g.addVertex(v4);
		g.addVertex(v5);
		g.addVertex(v6);
		g.addVertex(v7);
		g.addVertex(v8);
		
		//ArrayList<CellCorner> vertices = new ArrayList<CellCorner>({v1,v2,v3,v4,v5,v5,v6,v7,v8});		
		//System.out.println(g.addEdge(vertices[1], vertices[0]));
		
		//connect CellCorners
		g.addEdge(v2,v1);
		g.addEdge(v1,v3);
		g.addEdge(v2,v5);
		g.addEdge(v2,v4);
		g.addEdge(v5,v3);
		g.addEdge(v2,v6);
		g.addEdge(v6,v7);
		g.addEdge(v7,v1);
		g.addEdge(v6,v8);
		
		//Test 1: simple graph testing, Test 2:cycle test
		int run_test = 2;
		
		if(run_test==1){
			//output graph
			System.out.println(g.toString());

			//Display edges of a and the distance towards them
			Set<DefaultEdge> outcoming_edges = g.edgesOf(v1);

			Iterator<DefaultEdge> edge_it = outcoming_edges.iterator();

			while(edge_it.hasNext()){
				DefaultEdge v_edge = edge_it.next();
				CellCorner v_neighbor = g.getEdgeTarget(v_edge);
				System.out.print("a connects to: "+v_neighbor.toString());
				//compute distance between edges
				System.out.println(" and distances:"+Geometry.length(v1.getPosition(), v_neighbor.getPosition()));
			}

			//output the angle between {a,b,d} and {a,b,f}
			System.out.println("Angle{a,b,d}:"+v1.compute_angle(v2,v4));
			System.out.println("Angle{a,b,f}:"+v1.compute_angle(v2,v5));
			System.out.println("Angle{a,b,h}:"+v1.compute_angle(v2,v6));

			//output the z-component of the cross product of {a<-b->d}, {a<-b->f}, {a<-b->h}
			System.out.println("CP{a,b,d}'s z:"+v1.compute_crossProductZ(v2,v4));
			System.out.println("CP{a,b,f}'s z:"+v1.compute_crossProductZ(v2,v5));
			System.out.println("CP{a,b,h}'s z:"+v1.compute_crossProductZ(v2,v6));
		}
		
		if(run_test==2){

			//cycle through all nodes (degree>2) of the graph 
			//the limitation should avoid results as:
			// Cycle 1122287581(918164611) found: [V8, V6, V7, V1, V2, V6]
			//might be better solution to specific problem! TODO
			
			Set<CellCorner> all_v = g.vertexSet();

			Iterator<CellCorner> v_it = all_v.iterator();

			//Store the found cells TODO maybe also their neighbors right away
			Map<Integer, ArrayList<Integer>> cell_map = new HashMap<Integer, ArrayList<Integer>>();
			// TODO think also about the possibility of directly inputting nodes or is it useful to
			// have a separate NODE list? e.g. another HashMap with the vtk_ids?

			while(v_it.hasNext()){
				CellCorner v_next = v_it.next();
				if(g.degreeOf(v_next)>2)
					v_next.compute_cycles(g,cell_map);
			}

			//check who belongs to who (can an iterator be used twice? e.g. rewinded)
			v_it = all_v.iterator();

			while(v_it.hasNext()){
				CellCorner v_next = v_it.next();
				System.out.println("Cells of "+v_next.toString()+":"+v_next.get_cell_hashes().toString());
			}

			System.out.println("Cells found"+cell_map.toString());
		
		}
		
					
	}
	
}
