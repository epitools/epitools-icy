/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.graphs;

/**
 * DevelopmentType represents the entire abstraction
 * of the development of the tissue represented through
 * single frames taken and represented trough a TissueGraph. * 
 * 
 * @author Davide Heller
 * 
 */
public interface SpatioTemporalGraph {
	
	/**
	 * Method to access a specific TissueGraph by it's temporal
	 * index/frame no. 
	 * 
	 * @param frame_no Index of the Tissue Graph to be extracted
	 * @return The TissueGraph representing the frame number in input
	 */
	public FrameGraph getFrame(int frame_no);
	
	/**
	 * Method to set a specific TissueGraph at a given time point
	 * -frame number.
	 * 
	 * @param graph	TissueGraph to be inserted into Time structure
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
