/**
 * 
 */
package plugins.davhelle.cellgraph.misc;

import java.util.Arrays;
import java.util.HashMap;

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
	
	private int computeTransitionLength(int start){
		int transition_length = 0;
		for(int i=start; i<lost_edge_track.length; i++)
			if(lost_edge_track[i])
				transition_length++;
		
		return transition_length;
	}
	
	public T1Transition(SpatioTemporalGraph stGraph, int[] pair, boolean[] edge_track) {
		
		this.stGraph = stGraph;
		this.lost_edge_track = edge_track;
		
		assert pair.length == 2: "input pair is not of length 2";
		this.loser_nodes = pair;
		
		this.detection_time_point = findFirstMissingFrameNo();
		assert detection_time_point > 0: "transition could not be identified";
		
		this.transition_length = computeTransitionLength(detection_time_point);
		
		winner_nodes = new int[2];
		Arrays.fill(winner_nodes, -1);
		
	}
	
	@Override
	public String toString(){
			return String.format("[%d + %d, %d - %d]",
					winner_nodes[0],winner_nodes[1],
					loser_nodes[0],loser_nodes[1]);
	}


	public void findSideGain(HashMap<Node, PolygonalCellTile> cell_tiles) {
		
		//Geometry.edge
		
	}

	public int length() {
		return transition_length;
	}
}
