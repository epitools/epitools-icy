package plugins.davhelle.cellgraph;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;


/**
 * CellWriter generates coordinate files of the found cell centers.
 * These can be used, for example, to track cell centers with the 
 * MOSAIC tracking plugin in FIJI. Or import them as ROI set to 
 * FIJI and modify them with a interactive voronoi tool by 
 * Johannes Schindelin [DelaunayVoronoi plugin].
 * 
 * The import to the latter can be set up with a custom 
 * ImageJ macro:VoronoiMacro.ijm written by DH.
 * 
 * @author Davide Heller
 *
 */
public class CellWriter {
	
	private final String output_file_name;
	private int time_point;
	
	public CellWriter(String output_file_name, int time_point)
	{
		String file_ext = ".txt";
		this.output_file_name = output_file_name + time_point + file_ext;
		this.time_point = time_point;
	}
	
	
	public void write_tracking_file(ArrayList<Point> cell_centers){
		
		try{
			
			// Create file for mosaic particle tracking
			FileWriter fstream = new FileWriter(output_file_name);
			BufferedWriter out = new BufferedWriter(fstream);

			//Start file with frame number
			out.write("frame\t"+time_point+"\n");
			double dummy_z = 0.0;

			//Follow with all particle coordinates (additional parameters might be added afterwards)
			for(Point particle: cell_centers){
				out.write(String.valueOf(particle.x)+"\t");
				out.write(String.valueOf(particle.y)+"\t");

				//TODO change zInfo with the combined z information of all belonging CellCorners
				out.write(String.valueOf(dummy_z)+"\n");
			}

			//Close the output stream
			out.close();
			
			System.out.println("Wrote successfully to:"+output_file_name);
			
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
	}
	
	public void write_area_diff(ArrayList<Double> cell_areas){
		
		try{
			
			// Create file for mosaic particle tracking
			FileWriter fstream = new FileWriter(output_file_name);
			BufferedWriter out = new BufferedWriter(fstream);

			//Follow with all particle coordinates (additional parameters might be added afterwards)
			for(Double area_i: cell_areas){
				out.write(area_i.toString()+"\n");
			}

			//Close the output stream
			out.close();
			
			System.out.println("Wrote successfully to:"+output_file_name);
			
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
	}

}
