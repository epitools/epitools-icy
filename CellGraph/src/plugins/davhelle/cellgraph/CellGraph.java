/*=========================================================================
 *
 *  (C) Copyright (2012-2014) Basler Group, IMLS, UZH
 *  
 *  All rights reserved.
 *	
 *  author:	Davide Heller
 *  email:	davide.heller@imls.uzh.ch
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
import java.util.List;

import org.testng.Assert;

import com.vividsolutions.jts.geom.Geometry;

import plugins.adufour.ezplug.EzException;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarFile;
import plugins.adufour.ezplug.EzVarFloat;
import plugins.adufour.ezplug.EzVarFolder;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.graphs.FrameGenerator;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.TissueEvolution;
import plugins.davhelle.cellgraph.io.CsvTrackReader;
import plugins.davhelle.cellgraph.io.CsvTrackWriter;
import plugins.davhelle.cellgraph.io.DivisionReader;
import plugins.davhelle.cellgraph.io.FileNameGenerator;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.SegmentationProgram;
import plugins.davhelle.cellgraph.io.WktPolygonExporter;
import plugins.davhelle.cellgraph.io.WktPolygonImporter;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.misc.SmallCellRemover;
import plugins.davhelle.cellgraph.painters.ArrowPainter;
import plugins.davhelle.cellgraph.painters.DivisionPainter;
import plugins.davhelle.cellgraph.painters.GraphCoherenceOverlay;
import plugins.davhelle.cellgraph.painters.PolygonPainter;
import plugins.davhelle.cellgraph.painters.SiblingPainter;
import plugins.davhelle.cellgraph.painters.TrackIdPainter;
import plugins.davhelle.cellgraph.painters.TrackPainter;
import plugins.davhelle.cellgraph.tracking.HungarianTracking;
import plugins.davhelle.cellgraph.tracking.MosaicTracking;
import plugins.davhelle.cellgraph.tracking.NearestNeighborTracking;
import plugins.davhelle.cellgraph.tracking.TrackingAlgorithm;

/**
 * <b>CellGraph</b> is a plugin for the bioimage analysis tool ICY. 
 * 
 * http://icy.bioimageanalysis.org developed at
 * Quantitative Image Analysis Unit at Institut Pasteur 
 * 
 * Aim is to access the segmented live imaging samples from fluorescence 
 * confocal microscopy as spatio-temporal graph. Consecutive time points
 * will be furthermore linked by a tracking algorithm.
 * 
 * As the segmentation is usually not perfect the tool should also
 * allow for manual correction/experts input in the future.
 *
 * Finally a statistical analysis might also be included through an R pipeline
 * and Jchart built into ICY.
 * 
 * Current version requires as input either a skeleton (TODO definition)
 * representing the cell outline or VTK mesh and the original image/sequence to be open. 
 * 
 * Required libraries:
 * - JTS (Java Topology Suite) for the vtkmesh to polygon transformation
 * 		and to represent the Geometrical objects, version 1.13
 * 
 * - JGraphT to represent the graph structure of a single time point, version 0.9
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
	
	private enum TrackEnum{
		MOSAIC, NN, HUNGARIAN, CSV
	}

	//Ezplug fields 

	EzVarEnum<TrackEnum>		varTrackingAlgorithm;
	EzVarEnum<InputType>		varInput;
	EzVarEnum<SegmentationProgram> varTool;
	EzVarFile					varFile;
	EzVarBoolean				varDirectInput;
	EzVarSequence				varSequence;
	
	//Input limitation
	EzVarInteger				varMaxZ;
	EzVarInteger				varMaxT;
	EzVarInteger				varBorderEliminationNo;
	
	//EzPlug options
	EzVarBoolean				varRemovePainterFromSequence;
	EzVarBoolean				varUpdatePainterMode;
	EzVarBoolean				varCutBorder;
	
	//Tracking Parameters
	EzVarBoolean 				varDoTracking;
	EzVarBoolean				varBooleanHighlightMistakesBoolean;
	EzVarBoolean 				varBooleanDrawDisplacement;
	EzVarBoolean				varBooleanDrawGraphCoherence;
	EzVarInteger				varLinkrange;
	EzVarFloat					varDisplacement;
	EzVarDouble					varLambda1;
	EzVarDouble					varLambda2;
	EzVarBoolean				varBooleanCellIDs;
	EzVarBoolean				varBooleanLoadDivisions;
	
	//Remove cells
	EzVarBoolean				varRemoveSmallCells;
	EzVarDouble					varAreaThreshold;
	
	//Load structure into Swimming Pool
	EzVarBoolean				varUseSwimmingPool;
	
	//Save Track to CSV files
	EzVarBoolean varSaveTracking;
	EzVarFolder varTrackingFolder;
	
	//Load Track from CSV files
	EzVarFolder varLoadFile;
	
	//Stop flag for advanced thread handling TODO
	boolean						stopFlag;
	
	//sequence to paint on 
	Sequence sequence;
	EzVarFolder varWktFolder;
	EzVarBoolean varSaveWkt;
	
	@Override
	protected void initialize()
	{
		//Ezplug variable initialization
		//TODO optimize file name display, suggested methods
		//this.getUI().setMaximumSize(new Dimension(50, 150));
		//this.getUI().setResizable(false);

		//Working image and pre-existent overlays handling
		varSequence = new EzVarSequence("Input sequence");
		varRemovePainterFromSequence = new EzVarBoolean("Remove all painters", false);
		varUpdatePainterMode = new EzVarBoolean("Update painter", false);
		super.addEzComponent(varSequence);
		super.addEzComponent(varRemovePainterFromSequence);
		
		//Main Input GUI
		EzGroup groupInputPrameters = initializeInputGUI();
		
		//Tracking GUI
		varDoTracking = new EzVarBoolean("Do tracking", true);
		EzGroup groupTracking = initializeTrackingGUI();

		//Usage of temporary ICY-memory (java object swimming pool)
		varUseSwimmingPool = new EzVarBoolean("Use ICY-SwimmingPool", true);
		
		//Save skeletons using the well-known-text format (jts)
		varSaveWkt = new EzVarBoolean("Save Wkt Skeletons",false);
		varWktFolder = new EzVarFolder("WKT output folder", "");
		
		//Save tracking in csv format
		varSaveTracking = new EzVarBoolean("Save Tracking",false);
		varTrackingFolder = new EzVarFolder("Tracking output folder", "");
		EzGroup groupFiles = new EzGroup(
				"", 
				varUpdatePainterMode,
				groupInputPrameters,
				varSaveWkt,
				varWktFolder,
				varDoTracking,
				groupTracking,
				varSaveTracking,
				varTrackingFolder,
				varUseSwimmingPool
				);
		
		super.addEzComponent(groupFiles);
		
		//set visibility according to choice
		varRemovePainterFromSequence.addVisibilityTriggerTo(groupFiles, false);
		varSaveWkt.addVisibilityTriggerTo(varWktFolder, true);
		varDoTracking.addVisibilityTriggerTo(groupTracking, true);
		varDoTracking.addVisibilityTriggerTo(varSaveTracking,true);
		varSaveTracking.addVisibilityTriggerTo(varTrackingFolder, true);
		
		//cleaner way? This can still be tricked if the user selects it and then
		//changes the Tracking Algorithm to CSV
		varTrackingAlgorithm.addVisibilityTriggerTo(varSaveTracking, 
				TrackEnum.NN,TrackEnum.MOSAIC,TrackEnum.HUNGARIAN);
		
		varInput.addVisibilityTriggerTo(varSaveWkt,
				InputType.SKELETON,InputType.VTK_MESH);
		varDirectInput.addVisibilityTriggerTo(varTool, true);
		
		this.setTimeDisplay(true);
		
	}

	private EzGroup initializeInputGUI() {
		//What input is given
		varInput = new EzVarEnum<InputType>(
				"Input type",InputType.values(), InputType.SKELETON);
			
		//Should the data be directly imported from a particular tool data structure
		varDirectInput = new EzVarBoolean("Direct import", true);
		
		//In case yes, what particular program was used
		varTool = new EzVarEnum<SegmentationProgram>(
				"Seg.Tool used",SegmentationProgram.values(), SegmentationProgram.SeedWater);
		
		//Constraints on file, time and space
		varFile = new EzVarFile(
				"Input files", "/Users/davide/Documents/segmentation/");
	
		//varMaxZ = new EzVarInteger("Max z height (0 all)",0,0, 50, 1);
		varMaxT = new EzVarInteger("Time points to load:",2,1,100,1);
		
		//Border cut
		varCutBorder = new EzVarBoolean("Eliminate one border line",true);
		
		//small cell elimination
		varRemoveSmallCells = new EzVarBoolean("Remove too small cells", true);
		varAreaThreshold = new EzVarDouble("Area threshold", 10, 0, Double.MAX_VALUE, 0.1);
		varRemoveSmallCells.addVisibilityTriggerTo(varAreaThreshold, true);
		
		EzGroup inputTypeGroup = new EzGroup("Input type parameters",
				varDirectInput,
				varTool,
				varFile, 
				varMaxT,
				varCutBorder,
				varRemoveSmallCells,
				varAreaThreshold
				);
		
		EzGroup groupInputPrameters = new EzGroup("INPUT options",
				varInput,
				inputTypeGroup	
				);
		
		varInput.addVisibilityTriggerTo(varDirectInput, InputType.SKELETON);
		varInput.addVisibilityTriggerTo(varTool, InputType.SKELETON);
		
		return groupInputPrameters;
	}

	private EzGroup initializeTrackingGUI() {
		//Track view
		varLinkrange = new EzVarInteger(
				"Linkrange (frames)", 5,1,100,1);
		varDisplacement = new EzVarFloat(
				"Max. displacement (px)",5,1,20,(float)0.1);
		varBooleanCellIDs = new EzVarBoolean("Write TrackIDs", true);
		varBooleanLoadDivisions = new EzVarBoolean("Load division file", false);
		varBooleanDrawDisplacement = new EzVarBoolean("Draw displacement", false);
		varBooleanDrawGraphCoherence = new EzVarBoolean("Draw Graph coherence indeces",false);
		varBooleanHighlightMistakesBoolean = new EzVarBoolean("Highlight mistakes", true);
		varTrackingAlgorithm = new EzVarEnum<TrackEnum>("Algorithm",TrackEnum.values(), TrackEnum.NN);
		varBorderEliminationNo = new EzVarInteger("No of border layers to ex. in 1th frame",1,0,10,1);
		
		varLambda1 = new EzVarDouble("Min. Distance weight", 1, 0, 10, 0.1);
		varLambda2 = new EzVarDouble("Overlap Ratio weight", 1, 0, 10, 0.1);
		
		EzGroup groupTrackingParameters = new EzGroup("Algorithm parameters",
				varLinkrange,
				varDisplacement,
				varLambda1,
				varLambda2,
				varBorderEliminationNo,
				//varBooleanCellIDs,
				//varBooleanHighlightMistakesBoolean,
				//varBooleanLoadDivisions,
				//varBooleanDrawDisplacement,
				varBooleanDrawGraphCoherence);
		
		varLoadFile = new EzVarFolder("Select csv location", "");
		
		EzGroup groupTracking = new EzGroup("TRACKING options",
				varTrackingAlgorithm,
				groupTrackingParameters,
				varLoadFile
				);
		
		varTrackingAlgorithm.addVisibilityTriggerTo(varLoadFile, TrackEnum.CSV);
		varTrackingAlgorithm.addVisibilityTriggerTo(groupTrackingParameters, 
				TrackEnum.NN,TrackEnum.MOSAIC,TrackEnum.HUNGARIAN);
		
		groupTrackingParameters.setVisible(false);
		
		return groupTracking;
	}

	@Override
	protected void execute()
	{	
		stopFlag = false;
		
		if(wrongInputCheck())
			return;
	
		//Only remove previous painters
		if(varRemovePainterFromSequence.getValue())
			removeAllPainters();	
		else{
			
			if(varUpdatePainterMode.getValue())
				//Override current painters	
				removeAllPainters();
			
			//Create spatio temporal graph from mesh files
			TissueEvolution wing_disc_movie = new TissueEvolution();	
			generateSpatioTemporalGraph(wing_disc_movie);
			
			//safety check
			if(wing_disc_movie.size() != varMaxT.getValue())
				return;
			
			//Border identification + discard/mark
			applyBorderOptions(wing_disc_movie);
			
			//Small cell handling, executed after border options 
			if(varRemoveSmallCells.getValue())
				new SmallCellRemover(wing_disc_movie).removeCellsBelow(varAreaThreshold.getValue());
			
			if(varDoTracking.getValue())
				applyTracking(wing_disc_movie);
			else
				sequence.addPainter(new PolygonPainter(wing_disc_movie,Color.red));
			
			if(varSaveTracking.getValue())
				saveTracking(wing_disc_movie);
			
			//Load the created stGraph into ICY's shared memory, i.e. the swimmingPool
			if(varUseSwimmingPool.getValue())
				pushToSwimingPool(wing_disc_movie);	
		}
	}

	/**
	 * Safety checks for wrong GUI input
	 * 
	 * @return true if input is wrong
	 */
	private boolean wrongInputCheck() {
		boolean is_input_wrong = false;
		
		if(faultyInputCheck(varSequence.getValue() == null,
				"Plugin requires active sequence! Please open an image on which to display results"))
			return true;
		
		sequence = varSequence.getValue();
		
		if(varSaveWkt.getValue()){
			if(faultyInputCheck(varWktFolder.getValue() == null,
					"SaveWKT feature requires an ouput directory: please review!"))
				return true;

			File output_directory = varWktFolder.getValue();

			if(faultyInputCheck(!output_directory.isDirectory(), "Output WKT directory is not valid, please review"))
				return true;

		}
		
		//TODO integrate these into respective classes!
		if(varDoTracking.getValue()){
			
			if(varTrackingAlgorithm.getValue() == TrackEnum.CSV){
				if(faultyInputCheck(varLoadFile.getValue() == null,
						"Load CSV tracking feature requires an input directory: please review!"))
					return true;
				
				File input_directory = varLoadFile.getValue();
				
				if(faultyInputCheck(!input_directory.isDirectory(), "Input directory is not valid, please review"))
					return true;
				
				for(int i=0; i < varMaxT.getValue(); i++){
					File tracking_file = new File(input_directory, String.format(CsvTrackReader.tracking_file_pattern, i));
					if(faultyInputCheck(!tracking_file.exists(), "Missing tracking file:"+String.format(CsvTrackReader.tracking_file_pattern, i)))
						return true;
				}
				
				File division_file = new File(input_directory, CsvTrackReader.division_file_pattern);
				if(faultyInputCheck(!division_file.exists(), "Missing division file: "+CsvTrackReader.division_file_pattern))
					return true;
				
				File elimination_file = new File(input_directory, CsvTrackReader.elimination_file_pattern);
				if(faultyInputCheck(!elimination_file.exists(), "Missing elimination file: "+CsvTrackReader.elimination_file_pattern))
					return true;
			}
			
			if(varSaveTracking.getValue()){
				if(faultyInputCheck(varTrackingFolder.getValue() == null,
					"SaveTrack feature requires an ouput directory: please review!"))
					return true;
				
				File output_directory = varTrackingFolder.getValue();
				
				if(faultyInputCheck(!output_directory.isDirectory(), "Output directory is not valid, please review"))
					return true;
				
			}
		}

		return is_input_wrong;
	}
	
	private boolean faultyInputCheck(boolean check, String error_message) {
		boolean faultyInput = false;
		
		if(check){
			new AnnounceFrame(error_message);
			faultyInput = true;
		}
		return faultyInput;
	}

	private void saveTracking(TissueEvolution wing_disc_movie) {
		
		//TODO: save-check
		
		String output_folder = varTrackingFolder.getValue().getAbsolutePath();
		
		CsvTrackWriter track_writer = new CsvTrackWriter(wing_disc_movie,output_folder);
		track_writer.write();
		
		System.out.println("Successfully saved tracking to: "+output_folder);
		
	}

	private void removeAllPainters() {
		List<Painter> painters = sequence.getPainters();
		for (Painter painter : painters) {
			sequence.removePainter(painter);
			sequence.painterChanged(painter);    				
		}
	}

	private void generateSpatioTemporalGraph(TissueEvolution wing_disc_movie) {
		
		//TODO replace with Factory design and possibly with Thread executors!
		
		File input_file = null;
		
		try{
			//Set true to suppress default mechanism
			input_file = varFile.getValue(true);
		}
		catch(EzException e){
			new AnnounceFrame("Mesh file required to run plugin! Please set mesh file");
			return;
		}
		
		//Default file to use
		if(input_file == null){
			String default_file = "skeletons_crop_t28-68_t0000.tif";//frame_000.tif";
			//"Neo0_skeleton_001.png";
			String default_dir = "/Users/davide/data/neo/1/crop/"; ///Users/davide/Documents/segmentation/Epitools/converted_skeleton/";
			//old "/Users/davide/Documents/segmentation/Epitools/Neo0/Skeleton/";
			//previous default: /Users/davide/Documents/segmentation/seedwater_analysis/2013_05_17/ManualPmCrop5h/8bit/Outlines/Outline_0_000.tif
			input_file = new File(default_dir+default_file);
		}
		
		FileNameGenerator file_name_generator = new FileNameGenerator(
				input_file,
				varInput.getValue(), 
				varDirectInput.getValue(), 
				varTool.getValue());
		
		varFile.setButtonText(file_name_generator.getShortName());
		
		//Create FrameGenerator
		FrameGenerator frame_generator = new FrameGenerator(
				varInput.getValue(),
				varDirectInput.getValue(), 
				varTool.getValue());
		
		this.getUI().setProgressBarMessage("Creating Spatial Graphs...");

		//Generate a FrameGraph for each time point/input file
		int time_points_no = varMaxT.getValue();
		for(int i = 0; i< time_points_no; i++){
			
			//check existance
			String abs_path = file_name_generator.getFileName(i);

			try{
				File current_file = new File(abs_path);
				if(!current_file.exists())
					throw new SecurityException();
			}
			catch(Exception e){
				new AnnounceFrame("Missing time point: " + abs_path);
				return;
			}
			
			if(stopFlag){
				stopFlag = false;
				return;
			}
			
			System.out.println("reading frame "+i+": "+ abs_path);
			
			long startTime = System.currentTimeMillis();
			FrameGraph frame_from_generator = frame_generator.generateFrame(i, abs_path);
			long endTime = System.currentTimeMillis();
			
			System.out.println("\t Found " + frame_from_generator.size() + " cells in " + (endTime - startTime) + " milliseconds");

			//wing_disc_movie.setFrame(current_frame, current_file_no);
			wing_disc_movie.addFrame(frame_from_generator);
			
			this.getUI().setProgressBarValue(i/(double)time_points_no);
			
		}
		
		this.getUI().setProgressBarValue(0);
		
	}

	private void applyBorderOptions(TissueEvolution wing_disc_movie) {
		
		this.getUI().setProgressBarMessage("Setting Boundary Conditions...");
		
		if(varInput.getValue() == InputType.WKT){
			WktPolygonImporter wkt_importer = new WktPolygonImporter();
			BorderCells border = new BorderCells(wing_disc_movie);
			String export_folder = "/Users/davide/data/neo/0/skeletons_wkt/";
			for(int i=0; i < wing_disc_movie.size(); i++){
				String expected_wkt_file = String.format("%sborder_%03d.wkt",export_folder,i);
				ArrayList<Geometry> boundaries = wkt_importer.extractGeometries(expected_wkt_file);

				FrameGraph frame = wing_disc_movie.getFrame(i);
				border.markBorderCells(frame, boundaries.get(0));
			}
		}
		else{
			BorderCells borderUpdate = new BorderCells(wing_disc_movie);
			if(varCutBorder.getValue())
				borderUpdate.removeOneBoundaryLayerFromAllFrames();
			else
				borderUpdate.markOnly();

			//removing outer layers of first frame to ensure more accurate tracking
			if(varDoTracking.getValue()){
				BorderCells remover = new BorderCells(wing_disc_movie);
				for(int i=0; i < varBorderEliminationNo.getValue();i++)
					remover.removeOneBoundaryLayerFromFrame(0);

			}
		}
	}

	private void applyTracking(SpatioTemporalGraph wing_disc_movie){
		if(wing_disc_movie.size() > 1){
			
			this.getUI().setProgressBarMessage("Tracking Graphs...");
			
			TrackingAlgorithm tracker = null;
					
			switch(varTrackingAlgorithm.getValue()){
			case MOSAIC:
				tracker = new MosaicTracking(
						wing_disc_movie,
						varLinkrange.getValue(),
						varDisplacement.getValue());
				break;
			case NN:
				tracker = new NearestNeighborTracking(
						wing_disc_movie, 
						varLinkrange.getValue(),
						varLambda1.getValue(),
						varLambda2.getValue());
				break;
			case HUNGARIAN:
				tracker = new HungarianTracking(
						wing_disc_movie, 
						varLinkrange.getValue(),
						varLambda1.getValue(),
						varLambda2.getValue());
				break;
			case CSV:
				//do not save the track if loaded with CSV
				varSaveTracking.setValue(false);
				String output_folder = varLoadFile.getValue().getAbsolutePath();
				tracker = new CsvTrackReader(wing_disc_movie, output_folder);
				break;
			}
			
			// TODO try&catch
			tracker.track();
			wing_disc_movie.setTracking(true);
	
			paintTrackingResult(wing_disc_movie);
		}
		else{
			new AnnounceFrame("Tracking requires at least two time points! Please increase time points to load");
		}
	}

	private void paintTrackingResult(SpatioTemporalGraph wing_disc_movie) {
		if(varBooleanCellIDs.getValue()){
			Painter trackID = new TrackIdPainter(wing_disc_movie);
			sequence.addPainter(trackID);
		}
		
		if(varBooleanDrawDisplacement.getValue()){
			Painter displacementSegments = new ArrowPainter(wing_disc_movie, varDisplacement.getValue());
			sequence.addPainter(displacementSegments);
		}
		
		if(varBooleanDrawGraphCoherence.getValue()){
			sequence.addPainter(new GraphCoherenceOverlay(wing_disc_movie));
		}
		
		if(varBooleanLoadDivisions.getValue()){
			//read manual divisions and combine with tracking information
			try{
				DivisionReader division_reader = new DivisionReader(wing_disc_movie);
				division_reader.assignDivisions();
				sequence.addPainter(new DivisionPainter(wing_disc_movie, true,false,true));
			}
			catch(IOException e){
				System.out.println("Something went wrong in division reading");
			}
			
			sequence.addPainter(new SiblingPainter(wing_disc_movie));
		}
		else{
			//Paint corresponding cells in time
			TrackPainter correspondence = new TrackPainter(wing_disc_movie,varBooleanHighlightMistakesBoolean.getValue());
			sequence.addPainter(correspondence);
		}
	}

	private void pushToSwimingPool(TissueEvolution wing_disc_movie) {
		//remove all formerly present objects 
		//TODO review, might want to hold multiple object in future
		//also allow the user to have multiple different objects 
		//e.g. Track Manager seems to use it!
		Icy.getMainInterface().getSwimmingPool().removeAll();
		
		// Put my object in a Swimming Object
		SwimmingObject swimmingObject = new SwimmingObject(wing_disc_movie,"stGraph");
		
		// add the object in the swimming pool
		Icy.getMainInterface().getSwimmingPool().add( swimmingObject );
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

