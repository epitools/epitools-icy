/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.graphs;

import java.util.Iterator;
import java.util.ArrayList;

import org.jgrapht.alg.NeighborIndex;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.ListenableUndirectedGraph;
import org.jgrapht.graph.ListenableUndirectedWeightedGraph;

import plugins.davhelle.cellgraph.nodes.Cell;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Elimination;
import plugins.davhelle.cellgraph.nodes.Node;

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
public class FrameGraph extends ListenableUndirectedWeightedGraph<Node, DefaultWeightedEdge> {
	
	private NeighborIndex<Node, DefaultWeightedEdge> neighborList;
	private ArrayList<Division> divisions;
	private ArrayList<Elimination> eliminations;
	private int frame_no; 
	
	/**
	 * Constructor builds an empty ListenableUndirectedGraph at first
	 * and then addes the neighborList.
	 */
	public FrameGraph(int frame_no){
		super(DefaultWeightedEdge.class);
		
		//specify the frame no which the graph represents
		this.frame_no = frame_no;
		
		//create the neighborIndexList
		this.neighborList = new NeighborIndex<Node, DefaultWeightedEdge>(this);
		this.addGraphListener(neighborList);
		
		//initialize division list
		this.divisions = new ArrayList<Division>();
		this.eliminations = new ArrayList<Elimination>();
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
	
}
