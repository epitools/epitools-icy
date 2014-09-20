package headless;

import java.io.File;
import java.util.ArrayList;

import org.testng.Assert;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.WktPolygonImporter;
import plugins.davhelle.cellgraph.misc.BorderCells;

import com.vividsolutions.jts.geom.Geometry;

public class LoadNeoWtkFiles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		long startTime = System.currentTimeMillis();

		String export_folder = "/Users/davide/tmp/neo0_wkt/";
		
		//Generating Neo0 stGraph
		System.out.println("Creating graph..");
		SpatioTemporalGraph stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						new File(String.format("%sPolygons_000.wtk",export_folder)), 
						20, InputType.WKT).getStGraph();
		
		WktPolygonImporter wkt_importer = new WktPolygonImporter();
		BorderCells border = new BorderCells(stGraph);

		for(int i=0; i < stGraph.size(); i++){
			String expected_wkt_file = String.format("%sBorder_%d.wtk",export_folder,i);
			Assert.assertTrue(new File(expected_wkt_file).exists());
			
			ArrayList<Geometry> boundaries = wkt_importer.extractGeometries(expected_wkt_file);
			Assert.assertEquals(boundaries.size(), 1);
			
			FrameGraph frame = stGraph.getFrame(i);
			border.markBorderCells(frame, boundaries.get(0));
		}
		
		long loadTime = System.currentTimeMillis() - startTime;		
		System.out.printf("Loading Neo0 in wkt took:\t%d ms\n",loadTime);
		
	}

}
