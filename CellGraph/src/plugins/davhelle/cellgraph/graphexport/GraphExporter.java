package plugins.davhelle.cellgraph.graphexport;

import icy.gui.frame.progress.AnnounceFrame;

import java.io.FileWriter;

import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.IntegerEdgeNameProvider;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;

public class GraphExporter {
	
	GraphMLExporter<Node, DefaultWeightedEdge> graphML_exporter;
	
	public GraphExporter(ExportFieldType export_field) {
		VertexIdProvider vertex_id_provider = new VertexIdProvider();
		VertexLabelProvider vertex_label_provider = new VertexLabelProvider(export_field);
		EdgeNameProvider<DefaultWeightedEdge> edge_id_provider = new IntegerEdgeNameProvider<DefaultWeightedEdge>();
		EdgeNameProvider<DefaultWeightedEdge> edge_label_provider = null;
		
		graphML_exporter = new GraphMLExporter<Node, DefaultWeightedEdge>(
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
		
		System.out.println("Successfully exported frame0 to: "+file_name);
	}
	
}
