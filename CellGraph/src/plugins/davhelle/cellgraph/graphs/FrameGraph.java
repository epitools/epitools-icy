/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
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
 * Frame Graph represents the polygonal abstraction of a 
 * segmented image (frame) through a graph representation. For 
 * the purpose of higher usability a neighborList is inserted
 * as field and listener of the graph to give more comfort in
 * accessing the graph structure. Usually the segmentation
 * represented will be part of a series thus a field frame_no
 * is given.
 * 
 * @author Davide Heller
 *
 */
public class FrameGraph extends ListenableUndirectedWeightedGraph<Node, Edge> {
	
	private static final long serialVersionUID = 1L;
	
	private NeighborIndex<Node, Edge> neighborList;
	private ArrayList<Division> divisions;
	private ArrayList<Elimination> eliminations;
	private int frame_no;
	private String file_source;
	private Geometry boundary; 
	
	/**
	 * Constructor builds an empty ListenableUndirectedGraph at first
	 * and then addes the neighborList.
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
	
	public FrameGraph(){
		this(0);
	}
	
	public FrameGraph(int frame_no,SpatioTemporalGraph stGraph){
		this(frame_no);
		stGraph.setFrame(this, frame_no);
	}
	
	public Iterator<Node> iterator(){
		return this.vertexSet().iterator();
	}
	
	public Iterator<Division> divisionIterator(){
		return divisions.iterator();
	}
	
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
	
	public void addDivision(Division division){
		//safety check
		if(division.getTimePoint() == this.frame_no)
			//throw exception TODO
			this.divisions.add(division);
	}
	
	public int getDivisionNo(){
		return divisions.size();
	}
	
	public int getFrameNo(){
		return frame_no;
	}

	public void addElimination(Elimination elimination) {
		if(elimination.getTimePoint() == this.frame_no)
			this.eliminations.add(elimination);
	}
	
	public int getEliminationNo(){
		return eliminations.size();
	}
	
	public boolean hasTrackID(int track_id){
		
		boolean has_track_id = false;
		for(Node n: this.vertexSet())
			if(n.getTrackID() == track_id){
				has_track_id = true;
			}
		
		return has_track_id;
	}
	
	public Node getNode(int track_id){
		for(Node n: this.vertexSet())
			if(n.getTrackID() == track_id)
				return n;
		
		return null;
	}

	public void setFileSource(String file_name) {
		this.file_source = file_name;
	}
	
	public String getFileSource() {
		return this.file_source;
	}
	
	public boolean hasFileSource(){
		return !this.file_source.isEmpty();
	}
	
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
	
	public Edge getEdgeWithTrackId(long track_id){
		int[] node_ids = Edge.getCodePair(track_id);
		Node[] nodes = new Node[2];
		for(int i=0; i<2; i++)
			nodes[i] = getNode(node_ids[i]);
		
		return this.getEdge(nodes[0], nodes[1]);
	}

	public void setBoundary(Geometry boundary) {
		this.boundary = boundary;		
	}
	
	public Geometry getBoundary(){
		return boundary;
	}
	
	public boolean hasBoundary(){
		return boundary != null;
	}
}
