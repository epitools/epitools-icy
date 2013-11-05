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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import plugins.adufour.ezplug.EzException;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarFile;
import plugins.adufour.ezplug.EzVarFloat;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.graphs.FrameGenerator;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.TissueEvolution;
import plugins.davhelle.cellgraph.io.DivisionReader;
import plugins.davhelle.cellgraph.io.FileNameGenerator;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.JtsVtkReader;
import plugins.davhelle.cellgraph.io.SegmentationProgram;
import plugins.davhelle.cellgraph.io.SkeletonReader;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.misc.SmallCellRemover;
import plugins.davhelle.cellgraph.nodes.Cell;
import plugins.davhelle.cellgraph.nodes.ComparablePolygon;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.painters.ArrowPainter;
import plugins.davhelle.cellgraph.painters.DivisionPainter;
import plugins.davhelle.cellgraph.painters.GraphCoherenceOverlay;
import plugins.davhelle.cellgraph.painters.PolygonPainter;
import plugins.davhelle.cellgraph.painters.SiblingPainter;
import plugins.davhelle.cellgraph.painters.TrackIdPainter;
import plugins.davhelle.cellgraph.painters.TrackPainter;
import plugins.davhelle.cellgraph.tracking.MosaicTracking;
import plugins.davhelle.cellgraph.tracking.NearestNeighborTracking;
import plugins.davhelle.cellgraph.tracking.TrackingAlgorithm;

import com.vividsolutions.jts.geom.Polygon;

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
	
	private enum TrackEnum{
		MOSAIC, NN,
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
	
	//Stop flag for advanced thread handling TODO
	boolean						stopFlag;
	
	//sequence to paint on 
	Sequence sequence;
	

	
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
		
		EzGroup groupFiles = new EzGroup(
				"", 
				varUpdatePainterMode,
				groupInputPrameters,
				varDoTracking,
				groupTracking,
				varUseSwimmingPool
				);
		
		super.addEzComponent(groupFiles);
		
		//set visibility according to choice
		varRemovePainterFromSequence.addVisibilityTriggerTo(groupFiles, false);
		varDoTracking.addVisibilityTriggerTo(groupTracking, true);
		varDirectInput.addVisibilityTriggerTo(varTool, true);
		
		this.setTimeDisplay(true);
		
	}

	private EzGroup initializeInputGUI() {
		//What input is given
		varInput = new EzVarEnum<InputType>(
				"Input type",InputType.values(), InputType.SKELETON);
			
		//Should the data be directly imported from a particular tool datastructure
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
		
		
		EzGroup groupInputPrameters = new EzGroup("Input Parameters",
				varInput,
				varDirectInput,
				varTool,
				varFile, 
				varMaxT,
				varCutBorder,
				varRemoveSmallCells,
				varAreaThreshold		
				);
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
	
		EzGroup groupTracking = new EzGroup("TRACKING elements",
				varTrackingAlgorithm,
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
		return groupTracking;
	}

	@Override
	protected void execute()
	{		
		if(noOpenSequenceCheck())
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
			
			//Border identification + discard/mark
			applyBorderOptions(wing_disc_movie);
			
			//Small cell handling, executed after border options 
			if(varRemoveSmallCells.getValue())
				new SmallCellRemover(wing_disc_movie).removeCellsBelow(varAreaThreshold.getValue());
			
			if(varDoTracking.getValue())
				applyTracking(wing_disc_movie);
			else
				sequence.addPainter(new PolygonPainter(wing_disc_movie));
			
			//Load the created stGraph into ICY's shared memory, i.e. the swimmingPool
			if(varUseSwimmingPool.getValue())
				pushToSwimingPool(wing_disc_movie);	
		}
	}

	private void removeAllPainters() {
		List<Painter> painters = sequence.getPainters();
		for (Painter painter : painters) {
			sequence.removePainter(painter);
			sequence.painterChanged(painter);    				
		}
	}

	private boolean noOpenSequenceCheck() {
		boolean no_open_sequence = false;
		
		sequence = varSequence.getValue();
		if(sequence == null){
			new AnnounceFrame("Plugin requires active sequence! Please open an image on which to display results");
			no_open_sequence = true;
		}
		return no_open_sequence;
	}

	private void generateSpatioTemporalGraph(TissueEvolution wing_disc_movie) {
		
		//TODO replace with Factory design and possibly with Thread executors!
		
		File input_file = null;
		
		try{
			//Set true to suppress default mechanism
			input_file = varFile.getValue(false);
		}
		catch(EzException e){
			new AnnounceFrame("Mesh file required to run plugin! Please set mesh file");
			return;
		}
		
		//Default file to use
		if(input_file == null){
			String default_file = "frame_000.tif";
			//"Neo0_skeleton_001.png";
			String default_dir = "/Users/davide/Documents/segmentation/Epitools/converted_skeleton/";
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
				varTool.getValue(), 
				file_name_generator);
		
		/******************FRAME LOOP***********************************/
		for(int i = 0; i< varMaxT.getValue(); i++){
			
			long startTime = System.currentTimeMillis();
			FrameGraph frame_from_generator = frame_generator.generateFrame(i);
			long endTime = System.currentTimeMillis();
			System.out.println("Generator " + (endTime - startTime) + " milliseconds");
			System.out.println("Generated: "+frame_from_generator.size() + " cells found");

			startTime = System.currentTimeMillis();
			String abs_path = file_name_generator.getFileName(i);
			
			//check existance
			try{
				File current_file = new File(abs_path);
				if(!current_file.exists())
					throw new SecurityException();
			}
			catch(Exception e){
				new AnnounceFrame("Missing time point: " + abs_path);
				continue;
			}
			
			System.out.println("reading frame "+i+": "+ abs_path);
	
			/******************INPUT TO POLYGON TRANSFORMATION***********************************/
	
			
			ArrayList<Polygon> polygonMesh = null;
			
			switch(varInput.getValue()){
			case SKELETON:
				
				boolean REDO_SKELETON = false;
				
				if(varDirectInput.getValue())
					if(varTool.getValue().equals(SegmentationProgram.SeedWater) 
							|| varTool.getValue().equals(SegmentationProgram.MatlabLabelOutlines))
					REDO_SKELETON = true;
	
				SkeletonReader skeletonReader = new SkeletonReader(REDO_SKELETON, varTool.getValue());
				
				//TODO CHECK FOR DATA CORRECTION
				
				polygonMesh = skeletonReader.extractPolygons(abs_path);
				
				break;
			case VTK_MESH:
				JtsVtkReader polygonReader = new JtsVtkReader(); 

				polygonMesh = polygonReader.extractPolygons(abs_path);
				
				break;	
			}
	
			
			/******************GRAPH GENERATION***********************************/
			
			FrameGraph current_frame = new FrameGraph(file_name_generator.getFrameNo(i));
	
			//insert all polygons into graph as CellPolygons
			ArrayList<Cell> cell_list = new ArrayList<Cell>();
			
			//order polygons according to cell center position
			ComparablePolygon[] poly_array = new ComparablePolygon[polygonMesh.size()];
			for(int k=0; k < poly_array.length; k++)
				poly_array[k] = new ComparablePolygon(polygonMesh.get(k));
			
			//order polygons according to comparator class
			Arrays.sort(poly_array);
			
			//obtain the polygons back and create cells
			for(ComparablePolygon polygon: poly_array){
				Cell c = new Cell(polygon.getPolygon(),current_frame);
				cell_list.add(c);
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
				Iterator<Cell> neighbor_it = cell_list.iterator();
				while(neighbor_it.hasNext()){
					Cell b = neighbor_it.next();
					//avoid creating the connection twice
					if(!current_frame.containsEdge(a, b))
						if(a.getGeometry().touches(b.getGeometry()))
							current_frame.addEdge(a, b);
				}
			}
			
			endTime = System.currentTimeMillis();
			System.out.println("OldProced " + (endTime - startTime) + " milliseconds");
			
			/******************ST-GRAPH UPDATE***********************************/
			
			System.out.println("OldProced: "+current_frame.size()+ " cells found");
			
			//wing_disc_movie.setFrame(current_frame, current_file_no);
			wing_disc_movie.addFrame(frame_from_generator);
			
		}
		
	}

	private void applyBorderOptions(TissueEvolution wing_disc_movie) {
		BorderCells borderUpdate = new BorderCells(wing_disc_movie);
		if(varCutBorder.getValue()){
			borderUpdate.applyBoundaryCondition();
			sequence.addPainter(borderUpdate);
		}
		else
			borderUpdate.markOnly();
	
		//tracking: link the graph in the temporal dimension
		
		//removing outer layers of first frame to ensure accurate tracking
		if(varDoTracking.getValue()){
			BorderCells remover = new BorderCells(wing_disc_movie);
			for(int i=0; i < varBorderEliminationNo.getValue();i++)
				remover.removeOneBoundaryLayerFromFrame(0);
	
	
			//update to new boundary conditions
			remover.markOnly();
		}
	}

	private void applyTracking(SpatioTemporalGraph wing_disc_movie){
		if(wing_disc_movie.size() > 1){
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
				sequence.addPainter(new DivisionPainter(wing_disc_movie));
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

