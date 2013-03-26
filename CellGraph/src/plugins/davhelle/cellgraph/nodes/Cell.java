/**
 * 
 */
package plugins.davhelle.cellgraph.nodes;

import java.awt.Shape;


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
	
	//tracking information
	private Node next;
	private Node previous;
	private int track_id;
	
	//boundary information
	private boolean is_on_boundary;
	
	//division information
	private boolean observedDivision;
	
	/**
	 * Initializes the Node type representing a Cell as Polygon 
	 */
	public Cell(Polygon cell_polygon) {
		this.geometry = cell_polygon;
		
		//default for untracked cell
		this.next = null;
		this.previous = null;
		this.track_id = -1;
		
		//default boundary condition
		this.is_on_boundary = false;
		
		//default division information
		this.observedDivision = false;
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.NodeType#getCentroid()
	 */
	@Override
	public Point getCentroid() {
		return geometry.getCentroid();
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
		return observedDivision;
	}

	@Override
	public void setObservedDivision(boolean will_cell_divide) {
		this.observedDivision = will_cell_divide;
	}

}
