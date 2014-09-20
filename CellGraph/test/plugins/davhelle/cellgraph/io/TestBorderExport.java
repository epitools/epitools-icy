package plugins.davhelle.cellgraph.io;

import java.io.File;
import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.misc.BorderCells;

import com.vividsolutions.jts.geom.Geometry;

public class TestBorderExport {
  
	
	@Test
	public void testBorderExport() {

		SpatioTemporalGraph stGraph = loadTestStGraph();

		long startTime = System.currentTimeMillis();
		Geometry boundaries[] = new BorderCells(stGraph).markOnly();
		long boundaryTime = System.currentTimeMillis() - startTime;
		System.out.printf("Identifying the border took: %d ms\n",boundaryTime);
		
		//Test ouptput folder
		String test_folder = "/Users/davide/tmp/wkt_export/";
		
		WktPolygonExporter exporter = new WktPolygonExporter();
		for(int i=0; i<stGraph.size(); i++){
			String expected_wkt_file = String.format("%sboundary%d.wkt",test_folder,i);
			
			exporter.export(boundaries[i], expected_wkt_file);
			
			Assert.assertTrue(new File(expected_wkt_file).exists());
		}
	}


	public SpatioTemporalGraph loadTestStGraph() {
		File test_file = new File("/Users/davide/data/neo/1/crop/skeletons_crop_t28-68_t0000.tif");
		int test_file_no = 4;
			
		SpatioTemporalGraph stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						test_file, 
						test_file_no).getStGraph();
		return stGraph;
	}
	
	
	@Test
	public void testBorderImport(){
		SpatioTemporalGraph stGraph = loadTestStGraph();
		
		//Test ouptput folder
		String test_folder = "/Users/davide/tmp/wkt_export/";
		
		WktPolygonImporter importer = new WktPolygonImporter();
		BorderCells border = new BorderCells(stGraph);
		
		long startTime = System.currentTimeMillis();
		
		for(int i=0; i<stGraph.size(); i++){
			
			String expected_wkt_file = String.format("%sboundary%d.wkt",test_folder,i);
			Assert.assertTrue(new File(expected_wkt_file).exists());
			
			ArrayList<Geometry> boundaries = importer.extractGeometries(expected_wkt_file);
			Assert.assertEquals(boundaries.size(), 1);
			
			FrameGraph frame = stGraph.getFrame(i);
			border.markBorderCells(frame, boundaries.get(0));
			
		}
		
		long boundaryTime = System.currentTimeMillis() - startTime;		
		System.out.printf("Loading the border took: %d ms\n",boundaryTime);
	}
	
}
