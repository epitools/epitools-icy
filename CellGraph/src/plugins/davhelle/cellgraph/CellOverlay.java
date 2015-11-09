package plugins.davhelle.cellgraph;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.swimmingPool.SwimmingPool;

import java.util.List;

import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.export.ExportFieldType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.IntensitySummaryType;
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
import plugins.davhelle.cellgraph.overlays.EdgeColorTagOverlay;
import plugins.davhelle.cellgraph.overlays.EdgeIntensityOverlay;
import plugins.davhelle.cellgraph.overlays.EdgeOrientationOverlay;
import plugins.davhelle.cellgraph.overlays.EdgeStabilityOverlay;
import plugins.davhelle.cellgraph.overlays.EllipseFitColorOverlay;
import plugins.davhelle.cellgraph.overlays.EllipseFitterOverlay;
import plugins.davhelle.cellgraph.overlays.ElongationRatioOverlay;
import plugins.davhelle.cellgraph.overlays.GraphOverlay;
import plugins.davhelle.cellgraph.overlays.OverlayEnum;
import plugins.davhelle.cellgraph.overlays.PolygonClassOverlay;
import plugins.davhelle.cellgraph.overlays.PolygonOverlay;
import plugins.davhelle.cellgraph.overlays.ProjectionOverlay;
import plugins.davhelle.cellgraph.overlays.StGraphOverlay;
import plugins.davhelle.cellgraph.overlays.TrackIdOverlay;
import plugins.davhelle.cellgraph.overlays.TrackingOverlay;
import plugins.davhelle.cellgraph.overlays.TransitionOverlay;
import plugins.davhelle.cellgraph.overlays.VoronoiAreaDifferenceOverlay;
import plugins.davhelle.cellgraph.overlays.VoronoiOverlay;

/**
 * CellPainter is a plugin do generate overlays by using the
 * information contained in {@link SpatioTemporalGraph} objects 
 * generated by the {@link CellGraph} plugin or other suitable
 * methods.<br><br>
 * 
 * The objects are retrieved using the shared memory structure from 
 * icy, called swimming pool. The plugin assumes that an object has
 * already been loaded in the swimming pool.<br><br>
 * 
 * To see which overlays are available consult the {@link OverlayEnum}
 * or visit the <a href="http://tiny.uzh.ch/dm">project homepage</a>.
 * 
 * @author Davide Heller
 *
 */
public class CellOverlay extends EzPlug implements EzVarListener<OverlayEnum>{
	

	/**
	 * Input sequence on which to add the overlay
	 */
	EzVarSequence				varSequence;
	
	/**
	 * Flag to remove all overlays from the input sequence
	 */
	EzVarBoolean				varRemovePainterFromSequence;
	
	/**
	 * Main CellOverlay parameter to chose which Overlay do add 
	 */
	EzVarEnum<OverlayEnum> 		varPlotting;

	/**
	 * Description Label containing the information for the chosen overlay.
	 * updated by EzVarListern on varPlotting.
	 */
	EzLabel varDescriptionLabel;
	
	//EzParameters for each overlay type:
	
	//Cell
	EzVarBoolean				varBooleanPolygon;
	EzVarBoolean				varBooleanCCenter;
	EzVarEnum<CellColor> 		varPolygonColor;

	//Voronoi
	EzVarBoolean				varBooleanAreaDifference;
	EzVarBoolean				varBooleanVoronoiDiagram;

	//PolygonNumber
	EzVarInteger 				varHighlightClass;
	EzVarBoolean 				varBooleanColorClass;
	
	//Division
	EzVarBoolean 				varBooleanPlotDivisions;
	EzVarBoolean 				varBooleanPlotEliminations;
	EzVarBoolean 				varBooleanFillCells;
	EzVarBoolean 				varBooleanConnectDaughterCells;
	
	//Tracking 
	EzVarBoolean				varBooleanCellIDs;
	EzVarBoolean				varBooleanHighlightMistakesBoolean;
	
	//Displacement 
	EzVarInteger 				varDisplacementThreshold;
	
	//CellMarker
	EzVarEnum<CellColor>		varCellColor;
	EzVarBoolean 				varDrawColorTag;

	//EdgeMarker
	EzVarEnum<CellColor> 		varEdgeColor;
	EzVarInteger 				varEnvelopeBuffer;
	EzVarInteger					varEnvelopeVertex;
	EzVarInteger					varVertexMode;
	EzVarInteger 				varEdgeChannel;
	EzVarEnum<IntensitySummaryType> varIntensityMeasure_ECT;

	//T1 Transition
	public EzVarInteger 		varMinimalTransitionLength;
	public EzVarInteger 		varMinimalOldSurvival;

	//Edge intensity
	EzVarInteger 				varEnvelopeBuffer2;
	EzVarBoolean 				varBooleanNormalize;
	EzVarInteger 				varIntegerChannel;
	EzVarEnum<IntensitySummaryType>	varIntensityMeasure_EI;
	
	//Graph Export
	EzVarEnum<ExportFieldType>  varExportType;
	EzVarBoolean 				varFillingCheckbox;
	
	//Division orientation
	EzVarInteger 				varDoDetectionDistance;
	EzVarInteger 				varDoDetectionLength;
	
	//Edge Orientation
	EzVarDouble					varEdgeOrientationBuffer;
	EzVarEnum<IntensitySummaryType> varIntensityMeasure_EO;

	private EzVarBoolean varBooleanMeasureAll;

	
	@Override
	protected void initialize() {
		
		//Customize ezGUI for CellOverlay
		this.getUI().setRunButtonText("Add Overlay");
		this.getUI().setParametersIOVisible(false);
		
		//Initialize overlay Parameters
		EzGroup groupOverlays = initializeOverlayParameters();
		super.addEzComponent(groupOverlays);
		
		//Initialize visualization Parameters
		varSequence = new EzVarSequence("Image to add overlay to");
		varSequence.setToolTipText("Any image with same dimensions on which to add the overlay");
		varRemovePainterFromSequence = new EzVarBoolean("Remove previous overlays", false);
		varRemovePainterFromSequence.setToolTipText("Removes the old overlays before adding the new one");
		
		EzGroup groupVisualization = new EzGroup("2. SELECT DESTINATION",
				varSequence,
				varRemovePainterFromSequence);
		
		super.addEzComponent(groupVisualization);
	}
	
	/**
	 * Initializes all the ezGUI parameter handles for the
	 * relative Overlays and returns the group formed by all of them.
	 * 
	 * @return ezGUI group handle with all initialized overlay parameters
	 */
	private EzGroup initializeOverlayParameters(){

		//Which painter should be shown by default
		varPlotting = new EzVarEnum<OverlayEnum>("Overlay",
				OverlayEnum.values(),OverlayEnum.CELL_OUTLINE);
		varPlotting.setToolTipText("Choose the overlay to add from the dropdown list");
		
		//Cells Overlay
		varBooleanPolygon = new EzVarBoolean("Polygons", true);
		varBooleanCCenter = new EzVarBoolean("Centers", true);
		varPolygonColor = new EzVarEnum<CellColor>("Polygon color", CellColor.values(),CellColor.RED);
		EzGroup groupCellMap = new EzGroup("Overlay elements",
				varBooleanPolygon,
				varPolygonColor,
				varBooleanCCenter);
		
		//Voronoi Overlay
		varBooleanVoronoiDiagram = new EzVarBoolean("Voronoi Diagram", true);
		varBooleanAreaDifference = new EzVarBoolean("Area difference", false);
		EzGroup groupVoronoiMap = new EzGroup("Overlay elements",
				varBooleanAreaDifference,
				varBooleanVoronoiDiagram);	
		
		//Polygon Number Overlay
		varBooleanColorClass = new EzVarBoolean("Draw Numbers", false);
		varHighlightClass = new EzVarInteger("Highlight class (0=none)",0,0,10,1);
		EzGroup groupPolygonClass = new EzGroup("Overlay elements",
				varBooleanColorClass,
				varHighlightClass);
		
		//Division mode
		varBooleanPlotDivisions = new EzVarBoolean("Highlight divisions",true);
		varBooleanPlotEliminations = new EzVarBoolean("Highlight eliminations",false);
		varBooleanFillCells = new EzVarBoolean("Fill cells with color",true);
		varBooleanConnectDaughterCells = new EzVarBoolean("Connect daughter cells with line",false);
		EzGroup groupDivisions = new EzGroup("Overlay elements", 
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
		
		//Displacement mode
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
		varEnvelopeBuffer = new EzVarInteger("Edge Intensity Buffer [px]", 1, 10, 1);
		varEnvelopeVertex = new EzVarInteger("Vertex Intensity Buffer [px]", 1, 10, 1);
		varVertexMode = new EzVarInteger("Selection mode", 0, 0, 3, 1);
		varVertexMode.setToolTipText("junction vertex: 0=[include],1=[exclude],2=[only]");
		varEdgeChannel = new EzVarInteger("Color Channel",0,0,10,1);
		varIntensityMeasure_ECT = new EzVarEnum<IntensitySummaryType>(
				"Intensity Measurement", IntensitySummaryType.values(), IntensitySummaryType.Mean);
		EzGroup groupEdgeMarker = new EzGroup("Overlay elements",
				varEdgeColor,
				varEnvelopeBuffer,
				varEnvelopeVertex,
				varVertexMode,
				varEdgeChannel,
				varIntensityMeasure_ECT);

		//Save transitions
		varMinimalTransitionLength = new EzVarInteger("Minimal transition length [frames]",5,1,100,1);
		varMinimalOldSurvival = new EzVarInteger("Minimal old edge persistence [frames]",5,1,100,1);
		EzGroup groupTransitions = new EzGroup("Overlay elements",
				varMinimalTransitionLength,
				varMinimalOldSurvival);
		
		//Edge Intensity
		varEnvelopeBuffer2 = new EzVarInteger("Intensity Buffer [px]", 3, 1, 10, 1);
		varIntegerChannel = new EzVarInteger("Measure Channel",0,0,3,1);
		varIntensityMeasure_EI = new EzVarEnum<IntensitySummaryType>(
				"Measure summary", IntensitySummaryType.values(), IntensitySummaryType.Mean);
		varBooleanMeasureAll = new EzVarBoolean("Measure all frames",false);
		varBooleanNormalize = new EzVarBoolean("Measure relative intensity",false);
		varFillingCheckbox = new EzVarBoolean("Fill edge masks", true);
		EzGroup groupEdgeIntensity = new EzGroup("Edge Intensity elements",
				varEnvelopeBuffer2,
				varIntegerChannel,
				varIntensityMeasure_EI,
				varBooleanMeasureAll,
				varBooleanNormalize,
				varFillingCheckbox
				);
		
		//Edge Orientation
		varEdgeOrientationBuffer = new EzVarDouble("Edge buffer for intensity",3.0,1.0,10.0,1.0);
		varIntensityMeasure_EO = new EzVarEnum<IntensitySummaryType>(
				"Intensity Measure", IntensitySummaryType.values(), IntensitySummaryType.Mean);
		EzGroup groupEdgeOrientation = new EzGroup("Overlay elements",
				varEdgeOrientationBuffer,
				varIntensityMeasure_EO);
		
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
		
		//Describe the visibility of each overlay parameters
		varPlotting.addVisibilityTriggerTo(groupCellMap, OverlayEnum.CELL_OUTLINE);
		varPlotting.addVisibilityTriggerTo(groupPolygonClass, OverlayEnum.CELL_POLYGON_CLASS);
		varPlotting.addVisibilityTriggerTo(groupVoronoiMap, OverlayEnum.CELL_VORONOI_DIAGRAM);
		varPlotting.addVisibilityTriggerTo(groupTracking, OverlayEnum.TRACKING_REVIEW);
		varPlotting.addVisibilityTriggerTo(groupDisplacement, OverlayEnum.TRACKING_DISPLACEMENT);
		varPlotting.addVisibilityTriggerTo(groupDivisions, OverlayEnum.DIVISIONS_AND_ELIMINATIONS);
		varPlotting.addVisibilityTriggerTo(groupMarker, OverlayEnum.CELL_COLOR_TAG);
		varPlotting.addVisibilityTriggerTo(groupEdgeMarker, OverlayEnum.EDGE_COLOR_TAG);
		varPlotting.addVisibilityTriggerTo(groupTransitions, OverlayEnum.EDGE_T1_TRANSITIONS);
		varPlotting.addVisibilityTriggerTo(groupEdgeIntensity, OverlayEnum.EDGE_INTENSITY);
		varPlotting.addVisibilityTriggerTo(groupDivisionOrientation, OverlayEnum.DIVISION_ORIENTATION);
		varPlotting.addVisibilityTriggerTo(groupEdgeOrientation, OverlayEnum.EDGE_ORIENTATION);
		
		return new EzGroup("1. SELECT OVERLAY TO ADD",
				varPlotting,
				groupDescription,
				groupCellMap,
				groupPolygonClass,
				groupVoronoiMap,
				groupDivisions,
				groupTracking,
				groupDisplacement,
				groupMarker,
				groupEdgeMarker,
				groupTransitions,
				groupEdgeIntensity,
				groupDivisionOrientation,
				groupEdgeOrientation);
	}

	@Override
	protected void execute() {
		
		//Retrieve sequence on which to generate the overlay
		Sequence sequence = varSequence.getValue();
		if(sequence == null){
			new AnnounceFrame("Plugin requires active sequence! Please open an image on which to display results");
			return;
		}
		
		// Retrieve spatio-temporal graph object in Icy swimming pool
		SwimmingPool icySP = Icy.getMainInterface().getSwimmingPool();
		if(icySP.hasObjects("stGraph", true)){

			for (SwimmingObject swimmingObject: icySP.getObjects("stGraph", true)){

				if ( swimmingObject.getObject() instanceof SpatioTemporalGraph ){

					SpatioTemporalGraph stGraph = (SpatioTemporalGraph) swimmingObject.getObject();	

					//Optional output data statistics
					//System.out.println("CellVisualizer: loaded stGraph with "+stGraph.size()+" frames");
					//System.out.println("CellVisualizer:	"+stGraph.getFrame(0).size()+" cells in first frame");

					//Optional: Eliminates the previous overlays on output sequence
					if(varRemovePainterFromSequence.getValue()){
						List<Overlay> overlays = sequence.getOverlays();
						for (Overlay overlay : overlays) {
							sequence.removeOverlay(overlay);
							sequence.overlayChanged(overlay);    				
						}
					}
					
					//Disable previous legends in order for the new legend to be displayed properly
					List<Overlay> overlays = sequence.getOverlays();
					for (Overlay overlay : overlays) {
						if(overlay instanceof StGraphOverlay){
							StGraphOverlay stgO = (StGraphOverlay) overlay;
							stgO.setLegendVisibility(false);
						}
					}

					//Add overlay chosen by user
					OverlayEnum USER_CHOICE = varPlotting.getValue();
					addStGraphOverlay(stGraph, USER_CHOICE, sequence);

				}
			}
		}
		else
			new AnnounceFrame("No spatio temporal graph found in ICYsp, please run CellGraph plugin first!");
		
	}

	/**
	 * Adds the overlay selected by the user on to the sequence
	 * by evaluating the stGraph in input.
	 * 
	 * @param stGraph spatio-temporal graph containing the data 
	 * @param USER_CHOICE Overlay chosen by the user
	 * @param sequence to add the overlay on
	 */
	private void addStGraphOverlay(SpatioTemporalGraph stGraph,
			OverlayEnum USER_CHOICE, Sequence sequence) {
		switch (USER_CHOICE){
		case TEST:
			//new TestOverlay(stGraph,sequence)
			break;
		case CELL_PROJECTION:
			sequence.addOverlay(new ProjectionOverlay(stGraph,sequence));
			break;
//		case TRACKING_FLOW:
//			sequence.addOverlay(new FlowOverlay(stGraph));
//			break;
		case ELLIPSE_FIT:
			sequence.addOverlay(
					new EllipseFitterOverlay(stGraph,sequence));
			break;
		case DIVISION_ORIENTATION:
			sequence.addOverlay(
					new DivisionOrientationOverlay(stGraph,sequence,
							varDoDetectionDistance.getValue(),
							varDoDetectionLength.getValue()));
			break;
		case CELL_SEGMENTATION_BORDER: 
			sequence.addOverlay(
					new BorderOverlay(stGraph));
			break;

		case CELL_OUTLINE: 
			cellMode(stGraph,sequence);
			break;

		case CELL_POLYGON_CLASS: 
			boolean draw_polygonal_numbers = varBooleanColorClass.getValue();
			int highlight_polygonal_class = varHighlightClass.getValue();

			PolygonClassOverlay pc_painter =  new PolygonClassOverlay(
					stGraph,
					draw_polygonal_numbers,
					highlight_polygonal_class);

			sequence.addOverlay(
					pc_painter);
			break;

		case DIVISIONS_AND_ELIMINATIONS: 
			divisionMode(stGraph,sequence);
			break;

		case CELL_VORONOI_DIAGRAM: 
			voronoiMode(stGraph,sequence);
			break;

		case CELL_AREA: 
			sequence.addOverlay(
					new AreaGradientOverlay(
							stGraph));
			break;

		case TRACKING_REVIEW:

			trackingMode(
					stGraph,sequence,
					varBooleanCellIDs.getValue(),
					varBooleanHighlightMistakesBoolean.getValue(),
					false);
			break;
		case TRACKING_DISPLACEMENT:
			trackingMode(
					stGraph,sequence,
					varBooleanCellIDs.getValue(),
					varBooleanHighlightMistakesBoolean.getValue(),
					true);
			break;
		case EDGE_INTENSITY:
			sequence.addOverlay(
					new EdgeIntensityOverlay(
							stGraph, sequence, this.getUI(),
							varFillingCheckbox,
							varEnvelopeBuffer2,
							varIntensityMeasure_EI,
							varBooleanMeasureAll.getValue(),
							varBooleanNormalize.getValue(),
							varIntegerChannel.getValue()));
			break;

		case CELL_GRAPH_VIEW:	
			sequence.addOverlay(
					new GraphOverlay(
							stGraph));
			break;

		case CELL_COLOR_TAG:
			if(sequence.hasOverlay(CellColorTagOverlay.class) || sequence.hasOverlay(EdgeColorTagOverlay.class)){
				new AnnounceFrame("Only one marker overlay is allowed per sequence");
				break;
			}
			sequence.addOverlay(
					new CellColorTagOverlay(stGraph,varCellColor,varDrawColorTag,sequence));
			break;
			
		case EDGE_COLOR_TAG:
			if(sequence.hasOverlay(CellColorTagOverlay.class) || sequence.hasOverlay(EdgeColorTagOverlay.class)){
				new AnnounceFrame("Only one marker overlay is allowed per sequence");
				return;
			}
			sequence.addOverlay(
					new EdgeColorTagOverlay(
							stGraph,varEdgeColor,
							varEnvelopeBuffer,varEnvelopeVertex,varVertexMode,
							sequence,varIntensityMeasure_ECT,
							varEdgeChannel));
			break;

		case TRACKING_CORRECTION_HINTS:
			sequence.addOverlay(new CorrectionOverlay(stGraph));
			break;

		case EDGE_T1_TRANSITIONS:
			sequence.addOverlay(new TransitionOverlay(stGraph, this));
			break;

		case EDGE_STABILITY:
			sequence.addOverlay(new EdgeStabilityOverlay(stGraph));
			break;
			
		case EDGE_ORIENTATION:
			sequence.addOverlay(new EdgeOrientationOverlay(
					stGraph, sequence, this, 
					varEdgeOrientationBuffer,
					varIntensityMeasure_EO));
			break;
			
		case ELLIPSE_FIT_WRT_POINT_ROI:
			sequence.addOverlay(
					new EllipseFitColorOverlay(stGraph,sequence));
			break;
		case ELLIPSE_ELONGATION_RATIO:
			sequence.addOverlay(
					new ElongationRatioOverlay(stGraph,sequence));
			break;
		case TRACKING_STABLE_ONLY:
			sequence.addOverlay(
					new AlwaysTrackedCellsOverlay(stGraph));
			break;
		default:
			break;

		}
	}

	/**
	 * Helper method to construct the tracking related overlays
	 * 
	 * @param stGraph the graph from which to extract the information
	 * @param paint_cellID flag to draw the cell ids
	 * @param paint_mistakes flag to draw the detected events
	 * @param paint_displacement flag to draw the displacement of the cells
	 * @param sequence to add the overlay on
	 */
	private void trackingMode(SpatioTemporalGraph stGraph,Sequence sequence,
			boolean paint_cellID, boolean paint_mistakes,boolean paint_displacement) {
		
		if(!stGraph.hasTracking()){
			new AnnounceFrame("Loaded Graph has not been tracked, cannot paint tracking!");
			return;
		}
		
		if(paint_cellID){
			Overlay trackID = new TrackIdOverlay(stGraph);
			sequence.addOverlay(trackID);
		}
		
		if(paint_displacement){
			Overlay displacementSegments = new DisplacementOverlay(stGraph, varDisplacementThreshold.getValue());
			sequence.addOverlay(displacementSegments);
		}else{
			TrackingOverlay correspondence = new TrackingOverlay(stGraph,varBooleanHighlightMistakesBoolean.getValue());
			sequence.addOverlay(correspondence);
		}
		
	}


	/**
	 * Helper method to construct the voronoi diagram overlays
	 * 
	 * @param stGraph the graph from which to generate the overlay
	 * @param sequence to add the overlay on
	 */
	private void voronoiMode(SpatioTemporalGraph stGraph, Sequence sequence){
		VoronoiGenerator voronoiDiagram = new VoronoiGenerator(stGraph,sequence);
	
		if(varBooleanVoronoiDiagram.getValue()){
			Overlay voronoiCells = new VoronoiOverlay(
					stGraph, 
					voronoiDiagram.getNodeVoroniMapping());
			sequence.addOverlay(voronoiCells);
		}
	
		if(varBooleanAreaDifference.getValue()){
			Overlay voronoiDifference = new VoronoiAreaDifferenceOverlay(
					stGraph, 
					voronoiDiagram.getAreaDifference());
			sequence.addOverlay(voronoiDifference);	
		}
	}

	/**
	 * Helper method to construct the overlays for the geometry of each cell in the stGraph
	 * 
	 * @param stGraph the graph from which to extract the information
	 * @param sequence to add the overlay on
	 */
	private void cellMode(SpatioTemporalGraph stGraph,Sequence sequence){
		
		if(varBooleanCCenter.getValue()){
			Overlay centroids = new CentroidOverlay(stGraph);
			sequence.addOverlay(centroids);
		}
		
		if(varBooleanPolygon.getValue()){
			Overlay polygons = new PolygonOverlay(stGraph,varPolygonColor.getValue().getColor());
			sequence.addOverlay(polygons);
		}
		
	}

	/**
	 * Helper method to construct the division related overlays
	 * 
	 * @param stGraph the graph from which to extract the information
	 * @param sequence to add the overlay on
	 */
	private void divisionMode(SpatioTemporalGraph stGraph,Sequence sequence){
		
		DivisionOverlay dividing_cells = new DivisionOverlay(
				stGraph,
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
	}
}
