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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import plugins.adufour.ezplug.EzException;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarFile;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.graphexport.ExportFieldType;
import plugins.davhelle.cellgraph.graphexport.GraphExporter;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.CellWorkbook;
import plugins.davhelle.cellgraph.io.DivisionReader;
import plugins.davhelle.cellgraph.io.SkeletonWriter;
import plugins.davhelle.cellgraph.io.TagSaver;
import plugins.davhelle.cellgraph.misc.CellColor;
import plugins.davhelle.cellgraph.misc.VoronoiGenerator;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.painters.AlwaysTrackedCellsOverlay;
import plugins.davhelle.cellgraph.painters.AreaThresholdPainter;
import plugins.davhelle.cellgraph.painters.ArrowPainter;
import plugins.davhelle.cellgraph.painters.BorderPainter;
import plugins.davhelle.cellgraph.painters.CellMarker;
import plugins.davhelle.cellgraph.painters.CentroidPainter;
import plugins.davhelle.cellgraph.painters.ColorTagPainter;
import plugins.davhelle.cellgraph.painters.CorrectionOverlay;
import plugins.davhelle.cellgraph.painters.DivisionPainter;
import plugins.davhelle.cellgraph.painters.GraphPainter;
import plugins.davhelle.cellgraph.painters.PolygonClassPainter;
import plugins.davhelle.cellgraph.painters.PolygonConverterPainter;
import plugins.davhelle.cellgraph.painters.PolygonPainter;
import plugins.davhelle.cellgraph.painters.TrackIdPainter;
import plugins.davhelle.cellgraph.painters.TrackPainter;
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
public class CellPainter extends EzPlug {
	
	//plotting modes
	private enum PlotEnum{
		CELLS,
		BORDER, 
		VORONOI, 
		POLYGON_CLASS,
		POLYGON_TILE,
		AREA_THRESHOLD,
		TRACKING,
		GRAPH_PAINTER,
		ALWAYS_TRACKED,
		WRITE_OUT,
		DIVISIONS,
		GRAPH_EXPORT,
		COLOR_TAG,
		SAVE_TAG,
		SAVE_TAG_XLS,
		SAVE_SKELETONS,
		CORRECTION_HINTS
	}
	
	EzVarBoolean				varRemovePainterFromSequence;
	EzVarBoolean				varUpdatePainterMode;
	
	EzVarEnum<PlotEnum> 		varPlotting;

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

	@Override
	protected void initialize() {
		
		//Default options
		
		varSequence = new EzVarSequence("Input sequence");
		super.addEzComponent(varSequence);
		
		varRemovePainterFromSequence = new EzVarBoolean("Remove all painters", false);
		super.addEzComponent(varRemovePainterFromSequence);
		
		varUpdatePainterMode = new EzVarBoolean("Update painter", false);

		//Cells view
		varBooleanPolygon = new EzVarBoolean("Polygons", true);
		varBooleanCCenter = new EzVarBoolean("Centers", true);
		varBooleanWriteCenters = new EzVarBoolean("Write cell centers to disk",false);
		varBooleanDerivedPolygons = new EzVarBoolean("Derived Polygons", false);
		varPolygonColor = new EzVarEnum<CellColor>("Polygon color", CellColor.values(),CellColor.RED);
		EzGroup groupCellMap = new EzGroup("CELLS elements",
				varBooleanPolygon,
				varPolygonColor,
				varBooleanDerivedPolygons,
				varBooleanCCenter,
				varBooleanWriteCenters);
		

		//Voronoi view
		varBooleanVoronoiDiagram = new EzVarBoolean("Voronoi Diagram", true);
		varBooleanAreaDifference = new EzVarBoolean("Area difference", false);
		
		EzGroup groupVoronoiMap = new EzGroup("VORONOI elements",
				varBooleanAreaDifference,
				varBooleanVoronoiDiagram);	
		
		//Polygon No view
		//TODO add color code!
		varBooleanColorClass = new EzVarBoolean("Draw Numbers", false);
		varHighlightClass = new EzVarInteger("Highlight class (0=none)",0,0,10,1);
		
		EzGroup groupPolygonClass = new EzGroup("POLYGON_CLASS elements",
				varBooleanColorClass,
				varHighlightClass);
		
		//Area Threshold View
		varAreaThreshold = new EzVarDouble("Area threshold", 100, 0, 2000, 1);
		
		EzGroup groupAreaThreshold = new EzGroup("AREA_THRESHOLD elements",
				varAreaThreshold);
		
		//Which painter should be shown by default
		varPlotting = new EzVarEnum<PlotEnum>("Painter type",
				PlotEnum.values(),PlotEnum.CELLS);
		

		//Division mode
		varBooleanReadDivisions = new EzVarBoolean("Read divisions", false);
		varBooleanPlotDivisions = new EzVarBoolean("Highlight divisions (green)",true);
		varBooleanPlotEliminations = new EzVarBoolean("Highlight eliminations (red)",false);
		varBooleanFillCells = new EzVarBoolean("Fill cells with color",true);
		EzGroup groupDivisions = new EzGroup("DIVISIONS elements", 
				//varBooleanReadDivisions, TODO
				varBooleanPlotDivisions,
				varBooleanPlotEliminations,
				varBooleanFillCells
				);
		
		
		
		//TrackingMode
		varBooleanCellIDs = new EzVarBoolean("Write TrackIDs", true);
		varBooleanDrawDisplacement = new EzVarBoolean("Draw displacement", false);
		varBooleanHighlightMistakesBoolean = new EzVarBoolean("Highlight mistakes", true);
		
		EzGroup groupTracking = new EzGroup("TRACKING elements",
				varBooleanCellIDs,
				varBooleanDrawDisplacement,
				varBooleanHighlightMistakesBoolean);
				
		
		//Graph Export Mode
		varExportType = new EzVarEnum<ExportFieldType>("Export field", ExportFieldType.values(), ExportFieldType.DIVISION);
		varOutputFile = new EzVarFile("Output File", "/Users/davide/tmp/frame0_" + varExportType.getValue().name() + ".xml");
		varFrameNo = new EzVarInteger("Frame no:",0,0,100,1);
		
		EzGroup groupExport = new EzGroup("GRAPH_EXPORT elements",
				varExportType,
				varOutputFile,
				varFrameNo);
		
		//CellMarker mode
		varCellColor = new EzVarEnum<CellColor>("Cell color", CellColor.values(), CellColor.GREEN);
		EzGroup groupMarker = new EzGroup("COLOR_TAG elements",
				varCellColor);
		
		//SAVE_SKELETON mode
		varSaveSkeleton = new EzVarFile("Output File", "");
		EzGroup groupSaveSkeleton = new EzGroup("SAVE_SKELETON elements",varSaveSkeleton);

		//Painter
		EzGroup groupPainters = new EzGroup("Painters",
				varUpdatePainterMode,
				varPlotting,
				groupCellMap,
				groupPolygonClass,
				groupVoronoiMap,
				groupAreaThreshold,
				groupDivisions,
				groupTracking,
				groupExport,
				groupMarker,
				groupSaveSkeleton
		);
		
		varRemovePainterFromSequence.addVisibilityTriggerTo(groupPainters, false);
		varPlotting.addVisibilityTriggerTo(groupCellMap, PlotEnum.CELLS);
		varPlotting.addVisibilityTriggerTo(groupPolygonClass, PlotEnum.POLYGON_CLASS);
		varPlotting.addVisibilityTriggerTo(groupVoronoiMap, PlotEnum.VORONOI);
		varPlotting.addVisibilityTriggerTo(groupAreaThreshold, PlotEnum.AREA_THRESHOLD);
		//TODO varInput.addVisibilityTriggerTo(varBooleanDerivedPolygons, InputType.SKELETON);
		varPlotting.addVisibilityTriggerTo(groupTracking, PlotEnum.TRACKING);
		varPlotting.addVisibilityTriggerTo(groupDivisions, PlotEnum.DIVISIONS);
		varPlotting.addVisibilityTriggerTo(groupExport, PlotEnum.GRAPH_EXPORT);
		varPlotting.addVisibilityTriggerTo(groupMarker, PlotEnum.COLOR_TAG);
		varPlotting.addVisibilityTriggerTo(groupSaveSkeleton, PlotEnum.SAVE_SKELETONS);
		super.addEzComponent(groupPainters);
		
		
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
		else{

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
						
						//Painter type

						PlotEnum USER_CHOICE = varPlotting.getValue();

						switch (USER_CHOICE){
						case BORDER: 
							sequence.addPainter(
									new BorderPainter(wing_disc_movie));
							break;

						case CELLS: 
							cellMode(wing_disc_movie);
							break;

						case POLYGON_CLASS: 
							boolean draw_polygonal_numbers = varBooleanColorClass.getValue();
							int highlight_polygonal_class = varHighlightClass.getValue();
							sequence.addPainter(
									new PolygonClassPainter(wing_disc_movie,
											draw_polygonal_numbers,
											highlight_polygonal_class));
							break;
						
						case POLYGON_TILE:
							sequence.addPainter(
									new PolygonConverterPainter(wing_disc_movie));
							break;
							
						case DIVISIONS: 
							divisionMode(
									wing_disc_movie, 
									varBooleanReadDivisions.getValue());
							break;

						case VORONOI: 
							voronoiMode(wing_disc_movie);
							break;

						case AREA_THRESHOLD: 
							sequence.addPainter(
									new AreaThresholdPainter(
											wing_disc_movie, 
											varAreaThreshold.getValue()));
							break;

						case ALWAYS_TRACKED: 
							sequence.addPainter(
									new AlwaysTrackedCellsOverlay(
											wing_disc_movie));
							break;

						case WRITE_OUT: 
							
							//CsvWriter.custom_write_out(wing_disc_movie);
							writeOutMode(wing_disc_movie);
							break;
							
						case TRACKING:
							
							trackingMode(
									wing_disc_movie,
									varBooleanCellIDs.getValue(),
									varBooleanHighlightMistakesBoolean.getValue(),
									varBooleanDrawDisplacement.getValue());
							
						break;
						case GRAPH_PAINTER:
							
							sequence.addPainter(
									new GraphPainter(
											wing_disc_movie));
							
							break;
						case GRAPH_EXPORT:
							graphExportMode(
									wing_disc_movie,
									varExportType.getValue(),
									varOutputFile.getValue(false),
									varFrameNo.getValue());
							break;
						case COLOR_TAG:
							sequence.addPainter(
									new CellMarker(wing_disc_movie,varCellColor));
							sequence.addPainter(
									new ColorTagPainter(wing_disc_movie));
							break;
						case SAVE_TAG:
							new TagSaver(wing_disc_movie);
							break;
						case SAVE_TAG_XLS:
							new CellWorkbook(wing_disc_movie);
							break;
						case SAVE_SKELETONS:
							new SkeletonWriter(sequence, wing_disc_movie).write(varSaveSkeleton.getValue(false).getAbsolutePath());
							break;
						case CORRECTION_HINTS:
							sequence.addOverlay(new CorrectionOverlay(wing_disc_movie));
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
		else{
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
			Painter displacementSegments = new ArrowPainter(wing_disc_movie, maximum_displacement);
			sequence.addPainter(displacementSegments);
		}
		
		TrackPainter correspondence = new TrackPainter(wing_disc_movie,varBooleanHighlightMistakesBoolean.getValue());
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
}
