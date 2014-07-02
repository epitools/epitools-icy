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

package headless;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.misc.T1Transition;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

public class DetectT1Transition {

	public static void main(String[] args){
		
		final boolean use_test = false;
		
		SpatioTemporalGraph stGraph = null;
		
		//Input files
		if(use_test){
			File test_file = new File(
					"/Users/davide/data/neo/1/T1_examples/T1_at_x62-y56/T1atx62-y56t0000.tif");
			int no_of_test_files = 20;
			stGraph = StGraphUtils.createDefaultGraph(test_file,no_of_test_files);
		}
		else
			stGraph = StGraphUtils.loadNeo(1);
		
		assert stGraph != null: "Spatio temporal graph creation failed!";
		
		HashMap<Node, PolygonalCellTile> cell_tiles = StGraphUtils.createPolygonalTiles(stGraph);
		HashMap<Long, boolean[]> tracked_edges = new HashMap<Long,boolean[]>();
		
		initializeTrackedEdges(stGraph, tracked_edges);
		
		for(int i=1; i<stGraph.size(); i++)
		{
			FrameGraph frame_i = stGraph.getFrame(i);
			
			trackEdges(tracked_edges, frame_i);
			
			removeUntrackedEdges(tracked_edges, frame_i);
		}
		
		int transition_no = findTransitions(stGraph, cell_tiles, tracked_edges);
		
		System.out.printf("Found %d stable transitions\n",transition_no);
	
	}

	private static void initializeTrackedEdges(SpatioTemporalGraph stGraph,
			HashMap<Long, boolean[]> tracked_edges) {
		FrameGraph first_frame = stGraph.getFrame(0);
		for(Edge e: first_frame.edgeSet()){
			if(e.isTracked(first_frame)){
				long track_code = e.getPairCode(first_frame);
				tracked_edges.put(track_code, new boolean[stGraph.size()]);
				tracked_edges.get(track_code)[0] = true;
			}
		}
	}
	
	private static void trackEdges(
			HashMap<Long, boolean[]> tracked_edges,
			FrameGraph frame_i) {
		
		for(Edge e: frame_i.edgeSet()){
			if(e.isTracked(frame_i)){
				
				long edge_track_code = e.getPairCode(frame_i);
				
				if(tracked_edges.containsKey(edge_track_code))
					tracked_edges.get(edge_track_code)[frame_i.getFrameNo()] = true;
			}
		}
	}
	
	private static void removeUntrackedEdges(
			HashMap<Long, boolean[]> tracked_edges, FrameGraph frame_i) {
		//introduce the difference between lost edge because of tracking and because of T1
		ArrayList<Long> to_eliminate = new ArrayList<Long>();
		for(long track_code:tracked_edges.keySet()){
			int[] pair = Edge.getCodePair(track_code);
			for(int track_id: pair){
				if(!frame_i.hasTrackID(track_id)){
					to_eliminate.add(track_code);
					break;
				}
			}
		}
		
		for(long track_code:to_eliminate)
			tracked_edges.remove(track_code);
	}
	
	public static boolean hasStableTrack(boolean[] edge_track){
		for(boolean tracked_in_frame_i: edge_track)
			if(!tracked_in_frame_i)
				return false;
		
		return true;
	}
	

	private static int findTransitions(
			SpatioTemporalGraph stGraph,
			HashMap<Node, PolygonalCellTile> cell_tiles,
			HashMap<Long, boolean[]> tracked_edges) {
		
		int stable_transition_no = 0;
		
		//find changes in neighborhood
		for(long track_code:tracked_edges.keySet()){
			boolean[] edge_track = tracked_edges.get(track_code);
			
			if(!hasStableTrack(edge_track)){
				//determine when the change happened the first time
				int[] pair = Edge.getCodePair(track_code);
				T1Transition transition = new T1Transition(stGraph, pair, edge_track);
				
				if(transition.length() > 2){
					
					stable_transition_no++;

					System.out.printf("Accepted Side Loss: %s @ %d is persistent for %d frames\n",
							Arrays.toString(transition.getLoserNodes()),
							transition.getDetectionTime(),
							transition.length());
					
					//extract tile geometry and find find neighboring cells
					//check if they are connected in LOST frame 
					//and if they are unconnected in previous frame
					//transition.checkConnectivityAssumption();
					transition.findSideGain(cell_tiles);
					
					System.out.printf("\tProposed Side Gain: %s\n",
							Arrays.toString(transition.getWinnerNodes()));
					
				}
				else{
					System.out.printf("Rejected Side Loss: %s @ %d is persistent only for %d frames\n",
							Arrays.toString(transition.getLoserNodes()),
							transition.getDetectionTime(),
							transition.length());
				}
				
			}
			
		}

		return stable_transition_no;
	}

	
}
