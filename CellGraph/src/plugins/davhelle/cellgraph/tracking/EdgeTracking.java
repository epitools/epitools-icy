/**
 * 
 */
package plugins.davhelle.cellgraph.tracking;

import java.util.ArrayList;
import java.util.HashMap;

import plugins.adufour.ezplug.EzPlug;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Edge;

/**
 * Methods for tracking edges extracted from DetectT1Transition
 * 
 * @author Davide Heller
 *
 */
public class EdgeTracking {

	
	public static HashMap<Long, boolean[]> trackEdges(
			SpatioTemporalGraph stGraph) {
		HashMap<Long, boolean[]> tracked_edges = new HashMap<Long,boolean[]>();
		
		initializeTrackedEdges(stGraph, tracked_edges);
		for(int i=1; i<stGraph.size(); i++)
			analyzeFrame(stGraph, tracked_edges, i);
		
		return tracked_edges;
	}
	
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
	
	private static void analyzeFrame(SpatioTemporalGraph stGraph,
			HashMap<Long, boolean[]> tracked_edges, int i) {
		FrameGraph frame_i = stGraph.getFrame(i);
		trackEdgesInFrame(tracked_edges, frame_i);
		removeUntrackedEdges(tracked_edges, frame_i);
	}

	private static void initializeTrackedEdges(SpatioTemporalGraph stGraph,
			HashMap<Long, boolean[]> tracked_edges) {
		FrameGraph first_frame = stGraph.getFrame(0);
		for(Edge e: first_frame.edgeSet())
			if(e.isTracked(first_frame)){
				long track_code = e.getPairCode(first_frame);
				tracked_edges.put(track_code, new boolean[stGraph.size()]);
				tracked_edges.get(track_code)[0] = true;
			}
	}
	
	private static void trackEdgesInFrame(
			HashMap<Long, boolean[]> tracked_edges,
			FrameGraph frame_i) {
		
		for(Edge e: frame_i.edgeSet())
			if(e.isTracked(frame_i)){
				long edge_track_code = e.getPairCode(frame_i);
				if(tracked_edges.containsKey(edge_track_code))
					tracked_edges.get(edge_track_code)[frame_i.getFrameNo()] = true;
			}
	}
	
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
