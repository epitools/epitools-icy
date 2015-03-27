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
	//NEIGHBOR_STABILITY("An overlay to display the stability of each neighbor (graph based)"),  

	TEST("Test Overlay"),
	CELL_OUTLINE(PolygonOverlay.DESCRIPTION),
	CELL_AREA(AreaGradientOverlay.DESCRIPTION),
	SEGMENTATION_BORDER(BorderOverlay.DESCRIPTION), 
	
	VORONOI_DIAGRAM(VoronoiOverlay.DESCRIPTION), 
	POLYGON_CLASS(PolygonClassOverlay.DESCRIPTION),
	GRAPH_VIEW(GraphOverlay.DESCRIPTION),
	
	CELL_TRACKING(TrackingOverlay.DESCRIPTION),
	CELL_DISPLACEMENT(DisplacementOverlay.DESCRIPTION),
	ALWAYS_TRACKED_CELLS(AlwaysTrackedCellsOverlay.DESCRIPTION),
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
