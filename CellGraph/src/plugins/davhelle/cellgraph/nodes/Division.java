package plugins.davhelle.cellgraph.nodes;

import com.vividsolutions.jts.geom.Geometry;

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
	
	/**
	 * Node which divides
	 */
	private Node mother;
	/**
	 * 1st Node originating from the division
	 */
	private Node child1;
	/**
	 * 2nd Node originating from the division 
	 */
	private Node child2;
	/**
	 * Time point of the division. Defined as the frame in which the two children cells appear.
	 */
	private int time_point;
	/**
	 * Frame in which the division occurs, i.e. corresponding to the time point of division
	 */
	private FrameGraph division_frame;
	
	
	//Division Orientation analysis fields
	
	/**
	 * Geometry of the initial plane between the two children
	 */
	private Geometry planeGeometry;
	/**
	 * Angle between new junction and longest axis of mother cell
	 */
	private double divisionOrientation;
	/**
	 * Angle of the longest axis of the ellipse fitted to the mother cell
	 */
	private double longestMotherAxisOrientation;
	/**
	 * Angle of the new junction geometry between the two children cells
	 */
	private double newJunctionOrientation;

	/**
	 * Initialize the Division object. The children cells are given the
	 * tracking id + 1 & + 2
	 * 
	 * @param mother node which divides
	 * @param child1 first child node
	 * @param child2 second child node
	 * @param tracking_id tracking id of the mother cell
	 */
	public Division(Node mother, Node child1, Node child2, int tracking_id) {
		
		//set basic fields
		this.mother = mother;
		this.child1 = child1;
		this.child2 = child2;
		this.division_frame = child1.getBelongingFrame();
		this.time_point = division_frame.getFrameNo();
		
		//Initialize Division orientation fields
		divisionOrientation = 0.0;
		longestMotherAxisOrientation = 0.0;
		newJunctionOrientation = 0.0;
		planeGeometry = null;
		
		//sanity check
		if(division_frame != child2.getBelongingFrame())
			System.out.println("The division children do not share the same time point");
		
		//update children first node (themselves)
		child1.setFirst(child1);
		child2.setFirst(child2);
		//tracking id
		child1.setTrackID(tracking_id + 1);
		child2.setTrackID(tracking_id + 2);		
		//division association		
		child1.setDivision(this);
		child2.setDivision(this);
		
		//experimental, to allow multiple divisions former setDivision would be removed
		child1.setOrigin(this);
		child2.setOrigin(this);
		
		//Update Mother node
		mother.setDivision(this);
		//mother has no further future linking
		mother.setNext(null);
		//division tag for TrackingOverlay
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
	 * Define division assuming that child track-IDs have already been 
	 * set and the entire graph has been tracked.
	 * 
	 * Propagation is therefore done both forwards as backwards
	 * 
	 * @param mother node dividing
	 * @param child1 first children node
	 * @param child2 second children node
	 */
	public Division(Node mother, Node child1, Node child2) {
		
		//set main fields
		this.mother = mother;
		this.child1 = child1;
		this.child2 = child2;
		this.division_frame = child1.getBelongingFrame();
		this.time_point = division_frame.getFrameNo();
		
		//sanity check
		if(division_frame != child2.getBelongingFrame())
			System.out.println("The division children do not share the same time point");
		
		child1.setDivision(this);
		child2.setDivision(this);
		mother.setDivision(this);
		mother.setErrorTag(-5);
		
		//division notification to frame
		division_frame.addDivision(this);
		
		//propagation
		Node ancestor = mother.getPrevious();
		while(ancestor != null){
			ancestor.setDivision(this);
			ancestor = ancestor.getPrevious();
		}
			
		Node future1 = child1.getNext();
		while(future1 != null){
			future1.setDivision(this);
			future1 = future1.getNext();
		}

		Node future2 = child2.getNext();
		while(future2 != null){
			future2.setDivision(this);
			future2 = future2.getNext();
		}
		
		//Initialize Division orientation fields
		divisionOrientation = 0.0;
		longestMotherAxisOrientation = 0.0;
		newJunctionOrientation = 0.0;
		planeGeometry = null;
		
	}

	/**
	 * @return the node that divides
	 */
	public Node getMother() {
		return mother;
	}
	/**
	 * @param mother the node that divides
	 */
	public void setMother(Node mother) {
		this.mother = mother;
	}
	/**
	 * @return the first child node
	 */
	public Node getChild1() {
		return child1;
	}
	/**
	 * @param child1 the first child node
	 */
	public void setChild1(Node child1) {
		this.child1 = child1;
	}
	/**
	 * @return the second child node
	 */
	public Node getChild2() {
		return child2;
	}
	/**
	 * @param child2 the second child node
	 */
	public void setChild2(Node child2) {
		this.child2 = child2;
	}
	/**
	 * @return the time point at which the division occurs
	 */
	public int getTimePoint() {
		return time_point;
	}
	
	/**
	 * @param time_point time point at which the division occurs
	 */
	public void setTimePoint(int time_point) {
		this.time_point = time_point;
	}
	
	/**
	 * Check whether a node is the mother
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

	/**
	 * @return the orientation of the division, i.e. the angle between the longest axis of the mother cell and the children axis
	 */
	public double getDivisionOrientation() {
		return divisionOrientation;
	}

	/**
	 * @param divisionOrientation the division orientation to set
	 */
	public void setDivisionOrientation(double divisionOrientation) {
		this.divisionOrientation = divisionOrientation;
	}

	/**
	 * @return the angle of the longest axis of the ellipse fitted to the mother cell
	 */
	public double getLongestMotherAxisOrientation() {
		return longestMotherAxisOrientation;
	}

	/**
	 * @param longestMotherAxisOrientation the angle of the longest axis of the ellipse fitted to the mother cell
	 */
	public void setLongestMotherAxisOrientation(double longestMotherAxisOrientation) {
		this.longestMotherAxisOrientation = longestMotherAxisOrientation;
	}

	/**
	 * @return the newJunctionOrientation the angle of the junction between the children cells
	 */
	public double getNewJunctionOrientation() {
		return newJunctionOrientation;
	}

	/**
	 * @param newJunctionOrientation the angle of the junction between the children cells
	 */
	public void setNewJunctionOrientation(double newJunctionOrientation) {
		this.newJunctionOrientation = newJunctionOrientation;
	}

	/**
	 * @return the initial geometry of the plane connecting the two children cells
	 */
	public Geometry getPlaneGeometry() {
		return planeGeometry;
	}

	/**
	 * @param planeGeometry the initial geometry of the plane connecting the two children cells
	 */
	public void setPlaneGeometry(Geometry planeGeometry) {
		this.planeGeometry = planeGeometry;
	}
	
	/**
	 * @return true if the initial plane geometry has been set
	 */
	public boolean hasPlaneGeometry(){
		return this.planeGeometry != null;
	}




}
