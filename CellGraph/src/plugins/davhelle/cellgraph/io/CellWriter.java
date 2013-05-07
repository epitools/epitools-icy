/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import com.vividsolutions.jts.geom.Coordinate;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;


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
/**
 * @author davide
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
	
	public void write_tracking_file(SpatioTemporalGraph stGraph){

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
