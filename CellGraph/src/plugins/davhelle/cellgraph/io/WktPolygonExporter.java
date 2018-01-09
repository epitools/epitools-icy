package plugins.davhelle.cellgraph.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Class to save the polygons contained in the stGraph as Well Known Text geometries
 * for faster re-load using the WKT Polygon Importer as Polygon Reader.
 * 
 * @author Davide Heller
 *
 */
public class WktPolygonExporter {
	
	/**
	 * Description for Exporter Plugin
	 */
	public static final String DESCRIPTION = 
			"Export the loaded polygon geometries as Well-Known text(WKT).<br/>" +
			" The WKT format accelerates significantly the loading process<br/>" +
			" when choosen in CellGraph.<br/><br/>" +
			"To know more about this format visit:<br/>" +
			"en.wikipedia.org/wiki/Well-known_text";
	
	/**
	 * JTS Well Known Text writer 
	 */
	private WKTWriter writer;
	
	public WktPolygonExporter(){
		writer = new WKTWriter(2);
	}

	/**
	 * Exports all frames in the stGraph as WKT files
	 * 
	 * @param stGraph graph to export
	 * @param frame_file_name output path (folder)
	 */
	public WktPolygonExporter(SpatioTemporalGraph stGraph,String frame_file_name) {
		this();
		
		int frame_no_to_export = 0;
		FrameGraph frame = stGraph.getFrame(frame_no_to_export);
		
		exportFrame(frame, frame_file_name);
		
	}
	
	/**
	 * Transforms single JTS geometry to WKT and saves it at the output path
	 * 
	 * Used for boundary ring export
	 * 
	 * @param geometry single JTS geometry to export
	 * @param output_name output path
	 */
	public void export(Geometry geometry, String output_name){

		File frame_file = new File(output_name);

		try {

			frame_file.createNewFile();

			FileWriter fw = new FileWriter(frame_file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);

			String node_string = writer.write(geometry);
			if(node_string.length() > 0){
				bw.write(node_string);
				bw.newLine();
			}

			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Something went wrong while attempting to write "+output_name);
		}

	}
	
	/**
	 * Exports all frames in the graph to default location
	 * 
	 * @param stGraph
	 */
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

	/**
	 * Export individual frameGraph as WKT file
	 * 
	 * @param frame frame to export
	 * @param frame_file_name output path (folder)
	 */
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
