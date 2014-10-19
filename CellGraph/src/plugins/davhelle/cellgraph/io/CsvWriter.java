/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
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
	public static void trackedArea(SpatioTemporalGraph stGraph){
	
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
	public static void polygonClassAndArea(SpatioTemporalGraph stGraph){

		try{
			
			String file = chooseFile("area vs polygonal number");
			
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

	public static void frameAndArea(SpatioTemporalGraph stGraph) {
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
	
	public static void custom_write_out(
			SpatioTemporalGraph stGraph){
			//later add more controlled output, i.e. from UI
			//with various boolean switches (e.g. only dividing ecc..)
		try{
			
			
			String file = chooseFile("Custom data");
			
			FileWriter fstream = new FileWriter(file);
			BufferedWriter out = new BufferedWriter(fstream);
			
//			out.write("cellID, area\n");
			
//			for(int i=0; i<stGraph.size(); i++){
				
				FrameGraph frame_i = stGraph.getFrame(0);
				Iterator<Node> cell_it = frame_i.iterator();
				
//				while(cell_it.hasNext()){
//	
//					Node cell = cell_it.next();
//					//only dividing cells
//					if(!cell.hasObservedDivision())
//						continue;
//					
////					//only mother
////					if(!cell.getDivision().isMother(cell))
////						continue;
//					
//					//skip boundary cell
//					if(cell.onBoundary())
//						continue;
//						
//					//record area of dividing cell
//					//out.write(cell.getTrackID())
//					
//					while(cell.hasNext()){
//						out.write(cell.getGeometry().getArea()+",");
//						cell = cell.getNext();
//					}
//
//					//one cell per line
//					out.write("\n");
//
//				}
				
				
				//
				//no of divisions
				int division_no = 0;
				int division_no_W = 0;
				while(cell_it.hasNext()){
					Node cell = cell_it.next();
					
					if(cell.hasObservedDivision()){
						division_no++;


						for(Node neighbor: cell.getNeighbors())
							if(neighbor.hasObservedDivision()){
								division_no_W++;
								break;
							}

					}
				}
				
				//sample value 
				double p_dividing_neighbor = division_no_W / (double)division_no;
				
				out.write(p_dividing_neighbor+"\n");
				
				//random sampler
				Node[] cells = frame_i.vertexSet().toArray(new Node[frame_i.size()]);

				int sim_no = 1000;
				
				for(int sim_i=0; sim_i<sim_no; sim_i++){
					
					//no of random cells that have a dividing neighbor
					int rnd_division_no_W = 0;
					//take as many random cells as there were dividing cells
					for(int i=0; i<division_no; i++){
						
						Node rnd_cell = cells[Math.round((float)Math.random()*cells.length)];
						
						for(Node neighbor: rnd_cell.getNeighbors())
							if(neighbor.hasObservedDivision()){
								rnd_division_no_W++;
								break;
							}
							
					}
					
					double rnd_p_dividing_neighbor = rnd_division_no_W / (double)division_no;
					
					out.write(rnd_p_dividing_neighbor+"\n");
					
				}
				//write out

//			}

			out.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
	}
	
	/**
	 * @param builder_main
	 * @param output_file
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
