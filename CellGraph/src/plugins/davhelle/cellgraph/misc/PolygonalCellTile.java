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
		System.out.printf("Centroid:%.2f,%.2f\n",
				source_geo.getCentroid().getX(),
				source_geo.getCentroid().getY());

		int neighbor_no = 0;
		for(Node neighbor: n.getNeighbors()){
			System.out.printf("\tAnalyzing neighbor %d:\n",neighbor_no++);
			Geometry neighbor_geo = neighbor.getGeometry();
			
			Geometry intersection = source_geo.intersection(neighbor_geo);
			
			double intersection_length = intersection.getLength();
			
			
			//intersection length and geometry number will vary 
			//since an oblique geometry has length 1.41 vs 1 of vertical and horizontal
			System.out.printf("\tFound valid intersection with length: %.2f\n\t\twith %d geometries\n",
							intersection_length,intersection.getNumGeometries());
		
		
		}
	}

}
