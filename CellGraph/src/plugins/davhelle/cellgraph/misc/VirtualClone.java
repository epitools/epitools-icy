package plugins.davhelle.cellgraph.misc;

import java.awt.Shape;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.overlays.CellCloneOverlay;

/**
 * Class defining a group of cells as a clone. The object
 * helps obtaining information about clones such as the
 * overall shape (through the union of all cells) and it's
 * characteristics. While currently not tested the member cells
 * should be connected to one another see {@link CellCloneOverlay}  
 * 
 * @author Davide Heller
 *
 */
public class VirtualClone {
	
	Set<Node> cells;
	Set<Node> border_cells;
	Set<Node> neighbor_cells;
	Geometry geometry;
	Geometry boundary;
	Shape shape;
	int clone_id;

	/**
	 * Define the virtual clone by a set of cell nodes
	 * (see {@link Node})
	 * 
	 * @param cells
	 * @param clone_id 
	 */
	public VirtualClone(Set<Node> cells, int clone_id){
		
		this.cells = cells;
		this.clone_id = clone_id;
		
		//Compute union of geometry
		Geometry[] cell_geometries = new Geometry[cells.size()];
		Iterator<Node> node_it = cells.iterator();
		for(int i=0; i<cells.size(); i++){
			cell_geometries[i] = node_it.next().getGeometry();
		}		

		//create union
		this.geometry = CascadedPolygonUnion.union(Arrays.asList(cell_geometries));
		this.boundary = geometry.getBoundary();
		
		//create shape
		ShapeWriter writer = new ShapeWriter();
		this.shape = writer.toShape(geometry);
		
	}
	
	/**
	 * @return AWT shape of the clone geometry (cached)
	 */
	public Shape getShape(){
		return this.shape;
	}
	
	/**
	 * @return length in pixel of the clone's perimeter
	 */
	public double getPerimeter(){
		return this.geometry.getLength();
	}
	
	/**
	 * @return integer id of the clone
	 */
	public int getId(){
		return this.clone_id;
	}

	/**
	 * @return size in pixels of the clone's area
	 */
	public double getSize() {
		return this.geometry.getArea();
	}

	/**
	 * @return the number of cells within the clone
	 */
	public int getCellCount() {
		return this.cells.size();
	}
	
	/**
	 * @return centroid of the clone geometry
	 */
	public Point getCentroid(){
		return this.geometry.getCentroid();
	}
	
	/**
	 * test node membership in clone
	 * 
	 * @param n node to test
	 * @return true if the node n is part of the clone
	 */
	public boolean contains(Node n){
		return this.cells.contains(n);
	}
	
	/**
	 * Computes cells that intersect the border (geometry) of
	 * the clone. Computation is delayed to avoid computation
	 * during threshold adjustment phase in {@link CellCloneOverlay}
	 * 
	 * @return clone cells intersecting the clone border
	 */
	public Set<Node> getBorderCells(){
		if(border_cells == null){
			border_cells = new HashSet<Node>();
			for(Node n: this.cells)
				if(this.boundary.intersects(n.getGeometry()))
					border_cells.add(n);
		}
			
		return border_cells;
	}
	
	/**
	 * Returns neighboring cells in the frameGraph, i.e. that intersect the clone geometry
	 * but are not part of the clone.
	 * 
	 * @return cells neighboring the clone
	 */
	public Set<Node> getNeighborCells(){
		if(neighbor_cells == null){
			neighbor_cells = new HashSet<Node>();
			for(Node n: getBorderCells())
				for(Node neighbor: n.getNeighbors())
					if(!cells.contains(neighbor))
						neighbor_cells.add(neighbor);
		}
		
		return neighbor_cells;
	}
	
}
