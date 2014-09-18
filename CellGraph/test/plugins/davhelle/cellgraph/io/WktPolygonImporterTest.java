package plugins.davhelle.cellgraph.io;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

import plugins.davhelle.cellgraph.graphs.FrameGenerator;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.TissueEvolution;

public class WktPolygonImporterTest {
	  @Test
	  public void testSimpleWTKinput() {
		  
		  	String test_file_name = "/Users/davide/tmp/wkt_export/output.txt";
		  	File test_file = new File(test_file_name);
			
			Assert.assertTrue(test_file.exists(),"Input File does not exist");
			
			FrameGenerator frame_generator = new FrameGenerator(
					InputType.WKT,
					false, 
					null);
			
			SpatioTemporalGraph stGraph = new TissueEvolution();
			
			FrameGraph frame = frame_generator.generateFrame(0, test_file_name);
			
			int no_of_expected_cells = 190;
			Assert.assertEquals(frame.size(), no_of_expected_cells);
			
			stGraph.setFrame(frame,0);
			
			Assert.assertEquals(stGraph.size(), 1);
			
	  }
}
