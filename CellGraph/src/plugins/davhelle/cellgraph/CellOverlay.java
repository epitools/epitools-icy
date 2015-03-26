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
import plugins.davhelle.cellgraph.painters.AlwaysTrackedCellsOverlay;
import plugins.davhelle.cellgraph.painters.AreaGradientOverlay;
import plugins.davhelle.cellgraph.painters.BorderOverlay;
import plugins.davhelle.cellgraph.painters.CellMarkerOverlay;
import plugins.davhelle.cellgraph.painters.CentroidOverlay;
import plugins.davhelle.cellgraph.painters.CorrectionOverlay;
import plugins.davhelle.cellgraph.painters.DisplacementOverlay;
import plugins.davhelle.cellgraph.painters.DivisionOrientationOverlay;
import plugins.davhelle.cellgraph.painters.DivisionOverlay;
import plugins.davhelle.cellgraph.painters.EdgeIntensityOverlay;
import plugins.davhelle.cellgraph.painters.EdgeStabilityOverlay;
import plugins.davhelle.cellgraph.painters.EllipseFitColorOverlay;
import plugins.davhelle.cellgraph.painters.EllipseFitterOverlay;
import plugins.davhelle.cellgraph.painters.ElongationRatioOverlay;
import plugins.davhelle.cellgraph.painters.GraphOverlay;
import plugins.davhelle.cellgraph.painters.OverlayEnum;
import plugins.davhelle.cellgraph.painters.PolygonClassOverlay;
import plugins.davhelle.cellgraph.painters.PolygonConverterPainter;
import plugins.davhelle.cellgraph.painters.PolygonOverlay;
import plugins.davhelle.cellgraph.painters.TrackIdOverlay;
import plugins.davhelle.cellgraph.painters.TrackingOverlay;
import plugins.davhelle.cellgraph.painters.TransitionOverlay;
import plugins.davhelle.cellgraph.painters.VoronoiAreaDifferenceOverlay;
import plugins.davhelle.cellgraph.painters.VoronoiOverlay;


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
	EzVarBoolean 				varBooleanDrawDisplacement;
	
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
	private EzVarInteger varHighlightClass;
	private EzVarBoolean varBooleanColorClass;
	private EzVarEnum<CellColor> varPolygonColor;
	public EzVarInteger varMinimalTransitionLength;
	public EzVarInteger varMinimalOldSurvival;
	private EzVarBoolean varFillingCheckbox;

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
				OverlayEnum.values(),OverlayEnum.CELL_OVERLAY);
		

		//Division mode
		varBooleanPlotDivisions = new EzVarBoolean("Highlight divisions",true);
		varBooleanPlotEliminations = new EzVarBoolean("Highlight eliminations",false);
		varBooleanFillCells = new EzVarBoolean("Fill cells with color",true);
		EzGroup groupDivisions = new EzGroup("Overlay elements", 
				//varBooleanReadDivisions, TODO
				varBooleanPlotDivisions,
				varBooleanPlotEliminations,
				varBooleanFillCells
				);
		
		
		
		//TrackingMode
		varBooleanCellIDs = new EzVarBoolean("Write TrackIDs", true);
		varBooleanDrawDisplacement = new EzVarBoolean("Draw displacement", false);
		varBooleanHighlightMistakesBoolean = new EzVarBoolean("Highlight mistakes", true);
		
		EzGroup groupTracking = new EzGroup("Overlay elements",
				varBooleanCellIDs,
				varBooleanDrawDisplacement,
				varBooleanHighlightMistakesBoolean);
				
		//CellMarker mode
		varCellColor = new EzVarEnum<CellColor>("Cell color", CellColor.values(), CellColor.GREEN);
		EzGroup groupMarker = new EzGroup("Overlay elements",
				varCellColor);

		//Save transitions
		varMinimalTransitionLength = new EzVarInteger("Minimal transition length [frames]",5,1,100,1);
		varMinimalOldSurvival = new EzVarInteger("Minimal old edge persistence [frames]",5,1,100,1);
		EzGroup groupTransitions = new EzGroup("Overlay elements",
				varMinimalTransitionLength,
				varMinimalOldSurvival);
		
		//IntensitySlider
		varIntensitySlider = new EzVarDouble("Color Scaling", 
				0.5, 0.1, 1.0, 0.05);//, RangeEditorType.SLIDER,new HashMap<Double,String>());
		varFillingCheckbox = new EzVarBoolean("Fill edge masks", true);
		EzGroup groupEdgeIntensity = new EzGroup("Edge Intensity elements",
				varIntensitySlider,
				varFillingCheckbox);
		
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
				groupMarker,
				groupTransitions,
				groupEdgeIntensity
				
		);
		
		varPlotting.addVisibilityTriggerTo(groupCellMap, OverlayEnum.CELL_OVERLAY);
		varPlotting.addVisibilityTriggerTo(groupPolygonClass, OverlayEnum.POLYGON_CLASS);
		varPlotting.addVisibilityTriggerTo(groupVoronoiMap, OverlayEnum.VORONOI_DIAGRAM);
		varPlotting.addVisibilityTriggerTo(groupAreaThreshold, OverlayEnum.CELL_AREA);
		//TODO varInput.addVisibilityTriggerTo(varBooleanDerivedPolygons, InputType.SKELETON);
		varPlotting.addVisibilityTriggerTo(groupTracking, OverlayEnum.CELL_TRACKING);
		varPlotting.addVisibilityTriggerTo(groupDivisions, OverlayEnum.DIVISIONS_AND_ELIMINATIONS);
		varPlotting.addVisibilityTriggerTo(groupMarker, OverlayEnum.CELL_COLOR_TAG);
		varPlotting.addVisibilityTriggerTo(groupTransitions, OverlayEnum.T1_TRANSITIONS);
		varPlotting.addVisibilityTriggerTo(groupEdgeIntensity, OverlayEnum.EDGE_INTENSITY);
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

					//Overlay type

					OverlayEnum USER_CHOICE = varPlotting.getValue();

					switch (USER_CHOICE){
					case TEST:
						sequence.addOverlay(
								new AlwaysTrackedCellsOverlay(wing_disc_movie));
						break;
					case ELLIPSE_FIT:
						sequence.addOverlay(
								new EllipseFitterOverlay(wing_disc_movie,sequence));
						break;
					case DIVSION_ORIENTATION:
						sequence.addOverlay(
								new DivisionOrientationOverlay(wing_disc_movie,sequence));
						break;
					case SEGMENTATION_BORDER: 
						sequence.addOverlay(
								new BorderOverlay(wing_disc_movie));
						break;

					case CELL_OVERLAY: 
						cellMode(wing_disc_movie);
						break;

					case POLYGON_CLASS: 
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

					case VORONOI_DIAGRAM: 
						voronoiMode(wing_disc_movie);
						break;

					case CELL_AREA: 
						sequence.addOverlay(
								new AreaGradientOverlay(
										wing_disc_movie, 
										varAreaThreshold));
						break;

					case CELL_TRACKING:

						trackingMode(
								wing_disc_movie,
								varBooleanCellIDs.getValue(),
								varBooleanHighlightMistakesBoolean.getValue(),
								varBooleanDrawDisplacement.getValue());

						break;
					case EDGE_INTENSITY:

						sequence.addOverlay(
								new EdgeIntensityOverlay(
										wing_disc_movie, sequence, this.getUI(),
										varIntensitySlider,
										varFillingCheckbox));

						break;

					case GRAPH_VIEW:	
						sequence.addOverlay(
								new GraphOverlay(
										wing_disc_movie));
						break;

					case CELL_COLOR_TAG:
						sequence.addOverlay(
								new CellMarkerOverlay(wing_disc_movie,varCellColor,sequence));
						break;

					case CORRECTION_HINTS:
						sequence.addOverlay(new CorrectionOverlay(wing_disc_movie));
						break;

					case T1_TRANSITIONS:
						sequence.addOverlay(new TransitionOverlay(wing_disc_movie, this));
						break;

					case EDGE_STABILITY:
						sequence.addOverlay(new EdgeStabilityOverlay(wing_disc_movie));
						break;

					case ELLIPSE_FIT_WRT_POINT_ROI:
						sequence.addOverlay(
								new EllipseFitColorOverlay(wing_disc_movie,sequence));
						break;
					case ELONGATION_RATIO:
						sequence.addOverlay(
								new ElongationRatioOverlay(wing_disc_movie,sequence));
						break;
					case ALWAYS_TRACKED_CELLS:
						sequence.addOverlay(
								new AlwaysTrackedCellsOverlay(wing_disc_movie));
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
	 * @param paint_displacement
	 */
	private void trackingMode(SpatioTemporalGraph wing_disc_movie,
			boolean paint_cellID, boolean paint_mistakes, boolean paint_displacement) {
		
		if(!wing_disc_movie.hasTracking()){
			new AnnounceFrame("Loaded Graph has not been tracked, cannot paint tracking!");
			return;
		}
		
		if(paint_cellID){
			Overlay trackID = new TrackIdOverlay(wing_disc_movie);
			sequence.addOverlay(trackID);
		}
		
		if(paint_displacement){
			float maximum_displacement = 2;
			Overlay displacementSegments = new DisplacementOverlay(wing_disc_movie, maximum_displacement);
			sequence.addOverlay(displacementSegments);
		}
		
		TrackingOverlay correspondence = new TrackingOverlay(wing_disc_movie,varBooleanHighlightMistakesBoolean.getValue());
		sequence.addOverlay(correspondence);
		
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
				varBooleanFillCells.getValue());
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
