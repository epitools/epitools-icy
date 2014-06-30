/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.misc;

import java.util.HashMap;

import org.jgrapht.graph.DefaultWeightedEdge;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Edge;
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
	
	/**
	 * Finds the connecting cell edges to the respective neighbors
	 * and stores them. Additionally it registers the length of the
	 * found geometry as weight of the PolygonalCellTile
	 * 
	 * @param n considered node / cell
	 */
	public PolygonalCellTile(Node n){
		
		this.source_node = n;
		this.source_tiles = new HashMap<Node, Geometry>();
		
		FrameGraph frame = source_node.getBelongingFrame(); 
		Geometry source_geo = source_node.getGeometry();

		for(Node neighbor: n.getNeighbors()){
			
			Geometry neighbor_geo = neighbor.getGeometry();
			Geometry intersection = source_geo.intersection(neighbor_geo);
			
			source_tiles.put(neighbor, intersection);
			
			//updated weighted graph with edge length
			double intersection_length = intersection.getLength();
			Edge e = frame.getEdge(source_node, neighbor);
			frame.setEdgeWeight(e, intersection_length);
			
//			if(!e.hasGeometry()){
//				e.setGeometry(intersection);
//			}
			
			//int intersection_geometry_no = intersection.getNumGeometries();
			//intersection length and geometry number can differ 
			//since an oblique geometry has length 1.41 
			//versus length 1 of a vertical and horizontal geometry
		
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
	 * Returns the geometry representing the intersection between the 
	 * source and a neighboring cell
	 * 
	 * @param neighboring cell, if node is not a neighbor null is returned
	 * @return intersection geometry
	 */
	public Geometry getTileEdge(Node neighbor){
		if(source_tiles.containsKey(neighbor))
			return source_tiles.get(neighbor);
		else
			return null;
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
