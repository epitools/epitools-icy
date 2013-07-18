/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.nodes;

import java.awt.Shape;
import java.util.List;

import plugins.davhelle.cellgraph.graphs.FrameGraph;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Node represents the vertex object for the TissueGraph.
 * Among various implementations examples can be:
 * a Cell type representing the cell relation in the tissue
 * or a Corner type representing the vertex relation.
 * 
 * By default it is assumed that there is a JTS Geometry field.
 * 
 * @author Davide Heller
 *
 */
public interface Node{
	
	
	/**
	 * Given a geometrical representation of the node(JTS)
	 * the method returns the centroid of the latter geometry.
	 * In case of a Point, the latter will be returned.
	 * 
	 * @return centroid of the geometry
	 */
	public Point getCentroid();
	
	/**
	 * @return JTS geometry representing the NodeType
	 */
	public Geometry getGeometry();

	/**
	 * Abstract feature of a NodeType. For example for 
	 * a polygonal cell representation this might include
	 * the voroni tesselation.  
	 * 
	 * @return a defined property of the implementation 
	 */
	public Object getProperty();
	
	/**
	 * Abstract feature of a NodeType. For example for 
	 * a polygonal cell representation this might include
	 * the voroni tesselation.  
	 * 
	 * @param property to be set
	 */
	public void setProperty(Object property);
	
	
	/**
	 * Given that the HyperGraph structure has been tracked
	 * in time the method returns the idx assigned to the node.
	 * 
	 * @return constant tracking index for the cell or default -1
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
	 * Check whether object lies on the window boundary
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
	 * Set whether the node has been observed dividing
	 * 
	 * @param observedDivision
	 */
	public void setObservedDivision(boolean observedDivision);
	
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
}
