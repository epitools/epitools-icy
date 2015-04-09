/*=========================================================================
 *
 *  (C) Copyright (2012-2014) Basler Group, IMLS, UZH
 *  
 *  All rights reserved.
 *	
 *  author:	Davide Heller
 *  email:	davide.heller@imls.uzh.ch
 *  
 *=========================================================================*/

package plugins.davhelle.cellgraph.nodes;

import java.awt.Color;
import java.util.Arrays;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.misc.CantorPairing;

/**
 * @author Davide Heller
 *
 */
public class Edge extends DefaultWeightedEdge {

	private static final long serialVersionUID = 1L;
	
	//optional geometry field
	private Geometry geometry;
	private double value;
	private boolean touches_division;
	
	//linking fields
	private long trackId;
	private Edge next;
	private Edge previous;
	private FrameGraph frame;

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
	 * <br> Def. A tracked edge has both vertices tracked.
	 * 
	 * @param frame
	 * @return true if both vertices are tracked. False if either is not tracked <br>or the edge does not belong to the input graph
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
	
	public int getTrackHashCode(FrameGraph frame){
		
		//TODO: dangerous, this harms the reversibilty of the cantor function
		//also there is no safety check for the input being natural numbers
		if(!frame.containsEdge(this))
			return -1;

		int[] vertex_track_ids =  new int[2];

		Node source_node = frame.getEdgeSource(this);
		Node target_node = frame.getEdgeTarget(this);

		vertex_track_ids[0] = source_node.getTrackID();
		vertex_track_ids[1] = target_node.getTrackID();

		Arrays.sort(vertex_track_ids);

		int track_hash_code = Arrays.hashCode(vertex_track_ids);

		return track_hash_code;

	}
	
	public long getPairCode(FrameGraph frame){
		
		if(!frame.containsEdge(this))
			return -1;
		
		Node source_node = frame.getEdgeSource(this);
		Node target_node = frame.getEdgeTarget(this);

//		if(source_node.hasObservedDivision())
//			source_node = source_node.getDivision().getMother();
//		
//		if(target_node.hasObservedDivision())
//			target_node = target_node.getDivision().getMother();
		
		int a = source_node.getTrackID();
		int b = target_node.getTrackID();
		
		this.trackId = computePairCode(a, b);
		
		return trackId;
		
	}

	/**
	 * @param a
	 * @param b
	 * @return
	 */
	public static long computePairCode(int a, int b) {
		if(a<b)
			return CantorPairing.compute(a, b);
		else
			return CantorPairing.compute(b, a);
	}
	
	public static int[] getCodePair(long code){
		
		//TODO: proper -1 management
		
		if(code < 0){
			int[] invalid_pair = {-1,-1};
			return invalid_pair;
		}
		else
			return CantorPairing.reverse(code);
		
	}
	
	public boolean hasGeometry(){
		return geometry != null;
	}
	
	public void computeGeometry(FrameGraph frame){
		
		Node source = frame.getEdgeSource(this);
		Node target = frame.getEdgeTarget(this);
		
		Geometry source_geometry = source.getGeometry();
		Geometry target_geometry = target.getGeometry();
		
		if(source_geometry.intersects(target_geometry))
			this.geometry = source_geometry.intersection(target_geometry); 
		
	}
	
	/**
	 * @return the geometry
	 */
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * @param geometry the geometry to set
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
	 * @return the alternative_track_id
	 */
	public long getTrackId() {
		return trackId;
	}
	/**
	 * @param alternative_track_id the alternative_track_id to set
	 */
	public void setTrackId(long alternative_track_id) {
		this.trackId = alternative_track_id;
	}
	
	/**
	 * @return the next
	 */
	public Edge getNext() {
		return next;
	}
	/**
	 * @param next the next to set
	 */
	public void setNext(Edge next) {
		this.next = next;
	}
	/**
	 * @return the previous
	 */
	public Edge getPrevious() {
		return previous;
	}
	/**
	 * @param previous the previous to set
	 */
	public void setPrevious(Edge previous) {
		this.previous = previous;
	}
	public void setColorTag(Color colorTag) {
		this.colorTag = colorTag;		
	}
	public Color getColorTag() {
		return colorTag;		
	}
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
	 * @return the frame
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
