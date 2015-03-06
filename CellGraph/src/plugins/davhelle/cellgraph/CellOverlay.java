/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.painter.Painter;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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
import plugins.davhelle.cellgraph.graphexport.ExportFieldType;
import plugins.davhelle.cellgraph.graphexport.GraphExporter;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.CellWorkbook;
import plugins.davhelle.cellgraph.io.DivisionReader;
import plugins.davhelle.cellgraph.io.PdfPrinter;
import plugins.davhelle.cellgraph.io.SkeletonWriter;
import plugins.davhelle.cellgraph.io.TagSaver;
import plugins.davhelle.cellgraph.misc.CellColor;
import plugins.davhelle.cellgraph.misc.VoronoiGenerator;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.painters.AlwaysTrackedCellsOverlay;
import plugins.davhelle.cellgraph.painters.AreaGradientOverlay;
import plugins.davhelle.cellgraph.painters.DisplacementOverlay;
import plugins.davhelle.cellgraph.painters.BorderPainter;
import plugins.davhelle.cellgraph.painters.CellMarker;
import plugins.davhelle.cellgraph.painters.CentroidPainter;
import plugins.davhelle.cellgraph.painters.ColorTagPainter;
import plugins.davhelle.cellgraph.painters.CorrectionOverlay;
import plugins.davhelle.cellgraph.painters.DivisionOrientationOverlay;
import plugins.davhelle.cellgraph.painters.DivisionPainter;
import plugins.davhelle.cellgraph.painters.EdgeStabilityOverlay;
import plugins.davhelle.cellgraph.painters.EllipseFitColorOverlay;
import plugins.davhelle.cellgraph.painters.EllipseFitterOverlay;
import plugins.davhelle.cellgraph.painters.ElongationRatioOverlay;
import plugins.davhelle.cellgraph.painters.GraphPainter;
import plugins.davhelle.cellgraph.painters.IntesityGraphOverlay;
import plugins.davhelle.cellgraph.painters.NeighborChangeFrequencyOverlay;
import plugins.davhelle.cellgraph.painters.OverlayEnum;
import plugins.davhelle.cellgraph.painters.PolygonClassPainter;
import plugins.davhelle.cellgraph.painters.PolygonConverterPainter;
import plugins.davhelle.cellgraph.painters.PolygonPainter;
import plugins.davhelle.cellgraph.painters.TrackIdPainter;
import plugins.davhelle.cellgraph.painters.TrackingOverlay;
import plugins.davhelle.cellgraph.painters.TransitionOverlay;
import plugins.davhelle.cellgraph.painters.VoronoiAreaDifferencePainter;
import plugins.davhelle.cellgraph.painters.VoronoiPainter;

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
	
	EzVarBoolean				varBooleanReadDivisions;
	
	//Tracking Mode
	EzVarBoolean				varBooleanCellIDs;
	EzVarBoolean				varBooleanHighlightMistakesBoolean;
	EzVarBoolean 				varBooleanDrawDisplacement;
	
	EzVarDouble					varAreaThreshold;
	
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
	private EzVarFile varSaveSkeleton;
	private EzVarBoolean varBooleanPlotDivisions;
	private EzVarBoolean varBooleanPlotEliminations;
	private EzVarBoolean varBooleanFillCells;
	private EzVarInteger varHighlightClass;
	private EzVarBoolean varBooleanColorClass;
	private EzVarEnum<CellColor> varPolygonColor;
	private EzVarBoolean varSaveTransitions;
	public EzVarInteger varMinimalTransitionLength;
	public EzVarInteger varMinimalOldSurvival;
	private EzVarBoolean varSavePolyClass;
	private EzVarBoolean varSaveToPdf;

	@Override
	protected void initialize() {
		
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
		varSavePolyClass = new EzVarBoolean("Save polygon class statistics", false);
		EzGroup groupPolygonClass = new EzGroup("Overlay elements",
				varBooleanColorClass,
				varHighlightClass,
				varSavePolyClass);
		
		//Area Threshold View
		varAreaThreshold = new EzVarDouble("Area threshold", 0.9, 0, 10, 0.1);
		
		EzGroup groupAreaThreshold = new EzGroup("Overlay elements",
				varAreaThreshold);
		
		//Which painter should be shown by default
		varPlotting = new EzVarEnum<OverlayEnum>("Overlay",
				OverlayEnum.values(),OverlayEnum.CELL_OVERLAY);
		

		//Division mode
		varBooleanReadDivisions = new EzVarBoolean("Read divisions", false);
		varBooleanPlotDivisions = new EzVarBoolean("Highlight divisions (green)",true);
		varBooleanPlotEliminations = new EzVarBoolean("Highlight eliminations (red)",false);
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
				
		
		//Graph Export Mode
		varExportType = new EzVarEnum<ExportFieldType>("Export field", 
				ExportFieldType.values(), ExportFieldType.STANDARD);
		varOutputFile = new EzVarFile("Output File", "/Users/davide/analysis");
		varFrameNo = new EzVarInteger("Frame no:",0,0,100,1);
		
		EzGroup groupExport = new EzGroup("Overlay elements",
				varExportType,
				varOutputFile,
				varFrameNo);
		
		//CellMarker mode
		varCellColor = new EzVarEnum<CellColor>("Cell color", CellColor.values(), CellColor.GREEN);
		EzGroup groupMarker = new EzGroup("Overlay elements",
				varCellColor);
		
		//SAVE_SKELETON mode
		varSaveSkeleton = new EzVarFile("Output File", "");
		EzGroup groupSaveSkeleton = new EzGroup("SAVE_SKELETON elements",varSaveSkeleton);

		//Save transitions
		varMinimalTransitionLength = new EzVarInteger("Minimal transition length [frames]",5,1,100,1);
		varMinimalOldSurvival = new EzVarInteger("Minimal old edge persistence [frames]",5,1,100,1);
		varSaveTransitions = new EzVarBoolean("Save transition statistics to CSV", false);
		varSaveToPdf = new EzVarBoolean("Save transition picture to PDF", false);
		EzGroup groupTransitions = new EzGroup("Overlay elements",
				varMinimalTransitionLength,
				varMinimalOldSurvival,
				varSaveTransitions,
				varSaveToPdf);

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
				groupExport,
				groupMarker,
				//groupSaveSkeleton,
				groupTransitions
				
		);
		
		varPlotting.addVisibilityTriggerTo(groupCellMap, OverlayEnum.CELL_OVERLAY);
		varPlotting.addVisibilityTriggerTo(groupPolygonClass, OverlayEnum.POLYGON_CLASS);
		varPlotting.addVisibilityTriggerTo(groupVoronoiMap, OverlayEnum.VORONOI_DIAGRAM);
		varPlotting.addVisibilityTriggerTo(groupAreaThreshold, OverlayEnum.CELL_AREA);
		//TODO varInput.addVisibilityTriggerTo(varBooleanDerivedPolygons, InputType.SKELETON);
		varPlotting.addVisibilityTriggerTo(groupTracking, OverlayEnum.CELL_TRACKING);
		varPlotting.addVisibilityTriggerTo(groupDivisions, OverlayEnum.DIVISIONS_AND_ELIMINATIONS);
		varPlotting.addVisibilityTriggerTo(groupExport, OverlayEnum.GRAPHML_EXPORT);
		varPlotting.addVisibilityTriggerTo(groupMarker, OverlayEnum.CELL_COLOR_TAG);
		//varPlotting.addVisibilityTriggerTo(groupSaveSkeleton, OverlayEnum.SAVE_SKELETONS);
		varPlotting.addVisibilityTriggerTo(groupTransitions, OverlayEnum.T1_TRANSITIONS);
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
			List<Painter> painters = sequence.getPainters();
			for (Painter painter : painters) {
				sequence.removePainter(painter);
				sequence.painterChanged(painter);    				
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

					if(varUpdatePainterMode.getValue()){
						List<Painter> painters = sequence.getPainters();
						for (Painter painter : painters) {
							sequence.removePainter(painter);
							sequence.painterChanged(painter);    				
						}
					}

					//Overlay type

					OverlayEnum USER_CHOICE = varPlotting.getValue();

					switch (USER_CHOICE){
//					case TEST:
//						break;
					case ELLIPSE_FIT:
						sequence.addOverlay(
								new EllipseFitterOverlay(wing_disc_movie));
						break;
					case DIVSION_ORIENTATION:
						sequence.addOverlay(
								new DivisionOrientationOverlay(wing_disc_movie));
						break;
					case SEGMENTATION_BORDER: 
						sequence.addOverlay(
								new BorderPainter(wing_disc_movie));
						break;

					case CELL_OVERLAY: 
						cellMode(wing_disc_movie);
						break;

					case POLYGON_CLASS: 
						boolean draw_polygonal_numbers = varBooleanColorClass.getValue();
						int highlight_polygonal_class = varHighlightClass.getValue();


						PolygonClassPainter pc_painter =  new PolygonClassPainter(
								wing_disc_movie,
								draw_polygonal_numbers,
								highlight_polygonal_class);

						if(varSavePolyClass.getValue() && varSavePolyClass.isVisible()){
							pc_painter.saveToCsv();
						}

						sequence.addOverlay(
								pc_painter);
						break;

//					case POLYGON_TILE:
//						sequence.addOverlay(
//								new PolygonConverterPainter(wing_disc_movie));
//						break;

					case DIVISIONS_AND_ELIMINATIONS: 
						divisionMode(
								wing_disc_movie, 
								varBooleanReadDivisions.getValue());
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

//					case ALWAYS_TRACKED_CELLS: 
//						sequence.addOverlay(
//								new AlwaysTrackedCellsOverlay(
//										wing_disc_movie));
//						break;

//					case WRITE_OUT_DDN: 
//
//						//CsvWriter.custom_write_out(wing_disc_movie);
//						writeOutMode(wing_disc_movie);
//						break;

					case CELL_TRACKING:

						trackingMode(
								wing_disc_movie,
								varBooleanCellIDs.getValue(),
								varBooleanHighlightMistakesBoolean.getValue(),
								varBooleanDrawDisplacement.getValue());

						break;
					case EDGE_INTENSITY:

						sequence.addOverlay(
								new IntesityGraphOverlay(
										wing_disc_movie, sequence, this.getUI()));

						break;

					case GRAPH_VIEW:	
						sequence.addOverlay(
								new GraphPainter(
										wing_disc_movie));

						break;

						//Tagging

					case CELL_COLOR_TAG:
						sequence.addOverlay(
								new CellMarker(wing_disc_movie,varCellColor));
						sequence.addOverlay(
								new ColorTagPainter(wing_disc_movie));
						break;
//					case SAVE_COLOR_TAG:
//						new TagSaver(wing_disc_movie);
//						break;
					case SAVE_COLOR_TAG_XLS:
						new CellWorkbook(wing_disc_movie);
						break;

						//Export and Corrections

					case GRAPHML_EXPORT:
						graphExportMode(
								wing_disc_movie,
								varExportType.getValue(),
								varOutputFile.getValue(false),
								varFrameNo.getValue());
						break;	
//					case SAVE_SKELETONS:
//						new SkeletonWriter(sequence, wing_disc_movie).write(varSaveSkeleton.getValue(false).getAbsolutePath());
//						break;
					case CORRECTION_HINTS:
						sequence.addOverlay(new CorrectionOverlay(wing_disc_movie));
						break;


						// Edge Dynamics	

					case T1_TRANSITIONS:

						TransitionOverlay t1 = new TransitionOverlay(wing_disc_movie, this);

						if(varSaveTransitions.getValue())
							t1.saveToCsv();

						if(varSaveToPdf.getValue())
							t1.saveToPdf();

						sequence.addOverlay(t1);
						break;

					case EDGE_STABILITY:
						sequence.addOverlay(new EdgeStabilityOverlay(wing_disc_movie));
						break;
//					case NEIGHBOR_STABILITY:
//						sequence.addOverlay(new NeighborChangeFrequencyOverlay(wing_disc_movie));
//						break;
					case PDF_SCREENSHOT:
						new PdfPrinter(wing_disc_movie);
						break;
					case ELLIPSE_FIT_WRT_POINT_ROI:
						sequence.addOverlay(
								new EllipseFitColorOverlay(wing_disc_movie));
						break;
					case ELONGATION_RATIO:
						sequence.addOverlay(
								new ElongationRatioOverlay(wing_disc_movie));
						break;
					default:
						break;

					}
					//future statistical output statistics
					//			CsvWriter.trackedArea(wing_disc_movie);
					//			CsvWriter.frameAndArea(wing_disc_movie);
					//CsvWriter.custom_write_out(wing_disc_movie);

				}
			}
		else
			new AnnounceFrame("No spatio temporal graph found in ICYsp, please run CellGraph plugin first!");
		
	}

	private void graphExportMode(
			SpatioTemporalGraph wing_disc_movie,
			ExportFieldType export_type,
			File output_file,
			Integer frame_no) {
		
		//safety checks
		if(output_file == null)
			new AnnounceFrame("No output file specified! Please select");
		else if(frame_no >= wing_disc_movie.size())
			new AnnounceFrame("Requested frame no is not available! Please check");
		else if(varExportType.getValue() == ExportFieldType.STANDARD){
			
			String base_dir = output_file.getParent();
			
			//first frame TODO can add property to field like .getName() = '%s/frame%d.xml')
			GraphExporter exporter = new GraphExporter(ExportFieldType.COMPLETE_CSV);
			int i = 0;
			exporter.exportFrame(
					wing_disc_movie.getFrame(i), 
					String.format("%s/frame%d.xml",base_dir,i));
		
			//last frame
			exporter = new GraphExporter(ExportFieldType.COMPLETE_CSV);
			i = wing_disc_movie.size() - 1;
			exporter.exportFrame(
					wing_disc_movie.getFrame(i), 
					String.format("%s/frame%d.xml",base_dir,i));
			
			//seq_area
			exporter = new GraphExporter(ExportFieldType.SEQ_AREA);
			i = 0;
			exporter.exportFrame(
					wing_disc_movie.getFrame(i), 
					String.format("%s/seq_area.xml",base_dir,i));
			//seq_x
			exporter = new GraphExporter(ExportFieldType.SEQ_X);
			i = 0;
			exporter.exportFrame(
					wing_disc_movie.getFrame(i), 
					String.format("%s/seq_x.xml",base_dir,i));
			//seq_y
			exporter = new GraphExporter(ExportFieldType.SEQ_Y);
			i = 0;
			exporter.exportFrame(
					wing_disc_movie.getFrame(i), 
					String.format("%s/seq_y.xml",base_dir,i));
			
			
		}
		else
		{
			GraphExporter exporter = new GraphExporter(varExportType.getValue());
			FrameGraph frame_to_export = wing_disc_movie.getFrame(frame_no);
			exporter.exportFrame(frame_to_export, output_file.getAbsolutePath());
		}		
		
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
			Painter trackID = new TrackIdPainter(wing_disc_movie);
			sequence.addPainter(trackID);
		}
		
		if(paint_displacement){
			float maximum_displacement = 2;
			Painter displacementSegments = new DisplacementOverlay(wing_disc_movie, maximum_displacement);
			sequence.addPainter(displacementSegments);
		}
		
		TrackingOverlay correspondence = new TrackingOverlay(wing_disc_movie,varBooleanHighlightMistakesBoolean.getValue());
		sequence.addPainter(correspondence);
		
	}

	/**
	 * Write out data to the disc, user specified location
	 * 
	 * @param wing_disc_movie the stGraph structure to be analyzed
	 */
	private void writeOutMode(SpatioTemporalGraph wing_disc_movie) {		
		//write out through method possibly/problems with dynamic change
		//substitute this method with graph write out.
		//CsvWriter.custom_write_out(wing_disc_movie); 
		
		directDividingNeighborSimulation(wing_disc_movie);
	}

	/**
	 * @param wing_disc_movie
	 */
	private void directDividingNeighborSimulation(
			SpatioTemporalGraph wing_disc_movie) {
		final int sim_no = 10000;
		System.out.println(sim_no + "simulations");
		FrameGraph frame_i = wing_disc_movie.getFrame(0);
		Iterator<Node> cell_it = frame_i.iterator();	
		
		//no of divisions
		int division_no = 0;
		//" with at least one direct dividing neighbor (ddn)
		int division_no_with_ddn= 0;
		
		while(cell_it.hasNext()){
			Node cell = cell_it.next();
			
			//do not count if cell is on boundary
			if(cell.onBoundary())
				continue;
			
			if(cell.hasObservedDivision()){
				division_no++;

				for(Node neighbor: cell.getNeighbors())
					if(neighbor.hasObservedDivision()){
						division_no_with_ddn++;
						break;
					}
			}
		}
		
		//sample value 
		double p_dividing_neighbor = division_no_with_ddn / (double)division_no;
		
		System.out.println(p_dividing_neighbor);
		
		//random sampler (without replacement)
		Node[] cells = frame_i.vertexSet().toArray(new Node[frame_i.size()]);
		Random randomGenerator = new Random(System.currentTimeMillis());
		
		for(int sim_i=0; sim_i<sim_no; sim_i++){
			
			//no of random cells that have at least a dividing neighbor
			int rnd_division_no_W = 0;
			ArrayList<Integer> chosen_cell_ids = new ArrayList<Integer>();
			
			//choose as many random cells as there were dividing cells
			for(int i=0; i<division_no; i++){
				
				int rnd_cell_id = randomGenerator.nextInt(cells.length);
				Node rnd_cell = cells[rnd_cell_id];
				
				//do not choose a border cell or cell that has already been selected
				while(rnd_cell.onBoundary() || chosen_cell_ids.contains(rnd_cell_id)){
					rnd_cell_id = randomGenerator.nextInt(cells.length);
					rnd_cell = cells[rnd_cell_id];
				}
				
				for(Node neighbor: rnd_cell.getNeighbors())
					if(neighbor.hasObservedDivision()){
						rnd_division_no_W++;
						break;
					}			
			}
			
			double rnd_p_dividing_neighbor = rnd_division_no_W / (double)division_no;
			System.out.println(rnd_p_dividing_neighbor);
			
		}
	}

	@Override
	public void clean() {
		// TODO Auto-generated by Icy4Eclipse
	}

	private void voronoiMode(SpatioTemporalGraph wing_disc_movie){
		VoronoiGenerator voronoiDiagram = new VoronoiGenerator(wing_disc_movie);
	
		if(varBooleanVoronoiDiagram.getValue()){
			Painter voronoiCells = new VoronoiPainter(
					wing_disc_movie, 
					voronoiDiagram.getNodeVoroniMapping());
			sequence.addPainter(voronoiCells);
		}
	
		if(varBooleanAreaDifference.getValue()){
			Painter voronoiDifference = new VoronoiAreaDifferencePainter(
					wing_disc_movie, 
					voronoiDiagram.getAreaDifference());
			sequence.addPainter(voronoiDifference);	
		}
	}

	private void cellMode(SpatioTemporalGraph wing_disc_movie){
		
		if(varBooleanCCenter.getValue()){
			Painter centroids = new CentroidPainter(wing_disc_movie);
			sequence.addPainter(centroids);
		}
		
		if(varBooleanPolygon.getValue()){
			Painter polygons = new PolygonPainter(wing_disc_movie,varPolygonColor.getValue().getColor());
			sequence.addPainter(polygons);
		}
		
		if(varBooleanDerivedPolygons.getValue()){
			Painter derived_polygons = new PolygonConverterPainter(wing_disc_movie);
			sequence.addPainter(derived_polygons);
		}
		
		
	}

	private void divisionMode(SpatioTemporalGraph wing_disc_movie, boolean read_divisions){
		
		if(read_divisions){
			try{
				DivisionReader division_reader = new DivisionReader(wing_disc_movie);
				division_reader.backtrackDivisions();
				sequence.addPainter(division_reader);
			}
			catch(IOException e){
				System.out.println("Something went wrong in division reading");
			}
		}
		
		
		DivisionPainter dividing_cells = new DivisionPainter(
				wing_disc_movie,
				varBooleanPlotDivisions.getValue(),
				varBooleanPlotEliminations.getValue(),
				varBooleanFillCells.getValue());
		sequence.addPainter(dividing_cells);
		
	}

	@Override
	public void variableChanged(EzVar<OverlayEnum> source, OverlayEnum newValue) {
		varDescriptionLabel.setText(newValue.getDescription());		
	}
}
