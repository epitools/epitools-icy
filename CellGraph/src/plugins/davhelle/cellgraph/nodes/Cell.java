/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
/**
 * 
 */
package plugins.davhelle.cellgraph.nodes;

import java.awt.Color;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

import plugins.davhelle.cellgraph.graphs.FrameGraph;


import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Node class to represent each cell as polygon.
 * 
 * 
 * @author Davide Heller
 *
 */
public class Cell implements Node {
	
	//geometry to abstract the Node
	private Polygon geometry;
	
	//avoid redundant centroid computation
	private Point centroid;
	
	//FrameGraph to which 
	private final FrameGraph parent;
	
	//tracking information
	private Node next;
	private Node previous;
	private Node first;
	private List<Node> first_candidates;
	private int track_id;
	private int errorTag;

	//boundary information
	private boolean is_on_boundary;
	
	//division information
	private boolean has_observed_division;
	private Division division;
	
	//elimination information
	private boolean has_observed_elimination;
	private Elimination elimination;
	
	//cell color tag
	private Color color_tag;

	/**
	 * Initializes the Node type representing a Cell as Polygon 
	 */
	public Cell(Polygon cell_polygon, FrameGraph parent) {
		this.parent = parent;
		this.geometry = cell_polygon;
		this.centroid = geometry.getCentroid();
		
		//default for untracked cell
		this.next = null;
		this.previous = null;
		this.track_id = -1;
		this.first = null;
		this.first_candidates = new ArrayList<Node>();
		
		//default boundary condition
		this.is_on_boundary = false;
		
		//default division information
		this.has_observed_division = false;
		this.division = null;
		
		//default elimination information
		this.has_observed_elimination = false;
		this.elimination = null;
		
		//color_tag, default black
		this.color_tag = Color.black;
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.NodeType#getCentroid()
	 */
	@Override
	public Point getCentroid() {
		return centroid;
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.NodeType#getGeometry()
	 */
	@Override
	public Geometry getGeometry() {
		return geometry;
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.NodeType#getProperty()
	 */
	@Override
	public Object getProperty() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void setProperty(Object property) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.NodeType#getTrackID()
	 */
	@Override
	public int getTrackID() {
		return track_id;
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.NodeType#setTrackID(int)
	 */
	@Override
	public void setTrackID(int tracking_id) {
		this.track_id = tracking_id;
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.NodeType#toShape()
	 */
	@Override
	public Shape toShape() {
		ShapeWriter writer = new ShapeWriter();
		return writer.toShape(geometry);
	}

	@Override
	public void setNext(Node next_node) {
		this.next = next_node;		
	}

	@Override
	public Node getNext() {
		return this.next;
	}

	@Override
	public boolean onBoundary() {
		return is_on_boundary;
	}

	@Override
	public void setBoundary(boolean onBoundary) {
		this.is_on_boundary = onBoundary;
		
	}

	@Override
	public Node getPrevious() {
		return this.previous;
	}

	@Override
	public void setPrevious(Node last_node) {
		this.previous = last_node;		
	}

	@Override
	public boolean hasObservedDivision() {
		return has_observed_division;
	}

	@Override
	public void setObservedDivision(boolean will_cell_divide) {
		this.has_observed_division = will_cell_divide;
	}

	@Override
	public FrameGraph getBelongingFrame() {
		// TODO Auto-generated method stub
		return parent;
	}

	public Node getFirst() {
		return first;
	}

	public void setFirst(Node first) {
		this.first = first;
	}

	@Override
	public List<Node> getNeighbors() {
		// TODO dangerous! what if not yet inserted in graph?
		return parent.getNeighborsOf(this);
	}

	@Override
	public void addParentCandidate(Node first) {
		first_candidates.add(first);
	}

	@Override
	public List<Node> getParentCandidates() {
		return first_candidates;
	}

	@Override
	public void setDivision(Division division) {
		this.division = division;
		this.has_observed_division = true;
	}

	@Override
	public Division getDivision() {
		// TODO Auto-generated method stub
		return division;
	}
	
	@Override
	/**
	 * @return the errorTag
	 */
	public int getErrorTag() {
		return errorTag;
	}

	@Override
	/**
	 * @param errorTag the errorTag to set
	 */
	public void setErrorTag(int errorTag) {
		this.errorTag = errorTag;
	}

	@Override
	public boolean hasNext() {
		return (next != null);
	}

	@Override
	public boolean hasPrevious() {
		// TODO Auto-generated method stub
		return (previous != null);
	}

	@Override
	public void setGeometry(Geometry node_geometry) {
		//TODO safety check if the update is really a polygon
		this.geometry = (Polygon)node_geometry;
		//update centroid information as well
		this.centroid = geometry.getCentroid();
		
	}

	@Override
	public void setElimination(Elimination elimination) {
		this.elimination = elimination;
		this.has_observed_elimination = true;
	}

	@Override
	public Elimination getElimination() {
		return elimination;
	}

	@Override
	public boolean hasObservedElimination() {
		return this.has_observed_elimination;
	}
	
	@Override
	public Color getColorTag() {
		return color_tag;
	}

	@Override
	public void setColorTag(Color color_tag) {
		this.color_tag = color_tag;
	}

}
