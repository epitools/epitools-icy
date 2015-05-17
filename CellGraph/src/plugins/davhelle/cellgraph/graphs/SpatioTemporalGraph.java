package plugins.davhelle.cellgraph.graphs;

/**
 * Parent class of {@link FrameGraph} containing all frames belonging 
 * to one series. With the frames being tracked, a temporal connectivity is
 * added to the spatial connectivity of the individual frame. This is
 * the reason for naming the structure spatio - temporal - graph;
 * 
 * @author Davide Heller
 * 
 */
public interface SpatioTemporalGraph {
	
	/**
	 * Method to access a specific FrameGraph by it's temporal
	 * index/frame no. 
	 * 
	 * @param frame_no Index of the Tissue Graph to be extracted
	 * @return The TissueGraph representing the frame number in input
	 */
	public FrameGraph getFrame(int frame_no);
	
	/**
	 * Method to set a specific FrameGraph at a given time point
	 * -frame number.
	 * 
	 * @param graph	FrameGraph to be inserted into stGraph structure
	 * @param frame_no Time point at which the insertion takes place
	 */
	public void setFrame(FrameGraph graph, int frame_no);

	/**
	 * Get the number of time points/frames represented
	 * 
	 * @return number of frames represented
	 */
	public int size();
	
	
	/** 
	 * @return if tracking has been applied to the nodes
	 */
	public boolean hasTracking();
	
	
	/**
	 * @return if a voronoi diagram has been computed for the nodes
	 */
	public boolean hasVoronoi();
	
	/**
	 * @return true if stGraph nodes were fit with an ellipse
	 */
	public boolean hasEllipseFitting();
	
	/** 
	 * @return changes tracking state
	 */
	public void setTracking(boolean new_state);
	
	
	/**
	 * @return change voronoi state
	 */
	public void setVoronoi(boolean new_state);
	
	/**
	 * set whether or not an ellipse fitting is present
	 * @param new_state
	 */
	public void setEllipseFitting(boolean new_state);
}
