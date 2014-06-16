package plugins.davhelle.cellgraph.graphs;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

import plugins.davhelle.cellgraph.graphexport.ExportFieldType;
import plugins.davhelle.cellgraph.graphexport.GraphExporter;

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
		
		int expected_no_of_cells = 190;
		Assert.assertEquals(stGraph.getFrame(0).size(), expected_no_of_cells,"Cell identification incorrect");
		
  }
  
  @Test
  public void testGraphCreationAndExport() {
	  File test_file = new File("/Users/davide/data/neo/1/crop/skeletons_crop_t28-68_t0000.tif");
	  int no_of_test_files = 2;
	  
	  SpatioTemporalGraphGenerator graphGenerator = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						test_file, no_of_test_files);
	  
	  SpatioTemporalGraph stGraph = graphGenerator.getStGraph();
	  
	  GraphExporter exporter = new GraphExporter(ExportFieldType.AREA);
	  for(int frame_no=0;frame_no<stGraph.size();frame_no++){
		  FrameGraph frame_to_export = stGraph.getFrame(frame_no);
		  String output_file = String.format("/Users/davide/tmp/test_frame_%03d.xml",frame_no);
		  exporter.exportFrame(frame_to_export, output_file);
		  
		  Assert.assertTrue(new File(output_file).exists(), 
				  String.format("No GraphML file generated @%s",output_file));
	  }
  }
  
  @Test
  public void testCompleteDataSet(){
	  File test_file = new File("/Users/davide/data/neo/1/neo1_skeletons_tiff/neo1_skeleton_001.tif");
	  int no_of_test_files = 99;
	  
	  SpatioTemporalGraph stGraph = 
			  new SpatioTemporalGraphGenerator(
					  GraphType.TISSUE_EVOLUTION,
					  test_file, 
					  no_of_test_files).getStGraph();
	  
	  Assert.assertEquals(stGraph.size(),no_of_test_files,"Incorrect no of frames");
	  
	  
	  
  }
}
