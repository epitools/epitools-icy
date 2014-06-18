/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.misc;

import java.util.HashMap;

import org.jgrapht.graph.DefaultWeightedEdge;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Geometry;

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
	HashMap<Node,Geometry> source_tiles; 
	
	public PolygonalCellTile(Node n){
		
		this.source_node = n;
		this.source_tiles = new HashMap<Node, Geometry>();
		
		Geometry source_geo = source_node.getGeometry();

		for(Node neighbor: n.getNeighbors()){
			
			Geometry neighbor_geo = neighbor.getGeometry();
			Geometry intersection = source_geo.intersection(neighbor_geo);
			source_tiles.put(neighbor, intersection);
			
			//double intersection_length = intersection.getLength();
			//int intersection_geometry_no = intersection.getNumGeometries();
			//intersection length and geometry number will differ 
			//since an oblique geometry has length 1.41 vs 1 of vertical and horizontal
		
		}
	}
	
	/**
	 * updates weighted graph
	 * 
	 * @param n node to analyze
	 * @param frame Substitute with n.getBelongingFrame!
	 */
	public PolygonalCellTile(Node n, FrameGraph frame) {
		
		this(n);

		for(Node neighbor: n.getNeighbors()){
			Geometry intersection = source_tiles.get(neighbor);
			double intersection_length = intersection.getLength();
			DefaultWeightedEdge e = frame.getEdge(source_node, neighbor);
			
			frame.setEdgeWeight(e, intersection_length);
		}
		
	}

	/**
	 * Returns the number of identified intersections to neighboring cells
	 * 
	 * @return the number of intersections
	 */
	public int getTileIntersectionNo(){
		return source_tiles.size();
	}
	
	/**
	 * Provides a unique identifier for a pair of cells 
	 * given their trackID. The order is not relevant.
	 * 
	 * @param a one cell of the tuple
	 * @param b the other cell of the tuple 
	 * @return identifier for cell 
	 */
	public static String getCellPairKey(Node a, Node b){
		
		//order cells according to TrackId
		int min = a.getTrackID();
		int max = b.getTrackID();
		if(min > max){
			min = b.getTrackID();
			max = a.getTrackID();
		}
		
		//Concatenate the two with the interleaving symbol '<->'
		String key = String.format("%d<->%d",min,max);
		//given the incremental order also a unique integer could be formed
		
		return key;
	}

}
