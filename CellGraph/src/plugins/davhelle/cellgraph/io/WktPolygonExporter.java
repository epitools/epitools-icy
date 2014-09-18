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

	/**
	 * @param wing_disc_movie 
	 * 
	 */
	public WktPolygonExporter(SpatioTemporalGraph stGraph,String frame_file_name) {
		
		WKTWriter writer = new WKTWriter(2);
		
		int frame_no_to_export = 0;
		File frame_file = new File(frame_file_name);
		FrameGraph frame = stGraph.getFrame(frame_no_to_export);
		
		
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
