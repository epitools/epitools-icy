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

		SpatioTemporalGraph stGraph = loadWtkTestGraph();

		int no_of_expected_frames = 1; 
		Assert.assertEquals(stGraph.size(), no_of_expected_frames);

		FrameGraph first_frame = stGraph.getFrame(0);
		int no_of_expected_cells = 190;
		Assert.assertEquals(first_frame.size(), no_of_expected_cells);


	}

	public static SpatioTemporalGraph loadWtkTestGraph() {
		String test_file_name = "/Users/davide/tmp/wkt_export/output.txt";
		File test_file = new File(test_file_name);

		Assert.assertTrue(test_file.exists(),"Input File does not exist");

		FrameGenerator frame_generator = new FrameGenerator(
				InputType.WKT,
				false, 
				null);

		SpatioTemporalGraph stGraph = new TissueEvolution();

		FrameGraph frame = frame_generator.generateFrame(0, test_file_name);
		stGraph.setFrame(frame,0);
		return stGraph;
	}

	@Test
	public void testPerformance(){

		long startTime = System.currentTimeMillis();
		WktPolygonExporterTest.loadTestGraph(1);
		long endTime = System.currentTimeMillis();
		long old_time = endTime - startTime;


		long startTime2 = System.currentTimeMillis();
		WktPolygonImporterTest.loadWtkTestGraph();
		long endTime2 = System.currentTimeMillis();
		long new_time = endTime2 - startTime2;

		Assert.assertTrue(old_time > new_time, "The new method is slower");

		long dt = new_time-old_time;
		System.out.printf("New: %d\nOld: %d\nDifference: %d(%.3f times faster)\n",
				new_time,old_time,dt,old_time/(double)new_time);

	}
	
	@Test
	public void testMultipleFrameInput(){

		int file_no = 10;

		SpatioTemporalGraph stGraph = new TissueEvolution();
		FrameGenerator frame_generator = new FrameGenerator(
				InputType.WKT,
				false, 
				null);

		//Check file correctness
		for(int i=0; i < file_no; i++){
			String expected_wkt_file = String.format("/Users/davide/tmp/wkt_export/skeletons_crop_t28-68_t%04d.tif.wkt",i);
			Assert.assertTrue(new File(expected_wkt_file).exists());

			long startTime = System.currentTimeMillis();
			FrameGraph frame = frame_generator.generateFrame(i, expected_wkt_file);
			long endTime = System.currentTimeMillis();
			long new_time = endTime - startTime;

			stGraph.setFrame(frame,i);
			System.out.printf("Frame %d: Found %d cells in %d milliseconds\n",i,frame.size(),new_time);

		}

		Assert.assertEquals(stGraph.size(), file_no);

	}
}
