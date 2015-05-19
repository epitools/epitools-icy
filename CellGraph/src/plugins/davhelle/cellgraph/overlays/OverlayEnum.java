/*=========================================================================
 *
 *  (C) Copyright (2012-2015) Basler Group, IMLS, UZH
 *  
 *  author:	Davide Heller
 *  email:	davide.heller@imls.uzh.ch
 *  
 *=========================================================================*/

package plugins.davhelle.cellgraph.overlays;

/**
 * Enumeration of available overlays in the CellOverlay-Plugin
 * 
 * @author Davide Heller
 *
 */
public enum OverlayEnum{
	
	//on hold
	//POLYGON_TILE("Overlay that simplifies the geometry of each cell to have straight edges"),
	//NEIGHBOR_STABILITY("An overlay to display the stability of each neighbor (graph based)"),  

	TEST("Test Overlay"),
	CELL_OUTLINE(PolygonOverlay.DESCRIPTION),
	CELL_AREA(AreaGradientOverlay.DESCRIPTION),
	CELL_SEGMENTATION_BORDER(BorderOverlay.DESCRIPTION), 
	
	CELL_VORONOI_DIAGRAM(VoronoiOverlay.DESCRIPTION), 
	CELL_POLYGON_CLASS(PolygonClassOverlay.DESCRIPTION),
	CELL_GRAPH_VIEW(GraphOverlay.DESCRIPTION),
	
	TRACKING_REVIEW(TrackingOverlay.DESCRIPTION),
	TRACKING_DISPLACEMENT(DisplacementOverlay.DESCRIPTION),
	TRACKING_STABLE_ONLY(AlwaysTrackedCellsOverlay.DESCRIPTION),
	TRACKING_CORRECTION_HINTS(CorrectionOverlay.DESCRIPTION),
	
	DIVISIONS_AND_ELIMINATIONS(DivisionOverlay.DESCRIPTION),
	DIVISION_ORIENTATION(DivisionOrientationOverlay.DESCRIPTION),
	
	CELL_COLOR_TAG(CellMarkerOverlay.DESCRIPTION),
	EDGE_COLOR_TAG(EdgeMarkerOverlay.DESCRIPTION),
	
	EDGE_T1_TRANSITIONS(TransitionOverlay.DESCRIPTION), 
	EDGE_STABILITY(EdgeStabilityOverlay.DESCRIPTION),
	EDGE_INTENSITY(EdgeIntensityOverlay.DESCRIPTION),
	
	ELLIPSE_FIT(EllipseFitterOverlay.DESCRIPTION),
	ELLIPSE_FIT_WRT_POINT_ROI(EllipseFitColorOverlay.DESCRIPTION),
	ELLIPSE_ELONGATION_RATIO(ElongationRatioOverlay.DESCRIPTION);
	
	private String description;
	private OverlayEnum(String description){this.description = description;}
	public String getDescription(){return description;}
	
}
