package plugins.davhelle.cellgraph.export;

import icy.gui.frame.progress.AnnounceFrame;

import java.io.FileWriter;

import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.IntegerEdgeNameProvider;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

public class GraphExporter {
	
	public static final String DESCRIPTION = 
			"The loaded graph is saved as a GraphML file. This format is a" +
			" xml based graph format. Bejond the spatial graph structure" +
			" this format includes the following fields in the <vertex label>" +
			" attribute in CSV format:\n\n" +
			"* Cell tracking ID\n" + 
			"* Centroid position x\n" +
			"* Centroid position y\n" +
			"* Cell apical area\n" +
			"* Boundary cell [T/F]\n" +
			"* Observed cell division [T/F]\n" +
			"* Observed cell elimination [T/F]\n" +
			"* Division time\n" +
			"* Elimiation time\n";
	
	GraphMLExporter<Node, Edge> graphML_exporter;
	
	public GraphExporter(ExportFieldType export_field) {
		VertexIdProvider vertex_id_provider = new VertexIdProvider();
		VertexLabelProvider vertex_label_provider = new VertexLabelProvider(export_field);
		EdgeNameProvider<Edge> edge_id_provider = new IntegerEdgeNameProvider<Edge>();
		EdgeNameProvider<Edge> edge_label_provider = null;
		
		graphML_exporter = new GraphMLExporter<Node, Edge>(
				vertex_id_provider, 
				vertex_label_provider, 
				edge_id_provider, 
				edge_label_provider);
	}
	
	public void exportFrame(FrameGraph frame, String file_name){
		try{
			FileWriter writer = new FileWriter(file_name);
			graphML_exporter.export(writer, frame);
			writer.close();
		}
		catch(Exception e){
			new AnnounceFrame("No valid output file:" + file_name);
			return;
		}
		//See whether correct way would to close file
		//with finally statement like in 
		//http://stackoverflow.com/questions/2885173/java-how-to-create-and-write-to-a-file		
		
		System.out.printf("Successfully exported frame %d to: %s\n",frame.getFrameNo(),file_name);
	}
	
}
