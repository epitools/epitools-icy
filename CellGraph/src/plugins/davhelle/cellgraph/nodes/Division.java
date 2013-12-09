/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.nodes;

import plugins.davhelle.cellgraph.graphs.FrameGraph;

/**
 * The division object describes the event
 * of a cell division linking the nodes
 * connected to the event and the time
 * point at which the children have been
 * observed for the first time.
 * 
 * Every participating node is mutually
 * linked to the Division object.
 * 
 * 
 * @author Davide Heller
 *
 */
public class Division {
	
	
	//TODO perhaps make the field final, no need for getters
	private Node mother;
	private Node child1;
	private Node child2;
	private int time_point;
	private FrameGraph division_frame;

	public Division(Node mother, Node child1, Node child2, int tracking_id) {
		
		//TODO should mother be mother.first?
		this.mother = mother;
		this.child1 = child1;
		this.child2 = child2;
		this.division_frame = child1.getBelongingFrame();
		this.time_point = division_frame.getFrameNo();
		
		
		//sanity check
		if(division_frame != child2.getBelongingFrame())
			System.out.println("The division children do not share the same time point");
		
		//update children first node (themself)
		child1.setFirst(child1);
		child2.setFirst(child2);
		//tracking id
		child1.setTrackID(tracking_id + 1);
		child2.setTrackID(tracking_id + 2);		
		//division association		
		child1.setDivision(this);
		child2.setDivision(this);
		
		//Update Mother node
		mother.setDivision(this);
		//TODO best choice? or recurrent e.g. like first's first
		mother.setNext(null);
		//division tag, not exactly an error.. TODO
		mother.setErrorTag(-5);
		
		//division notification to frame
		division_frame.addDivision(this);
		
		//division propagation
		Node ancestor = mother.getPrevious();
		while(ancestor != null){
			ancestor.setDivision(this);
			ancestor = ancestor.getPrevious();
		}
		
	}

	/**
	 * @return the mother
	 */
	public Node getMother() {
		return mother;
	}
	/**
	 * @param mother the mother to set
	 */
	public void setMother(Node mother) {
		this.mother = mother;
	}
	/**
	 * @return the child1
	 */
	public Node getChild1() {
		return child1;
	}
	/**
	 * @param child1 the child1 to set
	 */
	public void setChild1(Node child1) {
		this.child1 = child1;
	}
	/**
	 * @return the child2
	 */
	public Node getChild2() {
		return child2;
	}
	/**
	 * @param child2 the child2 to set
	 */
	public void setChild2(Node child2) {
		this.child2 = child2;
	}
	/**
	 * @return the time_point
	 */
	public int getTimePoint() {
		return time_point;
	}
	
	/**
	 * @param time_point the time_point to set
	 */
	public void setTimePoint(int time_point) {
		this.time_point = time_point;
	}
	
	/**
	 * Check whether the participant is the mother
	 * cell of the division
	 * 
	 * @param participant to test
	 * @return true if participant is the mother cell
	 */
	public boolean isMother(Node participant){
		return mother.getFirst() == participant.getFirst();
	}
	
	/**
	 * Check whether the participant is a child
	 * cell of the division
	 * 
	 * @param participant to test
	 * @return true if participant is a child cell
	 */
	public boolean isChild(Node participant){
		if( child1.getFirst() == participant.getFirst())
			return true;
		else
			if(child2.getFirst() == participant.getFirst())
				return true;
			else
				return false;
	}
	
	
	
	/**
	 * Obtain the second child of the division
	 * 
	 * @param participant child of the division
	 * @return other child field, null if participant is not a child node
	 */
	public Node getBrother(Node participant){
		if(!isMother(participant)){
			if(participant.getFirst() == child1)
				return child2;
			else
				return child1;
		}
		else
			return null;
	}
	
	/**
	 * Check the presence of the brother cell in the child nodes neighborhood.
	 * 
	 * @param child node whose neighborhood is considered
	 * @return true if brother cell is in neighborhood, else false (also for wrong input)
	 */
	public boolean isBrotherPresent(Node child){
		Node brother = this.getBrother(child.getFirst());
		
		if(brother != null)
			for(Node neighbor: child.getNeighbors())	
				if(neighbor.getFirst() == brother)
					return true;

		return false;
	}
	
	/**
	 * Check whether brother cell was eliminated at 
	 * a previous time point 
	 * 
	 * @param child
	 * @return
	 */
	public boolean wasBrotherEliminated(Node child){
		Node brother = this.getBrother(child.getFirst());

		if(brother != null)
			if(brother.hasObservedElimination()){
				int current_time = child.getBelongingFrame().getFrameNo();
				int elimination_time = brother.getElimination().getTimePoint();
				if(current_time > elimination_time)
					return true;
			}
		
		return false;
	}
	
	
	public String toString(){
		//Report division to user
		return "\tDivision:"+mother.getTrackID()+"->("+child1.getTrackID()+","+child2.getTrackID()+")";
	}


}
