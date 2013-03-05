package plugins.davhelle.cellgraph;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableUndirectedGraph;

import com.vividsolutions.jts.geom.MultiPolygon;

import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

import plugins.adufour.ezplug.*;
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
 * image/sequence to be open. Selecting the different overlays (PlotEnum) the VTK structure will
 * be used to create the corresponding layer. Voronoi diagrams are computed using the code of
 *  
 * SimpleVoronoi - a fast Java implementation Fortune's Voronoi algorithm by Zhenyu Pan
 * [http://sourceforge.net/projects/simplevoronoi/]
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
		
		varFile = new EzVarFile("Mesh file", "/Users/davide/Documents/segmentation");
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
			
			/******************VTK MESH READ IN SECTION***********************************/
			File mesh_file;
			
			try{
				mesh_file = varFile.getValue(true);
			}
			catch(EzException e){
				new AnnounceFrame("Mesh file required to run plugin! Please set mesh file");
				return;
			}
			
			
			//Display of different time points requires
			//the sequence to be converted to a time stack
			//Sequence operation -> convert to frames
			
			int time_points = varMaxT.getValue();
			//MeshReader[] myReaders = new MeshReader[time_points];

			//		System.out.println(varFile.name + " = " + file_name);
			//		System.out.println(varSequence.name + " = " + varSequence.getValue());

			String file_name = mesh_file.getName();
			String file_path = mesh_file.getParent() + "/";
			String file_ext = ".vtk";
			
			varFile.setButtonText(file_name);
			
			//System.out.println(file_name + "\n" + file_path + "\n" + file_ext);
			
			int ext_start = file_name.indexOf(file_ext);
			//TODO: proper number read in (no. represented in 2 digit format...)
			int file_no_start = ext_start - 2;
			
			String file_name_wo_no = file_name.substring(0, file_no_start);
			String file_no_str = file_name.substring(file_no_start, ext_start);
			
			int start_file_no = Integer.parseInt(file_no_str);
			
			//cycle through all time points
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
				
				//myReaders[i] = new MeshReader();
		
				MeshReader vtk_mesh_file_reader = new MeshReader(abs_path); 

				//check for data correctness        
				if(vtk_mesh_file_reader.is_not_polydata()){
					new AnnounceFrame("NO Poly data found in: "+file_name);
					continue;
				}

				//extract point number from polydata to initialize graph structures         
				int point_no = vtk_mesh_file_reader.get_point_no();

				//Create structure to contain all mesh points, i.e. cell_corners
				ArrayList<CellCorner> corner_list = new ArrayList<CellCorner>(point_no);  

				//Graph for containing mesh lines as edges
				ListenableUndirectedGraph<CellCorner, DefaultEdge> cell_corner_graph = 
						new ListenableUndirectedGraph<CellCorner, DefaultEdge>(DefaultEdge.class);

				//Insert polydata information into graph structures
				//TODO: remove system.out from jts creation
				Collection jts_polys;
				jts_polys = vtk_mesh_file_reader.fill_graph(corner_list, cell_corner_graph);

				//identify the image we want to paint on
				Sequence sequence = varSequence.getValue();
				//obtain the painter to be applied
				PlotEnum user_choice = varEnum.getValue();

				/******************PLOTTING SECTION***********************************/

				//Raw vertices read from vtk file
				if( user_choice == PlotEnum.CORNER_MAP){
					Painter corner_map = new CornerPainter(corner_list, current_file_no);
					sequence.addPainter(corner_map);
				}
				
				//Raw edges read from vtk file colored according to z-height (if any)
				else if( user_choice == PlotEnum.HEAT_MAP){
					int max_z = varMaxZ.getValue();

					//TODO Better check for user input
					if(max_z == 0)
						max_z = sequence.getSizeZ();

					Painter heat_map = new MeshPainter(cell_corner_graph, max_z,current_file_no);
					sequence.addPainter(heat_map);
					
					JtsPainter jts_cell_center = new JtsPainter(jts_polys, current_file_no);
					//sequence.addPainter(jts_cell_center);
					
					//Extract polygon structure as MultiPolygon
					MultiPolygon all_jts_polys = jts_cell_center.getMultiPoly();
					
					JtsPainter multi_center = new JtsPainter(all_jts_polys, current_file_no);
					sequence.addPainter(multi_center);
					
					//only does boarder points of all points restart HERE
					//JtsMultiPainter border_points = new JtsMultiPainter(all_jts_polys, current_file_no);
					JtsMultiPainter border_points = new JtsMultiPainter(
							jts_cell_center.getPolyUnion(), current_file_no);
					sequence.addPainter(border_points);
					
					JtsBorderPolygonPainter fill_border = 
							new JtsBorderPolygonPainter(
									jts_cell_center.getJtsPolygons(),
									border_points.getBorderRing(),
									current_file_no);
					
					sequence.addPainter(fill_border);
				}

				else{
					
					/******************CELL IDENTIFICATION SECTION***********************************/
					
					//Every other visualization assumes the cells to be identified
					//as set of edges, thus first these sets have to be initiated
					
					//Look for cycles in the graph, i.e. find the polygon associated to each cell 
					Map<Integer, ArrayList<Integer>> cell_map = new HashMap<Integer, ArrayList<Integer>>();
					
					for(CellCorner v_next: corner_list)
						if(cell_corner_graph.degreeOf(v_next)>2)
							v_next.compute_cycles(cell_corner_graph,cell_map);

					//For performance test
					//long start_millis = System.currentTimeMillis();
					//long time_past = System.currentTimeMillis() - start_millis;
					//System.out.println("Time to search "+v_next.toString()+":"+time_past);

//					System.out.println("Number of cells found:"+cell_map.size());
//					System.out.println("Cells found"+cell_map.toString());
//					System.out.println(cell_corner_graph.toString());

					//TODO Might be externalized, no need to add Painter for each choice separately
					boolean draw_polygons = varBooleanPolygon.getValue();
					boolean draw_cell_centers = varBooleanCCenter.getValue();
					
					//Draw cells as individual entities (Polygons, set of edges)
					CellPainter cell_painter = new CellPainter(cell_map, 
							corner_list,draw_polygons,draw_cell_centers,current_file_no);
					
					if (user_choice == PlotEnum.CELL_MAP){
						sequence.addPainter(cell_painter);
						

						//Write center coordinates to disk
						if(varBooleanWriteCenters.getValue()){
							ArrayList<Point> cell_centers = cell_painter.getCellCenters();
							System.out.println("Poly.no. found by cs:"+cell_centers.size());
							//CellWriter cell_center_writer = new CellWriter(file_path+file_name_wo_no,current_file_no);
							//cell_center_writer.write_tracking_file(cell_centers);
						}
						continue;
					}
					
					/******************VORONOI SECTION***********************************/ 
					 
					ArrayList<Color> cell_color_list = cell_painter.getCellColors();
					ArrayList<Point> cell_center_list = cell_painter.getCellCenters();
					
					//Conversion to SimpleVoroni input data structure
					int cell_no = cell_center_list.size();
					double[] x_coors = new double[cell_no];
					double[] y_coors = new double[cell_no];
					
					for(int j=0; j<cell_no; j++){
						x_coors[j] = cell_center_list.get(j).getX();
						y_coors[j] = cell_center_list.get(j).getY();
					}
					
					double minimal_site_distance = 0.5;
					Voronoi voronoi_generator = new Voronoi(
							minimal_site_distance);
					
					List<GraphEdge> voronoi_edges = voronoi_generator.generateVoronoi(
							x_coors, y_coors,
							win_x[0], win_x[1],
							win_y[0], win_y[1]);
					
					boolean PLOT_VORONOI_CELL_IDS = varBooleanCellIDs.getValue();
					Painter vornoi_painter = new VoronoiPainter(voronoi_edges,PLOT_VORONOI_CELL_IDS,current_file_no);
					
					//Plot voronoi edges
					if (user_choice == PlotEnum.VORONOI_MAP){
						sequence.addPainter(vornoi_painter);
						
						if(PLOT_VORONOI_CELL_IDS){
							Painter my_cell_ids = new CellIdPainter(cell_center_list,current_file_no);
							sequence.addPainter(my_cell_ids);
						}
						
						continue;
					}
					
					//recognize and plot voronoi polygons
					//TODO better recognition of voronoi polygons
					//and elimination of outer cells clearly distorted 
					PolygonPainter voronoi_polygons = new PolygonPainter(
							cell_center_list,
							voronoi_edges,
							current_file_no,
							cell_color_list);
					
					if (user_choice == PlotEnum.VORONOI_CELL_MAP){
						sequence.addPainter(voronoi_polygons);
						continue;
					}
					
					//Compare native cell area and voronoi cell area
					//and color cell accordingly (see source)
					
					if(user_choice == PlotEnum.AREA_MAP){
						ArrayList<Polygon> cell_polygon_list = cell_painter.getPolygons();
						ArrayList<Polygon> voronoi_polygon_list = voronoi_polygons.getPolygons();		
						double color_amplification = varColorAmp.getValue().doubleValue();
						
						AreaDifferencePainter voronoi_area_diff = new AreaDifferencePainter(
										cell_polygon_list,
										voronoi_polygon_list,
										color_amplification,
										current_file_no);


						sequence.addPainter(voronoi_area_diff);

						//optionally paint area difference string in each correspondent cell center
						boolean PLOT_DIFFERENCE_STRING = varBooleanAreaString.getValue();
						if(PLOT_DIFFERENCE_STRING){
							ArrayList<Double> area_diff_val = voronoi_area_diff.getAreaDifference();
							
							Painter area_diff_text = new CellIdPainter(
									cell_center_list,
									area_diff_val,
									current_file_no);
							
							sequence.addPainter(area_diff_text);
						}
						
						//optionally write cell area differences to disk
						if(varBooleanWriteArea.getValue()){
							//ArrayList<Double> area_diff_val = voronoi_area_diff.getAreaDifference();
							CellWriter cell_area_writer = new CellWriter(file_path+"area_"+file_name_wo_no,current_file_no);
							//cell_area_writer.write_area_diff(area_diff_val);
							cell_area_writer.write_area(cell_center_list,cell_polygon_list,voronoi_polygon_list);
						}
						continue;
					}
					

				}


				//TODO: Do waiting without CPU work
				//		stopFlag = false;
				//		while (!stopFlag)
				//		{
				//			super.getUI().setProgressBarMessage("Waiting...");
				//			Thread.yield();
				//		}
				//		
				//		myMesh.detachFromAll();

		}
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
		
//		Sequence sequence = varSequence.getValue();
//		
//		if(sequence != null){
//			// remove previous painters
//			List<Painter> painters = sequence.getPainters();
//			for (Painter painter : painters) {
//				sequence.removePainter(painter);
//				sequence.painterChanged(painter);    				
//			}
//		}
		
		
	}
	
}

