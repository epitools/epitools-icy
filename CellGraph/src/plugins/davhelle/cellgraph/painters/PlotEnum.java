package plugins.davhelle.cellgraph.painters;

public enum PlotEnum{
	
	TEST("Test Overlay"),
	CELL_CENTROIDS("Simple Overlay to show cells and their outlines in a color of choice"),
	CELL_AREA("Overlay to color cells according to their area size in a gradient fashion"),
	TISSUE_BORDER("Overlay to show where the border of the segmentation was identified"), 
	
	VORONOI_DIAGRAM("Overlay displays the voronoi diagram computed from the cell centroids"), 
	POLYGON_CLASS("Displays the number of neighbors each cell has with color code or number"),
	POLYGON_TILE("Overlay that simplifies the geometry of each cell to have straight edges"),
	GRAPH_VIEW("Shows the connectivity (neighbors) of each cell"),
	
	CELL_TRACKING("Overlay to review the tracking in case it has been eliminated or to highlight different aspects"),
	ALWAYS_TRACKED_CELLS("Highlights only the cells that have been continuously tracked throughout the time lapse"),
	DIVISIONS_AND_ELIMINATIONS("Highlights the cells that underwent division or elimination"),
	CORRECTION_HINTS("Overlay to help identifying cells which have been segmented wrongly, best used in combination with CellEditor plugin"),
	
	GRAPHML_EXPORT("Exports the currently loaded graph into a GraphML file"),
	WRITE_OUT_DDN("Statistics output"),
	CELL_COLOR_TAG("Overlay to interactively mark cells with a color of choice (after excecution the color can be still changed), export with SAVE_COLOR TAG"),
	SAVE_COLOR_TAG("Saves the area of each cell in CSV that has been marked with the CELL_COLOR_TAG overlay"),
	SAVE_COLOR_TAG_XLS("Saves the area of each cell in XLS that has been marked with the CELL_COLOR_TAG overlay"),
	SAVE_SKELETONS("Saves the imported skeletons with modifications (e.g. small cell removal/border removal) as separate set"),
	
	T1_TRANSITIONS("Computes and displays the T1 transitions present in the time lapse [time consuming!]"), 
	EDGE_STABILITY("Displays a color code for how stable edges are (green=stable, red=not stable)[time consuming!]"),
	EDGE_INTENSITY("Transforms the edge geometries into ROIs and displays the underlying intensity of the image [time consuming!]"),
	NEIGHBOR_STABILITY("An overlay to display the stability of each neighbor (graph based)"),  
	
	ELLIPSE_FIT("Fits an ellipse to each cell geometry and displays the longest axis"),
	ELLIPSE_FIT_WRT_POINT_ROI("To enable this plugin a Point ROI must be present on the image. The overlay computes the angle towards the ELLIPSE_FIT"),
	ELONGATION_RATIO("Color codes the cell according to their elongation ratio"),
	DIVSION_ORIENTATION("Color codes the dividing cells according to their new junction orientation (Longest axis of mother cell vs New junction"),
	PDF_SCREENSHOT("Generates a screenshot in PDF format");
	
	private String description;
	private PlotEnum(String description){this.description = description;}
	public String getDescription(){return description;}
}
