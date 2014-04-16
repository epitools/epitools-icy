package plugins.davhelle.cellgraph.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import plugins.davhelle.cellgraph.graphexport.ExportFieldType;
import plugins.davhelle.cellgraph.graphexport.VertexLabelProvider;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

public class CsvTrackWriter {
	
	SpatioTemporalGraph wing_disc_movie;

	public CsvTrackWriter(SpatioTemporalGraph wing_disc_movie) {
		 this.wing_disc_movie = wing_disc_movie;
	}
	
	public void writeTrackingIds(String output_directory){
		
		for(int i=0; i < wing_disc_movie.size(); i++){
			
			FrameGraph frame = wing_disc_movie.getFrame(i);
			String file_name = output_directory + String.format("tracking_t%03d.csv",i);
			File output_file = new File(file_name);
			write(frame,output_file,ExportFieldType.TRACKING_POSITION);
		}
		
	}
	
	private void write(FrameGraph frame, File output_file, ExportFieldType export_information){
		VertexLabelProvider tracking_information_provider = new VertexLabelProvider(export_information);
		
		try {

			// if file doesn't exists, then create it
			if (!output_file.exists()) {
				output_file.createNewFile();
			}
			
			FileWriter fw = new FileWriter(output_file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(Node cell: frame.vertexSet()){
				bw.write(tracking_information_provider.getVertexName(cell));
				bw.newLine();
			}
			
			bw.close();
 
		} catch (IOException e) {
			e.printStackTrace();
		}			
	}
}
