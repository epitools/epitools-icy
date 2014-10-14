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

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.misc.PolygonalCellTileGenerator;
import plugins.davhelle.cellgraph.misc.T1Transition;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.tracking.EdgeTracking;

public class DetectT1Transition {

	public static void main(String[] args){
		
		final boolean use_test = true;
		
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
		
		HashMap<Node, PolygonalCellTile> cell_tiles = PolygonalCellTileGenerator.createPolygonalTiles(stGraph);
		
		System.out.println("\nAnalyzing the cell edges..");
		HashMap<Long, boolean[]> tracked_edges = EdgeTracking.trackEdges(stGraph);
		
		System.out.println("\nFinding T1 transitions..");
		int transition_no = findTransitions(stGraph, cell_tiles, tracked_edges).size();
		System.out.printf("Found %d stable transition/s\n",transition_no);
	
	}

	public static boolean hasStableTrack(boolean[] edge_track){
		for(boolean tracked_in_frame_i: edge_track)
			if(!tracked_in_frame_i)
				return false;
		
		return true;
	}
	

	public static ArrayList<T1Transition> findTransitions(
			SpatioTemporalGraph stGraph,
			HashMap<Node, PolygonalCellTile> cell_tiles,
			HashMap<Long, boolean[]> tracked_edges) {
		
		ArrayList<T1Transition> stable_transitions = new ArrayList<T1Transition>();
		
		//find changes in neighborhood
		edge_loop:
		for(long track_code:tracked_edges.keySet()){
			boolean[] edge_track = tracked_edges.get(track_code);
			
			if(!hasStableTrack(edge_track)){
				//determine whether a persistent Edge Change occurred
				int[] pair = Edge.getCodePair(track_code);
				
				T1Transition transition = new T1Transition(stGraph, pair, edge_track);
				
				if(transition.length() > 2 && !transition.onBoundary()){
					

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
					
					//check whether the winners contain dividing cells/exclude for now.
					for(int track_id: transition.getWinnerNodes())
						if(stGraph.getFrame(0).hasTrackID(track_id))
							if(stGraph.getFrame(0).getNode(track_id).hasObservedDivision())
								continue edge_loop;

					
					stable_transitions.add(transition);
				}
				else{
					
					if(transition.onBoundary()){
						System.out.printf("Rejected Side Loss: %s @ %d occurs on Boundary\n",
								Arrays.toString(transition.getLoserNodes()),
								transition.getDetectionTime(),
								transition.length());
					}
					else{
					System.out.printf("Rejected Side Loss: %s @ %d is persistent only for %d frames\n",
							Arrays.toString(transition.getLoserNodes()),
							transition.getDetectionTime(),
							transition.length());
					}
				}
				
			}
			
		}

		return stable_transitions;
	}

	
}
