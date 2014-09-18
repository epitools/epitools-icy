package plugins.davhelle.cellgraph.io;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;

public class WktPolygonExporterTest {
  @Test
  public void testSimpleWTKoutput() {
	  
	  	SpatioTemporalGraph stGraph = loadTestGraph();
		String frame_file_name = "/Users/davide/tmp/wkt_export/output.txt";
		
		new WktPolygonExporter(stGraph,frame_file_name);
		
		Assert.assertTrue(new File(frame_file_name).exists(),"Output File does not exist");
  }

  public static SpatioTemporalGraph loadTestGraph() {
	  File test_file = new File("/Users/davide/data/neo/1/crop/skeletons_crop_t28-68_t0000.tif");
	  int no_of_test_files = 1;

	  Assert.assertTrue(test_file.exists(),"Input File does not exist");

	  SpatioTemporalGraphGenerator graphGenerator = 
			  new SpatioTemporalGraphGenerator(
					  GraphType.TISSUE_EVOLUTION,
					  test_file, no_of_test_files);

	  SpatioTemporalGraph stGraph = graphGenerator.getStGraph();
	  return stGraph;
  }
}
