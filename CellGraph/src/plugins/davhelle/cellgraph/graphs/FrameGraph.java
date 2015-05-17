package plugins.davhelle.cellgraph.graphs;

import java.util.ArrayList;
import java.util.Iterator;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;

import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Elimination;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Frame Graph represents the polygonal network abstraction of a 
 * skeleton image (frame) using a graph description. The nodes in
 * the graph are polygonal geometries and the edges represent a
 * neighborhood relationship based on a geometrical intersection.
 * Usually the segmentation represented will be part of a 
 * series thus a field frame_no is given.
 * 
 * @author Davide Heller
 *
 */
public class FrameGraph extends ListenableUndirectedWeightedGraph<Node, Edge> {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Rapid neighbor lookup list
	 */
	private NeighborIndex<Node, Edge> neighborList;
	
	/**
	 * List of dividing vertices in this frame (tracking required)
	 */
	private ArrayList<Division> divisions;
	/**
	 * List of eliminated vertices in this frame (tracking required)
	 */
	private ArrayList<Elimination> eliminations;
	/**
	 * Temporal ID of the frame if part of a series
	 */
	private int frame_no;
	/**
	 * Path of the skeleton file from which the frameGraph was generated
	 */
	private String file_source;
	/**
	 * Linear Ring describing the boundary of the vertex geometries
	 */
	private Geometry boundary; 
	
	/**
	 * Constructor builds an empty ListenableUndirectedGraph at first
	 * and then adds the neighborList.
	 */
	public FrameGraph(int frame_no){
		super(Edge.class);
		
		//specify the frame no which the graph represents
		this.frame_no = frame_no;
		this.file_source = "";
		
		//create the neighborIndexList
		this.neighborList = new NeighborIndex<Node, Edge>(this);
		this.addGraphListener(neighborList);
		
		//initialize division list
		this.divisions = new ArrayList<Division>();
		this.eliminations = new ArrayList<Elimination>();
		
		//initialize empty boundary
		this.boundary = null;
	}
	
	/**
	 * default graph with temporal index 0
	 */
	public FrameGraph(){
		this(0);
	}
	
	/**
	 * default graph which will be inserted in the stGraph
	 * 
	 * @param frame_no temporal series index
	 * @param stGraph parent spatio-temporal graph
	 */
	public FrameGraph(int frame_no,SpatioTemporalGraph stGraph){
		this(frame_no);
		stGraph.setFrame(this, frame_no);
	}
	
	/**
	 * @return iterator for all cells in the frame
	 */
	public Iterator<Node> iterator(){
		return this.vertexSet().iterator();
	}
	
	/**
	 * @return iterator for all divisions in the frame
	 */
	public Iterator<Division> divisionIterator(){
		return divisions.iterator();
	}
	
	/**
	 * @return iterator for all eliminations in the frame
	 */
	public Iterator<Elimination> eliminationIterator(){
		return eliminations.iterator();
	}
	
	/**
	 * Method to quickly access the neighbors of a node
	 * 
	 * @param vertex Node of which to extract the vertices
	 * @return List of neighboring vertices
	 */
	public java.util.List<Node> getNeighborsOf(Node vertex){
		return neighborList.neighborListOf(vertex);
	}
	
	/**
	 * Methods to obtain the number of nodes in the graph. E.g. Number
	 * of cells in the tissue represented.
	 * 
	 * @return Number of nodes in the graph
	 */
	public int size(){
		return this.vertexSet().size();
	}
	
	/**
	 * @param division division to be added to the graph
	 */
	public void addDivision(Division division){
		//safety check
		if(division.getTimePoint() == this.frame_no)
			//throw exception TODO
			this.divisions.add(division);
	}
	
	/**
	 * @return number of divisions in the frame
	 */
	public int getDivisionNo(){
		return divisions.size();
	}
	
	/**
	 * @return temporal series index of the frame
	 */
	public int getFrameNo(){
		return frame_no;
	}

	/**
	 * @param elimination elimination to add to the graph
	 */
	public void addElimination(Elimination elimination) {
		if(elimination.getTimePoint() == this.frame_no)
			this.eliminations.add(elimination);
	}
	
	/**
	 * @return number of eliminations in the frame
	 */
	public int getEliminationNo(){
		return eliminations.size();
	}
	
	/**
	 * Verifies if a certain track_id is prenent in the frame
	 * 
	 * @param track_id tracking id to be searched
	 * @return true if a vertex with the tracking id is present
	 */
	public boolean hasTrackID(int track_id){
		
		boolean has_track_id = false;
		for(Node n: this.vertexSet())
			if(n.getTrackID() == track_id){
				has_track_id = true;
			}
		
		return has_track_id;
	}
	
	/**
	 * Retrieves node with a certain tracking id
	 * 
	 * @param track_id tracking id of the vertex to be extracted
	 * @return vertex with the tracking id, if id not found null
	 */
	public Node getNode(int track_id){
		for(Node n: this.vertexSet())
			if(n.getTrackID() == track_id)
				return n;
		
		return null;
	}

	/**
	 * Sets the path of the origin of the frameGraph
	 * 
	 * @param file_name path of the original file
	 */
	public void setFileSource(String file_name) {
		this.file_source = file_name;
	}
	
	/**
	 * @return path of the origin file
	 */
	public String getFileSource() {
		return this.file_source;
	}
	
	/**
	 * @return true if the frame graph has a specified path of the origin file
	 */
	public boolean hasFileSource(){
		return !this.file_source.isEmpty();
	}
	
	/**
	 * Checks if the frame graph contains an edge with a certain id
	 * 
	 * @param track_id edge tracking id
	 * @return true if an edge with the id has been found
	 */
	public boolean hasEdgeTrackId(long track_id){
		int[] node_ids = Edge.getCodePair(track_id);
		Node[] nodes = new Node[2];
		for(int i=0; i<2; i++){
			if(hasTrackID(node_ids[i]))
				nodes[i] = getNode(node_ids[i]);
			else
				return false;
		}
		
		return this.containsEdge(nodes[0],nodes[1]); 
	}
	
	/**
	 * Retrieves the edge with a certain tracking id
	 * 
	 * @param track_id edge tracking id to be retrieved
	 * @return edge with the specified tracking id, otherwise null (not found)
	 */
	public Edge getEdgeWithTrackId(long track_id){
		int[] node_ids = Edge.getCodePair(track_id);
		Node[] nodes = new Node[2];
		for(int i=0; i<2; i++)
			nodes[i] = getNode(node_ids[i]);
		
		return this.getEdge(nodes[0], nodes[1]);
	}

	/**
	 * Specify the geometrical boundary of the frame
	 * 
	 * @param boundary boundary geometry
	 */
	public void setBoundary(Geometry boundary) {
		this.boundary = boundary;		
	}
	
	/**
	 * @return the boundary of the object
	 */
	public Geometry getBoundary(){
		return boundary;
	}
	
	/**
	 * @return true if the frame has a specified boundary object
	 */
	public boolean hasBoundary(){
		return boundary != null;
	}
}
