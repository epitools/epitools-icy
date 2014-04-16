package plugins.davhelle.cellgraph.io;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.TissueEvolution;

public class CsvTrackWriterTest {
  @Test
  public void testEmptyInput() { 	
	  //Input Data
	  TissueEvolution empty_stg = new TissueEvolution(1);
	  empty_stg.addFrame(new FrameGraph());
	  String output_folder = "/Users/davide/tmp/NewFolder/";

	  //Execute Program
	  CsvTrackWriter track_writer = new CsvTrackWriter(empty_stg);
	  track_writer.writeTrackingIds(output_folder);

	  //Verify file structure
	  assertFileExistence(output_folder,"tracking_t000.csv");
  }
  
  private void assertFileExistence(String output_folder, String file_name) {
		  File tracking_file = new File(output_folder + file_name);
		  Assert.assertTrue(tracking_file.exists(), tracking_file.getAbsolutePath() + " does not exist!");
  }
 
}
