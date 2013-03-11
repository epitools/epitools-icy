package plugins.davhelle.cellgraph.graphs;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableUndirectedGraph;

import plugins.davhelle.cellgraph.nodes.NodeType;

/**
 * Tissue Graph represents the polygonal abstraction of a 
 * segmented image through a graph representation. For 
 * the purpose of higher usability a neighborList is inserted
 * as field and listener of the graph to give more comfort in
 * accessing the graph structure. Usually the segmentation
 * represented will be part of a series thus a field frame_no
 * is given.
 * 
 * @author Davide Heller
 *
 */
public class TissueGraph extends ListenableUndirectedGraph<NodeType, DefaultEdge> {
	
	private NeighborIndex<NodeType, DefaultEdge> neighborList;
	private int frame_no; 
	
	/**
	 * Constructor builds an empty ListenableUndirectedGraph at first
	 * and then addes the neighborList.
	 */
	public TissueGraph(int frame_no){
		super(DefaultEdge.class);
		
		//specify the frame no which the graph represents
		this.frame_no = frame_no;
		
		//create the neighborIndexList
		this.neighborList = new NeighborIndex<NodeType, DefaultEdge>(this);
		this.addGraphListener(neighborList);
	}
	
	public TissueGraph(){
		this(0);
	}
	
	/**
	 * Method to quickly access the neighbors of a node
	 * 
	 * @param vertex Node of which to extract the vertices
	 * @return List of neighboring vertices
	 */
	public java.util.List<NodeType> getNeighborsOf(NodeType vertex){
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
	
}
