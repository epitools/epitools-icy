package headless;

import java.io.File;
import java.util.Arrays;

import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.io.WktPolygonExporter;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.misc.SmallCellRemover;

public class GenerateNeoWtkFiles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
		long startTime = System.currentTimeMillis();
		
		String sample_folder = String.format("/Users/davide/data/neo/%d/",0);
		File skeleton_folder = new File(sample_folder+"skeletons");
		File[] skeletons = skeleton_folder.listFiles();
		Arrays.sort(skeletons);
		
		//Generating Neo0 stGraph
		System.out.println("Creating graph..");
		SpatioTemporalGraph stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						skeletons[1], 
						20).getStGraph();
		
		System.out.println("Identifying the border..");
		BorderCells border_generator = new BorderCells(stGraph);
		border_generator.applyBoundaryCondition();
		border_generator.removeOneBoundaryLayerFromFrame(0);
		Geometry[] boundaries = border_generator.markOnly();
		
		System.out.println("Removing small cells..");
		new SmallCellRemover(stGraph).removeCellsBelow(10.0);
		
		long LoadTime = System.currentTimeMillis() - startTime;		
		System.out.printf("Loading Neo0 took:\t%d ms\n",LoadTime);
		
		//Saving information in Well-Known-Text (WKT) Format
		WktPolygonExporter wkt_exporter = new WktPolygonExporter();
		String export_folder = "/Users/davide/tmp/neo0_wkt/";
		
		for(int i=0; i < stGraph.size(); i++){
			wkt_exporter.export(boundaries[i], String.format("%sBorder_%d.wtk",export_folder,i));
			wkt_exporter.exportFrame(stGraph.getFrame(i), String.format("%sPolygons_%d.wtk",export_folder,i));
		}
		
		long SaveTime = System.currentTimeMillis() - startTime - LoadTime;		
		System.out.printf("Saving Neo0 in wkt took:\t%d ms\n",SaveTime);

	}

}
