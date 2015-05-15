package headless;

import java.io.File;
import java.util.Arrays;

import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.io.CsvTrackReader;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.misc.SmallCellRemover;
import plugins.davhelle.cellgraph.tracking.HungarianTracking;

/**
 * Static helper methods for the creation of a spatio-temporal graph
 * through the input of skeleton images
 * 
 * @author Davide Heller
 *
 */
public class StGraphUtils {
	
	/**
	 * Creates a spatio-temporal graph with the following default settings:<br>
	 * - TissueEvolution <br>
	 * - No Border cut <br>
	 * - Small Cells removed if below 10px area <br>
	 * - HungarianTracking algorithm
	 * 
	 * @param test_file first skeleton file to be read
	 * @param no_of_test_files number of time points to be read (sequential file name pattern required)
	 * @return spatio-temporal graph of the input files
	 */
	public static SpatioTemporalGraph createDefaultGraph(
			File test_file, int no_of_test_files) {
		System.out.println("Creating graph..");
		SpatioTemporalGraph stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						test_file, 
						no_of_test_files).getStGraph();

		assert stGraph.size() == no_of_test_files: "wrong frame no";

		System.out.println("Identifying the border..");
		new BorderCells(stGraph).markOnly();
		
		System.out.println("Removing small cells..");
		new SmallCellRemover(stGraph).removeCellsBelow(10.0);

		System.out.println("Tracking cells..");
		new HungarianTracking(stGraph, 5, 5.0,1.0).track();
		return stGraph;
	}
	
	/**
	 * Specific setting to load the neo samples
	 * 
	 * @param i series id
	 * @return spatio-temporal graph of the input files
	 */
	public static SpatioTemporalGraph loadNeo(int i){
		
		String sample_folder = String.format("/Users/davide/data/neo/%d/",i);
		File skeleton_folder = new File(sample_folder+"skeletons");
		File tracking_folder = new File(sample_folder+"tracking");

		assert skeleton_folder.isDirectory(): "Skeleton Input is not a directory";
		assert tracking_folder.isDirectory(): "Tracking Input is not a directory";
		
		File[] skeletons = skeleton_folder.listFiles();
		Arrays.sort(skeletons);
		
		System.out.println("First skeleton:"+skeletons[0].getAbsolutePath());
				
		System.out.println("Creating graph..");
		SpatioTemporalGraph stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						skeletons[0], 
						skeletons.length).getStGraph();

		assert stGraph.size() == skeletons.length: "wrong frame no";

		System.out.println("Identifying the border..");
		BorderCells border_generator = new BorderCells(stGraph);
		border_generator.removeOneBoundaryLayerFromAllFrames();
		border_generator.removeOneBoundaryLayerFromFrame(0);
		
		System.out.println("Removing small cells..");
		new SmallCellRemover(stGraph).removeCellsBelow(10.0);

		System.out.println("Tracking cells..");
		
		new CsvTrackReader(stGraph, tracking_folder.getAbsolutePath()).track();
		return stGraph;
	}
	
	/**
	 * Load neo samples without tracking information
	 * 
	 * @param i
	 * @return spatio-temporal graph of the input files
	 */
	public static SpatioTemporalGraph loadNeoWoTracking(int i){
		
		String sample_folder = String.format("/Users/davide/data/neo/%d/",i);
		File skeleton_folder = new File(sample_folder+"skeletons");

		assert skeleton_folder.isDirectory(): "Skeleton Input is not a directory";
		
		File[] skeletons = skeleton_folder.listFiles();
		Arrays.sort(skeletons);
		
		System.out.println("First skeleton:"+skeletons[0].getAbsolutePath());
				
		System.out.println("Creating graph..");
		SpatioTemporalGraph stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						skeletons[0], 
						skeletons.length).getStGraph();

		assert stGraph.size() == skeletons.length: "wrong frame no";

		return stGraph;
	}
	
	
}
