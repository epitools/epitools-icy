package plugins.davhelle.cellgraph.graphs;

import java.util.ArrayList;

/**
 * Standard implementation of StGraph for representing developing tissues
 * like the imaginal wing disk of Drosophila melanogaster.
 * 
 * @author Davide Heller
 *
 */
public class TissueEvolution implements SpatioTemporalGraph {

	/**
	 * Container for individual time points
	 */
	private ArrayList<FrameGraph> frames;
	/**
	 * Flag if temporal connectivity has been added to the graph
	 */
	private boolean has_tracking;
	/**
	 * Flag if a voronoi diagram has been computed for the graph 
	 */
	private boolean has_voronoi;
	/**
	 * Flag if ellipses have been fitted to the indiviual cell polygons
	 */
	private boolean has_ellipse_fitting;
	
	/**
	 * Initialization with number of time points
	 */
	public TissueEvolution(int time_points) {
		this.has_tracking = false;
		this.has_voronoi = false;
		this.has_ellipse_fitting = false;
		this.frames = new ArrayList<FrameGraph>(time_points);
	}
	
	/**
	 * Default initialization with 0 time points
	 */
	public TissueEvolution(){
		this(0);
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.graphs.DevelopmentType#getFrame(int)
	 */
	@Override
	public FrameGraph getFrame(int frame_no) {
		return frames.get(frame_no);
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.graphs.DevelopmentType#setFrame(plugins.davhelle.cellgraph.graphs.TissueGraph, int)
	 */
	@Override
	public void setFrame(FrameGraph graph, int frame_no) {
		if(frames.size() > frame_no)
			frames.set(frame_no, graph);
		else
			frames.add(graph);
	}
	
	public void addFrame(FrameGraph graph){
		frames.add(graph);
	}

	@Override
	public int size() {
		return frames.size();
	}

	@Override
	public boolean hasTracking() {
		return has_tracking;
	}

	@Override
	public boolean hasVoronoi() {
		return has_voronoi;
	}

	@Override
	public void setTracking(boolean new_state) {
		this.has_tracking = new_state;
	}

	@Override
	public void setVoronoi(boolean new_state){
		this.has_voronoi = new_state;
	}

	@Override
	public boolean hasEllipseFitting() {
		return has_ellipse_fitting;
	}

	@Override
	public void setEllipseFitting(boolean new_state) {
		this.has_ellipse_fitting = new_state;  
	}

}
