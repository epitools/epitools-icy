package plugins.davhelle.cellgraph.overlays;

import plugins.davhelle.cellgraph.CellOverlay;

/**
 * Enumeration of available overlays in the {@link CellOverlay} plugin
 * 
 * @author Davide Heller
 *
 */
public enum OverlayEnum{
	
	TEST(TestOverlay.DESCRIPTION),
	CELL_OUTLINE(PolygonOverlay.DESCRIPTION),
	CELL_AREA(AreaGradientOverlay.DESCRIPTION),
	CELL_SEGMENTATION_BORDER(BorderOverlay.DESCRIPTION), 
	
	CELL_VORONOI_DIAGRAM(VoronoiOverlay.DESCRIPTION), 
	CELL_POLYGON_CLASS(PolygonClassOverlay.DESCRIPTION),
	CELL_GRAPH_VIEW(GraphOverlay.DESCRIPTION),
	CELL_PROJECTION(ProjectionOverlay.DESCRIPTION), 
	
	TRACKING_REVIEW(TrackingOverlay.DESCRIPTION),
	TRACKING_DISPLACEMENT(DisplacementOverlay.DESCRIPTION),
	TRACKING_STABLE_ONLY(AlwaysTrackedCellsOverlay.DESCRIPTION),
	TRACKING_CORRECTION_HINTS(CorrectionOverlay.DESCRIPTION),
	//TRACKING_FLOW(FlowOverlay.DESCRIPTION),
	
	DIVISIONS_AND_ELIMINATIONS(DivisionOverlay.DESCRIPTION),
	DIVISION_ORIENTATION(DivisionOrientationOverlay.DESCRIPTION),
	
	CELL_COLOR_TAG(CellColorTagOverlay.DESCRIPTION),
	EDGE_COLOR_TAG(EdgeColorTagOverlay.DESCRIPTION),
	
	EDGE_T1_TRANSITIONS(TransitionOverlay.DESCRIPTION), 
	EDGE_STABILITY(EdgeStabilityOverlay.DESCRIPTION),
	EDGE_ORIENTATION(EdgeOrientationOverlay.DESCRIPTION),

	EDGE_INTENSITY(EdgeIntensityOverlay.DESCRIPTION),
	CELL_INTENSITY(CellIntensityOverlay.DESCRIPTION), 
	
	ELLIPSE_FIT(EllipseFitterOverlay.DESCRIPTION),
	ELLIPSE_FIT_WRT_POINT_ROI(EllipseFitColorOverlay.DESCRIPTION),
	ELLIPSE_ELONGATION_RATIO(ElongationRatioOverlay.DESCRIPTION); 
	
	/**
	 * Description String visualized by the {@link CellOverlay} plugin
	 */
	private String description;
	/**
	 * @param description the description of the overlay
	 */
	private OverlayEnum(String description){this.description = description;}
	/**
	 * @return the description of the overlay
	 */
	public String getDescription(){return description;}
	
}
