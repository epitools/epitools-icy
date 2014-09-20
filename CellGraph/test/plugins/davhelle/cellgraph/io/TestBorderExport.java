package plugins.davhelle.cellgraph.io;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.misc.BorderCells;

import com.vividsolutions.jts.geom.Geometry;

public class TestBorderExport {
  
	
	@Test
	public void testBorderExport() {

		File test_file = new File("/Users/davide/data/neo/1/crop/skeletons_crop_t28-68_t0000.tif");
		int test_file_no = 4;
			
		SpatioTemporalGraph stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						test_file, 
						test_file_no).getStGraph();

		System.out.println("Identifying the border..");
		Geometry boundaries[] = new BorderCells(stGraph).markOnly();
		
		//Test ouptput folder
		String test_folder = "/Users/davide/tmp/wkt_export/";
		
		WktPolygonExporter exporter = new WktPolygonExporter();
		for(int i=0; i<stGraph.size(); i++){
			
			exporter.export(boundaries[i], i);
			
			String expected_wkt_file = String.format("%sboundary%d.wkt",test_folder,i);
			Assert.assertTrue(new File(expected_wkt_file).exists());
		}

	}
}
