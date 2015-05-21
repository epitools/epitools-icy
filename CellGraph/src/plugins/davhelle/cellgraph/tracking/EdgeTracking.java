package plugins.davhelle.cellgraph.tracking;

import java.util.ArrayList;
import java.util.HashMap;

import plugins.adufour.ezplug.EzPlug;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Edge;

/**
 * Class to track all edges contained in the first frame 
 * of a spatio-temporal graph assuming that a cell tracking
 * method has already been applied to the graph <br>
 * 
 * The edge tracking relies on the cell tracking as every
 * edge is identified by a time independent id computed
 * from the two tracking ids of the edge's cells.<br><br>
 * 
 * <b>Warning: Currently only mend for T1 transition detection!</b>
 * 
 * @author Davide Heller
 *
 */
public class EdgeTracking {
	//TODO transform static methods to object
	
	/**
	 * Tracks all the edges starting from the first frame in the graph
	 * 
	 * @param stGraph the graph whose edges to track
	 * @return An array of presence for each edge. Each array cell 
	 * tells if the edge is present or not at the corresponding time point. The key of the map is
	 * the cantor paring of the edges vertex ids.
	 */
	public static HashMap<Long, boolean[]> trackEdges(
			SpatioTemporalGraph stGraph) {
		HashMap<Long, boolean[]> tracked_edges = new HashMap<Long,boolean[]>();
		
		initializeTrackedEdges(stGraph, tracked_edges);
		for(int i=1; i<stGraph.size(); i++)
			analyzeFrame(stGraph, tracked_edges, i);
		
		return tracked_edges;
	}
	
	/**
	 * Tracks all the edges starting from the first frame in the graph
	 * 
	 * @param stGraph the graph whose edges to track
	 * @param plugin calling ezPlug GUI to feedback progress
	 * @return An array of presence for each edge. Each array cell 
	 * tells if the edge is present or not at the corresponding time point. The key of the map is
	 * the cantor paring of the edges vertex ids.
	 */
	public static HashMap<Long, boolean[]> trackEdges(
			SpatioTemporalGraph stGraph, EzPlug plugin) {
		HashMap<Long, boolean[]> tracked_edges = new HashMap<Long,boolean[]>();
		plugin.getUI().setProgressBarMessage("Tracking Edges..");
		plugin.getUI().setProgressBarValue(0.0);
		
		initializeTrackedEdges(stGraph, tracked_edges);
		for(int i=1; i<stGraph.size(); i++){
			analyzeFrame(stGraph, tracked_edges, i);
			plugin.getUI().setProgressBarValue((double)i/stGraph.size());
		}
		return tracked_edges;
	}

	/**
	 * Initializes the boolean arrays for each edge in the first frame
	 * 
	 * @param stGraph graph to analyze
	 * @param tracked_edges empty map
	 */
	private static void initializeTrackedEdges(SpatioTemporalGraph stGraph,
			HashMap<Long, boolean[]> tracked_edges) {
		FrameGraph first_frame = stGraph.getFrame(0);
		for(Edge e: first_frame.edgeSet())
			if(e.canBeTracked(first_frame)){
				long track_code = e.getPairCode(first_frame);
				tracked_edges.put(track_code, new boolean[stGraph.size()]);
				tracked_edges.get(track_code)[0] = true;
			}
	}
	
	/**
	 * Given the initialized map (see initializeTrackedEdges method)
	 * the analyzeFrame method fills the map for the time point i
	 * by verifying the presence of each included edge at the
	 * frame i of the stGraph. 
	 * 
	 * @param stGraph graph to analyze
	 * @param tracked_edges initialized output map
	 * @param i time point to analyze
	 */
	private static void analyzeFrame(SpatioTemporalGraph stGraph,
			HashMap<Long, boolean[]> tracked_edges, int i) {
		FrameGraph frame_i = stGraph.getFrame(i);
		trackEdgesInFrame(tracked_edges, frame_i);
		removeUntrackedEdges(tracked_edges, frame_i);
	}
	
	/**
	 * Checks the presence of edges in frame_i
	 * 
	 * @param tracked_edges presence map
	 * @param frame_i frame to check
	 */
	private static void trackEdgesInFrame(
			HashMap<Long, boolean[]> tracked_edges,
			FrameGraph frame_i) {
		
		for(Edge e: frame_i.edgeSet())
			if(e.canBeTracked(frame_i)){
				long edge_track_code = e.getPairCode(frame_i);
				if(tracked_edges.containsKey(edge_track_code))
					tracked_edges.get(edge_track_code)[frame_i.getFrameNo()] = true;
			}
	}
	
	/**
	 * Eliminates the edges that are not found because 
	 * one of the vertices is not present. Currently used
	 * to differentiate between lost edges of T1 transitions
	 * (i.e. cells are still there but not neighbors anymore) and
	 * lost edges because of missing cells (possible segmentation mistake or elimination) 
	 * 
	 * @param tracked_edges presence map
	 * @param frame_i frame to check
	 */
	private static void removeUntrackedEdges(
			HashMap<Long, boolean[]> tracked_edges, FrameGraph frame_i) {
		//introduce the difference between lost edge because of tracking and because of T1
		ArrayList<Long> to_eliminate = new ArrayList<Long>();
		for(long track_code:tracked_edges.keySet())
			for(int track_id: Edge.getCodePair(track_code))
				if(!frame_i.hasTrackID(track_id)){
					to_eliminate.add(track_code);
					break;
				}
		
		for(long track_code:to_eliminate)
			tracked_edges.remove(track_code);
	}
}
