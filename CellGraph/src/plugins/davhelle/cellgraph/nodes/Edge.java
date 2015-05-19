package plugins.davhelle.cellgraph.nodes;

import java.awt.Color;

import org.jgrapht.graph.DefaultWeightedEdge;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.misc.CantorPairing;
import plugins.davhelle.cellgraph.overlays.EdgeColorTagOverlay;

import com.vividsolutions.jts.geom.Geometry;

/**
 * The Edge class represents the connectivity in the {@link FrameGraph} class
 * 
 * In a practical application it symbolizes the junction between two cells
 * 
 * @author Davide Heller
 *
 */
public class Edge extends DefaultWeightedEdge {

	private static final long serialVersionUID = 1L;
	
	/**
	 * JTS geometry describing the Edge
	 */
	private Geometry geometry;
	/**
	 * Value associated to an edge
	 */
	private double value;
	/**
	 * Flag highlighting if one of the edge vertices divides
	 */
	private boolean touches_division;
	
	//tracking fields
	/**
	 * Temporal tracking: time independent id associated to the edge 
	 */
	private long trackId;
	/**
	 * Temporal tracking: next occurrence of the edge id in time
	 */
	private Edge next;
	/**
	 * Temporal tracking: previous occurrence of the edge id in time
	 */
	private Edge previous;
	/**
	 * Parent graph to which the edge is associated
	 */
	private FrameGraph frame;

	/**
	 * Color tag associated to {@link EdgeColorTagOverlay}
	 */
	private Color colorTag;
	
	
	/**
	 * An Edge For StGraphs
	 */
	public Edge() {
		this.geometry = null;
		this.value = -1.0;
		this.touches_division = false;
		this.trackId = -1;
		this.next = null;
		this.previous = null;
		this.colorTag = null;
		this.frame = null;
	}
	/**
	 * @return the value
	 */
	public double getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(double value) {
		this.value = value;
	}

	
	/**
	 * Checks whether the vertices of the edge are tracked.
	 * <br> 
	 * 
	 * Def. A tracked edge has both vertices tracked.
	 * 
	 * @param frame FrameGraph of the edge
	 * @return true if both vertices are tracked. False if not or wrong input frame
	 */
	public boolean canBeTracked(FrameGraph frame){
		boolean is_tracked = true;
		
		if(!frame.containsEdge(this))
			return false;
		
		if(frame.getEdgeSource(this).getTrackID() == -1)
			is_tracked = false;
		
		if(frame.getEdgeTarget(this).getTrackID() == -1)
			is_tracked = false;
		
		return is_tracked;
	}
	
	/**
	 * Tracking code associated to the edge. Based on the cantor
	 * pairing of the vertex ids. See {@link CantorPairing} for more details.
	 * 
	 * @param frame FrameGraph to which the edge belongs
	 * @return tracking id
	 */
	public long getPairCode(FrameGraph frame){
		
		if(!frame.containsEdge(this))
			return -1;
		
		Node source_node = frame.getEdgeSource(this);
		Node target_node = frame.getEdgeTarget(this);
		
		int a = source_node.getTrackID();
		int b = target_node.getTrackID();
		
		//this.trackId = computePairCode(a, b);
		
		return computePairCode(a, b);
		
	}

	/**
	 * Returns the {@link CantorPairing} of the ordered ids
	 * 
	 * @param a id 1
	 * @param b id 2
	 * @return cantor pairing of ordered ids
	 */
	public static long computePairCode(int a, int b) {
		if(a<b)
			return CantorPairing.compute(a, b);
		else
			return CantorPairing.compute(b, a);
	}
	
	/**
	 * Return the tracking ids of the vertices associated to the trackin id
	 * 
	 * @param code tracking id from the cantor pairing
	 * @return pair of generating vertex ids
	 */
	public static int[] getCodePair(long code){
		
		//TODO: proper -1 management
		
		if(code < 0){
			int[] invalid_pair = {-1,-1};
			return invalid_pair;
		}
		else
			return CantorPairing.reverse(code);
		
	}
	
	/**
	 * @return true if there is an associated JTS geometry
	 */
	public boolean hasGeometry(){
		return geometry != null;
	}
	
	/**
	 * Computes the geometrical intersection between the two vertex geometries
	 * and sets the result as geometry of the edge
	 * 
	 * @param frame FrameGraph to which the Edge belongs
	 */
	public void computeGeometry(FrameGraph frame){
		
		Node source = frame.getEdgeSource(this);
		Node target = frame.getEdgeTarget(this);
		
		Geometry source_geometry = source.getGeometry();
		Geometry target_geometry = target.getGeometry();
		
		if(source_geometry.intersects(target_geometry))
			this.geometry = source_geometry.intersection(target_geometry); 
		
	}
	
	/**
	 * @return the JTS geometry of the edge
	 */
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * @param geometry the JTS geometry representing the edge
	 */
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	
	/**
	 * @return True if either nodes is a dividing node
	 */
	public boolean hasDivision() {
		return touches_division;
	}
	
	public void setDivision(boolean touches_division){
		this.touches_division = touches_division;
	}
	
	/**
	 * @return the tracking id
	 */
	public long getTrackId() {
		return trackId;
	}
	/**
	 * @param alternative_track_id an alternative tracking id to set
	 */
	public void setTrackId(long alternative_track_id) {
		this.trackId = alternative_track_id;
	}
	
	/**
	 * @return the next temporal occurrence of the edge
	 */
	public Edge getNext() {
		return next;
	}
	/**
	 * @param next the next temporal occurrence of the edge
	 */
	public void setNext(Edge next) {
		this.next = next;
	}
	/**
	 * @return the previous occurrence of the edge
	 */
	public Edge getPrevious() {
		return previous;
	}
	
	/**
	 * @param previous the previous occurrence of the edge
	 */
	public void setPrevious(Edge previous) {
		this.previous = previous;
	}
	
	/**
	 * @param colorTag define a colorTag associated with the edge
	 */
	public void setColorTag(Color colorTag) {
		this.colorTag = colorTag;		
	}
	
	/**
	 * @return the color tag associated with the edge
	 */
	public Color getColorTag() {
		return colorTag;		
	}
	
	/**
	 * @return true if the edge has an associated color tag
	 */
	public boolean hasColorTag() {
		return colorTag != null;		
	}
	
	/**
	 * @return true if a future reference to the edge exists
	 */
	public boolean hasNext(){
		return next != null;
	}
	
	/**
	 * @return true if a previous reference to the edge exist
	 */
	public boolean hasPrevious(){
		return previous != null;
	}
	
	/**
	 * @return the frame to which the edge is inserted
	 */
	public FrameGraph getFrame() {
		return frame;
	}
	
	/**
	 * @param frame the frame in which Edge was inserted
	 */
	public void setFrame(FrameGraph frame) {
		if(frame.containsEdge(this))
			this.frame = frame;
	}

}
