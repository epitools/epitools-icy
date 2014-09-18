package plugins.davhelle.cellgraph.io;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;

public class WktPolygonExporterTest {
  @Test
  public void testSimpleWTKoutput() {
	  
	  	SpatioTemporalGraph stGraph = loadTestGraph(1);
		String frame_file_name = "/Users/davide/tmp/wkt_export/output.txt";
		
		new WktPolygonExporter(stGraph,frame_file_name);
		
		Assert.assertTrue(new File(frame_file_name).exists(),"Output File does not exist");
  }

  public static SpatioTemporalGraph loadTestGraph(int no_of_test_files) {
	  File test_file = new File("/Users/davide/data/neo/1/crop/skeletons_crop_t28-68_t0000.tif");

	  Assert.assertTrue(test_file.exists(),"Input File does not exist");

	  SpatioTemporalGraphGenerator graphGenerator = 
			  new SpatioTemporalGraphGenerator(
					  GraphType.TISSUE_EVOLUTION,
					  test_file, no_of_test_files);

	  SpatioTemporalGraph stGraph = graphGenerator.getStGraph();
	  return stGraph;
  }
  
  @Test
  public void testCompleteWriteOut(){
	  
	  SpatioTemporalGraph stGraph = WktPolygonExporterTest.loadTestGraph(10);
	  
	  //Check file correctness
	  for(int i=0; i < stGraph.size(); i++){
			FrameGraph frame = stGraph.getFrame(i);
			String expected_source_file = String.format("/Users/davide/data/neo/1/crop/skeletons_crop_t28-68_t%04d.tif",i); 
			Assert.assertEquals(frame.getFileSource(), expected_source_file);
	  }
	  
	  //Write out all frames at once
	  new WktPolygonExporter(stGraph);
	  
	  //Check file correctness
	  for(int i=0; i < stGraph.size(); i++){
		  String expected_wkt_file = String.format("/Users/davide/tmp/wkt_export/skeletons_crop_t28-68_t%04d.tif.wkt",i);
		  Assert.assertTrue(new File(expected_wkt_file).exists());
	  }
	  
  }
}
