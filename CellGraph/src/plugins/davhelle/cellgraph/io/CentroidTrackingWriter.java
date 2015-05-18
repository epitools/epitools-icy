package plugins.davhelle.cellgraph.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import com.vividsolutions.jts.geom.Coordinate;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;


/**
 * CentroidTrackingWriter generates coordinate files of the found cell centers.
 * These can be used, for example, to track cell centers with the 
 * MOSAIC tracking plugin in FIJI. Or import them as ROI set to 
 * FIJI and modify them with a interactive voronoi tool by 
 * Johannes Schindelin [DelaunayVoronoi plugin].<br><br>
 * 
 * The import to the latter can be set up with a custom written
 * ImageJ macro (VoronoiMacro.ijm) available at:
 * https://www.dropbox.com/sh/deoey8kc1w6wnej/AAAFthxbeX4MXnCKM506utFwa?dl=0
 * 
 * @author Davide Heller
 *
 */
public class CentroidTrackingWriter {
	
	/**
	 * Spatiotemporal graph to be written out
	 */
	private final SpatioTemporalGraph stGraph;
	
	/**
	 * Set up writer
	 * 
	 * @param stGraph graph to write out
	 * @param output_base_name base path for output files
	 */
	public CentroidTrackingWriter(SpatioTemporalGraph stGraph, String output_base_name)
	{
		String file_ext = ".txt";
		this.stGraph = stGraph; 
		
		for(int time_point=0; time_point < stGraph.size(); time_point++){
			String output_file_name = output_base_name + time_point + file_ext;
			write_tracking_file(output_file_name,time_point);
		}
	}
	
	/**
	 * Write out individual frames
	 * 
	 * @param output_file_name output path for frame file
	 * @param time_point time point of the frame to export
	 */
	private void write_tracking_file(String output_file_name, int time_point){

		try{

			// Create file for mosaic particle tracking
			FileWriter fstream = new FileWriter(output_file_name);
			BufferedWriter out = new BufferedWriter(fstream);

			//Start file with frame number
			out.write("frame\t"+time_point+"\n");

			//Extract frame of interest
			FrameGraph graph_i = stGraph.getFrame(time_point);

			//convert graph nodes to coordinates and write to file
			for(Node n: graph_i.vertexSet()){
				//JTS coordinate
				Coordinate centroid = 
						n.getCentroid().getCoordinate();

				out.write(String.valueOf((int)centroid.x)+"\t");
				out.write(String.valueOf((int)centroid.y)+"\t");

				//TODO change zInfo with the combined z information of all belonging CellCorners
				//System.out.println(centroid.z);
				out.write(String.valueOf(0.0)+"\n");
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
