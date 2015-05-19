/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;

import java.util.List;

import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarFile;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.export.ExportFieldType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.CellColor;
import plugins.davhelle.cellgraph.misc.VoronoiGenerator;
import plugins.davhelle.cellgraph.overlays.AlwaysTrackedCellsOverlay;
import plugins.davhelle.cellgraph.overlays.AreaGradientOverlay;
import plugins.davhelle.cellgraph.overlays.BorderOverlay;
import plugins.davhelle.cellgraph.overlays.CellColorTagOverlay;
import plugins.davhelle.cellgraph.overlays.CentroidOverlay;
import plugins.davhelle.cellgraph.overlays.CorrectionOverlay;
import plugins.davhelle.cellgraph.overlays.DisplacementOverlay;
import plugins.davhelle.cellgraph.overlays.DivisionOrientationOverlay;
import plugins.davhelle.cellgraph.overlays.DivisionOverlay;
import plugins.davhelle.cellgraph.overlays.EdgeIntensityOverlay;
import plugins.davhelle.cellgraph.overlays.EdgeMarkerOverlay;
import plugins.davhelle.cellgraph.overlays.EdgeStabilityOverlay;
import plugins.davhelle.cellgraph.overlays.EllipseFitColorOverlay;
import plugins.davhelle.cellgraph.overlays.EllipseFitterOverlay;
import plugins.davhelle.cellgraph.overlays.ElongationRatioOverlay;
import plugins.davhelle.cellgraph.overlays.GraphOverlay;
import plugins.davhelle.cellgraph.overlays.OverlayEnum;
import plugins.davhelle.cellgraph.overlays.PolygonClassOverlay;
import plugins.davhelle.cellgraph.overlays.PolygonConverterPainter;
import plugins.davhelle.cellgraph.overlays.PolygonOverlay;
import plugins.davhelle.cellgraph.overlays.StGraphOverlay;
import plugins.davhelle.cellgraph.overlays.TrackIdOverlay;
import plugins.davhelle.cellgraph.overlays.TrackingOverlay;
import plugins.davhelle.cellgraph.overlays.TransitionOverlay;
import plugins.davhelle.cellgraph.overlays.VoronoiAreaDifferenceOverlay;
import plugins.davhelle.cellgraph.overlays.VoronoiOverlay;


/**
 * Plugin containing all the visualizations based
 * on a formerly created spatioTemporal graph
 * which has been placed into the ICY data sharing
 * architecture "Swimming Pool".
 * 
 * Alpha version!
 * 
 * @author Davide Heller
 *
 */
public class CellOverlay extends EzPlug implements EzVarListener<OverlayEnum>{
	
	EzVarBoolean				varRemovePainterFromSequence;
	EzVarBoolean				varUpdatePainterMode;
	
	EzVarEnum<OverlayEnum> 		varPlotting;

	EzVarBoolean				varBooleanPolygon;
	EzVarBoolean				varBooleanDerivedPolygons;
	EzVarBoolean				varBooleanCCenter;

	EzVarBoolean				varBooleanAreaDifference;
	EzVarBoolean				varBooleanVoronoiDiagram;
	EzVarBoolean				varBooleanWriteCenters;
	EzVarBoolean				varBooleanWriteArea;
	
	//EzVarBoolean				varBooleanReadDivisions;
	
	//Tracking Mode
	EzVarBoolean				varBooleanCellIDs;
	EzVarBoolean				varBooleanHighlightMistakesBoolean;
	EzVarInteger 				varDisplacementThreshold;
	
	EzVarDouble					varAreaThreshold;
	EzVarDouble					varIntensitySlider;
	
	//Graph Export
	EzVarEnum<ExportFieldType>  varExportType;
	EzVarInteger				varFrameNo;
	
	//CellMarker
	EzVarEnum<CellColor>		varCellColor;
	
	EzLabel varDescriptionLabel;
	//sequence to paint on 
	EzVarSequence				varSequence;
	Sequence sequence;
	
	EzVarFile				varOutputFile;
	
	private EzVarBoolean varBooleanPlotDivisions;
	private EzVarBoolean varBooleanPlotEliminations;
	private EzVarBoolean varBooleanFillCells;
	private EzVarBoolean varBooleanConnectDaughterCells;
	private EzVarInteger varHighlightClass;
	private EzVarBoolean varBooleanColorClass;
	private EzVarEnum<CellColor> varPolygonColor;
	public EzVarInteger varMinimalTransitionLength;
	public EzVarInteger varMinimalOldSurvival;
	private EzVarBoolean varFillingCheckbox;
	private EzVarBoolean varDrawColorTag;
	private EzVarInteger varDoDetectionDistance;
	private EzVarInteger varDoDetectionLength;
	private EzVarEnum<CellColor> varEdgeColor;
	private EzVarInteger varEnvelopeBuffer;
	private EzVarInteger varEnvelopeBuffer2;
	private EzVarBoolean varBooleanNormalize;
	private EzVarInteger varIntegerChannel;

	@Override
	protected void initialize() {
		
		this.getUI().setRunButtonText("Add Overlay");
		this.getUI().setParametersIOVisible(false);
		
		//Deprecated
		varUpdatePainterMode = new EzVarBoolean("Update painter", false);

		//Cells view
		varBooleanPolygon = new EzVarBoolean("Polygons", true);
		varBooleanCCenter = new EzVarBoolean("Centers", true);
		varBooleanWriteCenters = new EzVarBoolean("Write cell centers to disk",false);
		varBooleanDerivedPolygons = new EzVarBoolean("Derived Polygons", false);
		varPolygonColor = new EzVarEnum<CellColor>("Polygon color", CellColor.values(),CellColor.RED);
		EzGroup groupCellMap = new EzGroup("Overlay elements",
				varBooleanPolygon,
				varPolygonColor,
				varBooleanDerivedPolygons,
				varBooleanCCenter,
				varBooleanWriteCenters);
		

		//Voronoi view
		varBooleanVoronoiDiagram = new EzVarBoolean("Voronoi Diagram", true);
		varBooleanAreaDifference = new EzVarBoolean("Area difference", false);
		
		EzGroup groupVoronoiMap = new EzGroup("Overlay elements",
				varBooleanAreaDifference,
				varBooleanVoronoiDiagram);	
		
		//Polygon No view
		//TODO add color code!
		varBooleanColorClass = new EzVarBoolean("Draw Numbers", false);
		varHighlightClass = new EzVarInteger("Highlight class (0=none)",0,0,10,1);
		EzGroup groupPolygonClass = new EzGroup("Overlay elements",
				varBooleanColorClass,
				varHighlightClass);
		
		//Area Threshold View
		varAreaThreshold = new EzVarDouble("Area threshold", 0.9, 0, 10, 0.1);
		
		EzGroup groupAreaThreshold = new EzGroup("Overlay elements",
				varAreaThreshold);
		
		//Which painter should be shown by default
		varPlotting = new EzVarEnum<OverlayEnum>("Overlay",
				OverlayEnum.values(),OverlayEnum.CELL_OUTLINE);
		

		//Division mode
		varBooleanPlotDivisions = new EzVarBoolean("Highlight divisions",true);
		varBooleanPlotEliminations = new EzVarBoolean("Highlight eliminations",false);
		varBooleanFillCells = new EzVarBoolean("Fill cells with color",true);
		varBooleanConnectDaughterCells = new EzVarBoolean("Connect daughter cells with line",false);
		EzGroup groupDivisions = new EzGroup("Overlay elements", 
				//varBooleanReadDivisions, TODO
				varBooleanPlotDivisions,
				varBooleanPlotEliminations,
				varBooleanFillCells,
				varBooleanConnectDaughterCells
				);
		
		
		
		//TrackingMode
		varBooleanCellIDs = new EzVarBoolean("Write TrackIDs", true);
		varBooleanHighlightMistakesBoolean = new EzVarBoolean("TrackColor [outline/fill]", true);
		
		EzGroup groupTracking = new EzGroup("Overlay elements",
				varBooleanCellIDs,
				varBooleanHighlightMistakesBoolean);
		
		varDisplacementThreshold = new EzVarInteger("Displacement threshold [x]", 1, 100, 1);
		EzGroup groupDisplacement = new EzGroup("Overlay elements",
				varDisplacementThreshold);
				
		//CellMarker mode
		varCellColor = new EzVarEnum<CellColor>("Cell color", CellColor.values(), CellColor.GREEN);
		varDrawColorTag = new EzVarBoolean("Only outline cells", false);
		EzGroup groupMarker = new EzGroup("Overlay elements",
				varCellColor,
				varDrawColorTag);
		
		//EdgeMarker mode
		varEdgeColor = new EzVarEnum<CellColor>("Edge color", CellColor.values(), CellColor.CYAN);
		varEnvelopeBuffer = new EzVarInteger("Intensity Buffer [px]", 1, 10, 1);
		EzGroup groupEdgeMarker = new EzGroup("Overlay elements",
				varEdgeColor,
				varEnvelopeBuffer);

		//Save transitions
		varMinimalTransitionLength = new EzVarInteger("Minimal transition length [frames]",5,1,100,1);
		varMinimalOldSurvival = new EzVarInteger("Minimal old edge persistence [frames]",5,1,100,1);
		EzGroup groupTransitions = new EzGroup("Overlay elements",
				varMinimalTransitionLength,
				varMinimalOldSurvival);
		
		//Edge Intensity
		varIntensitySlider = new EzVarDouble("Color Scaling", 
				0.5, 0.1, 1.0, 0.05);//, RangeEditorType.SLIDER,new HashMap<Double,String>());
		varFillingCheckbox = new EzVarBoolean("Fill edge masks", true);
		varEnvelopeBuffer2 = new EzVarInteger("Intensity Buffer [px]", 3, 1, 10, 1);
		varBooleanNormalize = new EzVarBoolean("Normalize intensity",true);
		varIntegerChannel = new EzVarInteger("Channel to measure",0,0,3,1);
		EzGroup groupEdgeIntensity = new EzGroup("Edge Intensity elements",
				varEnvelopeBuffer2,
				varIntegerChannel,
				varBooleanNormalize,
				varIntensitySlider,
				varFillingCheckbox
				);
		
		//Division orientation variable
		varDoDetectionDistance = new EzVarInteger("Detection start",11,1,100,1);
		varDoDetectionLength = new EzVarInteger("Detection length",5,1,100,1);
		EzGroup groupDivisionOrientation = new EzGroup("Overlay elements",
				varDoDetectionDistance,	
				varDoDetectionLength);
		
		
		//Description label
		varPlotting.addVarChangeListener(this);
		varDescriptionLabel = new EzLabel(varPlotting.getValue().getDescription());
		EzGroup groupDescription = new EzGroup("Overlay summary",
				varDescriptionLabel);
		
		//Painter
		EzGroup groupPainters = new EzGroup("1. SELECT OVERLAY TO ADD",
				//varUpdatePainterMode,
				varPlotting,
				groupDescription,
				groupCellMap,
				groupPolygonClass,
				groupVoronoiMap,
				groupAreaThreshold,
				groupDivisions,
				groupTracking,
				groupDisplacement,
				groupMarker,
				groupEdgeMarker,
				groupTransitions,
				groupEdgeIntensity,
				groupDivisionOrientation
				
		);
		
		varPlotting.addVisibilityTriggerTo(groupCellMap, OverlayEnum.CELL_OUTLINE);
		varPlotting.addVisibilityTriggerTo(groupPolygonClass, OverlayEnum.CELL_POLYGON_CLASS);
		varPlotting.addVisibilityTriggerTo(groupVoronoiMap, OverlayEnum.CELL_VORONOI_DIAGRAM);
		varPlotting.addVisibilityTriggerTo(groupAreaThreshold, OverlayEnum.CELL_AREA);
		//TODO varInput.addVisibilityTriggerTo(varBooleanDerivedPolygons, InputType.SKELETON);
		varPlotting.addVisibilityTriggerTo(groupTracking, OverlayEnum.TRACKING_REVIEW);
		varPlotting.addVisibilityTriggerTo(groupDisplacement, OverlayEnum.TRACKING_DISPLACEMENT);
		varPlotting.addVisibilityTriggerTo(groupDivisions, OverlayEnum.DIVISIONS_AND_ELIMINATIONS);
		varPlotting.addVisibilityTriggerTo(groupMarker, OverlayEnum.CELL_COLOR_TAG);
		varPlotting.addVisibilityTriggerTo(groupEdgeMarker, OverlayEnum.EDGE_COLOR_TAG);
		varPlotting.addVisibilityTriggerTo(groupTransitions, OverlayEnum.EDGE_T1_TRANSITIONS);
		varPlotting.addVisibilityTriggerTo(groupEdgeIntensity, OverlayEnum.EDGE_INTENSITY);
		varPlotting.addVisibilityTriggerTo(groupDivisionOrientation, OverlayEnum.DIVISION_ORIENTATION);
		super.addEzComponent(groupPainters);
		
		
		//VISUALIZATION GROUP
		varSequence = new EzVarSequence("Image to add overlay to");
		varRemovePainterFromSequence = new EzVarBoolean("Remove previous overlays", false);
		
		EzGroup groupVisualization = new EzGroup("2. SELECT DESTINATION",
				varSequence,
				varRemovePainterFromSequence);
		
		super.addEzComponent(groupVisualization);
	}

	@Override
	protected void execute() {
		sequence = varSequence.getValue();
		
		if(sequence == null){
			new AnnounceFrame("Plugin requires active sequence! Please open an image on which to display results");
			return;
		}
		
		//First boolean choice to remove previous painters
		if(varRemovePainterFromSequence.getValue()){
			List<Overlay> overlays = sequence.getOverlays();
			for (Overlay overlay : overlays) {
				sequence.removeOverlay(overlay);
				sequence.overlayChanged(overlay);    				
			}
		}
		
		// watch if objects are already in the swimming pool:
		//TODO time_stamp collection?

		if(Icy.getMainInterface().getSwimmingPool().hasObjects("stGraph", true))

			for ( SwimmingObject swimmingObject : 
				Icy.getMainInterface().getSwimmingPool().getObjects(
						"stGraph", true) ){

				if ( swimmingObject.getObject() instanceof SpatioTemporalGraph ){

					SpatioTemporalGraph wing_disc_movie = (SpatioTemporalGraph) swimmingObject.getObject();	

					//Data statistics

					//System.out.println("CellVisualizer: loaded stGraph with "+wing_disc_movie.size()+" frames");
					//System.out.println("CellVisualizer:	"+wing_disc_movie.getFrame(0).size()+" cells in first frame");

					//Eliminates the previous painter and runs the 
					//the program (update mode)

					if(varRemovePainterFromSequence.getValue()){
						List<Overlay> overlays = sequence.getOverlays();
						for (Overlay overlay : overlays) {
							sequence.removeOverlay(overlay);
							sequence.overlayChanged(overlay);    				
						}
					}
					
					//Disable previous legends
					List<Overlay> overlays = sequence.getOverlays();
					for (Overlay overlay : overlays) {
						if(overlay instanceof StGraphOverlay){
							StGraphOverlay stgO = (StGraphOverlay) overlay;
							stgO.setLegendVisibility(false);
						}
					}

					//Overlay type

					OverlayEnum USER_CHOICE = varPlotting.getValue();

					switch (USER_CHOICE){
					case TEST:
						break;
					case ELLIPSE_FIT:
						sequence.addOverlay(
								new EllipseFitterOverlay(wing_disc_movie,sequence));
						break;
					case DIVISION_ORIENTATION:
						sequence.addOverlay(
								new DivisionOrientationOverlay(wing_disc_movie,sequence,
										varDoDetectionDistance.getValue(),
										varDoDetectionLength.getValue()));
						break;
					case CELL_SEGMENTATION_BORDER: 
						sequence.addOverlay(
								new BorderOverlay(wing_disc_movie));
						break;

					case CELL_OUTLINE: 
						cellMode(wing_disc_movie);
						break;

					case CELL_POLYGON_CLASS: 
						boolean draw_polygonal_numbers = varBooleanColorClass.getValue();
						int highlight_polygonal_class = varHighlightClass.getValue();

						PolygonClassOverlay pc_painter =  new PolygonClassOverlay(
								wing_disc_movie,
								draw_polygonal_numbers,
								highlight_polygonal_class);

						sequence.addOverlay(
								pc_painter);
						break;

					case DIVISIONS_AND_ELIMINATIONS: 
						divisionMode(wing_disc_movie);
						break;

					case CELL_VORONOI_DIAGRAM: 
						voronoiMode(wing_disc_movie);
						break;

					case CELL_AREA: 
						sequence.addOverlay(
								new AreaGradientOverlay(
										wing_disc_movie, 
										varAreaThreshold));
						break;

					case TRACKING_REVIEW:

						trackingMode(
								wing_disc_movie,
								varBooleanCellIDs.getValue(),
								varBooleanHighlightMistakesBoolean.getValue(),
								false);
						break;
					case TRACKING_DISPLACEMENT:
						trackingMode(
								wing_disc_movie,
								varBooleanCellIDs.getValue(),
								varBooleanHighlightMistakesBoolean.getValue(),
								true);
						break;
					case EDGE_INTENSITY:

						sequence.addOverlay(
								new EdgeIntensityOverlay(
										wing_disc_movie, sequence, this.getUI(),
										varIntensitySlider,
										varFillingCheckbox,
										varEnvelopeBuffer2,
										varBooleanNormalize.getValue(),
										varIntegerChannel.getValue()));

						break;

					case CELL_GRAPH_VIEW:	
						sequence.addOverlay(
								new GraphOverlay(
										wing_disc_movie));
						break;

					case CELL_COLOR_TAG:
						if(sequence.hasOverlay(CellColorTagOverlay.class) || sequence.hasOverlay(EdgeMarkerOverlay.class)){
							new AnnounceFrame("Only one marker overlay is allowed per sequence");
							return;
						}
						sequence.addOverlay(
								new CellColorTagOverlay(wing_disc_movie,varCellColor,varDrawColorTag,sequence));
						break;
						
					case EDGE_COLOR_TAG:
						if(sequence.hasOverlay(CellColorTagOverlay.class) || sequence.hasOverlay(EdgeMarkerOverlay.class)){
							new AnnounceFrame("Only one marker overlay is allowed per sequence");
							return;
						}
						sequence.addOverlay(
								new EdgeMarkerOverlay(wing_disc_movie,varEdgeColor,varEnvelopeBuffer,sequence));
						break;

					case TRACKING_CORRECTION_HINTS:
						sequence.addOverlay(new CorrectionOverlay(wing_disc_movie));
						break;

					case EDGE_T1_TRANSITIONS:
						sequence.addOverlay(new TransitionOverlay(wing_disc_movie, this));
						break;

					case EDGE_STABILITY:
						sequence.addOverlay(new EdgeStabilityOverlay(wing_disc_movie));
						break;

					case ELLIPSE_FIT_WRT_POINT_ROI:
						sequence.addOverlay(
								new EllipseFitColorOverlay(wing_disc_movie,sequence));
						break;
					case ELLIPSE_ELONGATION_RATIO:
						sequence.addOverlay(
								new ElongationRatioOverlay(wing_disc_movie,sequence));
						break;
					case TRACKING_STABLE_ONLY:
						sequence.addOverlay(
								new AlwaysTrackedCellsOverlay(wing_disc_movie));
						break;
					default:
						break;

					}

				}
			}
		else
			new AnnounceFrame("No spatio temporal graph found in ICYsp, please run CellGraph plugin first!");
		
	}

	/**
	 * Repaint the tracking
	 * 
	 * @param wing_disc_movie
	 * @param paint_cellID
	 * @param paint_mistakes
	 */
	private void trackingMode(SpatioTemporalGraph wing_disc_movie,
			boolean paint_cellID, boolean paint_mistakes,boolean paint_displacement) {
		
		if(!wing_disc_movie.hasTracking()){
			new AnnounceFrame("Loaded Graph has not been tracked, cannot paint tracking!");
			return;
		}
		
		if(paint_cellID){
			Overlay trackID = new TrackIdOverlay(wing_disc_movie);
			sequence.addOverlay(trackID);
		}
		
		if(paint_displacement){
			Overlay displacementSegments = new DisplacementOverlay(wing_disc_movie, varDisplacementThreshold.getValue());
			sequence.addOverlay(displacementSegments);
		}else{
			TrackingOverlay correspondence = new TrackingOverlay(wing_disc_movie,varBooleanHighlightMistakesBoolean.getValue());
			sequence.addOverlay(correspondence);
		}
		
	}


	private void voronoiMode(SpatioTemporalGraph wing_disc_movie){
		VoronoiGenerator voronoiDiagram = new VoronoiGenerator(wing_disc_movie,sequence);
	
		if(varBooleanVoronoiDiagram.getValue()){
			Overlay voronoiCells = new VoronoiOverlay(
					wing_disc_movie, 
					voronoiDiagram.getNodeVoroniMapping());
			sequence.addOverlay(voronoiCells);
		}
	
		if(varBooleanAreaDifference.getValue()){
			Overlay voronoiDifference = new VoronoiAreaDifferenceOverlay(
					wing_disc_movie, 
					voronoiDiagram.getAreaDifference());
			sequence.addOverlay(voronoiDifference);	
		}
	}

	private void cellMode(SpatioTemporalGraph wing_disc_movie){
		
		if(varBooleanCCenter.getValue()){
			Overlay centroids = new CentroidOverlay(wing_disc_movie);
			sequence.addOverlay(centroids);
		}
		
		if(varBooleanPolygon.getValue()){
			Overlay polygons = new PolygonOverlay(wing_disc_movie,varPolygonColor.getValue().getColor());
			sequence.addOverlay(polygons);
		}
		
		if(varBooleanDerivedPolygons.getValue()){
			Overlay derived_polygons = new PolygonConverterPainter(wing_disc_movie);
			sequence.addOverlay(derived_polygons);
		}
		
		
	}

	private void divisionMode(SpatioTemporalGraph wing_disc_movie){
		
		//TODO review division readin
//		if(read_divisions){
//			try{
//				DivisionReader division_reader = new DivisionReader(wing_disc_movie);
//				division_reader.backtrackDivisions();
//				sequence.addOverlay(division_reader);
//			}
//			catch(IOException e){
//				System.out.println("Something went wrong in division reading");
//			}
//		}
		
		DivisionOverlay dividing_cells = new DivisionOverlay(
				wing_disc_movie,
				varBooleanPlotDivisions.getValue(),
				varBooleanPlotEliminations.getValue(),
				varBooleanFillCells.getValue(),
				varBooleanConnectDaughterCells.getValue());
		sequence.addOverlay(dividing_cells);
		
	}

	@Override
	public void variableChanged(EzVar<OverlayEnum> source, OverlayEnum newValue) {
		varDescriptionLabel.setText(newValue.getDescription());		
	}
	
	@Override
	public void clean() {
		// TODO Auto-generated by Icy4Eclipse
	}
}
