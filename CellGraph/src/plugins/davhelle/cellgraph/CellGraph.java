package plugins.davhelle.cellgraph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.vividsolutions.jts.geom.Polygon;

import plugins.adufour.ezplug.*;
import plugins.davhelle.cellgraph.graphs.TissueEvolution;
import plugins.davhelle.cellgraph.graphs.TissueGraph;
import plugins.davhelle.cellgraph.io.CsvWriter;
import plugins.davhelle.cellgraph.io.DivisionReader;
import plugins.davhelle.cellgraph.io.JtsVtkReader;
import plugins.davhelle.cellgraph.jts_poc.JtsPainter;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.misc.MosaicTracking;
import plugins.davhelle.cellgraph.nodes.CellPolygon;
import plugins.davhelle.cellgraph.nodes.NodeType;
import plugins.davhelle.cellgraph.painters.PolygonClassPainter;
import plugins.davhelle.cellgraph.painters.TrackPainter;
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
 * For various geometric computations two libraries have been used
 * Geometry - by Geotechnical Software Services [no.geosoft.cc.geometry package]
 * PolygonUtils - from org.shodor.math11.Line
 * 
 * @author Davide Heller
 *
 */

public class CellGraph extends EzPlug implements EzStoppable
{
	//plotting modes
	private enum PlotEnum{
		CORNER_MAP, HEAT_MAP,CELL_MAP,VORONOI_MAP,VORONOI_CELL_MAP, AREA_MAP
	}
	
	//observation window size specification
	private final double[] win_x = {0,512};
	private final double[] win_y = {0,512};

	//Ezplug fields 
	EzVarEnum<PlotEnum> 		varEnum;
	EzVarFile					varFile;
	EzVarSequence				varSequence;
	EzVarBoolean				varBoolean;
	EzVarBoolean				varBooleanPolygon;
	EzVarBoolean				varBooleanCCenter;
	EzVarBoolean				varBooleanCellIDs;
	EzVarBoolean				varBooleanAreaString;
	EzVarBoolean				varBooleanWriteCenters;
	EzVarBoolean				varBooleanWriteArea;
	EzVarInteger				varMaxZ;
	EzVarInteger				varMaxT;
	EzVarDouble					varColorAmp;
	
	//Stop flag for advanced thread handling TODO
	boolean						stopFlag;
	
	
	@Override
	protected void initialize()
	{
		//Ezplug variable initialization
		//TODO optimize file name display 
		
		varEnum = new EzVarEnum<PlotEnum>("Painter type",PlotEnum.values(),PlotEnum.CORNER_MAP);
		
		varBoolean = new EzVarBoolean("Remove painter", false);
		varBooleanPolygon = new EzVarBoolean("Polygons", true);
		varBooleanCCenter = new EzVarBoolean("Centers", true);
		varBooleanCellIDs = new EzVarBoolean("Cell ids",false);
		varBooleanAreaString = new EzVarBoolean("Area differences",false);
		varBooleanWriteCenters = new EzVarBoolean("Write cell centers to disk",false);
		varBooleanWriteArea = new EzVarBoolean("Write cell area properties to disk",false);
		
		varFile = new EzVarFile("Mesh file", "/Users/davide/Documents/segmentation/trial");
		varSequence = new EzVarSequence("Input sequence");
		varMaxZ = new EzVarInteger("Max z height (0 all)",0,0, 50, 1);
		varMaxT = new EzVarInteger("Time points to load:",1,0,100,1);
		varColorAmp = new EzVarDouble("Color amplification",1, 0, 100, 0.1);
		
		super.addEzComponent(varSequence);
		super.addEzComponent(varBoolean);
		
		//Painter group options
		EzGroup groupCellMap = new EzGroup("CELL_MAP elements",varBooleanPolygon,varBooleanCCenter,varBooleanWriteCenters);
		EzGroup groupHeatMap = new EzGroup("HEAT_MAP elements",varMaxZ);
		EzGroup groupVoronoiMap = new EzGroup("VORONOI_MAP elements",varBooleanCellIDs);
		EzGroup groupAreaMap = new EzGroup("AREA_MAP elements",varBooleanAreaString,varBooleanWriteArea,varColorAmp);
		
		//Painter Choice
		EzGroup groupFiles = new EzGroup(
				"Representation", varFile, varMaxT, varEnum,
				groupCellMap, groupHeatMap, groupVoronoiMap,groupAreaMap);
		
		super.addEzComponent(groupFiles);
		
		varBoolean.addVisibilityTriggerTo(groupFiles, false);
		varEnum.addVisibilityTriggerTo(groupCellMap, PlotEnum.CELL_MAP);
		varEnum.addVisibilityTriggerTo(groupHeatMap, PlotEnum.HEAT_MAP);
		varEnum.addVisibilityTriggerTo(groupVoronoiMap, PlotEnum.VORONOI_MAP);
		varEnum.addVisibilityTriggerTo(groupAreaMap, PlotEnum.AREA_MAP);
		
	}
	
	@Override
	protected void execute()
	{
		// main plugin code goes here, and runs in a separate thread

		//First boolean choice to remove previous painters
		//on the same sequence
		if(varBoolean.getValue()){
			Sequence sequence = varSequence.getValue();
			
			if(sequence != null){
				// remove previous painters
				List<Painter> painters = sequence.getPainters();
				//List<Painter> painters = coverSequence.getPainters(ContourPainter2.class);
				for (Painter painter : painters) {
					sequence.removePainter(painter);
					sequence.painterChanged(painter);    				
				}
			}
			
		}
		else{
			
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
			
			/******************SPATIO TEMPORAL(ST) GRAPH CREATION***********************************/
			//TODO prior setup?
//			TissueEvolution wing_disc_movie = new TissueEvolution(time_points);
			TissueEvolution wing_disc_movie = new TissueEvolution();
			
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
				
				TissueGraph current_frame = new TissueGraph(current_file_no);

				//insert all polygons into graph as CellPolygons
				ArrayList<CellPolygon> cellList = new ArrayList<CellPolygon>();
				Iterator<Polygon> poly_it = polygonMesh.iterator();
				while(poly_it.hasNext()){
					CellPolygon c = new CellPolygon(poly_it.next());
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
				Iterator<NodeType> cell_it = current_frame.iterator();
				
				while(cell_it.hasNext()){
					CellPolygon a = (CellPolygon)cell_it.next();
					Iterator<CellPolygon> neighbor_it = cellList.iterator();
					while(neighbor_it.hasNext()){
						CellPolygon b = neighbor_it.next();
						if(a.getGeometry().touches(b.getGeometry()))
							current_frame.addEdge(a, b);
					}
				}
				/******************ST-GRAPH UPDATE***********************************/
				
				//wing_disc_movie.setFrame(current_frame, current_file_no);
				wing_disc_movie.addFrame(current_frame);
				
				/******************ST-GRAPH PAINTING***********************************/

				//identify the image we want to paint on
				Sequence sequence = varSequence.getValue();
				
				//TODO this assumes you are always starting with frame 0
				JtsPainter jts_cell_center = new JtsPainter(polygonMesh, i);
				sequence.addPainter(jts_cell_center);
				
			}
			
//			System.out.println("Successfully read in "+wing_disc_movie.size()+" movie frames");
				
			Sequence sequence = varSequence.getValue();			
			
			/******************ST-GRAPH TRACKING***********************************/


				//Border identification and discarding of outer ring
				BorderCells borderUpdate = new BorderCells(wing_disc_movie);
				borderUpdate.applyBoundaryCondition();
				
				//to remove another layer just reapply the method
//				borderUpdate.applyBoundaryCondition();
				
				//Paint border conditions
				sequence.addPainter(borderUpdate);
				
				//Tracking
//				if(wing_disc_movie.size() > 1){
//					MosaicTracking tracker = new MosaicTracking(wing_disc_movie);
//					//perform tracking TODO trycatch
//					tracker.track();
//
//					//Paint corresponding cells in time
//					TrackPainter correspondence = new TrackPainter(wing_disc_movie);
//					sequence.addPainter(correspondence);
//				}
				
				//Area statistics
//				CsvWriter.trackedArea(wing_disc_movie);
//				CsvWriter.frameAndArea(wing_disc_movie);
				
				//Painter to depict the polygon class within each polygon
//				PolygonClassPainter pc_painter = new PolygonClassPainter(wing_disc_movie);
//				sequence.addPainter(pc_painter);

				//Divisions read in 
//				try{
//				DivisionReader division_reader = new DivisionReader(wing_disc_movie);
//				sequence.addPainter(division_reader);
//				}
//				catch(IOException e){
//					System.out.println("Something went wrong in division reading");
//				}

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

