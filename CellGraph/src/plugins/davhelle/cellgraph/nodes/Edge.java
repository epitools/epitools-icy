/*=========================================================================
 *
 *  (C) Copyright (2012-2014) Basler Group, IMLS, UZH
 *  
 *  All rights reserved.
 *	
 *  author:	Davide Heller
 *  email:	davide.heller@imls.uzh.ch
 *  
 *=========================================================================*/

package plugins.davhelle.cellgraph.nodes;

import java.util.Arrays;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.misc.CantorPairing;

/**
 * @author Davide Heller
 *
 */
public class Edge extends DefaultWeightedEdge {

	//optional geometry field
	private Geometry geometry;

	/**
	 * An Edge For StGraphs
	 */
	public Edge() {
		this.geometry = null;
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Checks whether the vertices of the edge are tracked.
	 * <br> Def. A tracked edge has both vertices tracked.
	 * 
	 * @param frame
	 * @return true if both vertices are tracked. False if either is not tracked <br>or the edge does not belong to the input graph
	 */
	public boolean isTracked(FrameGraph frame){
		boolean is_tracked = true;
		
		if(!frame.containsEdge(this))
			return false;
		
		if(frame.getEdgeSource(this).getTrackID() == -1)
			is_tracked = false;
		
		if(frame.getEdgeTarget(this).getTrackID() == -1)
			is_tracked = false;
		
		return is_tracked;
	}
	
	public int getTrackHashCode(FrameGraph frame){
		
		//TODO: dangerous, this harms the reversibilty of the cantor function
		//also there is no safety check for the input being natural numbers
		if(!frame.containsEdge(this))
			return -1;

		int[] vertex_track_ids =  new int[2];

		Node source_node = frame.getEdgeSource(this);
		Node target_node = frame.getEdgeTarget(this);

		vertex_track_ids[0] = source_node.getTrackID();
		vertex_track_ids[1] = target_node.getTrackID();

		Arrays.sort(vertex_track_ids);

		int track_hash_code = Arrays.hashCode(vertex_track_ids);

		return track_hash_code;

	}
	
	public long getPairCode(FrameGraph frame){
		
		if(!frame.containsEdge(this))
			return -1;
		
		Node source_node = frame.getEdgeSource(this);
		Node target_node = frame.getEdgeTarget(this);

//		if(source_node.hasObservedDivision())
//			source_node = source_node.getDivision().getMother();
//		
//		if(target_node.hasObservedDivision())
//			target_node = target_node.getDivision().getMother();
		
		int a = source_node.getTrackID();
		int b = target_node.getTrackID();
		
		if(a<b)
			return CantorPairing.compute(a, b);
		else
			return CantorPairing.compute(b, a);
		
	}
	
	public static int[] getCodePair(long code){
		
		//TODO: proper -1 management
		
		if(code < 0){
			int[] invalid_pair = {-1,-1};
			return invalid_pair;
		}
		else
			return CantorPairing.reverse(code);
		
	}
	
	public boolean hasGeometry(){
		return geometry != null;
	}
	
	public void computeGeometry(FrameGraph frame){
		
		Node source = frame.getEdgeSource(this);
		Node target = frame.getEdgeTarget(this);
		
		Geometry source_geometry = source.getGeometry();
		Geometry target_geometry = target.getGeometry();
		
		if(source_geometry.intersects(target_geometry))
			this.geometry = source_geometry.intersection(target_geometry); 
		
	}
	
	/**
	 * @return the geometry
	 */
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * @param geometry the geometry to set
	 */
	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}


}
