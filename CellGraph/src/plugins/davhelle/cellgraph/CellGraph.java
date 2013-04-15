package plugins.davhelle.cellgraph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.vividsolutions.jts.geom.Polygon;

import plugins.adufour.ezplug.*;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.TissueEvolution;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.io.DivisionReader;
import plugins.davhelle.cellgraph.io.JtsVtkReader;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.misc.VoronoiGenerator;
import plugins.davhelle.cellgraph.nodes.Cell;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.painters.ArrowPainter;
import plugins.davhelle.cellgraph.painters.CentroidPainter;
import plugins.davhelle.cellgraph.painters.DivisionPainter;
import plugins.davhelle.cellgraph.painters.PolygonClassPainter;
import plugins.davhelle.cellgraph.painters.PolygonPainter;
import plugins.davhelle.cellgraph.painters.SiblingPainter;
import plugins.davhelle.cellgraph.painters.TrackIdPainter;
import plugins.davhelle.cellgraph.painters.TrackPainter;
import plugins.davhelle.cellgraph.painters.VoronoiAreaDifferencePainter;
import plugins.davhelle.cellgraph.painters.VoronoiPainter;
import plugins.davhelle.cellgraph.tracking.MosaicTracking;
import plugins.davhelle.cellgraph.tracking.NearestNeighborTracking;
import plugins.davhelle.cellgraph.tracking.TrackingAlgorithm;

import icy.gui.frame.progress.AnnounceFrame;
import icy.painter.Painter;
import icy.sequence.Sequence;

/**
 * <b>CellGraph</b> is a plugin for the bioimage analysis tool ICY. 
 * 
 * http://icy.bioimageanalysis.org developed at
 * Quantitative Image Analysis Unit at Institut Pasteur 
 * 
 * Aim is to access the segmented live imaging samples from fluorescence 
 * confocal microscopy as spatio-temporal graph. Consecutive time points
 * should furthermore be linked by a tracking algorithm.
 * 
 * As the segmentation is usually not perfect the tool should also
 * allow for manual correction/experts input to improve the
 *
 * Finally a statistical analysis might also be included through an R pipeline
 * and Jchart built into ICY.
 * 
 * Current version requires as input a VTK mesh representing the cell membranes and the original
 * image/sequence to be open. 
 * 
 * Required libraries:
 * - JTS (Java Topology Suite) for the vtkmesh to polygon transformation
 * 		and to represent the Geometrical objects
 * 
 * - JGraphT to represent the graph structure of a single time point
 * 
 * For the GUI part EzPlug by Alexandre Dufour has been used.
 * [http://icy.bioimageanalysis.org/plugin/EzPlug]
 * main implementation follow this tutorial:
 * [from tutorial:http://icy.bioimageanalysis.org/plugin/EzPlug_Tutorial]
 * 
 * 
 * @author Davide Heller
 *
 */

public class CellGraph extends EzPlug implements EzStoppable
{
	//plotting modes
	private enum PlotEnum{
		CELLS,VORONOI, POLYGON_CLASS, BORDER, TRACKING, READ_DIVISIONS,
	}
	
	private enum TrackEnum{
		MOSAIC, NN,
	}

	//Ezplug fields 
	EzVarEnum<PlotEnum> 		varPlotting;
	EzVarEnum<TrackEnum>		varTracking;
	EzVarFile					varFile;
	EzVarSequence				varSequence;
	
	//EzPlug options
	EzVarBoolean				varRemovePainterFromSequence;
	EzVarBoolean				varBooleanPolygon;
	EzVarBoolean				varBooleanCCenter;
	EzVarBoolean				varBooleanCellIDs;
	EzVarBoolean				varBooleanAreaDifference;
	EzVarBoolean				varBooleanVoronoiDiagram;
	EzVarBoolean				varBooleanWriteCenters;
	EzVarBoolean				varBooleanWriteArea;
	EzVarBoolean				varBooleanLoadDivisions;
	EzVarBoolean 				varBooleanDrawDisplacement;
	EzVarInteger				varMaxZ;
	EzVarInteger				varMaxT;
	EzVarInteger				varLinkrange;
	EzVarFloat					varDisplacement;
	
	//Stop flag for advanced thread handling TODO
	boolean						stopFlag;
	
	//sequence to paint on 
	Sequence sequence;

	
	@Override
	protected void initialize()
	{
		//Ezplug variable initialization
		//TODO optimize file name display 
		
		varSequence = new EzVarSequence("Input sequence");
		varRemovePainterFromSequence = new EzVarBoolean("Remove painter", false);
		
		super.addEzComponent(varSequence);
		super.addEzComponent(varRemovePainterFromSequence);
		
		//Constraints on file, time and space
		varFile = new EzVarFile("Mesh file", "/Users/davide/Documents/segmentation/trial");
		//varMaxZ = new EzVarInteger("Max z height (0 all)",0,0, 50, 1);
		varMaxT = new EzVarInteger("Time points to load:",1,0,100,1);
		
		//Cells view
		varBooleanPolygon = new EzVarBoolean("Polygons", true);
		varBooleanCCenter = new EzVarBoolean("Centers", true);
		varBooleanWriteCenters = new EzVarBoolean("Write cell centers to disk",false);
		EzGroup groupCellMap = new EzGroup("CELLS elements",
				varBooleanPolygon,
				varBooleanCCenter,
				varBooleanWriteCenters);
		

		//Voronoi view
		varBooleanVoronoiDiagram = new EzVarBoolean("Voronoi Diagram", true);
		varBooleanAreaDifference = new EzVarBoolean("Area difference", false);
		
		EzGroup groupVoronoiMap = new EzGroup("VORONOI elements",
				varBooleanAreaDifference,
				varBooleanVoronoiDiagram);	
		
		//Track view
		varLinkrange = new EzVarInteger(
				"Linkrange (frames)", 5,1,100,1);
		varDisplacement = new EzVarFloat(
				"Max. displacement (px)",5,1,20,(float)0.1);
		varBooleanCellIDs = new EzVarBoolean("Write TrackIDs", true);
		varBooleanLoadDivisions = new EzVarBoolean("Load division file", false);
		varBooleanDrawDisplacement = new EzVarBoolean("Draw displacement", false);
		varTracking = new EzVarEnum<TrackEnum>("Algorithm",TrackEnum.values(), TrackEnum.NN);
		EzGroup groupTracking = new EzGroup("TRACKING elements",
				varTracking,
				varLinkrange,
				varDisplacement,
				varBooleanCellIDs,
				varBooleanLoadDivisions,
				varBooleanDrawDisplacement);
		
		//Which painter should be shown by default
		varPlotting = new EzVarEnum<PlotEnum>("Painter type",PlotEnum.values(),PlotEnum.CELLS);
		
		//Painter Choice
		EzGroup groupFiles = new EzGroup(
				"Representation", varFile, varMaxT, varPlotting,
				groupCellMap, groupVoronoiMap,groupTracking);
		
		super.addEzComponent(groupFiles);
		
		//set visibility according to choice
		varRemovePainterFromSequence.addVisibilityTriggerTo(groupFiles, false);
		varPlotting.addVisibilityTriggerTo(groupCellMap, PlotEnum.CELLS);
		varPlotting.addVisibilityTriggerTo(groupVoronoiMap, PlotEnum.VORONOI);
		varPlotting.addVisibilityTriggerTo(groupTracking, PlotEnum.TRACKING);
		
		this.setTimeDisplay(true);
		
	}
	
	@Override
	protected void execute()
	{
		// main plugin code goes here, and runs in a separate thread

		//check that input sequence is present
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
			
			//Create spatio temporal graph from mesh files
			
			TissueEvolution wing_disc_movie = new TissueEvolution();	
			generateSpatioTemporalGraph(wing_disc_movie);
			
			//Border identification and discarding of outer ring
			BorderCells borderUpdate = new BorderCells(wing_disc_movie);
			borderUpdate.applyBoundaryCondition();
			
			//to remove another layer just reapply the method
			//borderUpdate.applyBoundaryCondition();

			//Display results according to user choice
			
			PlotEnum USER_CHOICE = varPlotting.getValue();
			
			switch (USER_CHOICE){
				case BORDER: sequence.addPainter(borderUpdate);
					break;
				case CELLS: cellMode(wing_disc_movie);
					break;
				case POLYGON_CLASS: sequence.addPainter(new PolygonClassPainter(wing_disc_movie));
					break;
				case READ_DIVISIONS: divisionMode(wing_disc_movie);
					break;
				case TRACKING: trackingMode(wing_disc_movie);
					break;
				case VORONOI: voronoiMode(wing_disc_movie);
					break;
			}

			//future statistical output statistics
//			CsvWriter.trackedArea(wing_disc_movie);
//			CsvWriter.frameAndArea(wing_disc_movie);

		}

	}
	
	private void divisionMode(SpatioTemporalGraph wing_disc_movie){
		//Divisions read in 
		try{
			DivisionReader division_reader = new DivisionReader(wing_disc_movie);
			division_reader.backtrackDivisions();
			sequence.addPainter(division_reader);
		}
		catch(IOException e){
			System.out.println("Something went wrong in division reading");
		}
	}
	
	private void cellMode(SpatioTemporalGraph wing_disc_movie){
		if(varBooleanCCenter.getValue()){
			Painter centroids = new CentroidPainter(wing_disc_movie);
			sequence.addPainter(centroids);
		}
		
		if(varBooleanPolygon.getValue()){
			Painter polygons = new PolygonPainter(wing_disc_movie);
			sequence.addPainter(polygons);
		}
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
	
	private void trackingMode(SpatioTemporalGraph wing_disc_movie){
		//Tracking
		if(wing_disc_movie.size() > 1){
			
			TrackingAlgorithm tracker = null;
					
			switch(varTracking.getValue()){
			
			case MOSAIC:
				tracker = new MosaicTracking(
						wing_disc_movie,
						varLinkrange.getValue(),
						varDisplacement.getValue());
				break;
			case NN:
				tracker = new NearestNeighborTracking(
						wing_disc_movie, varLinkrange.getValue());
				break;
			}

			// TODO perform tracking trycatch
			tracker.track();

			if(varBooleanCellIDs.getValue()){
				Painter trackID = new TrackIdPainter(wing_disc_movie);
				sequence.addPainter(trackID);
			}
			
			if(varBooleanDrawDisplacement.getValue()){
				Painter displacementSegments = new ArrowPainter(wing_disc_movie, varDisplacement.getValue());
				sequence.addPainter(displacementSegments);
			}
			
			
			if(varBooleanLoadDivisions.getValue()){
				//read manual divisions and combine with tracking information
				try{
					DivisionReader division_reader = new DivisionReader(wing_disc_movie);
					division_reader.assignDivisions();
					sequence.addPainter(new DivisionPainter(wing_disc_movie));
				}
				catch(IOException e){
					System.out.println("Something went wrong in division reading");
				}
				
				sequence.addPainter(new SiblingPainter(wing_disc_movie));
			}
			else{
				//Paint corresponding cells in time
				TrackPainter correspondence = new TrackPainter(wing_disc_movie);
				sequence.addPainter(correspondence);
			}
			

		}
		else{
			new AnnounceFrame("Tracking requires at least two time points! Please increase time points to load");
		}
	}
	
 	private void generateSpatioTemporalGraph(TissueEvolution wing_disc_movie) {
		
		/******************FILE NAME GENERATION***********************************/
		
		//TODO: proper number read in (no. represented in 2 digit format...)
		//TODO: do it like fiji-loci-fileImporter : parse Option
		
		File mesh_file;
		
		try{
			mesh_file = varFile.getValue(true);
		}
		catch(EzException e){
			new AnnounceFrame("Mesh file required to run plugin! Please set mesh file");
			return;
		}
	
		int time_points = varMaxT.getValue();

		String file_name = mesh_file.getName();
		String file_path = mesh_file.getParent() + "/";
		String file_ext = ".vtk";
		
		int ext_start = file_name.indexOf(file_ext);
		int file_no_start = ext_start - 2;
		
		String file_name_wo_no = file_name.substring(0, file_no_start);
		String file_no_str = file_name.substring(file_no_start, ext_start);
		
		int start_file_no = Integer.parseInt(file_no_str);
		
		varFile.setButtonText(file_name);
		
		
		
		/******************FRAME LOOP***********************************/
		for(int i = 0; i<time_points; i++){
			
			//successive file name generation TODO:make it safe!
			int current_file_no = start_file_no + i;
			String abs_path = "";
			
			if(current_file_no < 10)
				abs_path = file_path + file_name_wo_no + "0" + current_file_no + file_ext;  
			else
				abs_path = file_path + file_name_wo_no + current_file_no + file_ext;
				
			//System.out.println(abs_path);
			
			try{
				File current_file = new File(abs_path);
				if(!current_file.exists())
					throw new SecurityException();
			}
			catch(Exception e){
				new AnnounceFrame("Missing time point: " + abs_path);
				continue;
			}
			
			/******************VTK MESH TO POLYGON TRANSFORMATION***********************************/

	
			JtsVtkReader polygonReader = new JtsVtkReader(abs_path); 

			//check for data correctness        
			if(polygonReader.is_not_polydata()){
				new AnnounceFrame("NO Poly data found in: "+file_name);
				continue;
			}

			ArrayList<Polygon> polygonMesh = polygonReader.extractPolygons();
			
			/******************GRAPH GENERATION***********************************/
			
			FrameGraph current_frame = new FrameGraph(current_file_no);

			//insert all polygons into graph as CellPolygons
			ArrayList<Cell> cellList = new ArrayList<Cell>();
			Iterator<Polygon> poly_it = polygonMesh.iterator();
			while(poly_it.hasNext()){
				Cell c = new Cell(poly_it.next(),current_frame);
				cellList.add(c);
				current_frame.addVertex(c);
			}
			
			/* Algorithm to find neighborhood relationships between polygons
			 * 
			 * for every polygon
			 * 	for every non assigned face
			 * 	 find neighboring polygon
			 * 	  add edge to graph if neighbor found
			 * 	  if all faces assigned 
			 * 	   exclude from list
			 *    
			 */

			//TODO more efficient search
			Iterator<Node> cell_it = current_frame.iterator();
			
			while(cell_it.hasNext()){
				Cell a = (Cell)cell_it.next();
				Iterator<Cell> neighbor_it = cellList.iterator();
				while(neighbor_it.hasNext()){
					Cell b = neighbor_it.next();
					if(a.getGeometry().touches(b.getGeometry()))
						current_frame.addEdge(a, b);
				}
			}
			/******************ST-GRAPH UPDATE***********************************/
			
			//wing_disc_movie.setFrame(current_frame, current_file_no);
			wing_disc_movie.addFrame(current_frame);
			

		}
		
	}

	@Override
	public void clean()
	{
		// use this method to clean local variables or input streams (if any) to avoid memory leaks
		
	}
	
	@Override
	public void stopExecution()
	{
		// this method is from the EzStoppable interface
		// if this interface is implemented, a "stop" button is displayed
		// and this method is called when the user hits the "stop" button
		stopFlag = true;
		
	}
	
}

