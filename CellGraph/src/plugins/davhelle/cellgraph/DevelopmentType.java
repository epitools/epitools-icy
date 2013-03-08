package plugins.davhelle.cellgraph;

/**
 * DevelopmentType represents the entire abstraction
 * of the development of the tissue represented through
 * single frames taken and represented trough a TissueGraph. * 
 * 
 * @author Davide Heller
 * 
 */
public interface DevelopmentType {
	
	/**
	 * Method to access a specific TissueGraph by it's temporal
	 * index/frame no. 
	 * 
	 * @param frame_no Index of the Tissue Graph to be extracted
	 * @return The TissueGraph representing the frame number in input
	 */
	public TissueGraph getFrame(int frame_no);
	
	/**
	 * Method to set a specific TissueGraph at a given time point
	 * -frame number.
	 * 
	 * @param graph	TissueGraph to be inserted into Time structure
	 * @param frame_no Time point at which the insertion takes place
	 */
	public void setFrame(TissueGraph graph, int frame_no);

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
}
