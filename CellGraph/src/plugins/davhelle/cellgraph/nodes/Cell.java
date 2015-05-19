package plugins.davhelle.cellgraph.nodes;

import java.awt.Color;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.painters.CellMarkerOverlay;
import plugins.davhelle.cellgraph.painters.TrackingOverlay;


import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Node class to represent polygonal cells.
 * 
 * @author Davide Heller
 *
 */
public class Cell implements Node {
	
	/**
	 * JTS Polygon geometry representing the cell
	 */
	private Polygon geometry;
	
	/**
	 * Cache of centroid geometry to avoid redundant computation 
	 */
	private Point centroid;
	
	/**
	 * FrameGraph to which the node is linked 
	 */
	private final FrameGraph parent;
	
	/**
	 * Temporal linking: next occurrence in time
	 */
	private Node next;
	
	/**
	 * Temporal linking: previous occurrence in time
	 */
	private Node previous;
	
	/**
	 * Temporal linking: connection to first known occurrence 
	 */
	private Node first;
	
	/**
	 * Candidate list for tracking algorithms
	 */
	private List<Node> first_candidates;
	
	/**
	 * Temporal linking: time independent tracking id
	 */
	private int track_id;
	
	/**
	 * Error id (see {@link TrackingOverlay})
	 */
	private int errorTag;

	/**
	 * Flag if node is located on segmentation boundary see {@link BorderCells}
	 */
	private boolean is_on_boundary;
	
	/**
	 * Flag for division occurrence during the time lapse
	 */
	private boolean has_observed_division;
	
	/**
	 * Division object if node divides
	 */
	private Division division;
	
	/**
	 * Flag for generating division occurrence (i.e. node is an observed child node) 
	 */
	private boolean has_observed_origin;
	
	/**
	 * Division object if node is an observed child
	 */
	private Division origin;
	
	/**
	 * Flag for elimination occurrence during the time lapse
	 */
	private boolean has_observed_elimination;
	/**
	 * Elimination object if elimination occurs
	 */
	private Elimination elimination;
	
	/**
	 * Color tag created in combination with {@link CellMarkerOverlay}
	 */
	private Color color_tag;

	/**
	 * Initializes the Node type representing a Cell as Polygon 
	 * 
	 * @param cell_polygon JTS geometry representing the cell
	 * @param parent FrameGraph containing the node
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
		this.has_observed_origin = false;
		this.origin = null;
		
		//default elimination information
		this.has_observed_elimination = false;
		this.elimination = null;
		
		//color_tag, default black
		this.color_tag = null;
	}

	@Override
	public Point getCentroid() {
		return centroid;
	}

	@Override
	public Geometry getGeometry() {
		return geometry;
	}

	@Override
	public int getTrackID() {
		return track_id;
	}

	@Override
	public void setTrackID(int tracking_id) {
		this.track_id = tracking_id;
	}

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
	public FrameGraph getBelongingFrame() {
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
		// TODO implicit assumption that node has been inserted in a graph..
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
		if(division != null)
			this.has_observed_division = true;
		else
			this.has_observed_division = false;
	}

	@Override
	public Division getDivision() {
		return division;
	}
	
	@Override
	public int getErrorTag() {
		return errorTag;
	}

	@Override
	public void setErrorTag(int errorTag) {
		this.errorTag = errorTag;
	}

	@Override
	public boolean hasNext() {
		return (next != null);
	}

	@Override
	public boolean hasPrevious() {
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
		if(elimination != null)
			this.has_observed_elimination = true;
		else
			this.has_observed_elimination = false;
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
	
	@Override
	public boolean hasColorTag(){
		return this.color_tag != null;
	}

	@Override
	public int getFrameNo() {
		if(parent != null)
			return parent.getFrameNo();
		else
			return 0;
	}

	@Override
	public boolean hasObservedOrigin() {
		return has_observed_origin;
	}

	@Override
	public Division getOrigin() {
		return origin;
	}

	@Override
	public void setOrigin(Division origin) {
		this.origin = origin;
		if(origin != null)
			this.has_observed_origin = true;
		else
			this.has_observed_origin = false;
	}

}
