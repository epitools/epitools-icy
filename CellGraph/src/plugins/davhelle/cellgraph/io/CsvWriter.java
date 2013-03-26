package plugins.davhelle.cellgraph.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Iterator;

import plugins.davhelle.cellgraph.graphs.DevelopmentType;
import plugins.davhelle.cellgraph.graphs.TissueEvolution;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;
import icy.gui.dialog.SaveDialog;

/**
 * Class to ease the generation of data output.
 * Every method returns a file dialog to ask the user where he wants to save the statistic.
 * 
 * @author Davide Heller
 *
 */
public class CsvWriter {
	
	/**
	 * File dialog generation
	 * 
	 * @param statistic_description distinctive name for the statistic to be saved
	 * @return path to file chosen by user
	 */
	private static String chooseFile(String statistic_description){
		
		String fileName = SaveDialog.chooseFile(
				"Please choose where to save the "+statistic_description+" statistic", 
				"/Users/davide/Dropbox/IMLS/statistics/",
				"data",
				".csv");
	
		return fileName;
	}
	
	/**
	 * For every cell write area size for every time point available
	 * 
	 * 
	 * @param stGraph
	 */
	public static void trackedArea(DevelopmentType stGraph){
	
		// Create file for mosaic particle tracking
		try{
			
			String file = chooseFile("area development");
			
			FileWriter fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);
			
			if(stGraph.hasTracking()){
				
				FrameGraph frame_0 = stGraph.getFrame(0);
				Iterator<Node> cell_it = frame_0.iterator();
				
				//out.write("cell_id");
//				for(int i=0; i<stGraph.size(); i++)
//					out.write(", "+i);
//				out.write("\n");
				
				while(cell_it.hasNext()){
					Node cell = cell_it.next();
					
					//out.write("c_"+cell.getTrackID()+", ");
					while(cell != null){
						double area = cell.getGeometry().getArea();
						//System.out.println(area);
						out.write(Double.toString(area)+", ");
						cell = cell.getNext();
					}

					out.write("\n");

				}

			}

			out.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
	}
	
	/**
	 * For each non boundary cell in frame 0, write polygon class (edge no) and 
	 * the area to a csv file. 
	 * 
	 * @param stGraph
	 */
	public static void polygonClassAndArea(DevelopmentType stGraph){

		try{
			
			String file = chooseFile("area vs pn");
			
			FileWriter fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);
			
			if(stGraph.hasTracking()){
				
				FrameGraph frame_0 = stGraph.getFrame(0);
				Iterator<Node> cell_it = frame_0.iterator();
				
				out.write("polygon_no, area\n");
				
				while(cell_it.hasNext()){
					Node cell = cell_it.next();

					if(!cell.onBoundary())
						out.write(frame_0.degreeOf(cell)+","+cell.getGeometry().getArea());

					out.write("\n");

				}

			}

			out.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
	}

	public static void frameAndArea(DevelopmentType stGraph) {
		try{
			
			String file = chooseFile("frame and area");
			
			FileWriter fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);
			
			out.write("frame, area\n");
			
			for(int i=0; i<stGraph.size(); i++){
				
				FrameGraph frame_i = stGraph.getFrame(i);
				Iterator<Node> cell_it = frame_i.iterator();	
				
				while(cell_it.hasNext()){
					Node cell = cell_it.next();

					if(!cell.onBoundary())
						out.write(i+","+cell.getGeometry().getArea());

					out.write("\n");

				}

			}

			out.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
	}
	
	

}
