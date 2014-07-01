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

package plugins.davhelle.cellgraph.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Class to represent the gain and loss dynamics of edges between cells
 * 
 * @author Davide Heller
 *
 */
public class T1Transition {
	
	private SpatioTemporalGraph stGraph;

	int[] loser_nodes;
	int[] winner_nodes;
	
	boolean[] lost_edge_track;
	
	int detection_time_point;
	int transition_length;
	
	private int findFirstMissingFrameNo(){
		//TODO what if this is not the correct start
		
		for(int i=0; i<lost_edge_track.length; i++)
			if(!lost_edge_track[i])
				return i;
		
		return -1;
	}
	
	private int computeTransitionLength(){
		int transition_length = 0;
		int[] transition_vector = new int[lost_edge_track.length];
		for(int i=detection_time_point; i<lost_edge_track.length; i++)
			if(!lost_edge_track[i]){
				transition_vector[i] = transition_vector[i-1] + 1;
				transition_length++;
			}
			else
				break;
		
		System.out.printf("Transition vector of %s:\t%s\n",
				Arrays.toString(loser_nodes),
				Arrays.toString(transition_vector));
		
		return transition_length;
	}
	
	public T1Transition(SpatioTemporalGraph stGraph, int[] pair, boolean[] edge_track) {
		
		this.stGraph = stGraph;
		this.lost_edge_track = edge_track;
		
		assert pair.length == 2: "input pair is not of length 2";
		this.loser_nodes = pair;
		
		this.detection_time_point = findFirstMissingFrameNo();
		assert detection_time_point > 0: "transition could not be identified";
		
		this.transition_length = computeTransitionLength();
		
		winner_nodes = new int[2];
		Arrays.fill(winner_nodes, -1);
		
	}
	
	@Override
	public String toString(){
			return String.format("[%d + %d, %d - %d] @ %d",
					winner_nodes[0],winner_nodes[1],
					loser_nodes[0],loser_nodes[1],
					detection_time_point);
	}
	
	public int[] getLoserNodes(){
		return loser_nodes;
	}
	
	public int[] getWinnerNodes(){
		return winner_nodes;
	}
	
	public int getDetectionTime(){
		return detection_time_point;
	}


	public void findSideGain(HashMap<Node, PolygonalCellTile> cell_tiles) {
		
		FrameGraph previous_frame = stGraph.getFrame(detection_time_point - 1);
		
		assert previous_frame.hasTrackID(loser_nodes[0]): "Looser node not found in previous frame";
		assert previous_frame.hasTrackID(loser_nodes[1]): "Looser node not found in previous frame";
		
		Node l1 = previous_frame.getNode(loser_nodes[0]);
		Node l2 = previous_frame.getNode(loser_nodes[1]);
		
		//TODO: substitute with intersection?
		Geometry lost_edge = cell_tiles.get(l1).getTileEdge(l2);
		
		ArrayList<Integer> side_gain_nodes = new ArrayList<Integer>();
		
		for(Node n: l1.getNeighbors())
			if(lost_edge.intersects(n.getGeometry()))
				if(!n.equals(l2))
					side_gain_nodes.add(n.getTrackID());
		
		assert side_gain_nodes.size() == 2:
			String.format("Winner nodes are more than expected %s",side_gain_nodes.toString());
		
		winner_nodes[0] = side_gain_nodes.get(0);
		winner_nodes[1] = side_gain_nodes.get(1);
		
		FrameGraph detection_frame = stGraph.getFrame(detection_time_point);
		
		assert detection_frame.hasTrackID(winner_nodes[0]): "Winner node not found in detection frame";
		assert detection_frame.hasTrackID(winner_nodes[1]): "Winner node not found in detection frame";
		
		Node w1 = detection_frame.getNode(winner_nodes[0]);
		Node w2 = detection_frame.getNode(winner_nodes[1]);
		
		assert detection_frame.containsEdge(w1, w2): "No winner edge found in detection frame";
		
	}

	public int length() {
		return transition_length;
	}
}
