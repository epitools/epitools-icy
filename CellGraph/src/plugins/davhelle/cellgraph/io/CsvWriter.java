package plugins.davhelle.cellgraph.io;

import icy.gui.dialog.SaveDialog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Helper methods for writing of CSV files based on StringBuilder
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
	 * For every cell in the first frame of the Spatiotemporal graph
	 * write out the temporal evolution of the area
	 * 
	 * @param stGraph graph to be written out
	 */
	public static void trackedArea(SpatioTemporalGraph stGraph){
	
		final boolean VERBOSE = false;
		
		String file_name = chooseFile("area development");
		if(file_name == null){
			System.out.println("Writing aborted!");
			return;
		}
		
		StringBuilder builder_main = new StringBuilder();

		if(stGraph.hasTracking()){

			FrameGraph frame_0 = stGraph.getFrame(0);
			Iterator<Node> cell_it = frame_0.iterator();

			if(VERBOSE){
				builder_main.append("cell_id");
				for(int i=0; i<stGraph.size(); i++)
					builder_main.append(", "+i);
				builder_main.append("\n");
			}
			
			while(cell_it.hasNext()){
				Node cell = cell_it.next();

				if(VERBOSE)
					builder_main.append("c_"+cell.getTrackID()+", ");
				
				while(cell != null){
					double area = cell.getGeometry().getArea();
					builder_main.append(Double.toString(area)+", ");
					cell = cell.getNext();
				}

				builder_main.append("\n");
			}
		}
		
		writeOutBuilder(builder_main, new File(file_name));
		
	}
	
	/**
	 * For each non boundary cell in frame 0, write polygon class (edge no) and 
	 * the area to a csv file. 
	 * 
	 * @param stGraph graph to be written out
	 */
	public static void polygonClassAndArea(SpatioTemporalGraph stGraph){

		String file_name = chooseFile("area vs polygonal number");
		if(file_name == null){
			System.out.println("Writing aborted!");
			return;
		}
		
		StringBuilder builder_main = new StringBuilder();

		if(stGraph.hasTracking()){

			FrameGraph frame_0 = stGraph.getFrame(0);
			Iterator<Node> cell_it = frame_0.iterator();

			builder_main.append("polygon_no, area\n");

			while(cell_it.hasNext()){
				Node cell = cell_it.next();

				if(!cell.onBoundary())
					builder_main.append(frame_0.degreeOf(cell)+","+cell.getGeometry().getArea());

				builder_main.append("\n");

			}

		}

		writeOutBuilder(builder_main, new File(file_name));
	}

	/**
	 * Helper method to write a StringBuilder object to a text file
	 * 
	 * @param builder_main StringBuilder to be written out
	 * @param output_file destination path
	 */
	public static void writeOutBuilder(StringBuilder builder_main, File output_file) {
		FileWriter fstream;
		try {
			fstream = new FileWriter(output_file);
			BufferedWriter writer = new BufferedWriter(fstream);
			writer.append(builder_main);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	

}
