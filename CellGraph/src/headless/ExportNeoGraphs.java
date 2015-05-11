package headless;

import plugins.davhelle.cellgraph.export.ExportFieldType;
import plugins.davhelle.cellgraph.export.GraphExporter;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;

/**
 * For every neo sample export for every frame
 * a graphML structure.
 * 
 * @author Davide Heller
 *
 */
public class ExportNeoGraphs {

	public static void main(String[] args) {
		for(int neo_no = 0; neo_no < 3; neo_no++){

			SpatioTemporalGraph stGraph = LoadNeoWktFiles.loadNeo(neo_no);

			for(int i=0; i< stGraph.size(); i++){

				String output_file_name = String.format(
						"/Users/davide/tmp/graphML_export/neo%d/frame%d.xml",
						neo_no,i);
				
				GraphExporter exporter = new GraphExporter(ExportFieldType.COMPLETE_CSV);
				FrameGraph frame_to_export = stGraph.getFrame(i);
				exporter.exportFrame(frame_to_export, output_file_name);
			}
		}
	}
}
