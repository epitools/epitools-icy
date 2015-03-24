/*=========================================================================
 *
 *  (C) Copyright (2012-2015) Basler Group, IMLS, UZH
 *  
 *  author:	Davide Heller
 *  email:	davide.heller@imls.uzh.ch
 *  
 *=========================================================================*/

package plugins.davhelle.cellgraph.painters;

/**
 * Enumeration of available overlays in the CellOverlay-Plugin
 * 
 * @author Davide Heller
 *
 */
public enum OverlayEnum{
	//on hold
	//POLYGON_TILE("Overlay that simplifies the geometry of each cell to have straight edges"),
	//ALWAYS_TRACKED_CELLS("Highlights only the cells that have been continuously tracked throughout the time lapse"),
	//WRITE_OUT_DDN("Statistics output"),
	//SAVE_SKELETONS("Saves the imported skeletons with modifications (e.g. small cell removal/border removal) as separate set"),
	//NEIGHBOR_STABILITY("An overlay to display the stability of each neighbor (graph based)"),  
	
	//To be transfered to CellEditor
	PDF_SCREENSHOT("Generates a screenshot in PDF format"),
	GRAPHML_EXPORT("Exports the currently loaded graph into a GraphML file"),
	
	
	TEST("Test Overlay"),
	CELL_OVERLAY(PolygonOverlay.DESCRIPTION),
	CELL_AREA(AreaGradientOverlay.DESCRIPTION),
	SEGMENTATION_BORDER(BorderOverlay.DESCRIPTION), 
	
	VORONOI_DIAGRAM(VoronoiOverlay.DESCRIPTION), 
	POLYGON_CLASS(PolygonClassOverlay.DESCRIPTION),
	GRAPH_VIEW(GraphOverlay.DESCRIPTION),
	
	CELL_TRACKING(TrackingOverlay.DESCRIPTION),
	DIVISIONS_AND_ELIMINATIONS(DivisionOverlay.DESCRIPTION),
	CORRECTION_HINTS(CorrectionOverlay.DESCRIPTION),
	CELL_COLOR_TAG(CellMarkerOverlay.DESCRIPTION),
	
	T1_TRANSITIONS(TransitionOverlay.DESCRIPTION), 
	EDGE_STABILITY(EdgeStabilityOverlay.DESCRIPTION),
	EDGE_INTENSITY(EdgeIntensityOverlay.DESCRIPTION),
	
	ELLIPSE_FIT(EllipseFitterOverlay.DESCRIPTION),
	ELLIPSE_FIT_WRT_POINT_ROI(EllipseFitColorOverlay.DESCRIPTION),
	ELONGATION_RATIO(ElongationRatioOverlay.DESCRIPTION),
	DIVSION_ORIENTATION(DivisionOrientationOverlay.DESCRIPTION);
	
	private String description;
	private OverlayEnum(String description){this.description = description;}
	public String getDescription(){return description;}
}
