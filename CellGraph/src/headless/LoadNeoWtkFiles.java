package headless;

import java.io.File;
import java.util.ArrayList;

import org.testng.Assert;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.io.CsvTrackReader;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.WktPolygonImporter;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.tracking.NearestNeighborTracking;
import plugins.davhelle.cellgraph.tracking.TrackingAlgorithm;

import com.vividsolutions.jts.geom.Geometry;

public class LoadNeoWtkFiles {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		long startTime = System.currentTimeMillis();

		int neo_no = 0;
		
		int time_points = 3;
		
		SpatioTemporalGraph st_graph = loadStGraph(neo_no, time_points);
		
		long loadTime = System.currentTimeMillis() - startTime;		
		
		TrackingAlgorithm tracker = new NearestNeighborTracking(st_graph, 5, 1, 1);
		tracker.track();
		
		long trackTime = System.currentTimeMillis() - startTime - loadTime;		

		System.out.printf("Loading Neo0 in wkt took:\t%d ms\n",loadTime);
		System.out.printf("Tracking Neo0 took:\t%d ms\n",trackTime);
	}

	/**
	 * Load the only the spatial graph of a neo sample using the wkt format
	 * 
	 * Attention: Path is hard-coded for IMLS-NBM-DAHE (MBP-Retina)
	 * 
	 * @param neo_no
	 * @param time_points
	 * @return 
	 */
	public static SpatioTemporalGraph loadStGraph(int neo_no, int time_points) {
		String export_folder = String.format("/Users/davide/data/neo/%d/skeletons_wkt/",neo_no);
		String input_name = String.format("%sskeleton_000.wkt",export_folder);
		File input_file = new File(input_name);
		
		//Generating Neo0 stGraph
		SpatioTemporalGraph stGraph = loadWktStGraph(time_points,
				export_folder, input_file);
		
		return stGraph;
	}

	/**
	 * @param time_points
	 * @param export_folder
	 * @param input_file
	 * @return
	 */
	public static SpatioTemporalGraph loadWktStGraph(int time_points,
			String export_folder, File input_file) {
		System.out.println("Creating graph..");
		
		SpatioTemporalGraph stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						input_file, 
						time_points, InputType.WKT).getStGraph();
		
		loadBorder(input_file.getParent(), stGraph);
		return stGraph;
	}
	
	public static SpatioTemporalGraph loadMarcm(int no){ 
		int time_points = 60;
		
		String export_folder = String.format("/Users/davide/data/marcm/%d/",no);
		String input_name = "skeletons_wkt/skeleton_000.wkt";
		File input_file = new File(export_folder +input_name);
		
		//Graph generation
		SpatioTemporalGraph stGraph = loadWktStGraph(time_points,
				export_folder, input_file);
		
		//Tracking
		File tracking_folder = new File(export_folder+"tracking");
		new CsvTrackReader(stGraph, tracking_folder.getAbsolutePath()).track();
		
		return stGraph;
		
	}
	
	public static SpatioTemporalGraph loadNeo(int neo_no){
		
		//Graph generation
		int time_points = 100;
		if(neo_no == 1)
			time_points = 99;
		
		String export_folder = String.format("/Users/davide/data/neo/%d/skeletons_wkt/",neo_no);
		String input_name = String.format("%sskeleton_000.wkt",export_folder);
		File input_file = new File(input_name);
		
		SpatioTemporalGraph stGraph = loadWktStGraph(time_points,
				export_folder, input_file);
		
		//Tracking
		String sample_folder = String.format("/Users/davide/data/neo/%d/",neo_no);
		File tracking_folder = new File(sample_folder+"tracking");
		new CsvTrackReader(stGraph, tracking_folder.getAbsolutePath()).track();
		
		return stGraph;
	}

	public static void loadBorder(String export_folder,
			SpatioTemporalGraph stGraph) {
		WktPolygonImporter wkt_importer = new WktPolygonImporter();
		BorderCells border = new BorderCells(stGraph);

		for(int i=0; i < stGraph.size(); i++){
			
			long startTime = System.currentTimeMillis();

			String expected_wkt_file = String.format("%s/border_%03d.wkt",export_folder,i);
			Assert.assertTrue(new File(expected_wkt_file).exists(),String.format("%s does not exist",expected_wkt_file));
			
			ArrayList<Geometry> boundaries = wkt_importer.extractGeometries(expected_wkt_file);
			Assert.assertEquals(boundaries.size(), 1);
			
			FrameGraph frame = stGraph.getFrame(i);
			border.markBorderCells(frame, boundaries.get(0));
			
			long loadTime = System.currentTimeMillis() - startTime;		
			System.out.printf("Boundary %d: loaded in %d ms\n",i,loadTime);

		}
	}

}
