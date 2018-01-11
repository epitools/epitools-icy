package plugins.davhelle.cellgraph.misc;

import java.awt.Shape;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
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
	Geometry geometry;
	Shape shape;

	/**
	 * Define the virtual clone by a set of cell nodes
	 * (see {@link Node})
	 * 
	 * @param cells
	 */
	public VirtualClone(Set<Node> cells){
		
		this.cells = cells;
		
		//Compute union of geometry
		Geometry[] cell_geometries = new Geometry[cells.size()];
		Iterator<Node> node_it = cells.iterator();
		for(int i=0; i<cells.size(); i++){
			cell_geometries[i] = node_it.next().getGeometry();
		}		

		//create union
		this.geometry = CascadedPolygonUnion.union(Arrays.asList(cell_geometries));
		
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
	
}
