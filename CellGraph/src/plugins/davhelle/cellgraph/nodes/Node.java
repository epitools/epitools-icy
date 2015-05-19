/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.nodes;

import java.awt.Color;
import java.awt.Shape;
import java.util.List;

import plugins.davhelle.cellgraph.graphs.FrameGraph;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Node interface represents the vertex object for the FrameGraph.
 * 
 * Currently the main implementation is the {@link Cell} representing
 * a polygonal shape in a tissue.
 * 
 * By default it is assumed that there is a JTS Geometry field.
 * 
 * @author Davide Heller
 *
 */
public interface Node{
	
	
	/**
	 * Given a geometrical representation of the node (JTS geometry)
	 * the method returns the centroid of the latter geometry.
	 * In case of a Point, the latter will be returned.
	 * 
	 * @return centroid of the geometry
	 */
	public Point getCentroid();
	
	/**
	 * @param node_geometry JTS geometry representing the Node
	 */
	public void setGeometry(Geometry node_geometry);
	
	/**
	 * @return JTS geometry representing the Node
	 */
	public Geometry getGeometry();
	
	/**
	 * Given that the spatio-temporal graph structure has been connected
	 * in time the method returns the index assigned to the node.
	 * 
	 * @return time independent tracking index for the cell or default -1
	 */
	public int getTrackID();
	
	/**
	 * Assign a tracking id to the node after tracking
	 * in time.
	 * 
	 * @param tracking_id
	 */
	public void setTrackID(int tracking_id);
	
	/**
	 * Store reference to same node in successive time
	 * frame
	 * @param next_node corresponding node in t+i (ideally i=1)
	 */
	public void setNext(Node next_node);
	
	/**
	 * Store reference to same node in previous time
	 * frame
	 * @param next_node corresponding node in t-i (ideally i=1)
	 */
	public void setPrevious(Node next_node);
	
	/**
	 * Returns if the Node has an associated node 
	 * in a successive time point (not necessarily contiguous),
	 * i.e. node stored in next field.
	 * 
	 * @return true if next field exists.
	 */
	public boolean hasNext();
	
	
	/**
	 * Get reference to same node in successive time frame (first known)
	 * @return reference to corresponding node in t+i (ideally i=1)
	 */
	public Node getNext();
	
	/**
	 * Get reference to same node in previous time frame (first known)
	 * @return reference to corresponding node in t-i (ideally i=1)
	 */
	public Node getPrevious();
	
	/**
	 * Tells whether the cell has been tracked
	 * in the previous frame
	 * 
	 * @return true if cell has correspondence in a previous frame
	 */
	public boolean hasPrevious();
	
	/**
	 * Check whether object lies on the segmentation boundary
	 * 
	 * @return true if object is part of boundary
	 */
	public boolean onBoundary();

	/**
	 * Set whether object lies on the window boundary
	 * 
	 * @param onBoundary 
	 */
	public void setBoundary(boolean onBoundary);
	
	/**
	 * Tells whether the node has been observed dividing
	 * 
	 * @return true if the node will divide, otherwise false
	 */
	public boolean hasObservedDivision();
		
	/**
	 * Associate a division with the node. Relationship can
	 * be either being a child or the mother cell.
	 * 
	 * @param division
	 */
	public void setDivision(Division division);
	
	/**
	 * Get the associated Division object
	 * 
	 * @return the Division object describing the event
	 */
	public Division getDivision();
	
	/**
	 * Transforms the geometrical representation of the node
	 * into an awt.shape.
	 * 
	 * @return java.awt.Shape of the node's geometry(JTS)
	 */
	public Shape toShape();
	
	/**
	 * Obtain the FrameGraph to which the node belongs
	 * 
	 * @return reference to FrameGraph container
	 */
	public FrameGraph getBelongingFrame();

	/**
	 * obtain reference to first frame if tracked
	 * @return reference to corresponding node in first frame
	 */
	public Node getFirst();
	
	
	/**
	 * set corresponding node in first frame
	 * 
	 * @param first corresponding node in first frame
	 */
	public void setFirst(Node first);
	
	/**
	 * Get neighbors of node
	 * 
	 * @return
	 */
	public List<Node> getNeighbors();

	/**
	 * Add candidate to the list of nodes that 
	 * could be the ancestor of this node
	 * 
	 * @param first candidate note
	 */
	public void addParentCandidate(Node first);

	/**
	 * Get the list of candidate nodes that
	 * could be the ancestor of this node
	 * 
	 * @return the list of candidate nodes
	 */
	public List<Node> getParentCandidates();
	
	
	/**
	 * Add an error tag to the node for
	 * user feedback
	 * 
	 * @param errorTag
	 */
	public void setErrorTag(int errorTag);
	
	/**
	 * Obtain the error tag
	 * 
	 * @return error tag
	 */
	public int getErrorTag();

	/**
	 * Associate an elimination event with the node;
	 * 
	 * @param elimination
	 */
	public void setElimination(Elimination elimination);
	
	/**
	 * Obtain the associated elimination event
	 * 
	 * @return elimination object
	 */
	public Elimination getElimination();
	
	/**
	 * Test whether the object is associated
	 * with an elimination event during the observation
	 * 
	 * @return true if the Node will be eliminated during the observation period
	 */
	public boolean hasObservedElimination();
	
	/**
	 * Assign color tag to the cell
	 * 
	 * @return the color tag
	 */
	public Color getColorTag();
	
	/**
	 * Assign color tag to the cell
	 * 
	 * @return the color tag
	 */
	public boolean hasColorTag();
	
	/**
	 * Get the color assigned to the cell
	 * 
	 * @param color_tag the color tag to set
	 */
	public void setColorTag(Color color_tag);

	/**
	 * Returns the time point of the frame if there is one associated
	 * otherwise 0
	 * @return
	 */
	public int getFrameNo();
	
	/**
	 * EXPERIMENTAL: Flag for observed origin
	 * 
	 * @return true if the node is a child node from an observed division
	 */
	public boolean hasObservedOrigin();
	
	/**
	 * EXPERIMENTAL: Return the division object from which the node (child) was generated
	 * 
	 * @return division from observed origin
	 */
	public Division getOrigin();
	
	/**
	 * EXPERIMENTAL: Set the division object that generated the node (child)
	 * 
	 * @param origin the division that generated the node
	 */
	public void setOrigin(Division origin);
	
}
