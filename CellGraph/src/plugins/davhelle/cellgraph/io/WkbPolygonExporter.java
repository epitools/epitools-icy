/**
 * 
 */
package plugins.davhelle.cellgraph.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.io.OutputStreamOutStream;
import com.vividsolutions.jts.io.WKBWriter;

/**
 * 
 * Similar to WKT but this version saves binary. Possible SpeedGAIN. 
 * @author Davide Heller
 *
 */
public class WkbPolygonExporter {

	private WKBWriter writer;
	
	/**
	 * 
	 */
	private WkbPolygonExporter() {
		writer = new WKBWriter();
	}

	
	public WkbPolygonExporter(SpatioTemporalGraph stGraph){
		this();
		
		String temporary_folder = "/Users/davide/tmp/wkt_export/"; 
		
		for(int i=0; i < stGraph.size(); i++){
			FrameGraph frame = stGraph.getFrame(i);

			String output_name = temporary_folder;

			if(frame.hasFileSource()){
				File source_file = new File(frame.getFileSource());
				output_name += source_file.getName();
			}
			else
				output_name += Integer.toString(i);
			
			output_name += ".wkb";

			exportFrame(frame, output_name);
		}
	}


	private void exportFrameHEX(FrameGraph frame, String frame_file_name) {

		File frame_file = new File(frame_file_name);
		
		try {
			
			frame_file.createNewFile();
			
			FileWriter fw = new FileWriter(frame_file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(Node cell: frame.vertexSet()){
				
				byte[] geo_bin = writer.write(cell.getGeometry());
				String node_string = WKBWriter.bytesToHex(geo_bin);
				
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
	
	private void exportFrame(FrameGraph frame, String frame_file_name) {

		File frame_file = new File(frame_file_name);
		
		try {

			frame_file.createNewFile();

			FileOutputStream os = new FileOutputStream(frame_file);
			OutputStreamOutStream os_jts = new OutputStreamOutStream(os);

			for(Node cell: frame.vertexSet())
				writer.write(cell.getGeometry(),os_jts);

			os.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Something went wrong while attempting to write: "+frame_file_name);
		}
		
	}

}
