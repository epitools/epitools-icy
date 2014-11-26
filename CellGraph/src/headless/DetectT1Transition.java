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
import plugins.davhelle.cellgraph.io.CsvWriter;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.misc.PolygonalCellTileGenerator;
import plugins.davhelle.cellgraph.misc.T1Transition;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.painters.TransitionOverlay;
import plugins.davhelle.cellgraph.tracking.EdgeTracking;

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
			stGraph = LoadNeoWtkFiles.loadNeo(0);
		assert stGraph != null: "Spatio temporal graph creation failed!";
		
		HashMap<Node, PolygonalCellTile> cell_tiles = PolygonalCellTileGenerator.createPolygonalTiles(stGraph);
		
		System.out.println("\nAnalyzing the cell edges..");
		HashMap<Long, boolean[]> tracked_edges = EdgeTracking.trackEdges(stGraph);
		
		StringBuilder builder = new StringBuilder();
		for(long track_code:tracked_edges.keySet()){
			boolean[] edge_track = tracked_edges.get(track_code);
			
			if(hasStableTrack(edge_track)){
				int[] cell_ids = Edge.getCodePair(track_code);
				
				for(int i=0; i<stGraph.size(); i++){
					FrameGraph frame_i = stGraph.getFrame(i);

					Node[] cell_nodes = new Node[cell_ids.length];
					
					for(int j=0; j<cell_ids.length; j++){
						int loser_id = cell_ids[j];
						if(frame_i.hasTrackID(loser_id))
							cell_nodes[j] = frame_i.getNode(loser_id);
					}
					
					if(frame_i.containsEdge(cell_nodes[0], cell_nodes[1])){
						Edge e = frame_i.getEdge(cell_nodes[0], cell_nodes[1]);
						builder.append(String.format("%.2f", frame_i.getEdgeWeight(e)));
						builder.append(',');
					}
					else{
						builder.append(0.0);
						builder.append(',');
					}
					
					
				}
				
				builder.setLength(builder.length() - 1);
				builder.append('\n');
					
			}
		}
		File main_output_file = new File("/Users/davide/tmp/test_full_edge.csv");
		CsvWriter.writeOutBuilder(builder, main_output_file);
		
		//System.out.println("\nFinding T1 transitions..");
		//int transition_no = findTransitions(stGraph, cell_tiles, tracked_edges,5,5).size();
		//System.out.printf("Found %d stable transition/s\n",transition_no);
	
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
			HashMap<Long, boolean[]> tracked_edges,
			int minimalTransitionLength,
			int minimalOldEdgeSurvivalLength
			) {
		
		ArrayList<T1Transition> stable_transitions = new ArrayList<T1Transition>();
		
		//find changes in neighborhood
		ArrayList<T1Transition> divisions_with_transitions = new ArrayList<T1Transition>();
		
		edge_loop:
		for(long track_code:tracked_edges.keySet()){
			boolean[] edge_track = tracked_edges.get(track_code);
			
			if(!hasStableTrack(edge_track)){
				//determine whether a persistent Edge Change occurred
				int[] pair = Edge.getCodePair(track_code);
				
				T1Transition transition = new T1Transition(stGraph, pair, edge_track);
				
				if(
						transition.length() > minimalTransitionLength && 					//new edge detected at least for X frames consecutively
						!transition.onBoundary() && 				//transition should not occur on boundary
						transition.getOldEdgeSurvivalLength() > minimalOldEdgeSurvivalLength 	//old edge visible for at least X frames
						){
					

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
					
					//Verify winner cells
					for(int track_id: transition.getWinnerNodes()){
						//are tracked
						if(track_id == -1)
							continue edge_loop;
						//are not mother cells
						//check whether the winners contain dividing cells/exclude for now.
						//this excludes cells that are going to divide but not the daughter
						//cells since their id cannot be present in the first frame!
						else if(stGraph.getFrame(0).hasTrackID(track_id))
							if(stGraph.getFrame(0).getNode(track_id).hasObservedDivision()){
								divisions_with_transitions.add(transition);
								continue edge_loop;
							}
					}

					
					
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
