package plugins.davhelle.cellgraph.graphs;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

public class SpatioTemporalGraphGeneratorTest {
  @Test
  public void testSimpleGraphCreation() {
	  
		File test_file = new File("/Users/davide/data/neo/1/crop/skeletons_crop_t28-68_t0000.tif");
		int no_of_test_files = 1;
		
		Assert.assertTrue(test_file.exists(),"Input File does not exist");
		
		SpatioTemporalGraphGenerator graphGenerator = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						test_file, no_of_test_files);
		
		SpatioTemporalGraph stGraph = graphGenerator.getStGraph();
		
		Assert.assertEquals(stGraph.size(), 1,"Frame number is wrong!");
  }
}
