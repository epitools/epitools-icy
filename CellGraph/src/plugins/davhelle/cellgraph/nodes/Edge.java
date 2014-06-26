/**
 * 
 */
package plugins.davhelle.cellgraph.nodes;

import org.jgrapht.graph.DefaultWeightedEdge;

import plugins.davhelle.cellgraph.graphs.FrameGraph;

/**
 * @author Davide Heller
 *
 */
public class Edge extends DefaultWeightedEdge {

	/**
	 * An Edge For StGraphs
	 */
	public Edge() {
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

}
