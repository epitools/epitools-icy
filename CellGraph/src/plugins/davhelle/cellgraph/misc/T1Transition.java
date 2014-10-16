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

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Class to represent the gain and loss dynamics of edges between cells
 * 
 * @author Davide Heller
 *
 */
public class T1Transition {
	
	private SpatioTemporalGraph stGraph;

	/**
	 * Track Id's of nodes that will
	 * respectively loose or win a 
	 * neighbor relationship. 
	 */
	int[] loserNodes;
	int[] winnerNodes;
	
	/**
	 * For every time point store the
	 * presence or absence of the old
	 * edge
	 */
	boolean[] lostEdgeTrack;
	
	/**
	 * Store when the first stable transition
	 * Occurs. 
	 */
	int detectionTimePoint;

	/**
	 * Store for how long the new edge
	 * could be observed consecutively
	 */
	int transitionLength;
	
	/**
	 * Store the amount of time points
	 * where the old edge could be
	 * detected 
	 */
	int oldEdgeSurvivalLength;
	

	public T1Transition(SpatioTemporalGraph stGraph, int[] pair, boolean[] edge_track) {
		
		this.stGraph = stGraph;
		this.lostEdgeTrack = edge_track;
		
		assert pair.length == 2: "input pair is not of length 2";
		this.loserNodes = pair;
		
		this.detectionTimePoint = findFirstMissingFrameNo();
		assert detectionTimePoint > 0: "transition could not be identified";
		
		this.oldEdgeSurvivalLength = 0;
		this.transitionLength = computeTransitionLength();
		
		winnerNodes = new int[2];
		Arrays.fill(winnerNodes, -1);
		
	}

	private int findFirstMissingFrameNo(){
		//TODO what if this is not the correct start
		
		for(int i=0; i<lostEdgeTrack.length; i++)
			if(!lostEdgeTrack[i])
				return i;
		
		return -1;
	}
	
	/**
	 * Determine how long the transition was observed
	 * 
	 * @return
	 */
	private int computeTransitionLength(){
		
		//compute the transition vector, i.e. length of every transition
		int[] transition_vector = new int[lostEdgeTrack.length];
		for(int i=detectionTimePoint; i<lostEdgeTrack.length; i++)
			if(!lostEdgeTrack[i]){
				transition_vector[i] = transition_vector[i-1] + 1;
				transition_vector[i-1] = 0;
			}
			else
				oldEdgeSurvivalLength++;
		
		//find the first transition that is higher than set detection threshold
		int max_length = 0;
		for(int i=0; i<transition_vector.length; i++) {
			if(transition_vector[i] > max_length){
				max_length = transition_vector[i];
				detectionTimePoint = i - max_length  + 1;
			}
		}
		
//		//Count the number of Transitions (i.e. minimum number of consecutive losses is 3)
//		Arrays.sort(transition_vector);
//		int array_end = transition_vector.length - 1;
//		for(int i=array_end; i>=0; i--){
//			if(transition_vector[i] < 1){
//				System.out.printf("Found %d permanent track/s out of %d persistent change/s: %s\n",
//						array_end - i, 
//						transition_length,
//						Arrays.toString(
//								Arrays.copyOfRange(transition_vector, i+1, array_end+1)));
//				break;
//			}
//		}
		
		//review which transition is given as output
		return max_length;
	}
	
	
	@Override
	public String toString(){
			return String.format("[%d + %d, %d - %d] @ %d",
					winnerNodes[0],winnerNodes[1],
					loserNodes[0],loserNodes[1],
					detectionTimePoint);
	}
	
	public int[] getLoserNodes(){
		return loserNodes;
	}
	
	public boolean hasWinners(){
		for(int winner: winnerNodes)
			if(winner == -1)
				return false;

		return true;
	}
	
	public int[] getWinnerNodes(){
		return winnerNodes;
	}
	
	public int getDetectionTime(){
		return detectionTimePoint;
	}

	/**
	 * Return how many time points
	 * contain the old edge.
	 * 
	 * @return the oldEdgeSurvivalLength
	 */
	public int getOldEdgeSurvivalLength() {
		return oldEdgeSurvivalLength;
	}
	
	//Check if looser nodes are on the boundary
	public boolean onBoundary(){
		FrameGraph previous_frame = stGraph.getFrame(detectionTimePoint - 1);
		
		Node l1 = previous_frame.getNode(loserNodes[0]);
		Node l2 = previous_frame.getNode(loserNodes[1]);
		
		if(l1.onBoundary() || l2.onBoundary())
			return true;
		else
			return false;
	}
	
	public void findSideGain(HashMap<Node, PolygonalCellTile> cell_tiles) {
		
		FrameGraph previous_frame = stGraph.getFrame(detectionTimePoint - 1);
		
		assert previous_frame.hasTrackID(loserNodes[0]): "Looser node not found in previous frame";
		assert previous_frame.hasTrackID(loserNodes[1]): "Looser node not found in previous frame";
		
		Node l1 = previous_frame.getNode(loserNodes[0]);
		Node l2 = previous_frame.getNode(loserNodes[1]);
				
		//TODO: substitute with intersection?
//		assert previous_frame.containsEdge(l1, l2): "Input edge is missing in previous frame";
//		Edge lost_edge = previous_frame.getEdge(l1, l2);
//		
//		Geometry lost_edge_geometry = 
//		if(lost_edge.hasGeometry()){
//			
//		}
		Geometry lost_edge_geometry = cell_tiles.get(l1).getTileEdge(l2);
		
		ArrayList<Integer> side_gain_nodes = new ArrayList<Integer>();
		
		for(Node n: l1.getNeighbors())
			if(lost_edge_geometry.intersects(n.getGeometry()))
				if(!n.equals(l2))
					side_gain_nodes.add(n.getTrackID());
		
		assert side_gain_nodes.size() == 2:
			String.format("Winner nodes are more than expected %s",side_gain_nodes.toString());
		
		if(side_gain_nodes.size() == 1){
			System.out.printf("Problems with winner node %d in frame %d, Loosers (%d,%d) ",
					side_gain_nodes.get(0),
					detectionTimePoint,
					loserNodes[0],
					loserNodes[1]
					);
		}
		
		winnerNodes[0] = side_gain_nodes.get(0);
		winnerNodes[1] = side_gain_nodes.get(1);
		
//		FrameGraph detection_frame = stGraph.getFrame(detection_time_point);
//		
//		assert detection_frame.hasTrackID(winner_nodes[0]): "Winner node not found in detection frame";
//		assert detection_frame.hasTrackID(winner_nodes[1]): "Winner node not found in detection frame";
//		
//		Node w1 = detection_frame.getNode(winner_nodes[0]);
//		Node w2 = detection_frame.getNode(winner_nodes[1]);
//		
//		assert detection_frame.containsEdge(w1, w2): "No winner edge found in detection frame";
		
	}

	public int length() {
		return transitionLength;
	}
}
