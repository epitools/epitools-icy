/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.misc;

import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.nodes.Node;

/**
 * PolygonalCellTile generates a Tile representation of a 
 * more complex cell geometry. A tile is defined by a minimal
 * set of faces determined by the number of neighbors shared by the cell.
 * 
 * @author Davide Heller
 *
 */
public class PolygonalCellTile {

	Node source_node;
	
	public PolygonalCellTile(Node n){
		this.source_node = n;
		
		Geometry source_geo = source_node.getGeometry();
		
		for(Node neighbor: n.getNeighbors()){
			Geometry neighbor_geo = neighbor.getGeometry();
			
			Geometry intersection = source_geo.intersection(neighbor_geo);
			
			double intersection_length = intersection.getLength();
			
			System.out.println(
					String.format("Found valid intersection with length: %.2f",
							intersection_length));
		}
	}

}
