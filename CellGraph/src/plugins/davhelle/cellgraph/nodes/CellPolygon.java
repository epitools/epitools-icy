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
public class CellPolygon implements NodeType {
	
	//geometry to abstract the Node
	private Polygon cell;
	
	//tracking information
	private NodeType next;
	private NodeType last;
	private int track_id;
	
	//boundary information
	private boolean is_on_boundary;
	
	//division information
	private boolean will_divide;
	
	/**
	 * Initializes the Node type representing a Cell as Polygon 
	 */
	public CellPolygon(Polygon cell_polygon) {
		this.cell = cell_polygon;
		
		//default for untracked cell
		this.next = null;
		this.last = null;
		this.track_id = -1;
		
		//default boundary condition
		this.is_on_boundary = false;
		
		//default division information
		this.will_divide = false;
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.NodeType#getCentroid()
	 */
	@Override
	public Point getCentroid() {
		return cell.getCentroid();
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.NodeType#getGeometry()
	 */
	@Override
	public Geometry getGeometry() {
		return cell;
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
		return writer.toShape(cell);
	}

	@Override
	public void setNext(NodeType next_node) {
		this.next = next_node;		
	}

	@Override
	public NodeType getNext() {
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
	public NodeType getLast() {
		return this.last;
	}

	@Override
	public void setLast(NodeType last_node) {
		this.last = last_node;		
	}

	@Override
	public boolean willDivide() {
		return will_divide;
	}

	@Override
	public void setDivisionFlag(boolean will_cell_divide) {
		this.will_divide = will_cell_divide;
	}

}
