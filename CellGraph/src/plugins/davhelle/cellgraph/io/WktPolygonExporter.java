/**
 * 
 */
package plugins.davhelle.cellgraph.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.vividsolutions.jts.io.WKTWriter;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * 
 * This class saves the polygons read from the image for faster reload
 * 
 * @author Davide Heller
 *
 */
public class WktPolygonExporter {
	
	private WKTWriter writer;
	
	public WktPolygonExporter(){
		writer = new WKTWriter(2);
	}

	/**
	 * @param wing_disc_movie 
	 * 
	 */
	public WktPolygonExporter(SpatioTemporalGraph stGraph,String frame_file_name) {
		this();
		
		int frame_no_to_export = 0;
		FrameGraph frame = stGraph.getFrame(frame_no_to_export);
		
		exportFrame(frame, frame_file_name);
		
	}
	
	public WktPolygonExporter(SpatioTemporalGraph stGraph){
		this();
		
		String temporary_folder = "/Users/davide/tmp/wkt_export/"; 
		
		for(int i=0; i < stGraph.size(); i++){
			FrameGraph frame = stGraph.getFrame(i);

			String output_name = "";

			if(frame.hasFileSource()){
				File source_file = new File(frame.getFileSource());
				output_name = temporary_folder + source_file.getName() + ".wkt";
			}
			else
				output_name = temporary_folder + i + ".wkt";

			exportFrame(frame, output_name);
		}
	}

	public void exportFrame(FrameGraph frame, String frame_file_name) {		
		
		File frame_file = new File(frame_file_name);
		
		try {
			
			frame_file.createNewFile();
			
			FileWriter fw = new FileWriter(frame_file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(Node cell: frame.vertexSet()){
				String node_string = writer.write(cell.getGeometry());
				if(node_string.length() > 0){
					bw.write(node_string);
					bw.newLine();
				}
			}
			
			bw.close();
 
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Something went wrong while attempting to write: "+frame_file_name);
		}
	}
		
}
