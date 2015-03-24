package plugins.davhelle.cellgraph.io;

import icy.gui.frame.progress.AnnounceFrame;

import java.io.FileWriter;

import plugins.davhelle.cellgraph.export.ExportFieldType;
import plugins.davhelle.cellgraph.export.VertexLabelProvider;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.CellColor;
import plugins.davhelle.cellgraph.nodes.Node;

public class TagSaver {
	
	SpatioTemporalGraph stGraph;
	
	public TagSaver(SpatioTemporalGraph stGraph){
		
		VertexLabelProvider vertexLabelProvider = new
				VertexLabelProvider(ExportFieldType.AREA);
		
		String file_name = "/Users/davide/tmp/tag_output.csv";

		try{
			FileWriter writer = new FileWriter(file_name);
			
			for(CellColor cell_color: CellColor.values()){
				writer.write(cell_color.toString());
				for(Node cell: stGraph.getFrame(0).vertexSet()){
					if(cell.getColorTag() == cell_color.getColor()){
						writer.write(",");
						writer.write(
								vertexLabelProvider.getVertexName(
										cell));
					}
				}
				writer.write("\n");
			}
						
			writer.close();
		}
		catch(Exception e){
			new AnnounceFrame("No valid output file:" + file_name);
			return;
		}
	}
}
