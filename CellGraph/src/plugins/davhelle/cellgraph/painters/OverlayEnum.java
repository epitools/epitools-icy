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
	
	TEST("Test Overlay"),
	CELL_OVERLAY(PolygonOverlay.DESCRIPTION),
	CELL_AREA(AreaGradientOverlay.DESCRIPTION),
	SEGMENTATION_BORDER(BorderOverlay.DESCRIPTION), 
	
	VORONOI_DIAGRAM(VoronoiPainter.DESCRIPTION), 
	POLYGON_CLASS(PolygonClassOverlay.DESCRIPTION),
	//POLYGON_TILE("Overlay that simplifies the geometry of each cell to have straight edges"),
	GRAPH_VIEW(GraphOverlay.DESCRIPTION),
	
	CELL_TRACKING("Overlay to review the tracking in case it has been eliminated or to highlight different aspects"),
	//ALWAYS_TRACKED_CELLS("Highlights only the cells that have been continuously tracked throughout the time lapse"),
	DIVISIONS_AND_ELIMINATIONS(DivisionOverlay.DESCRIPTION),
	CORRECTION_HINTS(CorrectionOverlay.DESCRIPTION),
	
	GRAPHML_EXPORT("Exports the currently loaded graph into a GraphML file"),
	//WRITE_OUT_DDN("Statistics output"),
	CELL_COLOR_TAG(CellMarkerOverlay.DESCRIPTION),
	//SAVE_SKELETONS("Saves the imported skeletons with modifications (e.g. small cell removal/border removal) as separate set"),
	
	T1_TRANSITIONS("Computes and displays the T1 transitions present in the time lapse [time consuming!]"), 
	EDGE_STABILITY(EdgeStabilityOverlay.DESCRIPTION),
	EDGE_INTENSITY(EdgeIntensityOverlay.DESCRIPTION),
	//NEIGHBOR_STABILITY("An overlay to display the stability of each neighbor (graph based)"),  
	
	ELLIPSE_FIT(EllipseFitterOverlay.DESCRIPTION),
	ELLIPSE_FIT_WRT_POINT_ROI(EllipseFitColorOverlay.DESCRIPTION),
	ELONGATION_RATIO(ElongationRatioOverlay.DESCRIPTION), // could add csv option here
	PDF_SCREENSHOT("Generates a screenshot in PDF format"),
	DIVSION_ORIENTATION(DivisionOrientationOverlay.DESCRIPTION);
	
	private String description;
	private OverlayEnum(String description){this.description = description;}
	public String getDescription(){return description;}
}
