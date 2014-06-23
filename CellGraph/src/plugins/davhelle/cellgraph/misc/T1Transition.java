/**
 * 
 */
package plugins.davhelle.cellgraph.misc;

import java.util.ArrayList;

import org.jgrapht.graph.DefaultWeightedEdge;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Class to represent the gain and loss dynamics of edges between cells
 * 
 * @author Davide Heller
 *
 */
public class T1Transition {
	
	public DefaultWeightedEdge lost_edge;
	public DefaultWeightedEdge gained_edge;
	public ArrayList<Node> winners;
	public ArrayList<Node> losers;
	FrameGraph detection_frame;
	int detection_time_point;
	int detection_length;
	
	public T1Transition(FrameGraph detection_frame){
		this.detection_frame = detection_frame;
		this.detection_time_point = detection_frame.getFrameNo();
		this.detection_length = 1;
		this.winners = new ArrayList<Node>();
		this.losers = new ArrayList<Node>();
	}
	
	public void increase_transition_length(){
		detection_length++;
	}

	public boolean sanity_check() {
		if(winners.size() == 2 &&
				losers.size() == 2)
			return true;
		else
			return false;
	}
	
	public void addWinner(Node winner){
		winners.add(winner.getFirst());
	}
	
	public void addLooser(Node loser){
		losers.add(loser.getFirst());
	}
	
	@Override
	public String toString(){
		if(this.sanity_check())
			return String.format("[%d + %d, %d - %d]",
					winners.get(0).getTrackID(),winners.get(1).getTrackID(),
					losers.get(0).getTrackID(),losers.get(1).getTrackID());
			else
				return "Sanity check not passed";
				
	}

	/**
	 * Test whether the T1 transition is active by 
	 * checking if the gained edge still exists at
	 * the frame of input.
	 * 
	 * @param frame Frame in which to test the transition existence
	 * @return true if the gained edge exists
	 */
	public boolean isActive(FrameGraph frame) {
		
		if(frame.getFrameNo() < detection_time_point)
			return false; //assumption that the gained edge did not exist beforehand
		else{
			Node source = frame.getEdgeSource(gained_edge);
			Node target = frame.getEdgeTarget(gained_edge);
			
			assert source != target: "Same node for source and target!";
			
			while(source.hasNext()){
				if(source.getBelongingFrame() != frame)
					break;
				else
					source = source.getNext();
			}
			
			while(target.hasNext()){
				if(target.getBelongingFrame() != frame)
					break;
				else
					target = target.getNext();
			}
			
			assert source.getBelongingFrame() == target.getBelongingFrame(): "Final nodes do not belong to same frame!";
			
			if(frame.containsEdge(source, target))
				return true;
			else
				return false;
		}
	}
}
