package plugins.davhelle.cellgraph.io;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FileNameGeneratorTest {
	
  @Test
  public void testMatlabOutputNames() {
	  
	  String default_file = "Neo0_skeleton_001.png";
	  String default_dir = "/Users/davide/Documents/segmentation/Epitools/Neo0/Skeleton/";

	  File testFile = new File(default_dir+default_file);
	  boolean is_direct_input = true;
	  FileNameGenerator testGenerator = new FileNameGenerator(
			  testFile,
			  InputType.SKELETON,
			  is_direct_input,
			  SegmentationProgram.MatlabLabelOutlines);
	  
	  int no_of_files = 100;
	  assertFileExistence(testGenerator, no_of_files);
	  
  }
  
  @Test
  public void testSeedWaterOuput(){
	  
	  String default_file = "Outline_0_000.tif";
	  String default_dir = "/Users/davide/Documents/segmentation/seedwater_analysis/2013_05_17/ManualPmCrop5h/8bit/Outlines/";

	  File testFile = new File(default_dir+default_file);
	  boolean is_direct_input = true;
	  FileNameGenerator testGenerator = new FileNameGenerator(
			  testFile,
			  InputType.SKELETON,
			  is_direct_input,
			  SegmentationProgram.SeedWater);
	  
	  
	  int no_of_files = 40;
	  assertFileExistence(testGenerator, no_of_files);
  }
  
  @Test
  public void testPackingAnalyzerOutput(){
	  
	  String default_file = "MAX_WoGaps000.png";
	  String default_dir = "/Users/davide/Documents/segmentation/packingAnalyzer_files/packing_analyser_gammazero/";

	  File testFile = new File(default_dir+default_file);
	  boolean is_direct_input = true;
	  FileNameGenerator testGenerator = new FileNameGenerator(
			  testFile,
			  InputType.SKELETON,
			  is_direct_input,
			  SegmentationProgram.PackingAnalyzer);
	  
	  
	  int no_of_files = 76;
	  assertFileExistence(testGenerator, no_of_files);
	  
  }
  
  @Test
  private void testVTKInput(){
	  
	  
	  String default_file = "mesh_frame000.vtk";
	  String default_dir = "/Users/davide/Documents/segmentation/vtk_pipeline_trial/trial/";

	  File testFile = new File(default_dir+default_file);
	  boolean is_direct_input = false;
	  FileNameGenerator testGenerator = new FileNameGenerator(
			  testFile,
			  InputType.VTK_MESH,
			  is_direct_input,
			  SegmentationProgram.PackingAnalyzer);
	  
	  
	  int no_of_files = 75;
	  assertFileExistence(testGenerator, no_of_files);
	  
  }

  private void assertFileExistence(FileNameGenerator testGenerator, int no_of_files) {
	  for(int i=0; i<no_of_files; i++){
		  String skeleton_path = testGenerator.getFileName(i);
		  File skeleton_file = new File(skeleton_path);
		  Assert.assertTrue(skeleton_file.exists(), skeleton_path + " does not exist!");
	  }
  }

}
