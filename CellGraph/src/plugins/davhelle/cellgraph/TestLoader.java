/**
 * 
 */
package plugins.davhelle.cellgraph;

import icy.file.Loader;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;

import java.awt.Color;
import java.io.File;

import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.painters.PolygonPainter;

/**
 * Test class to automatically load a test image and the connected graph
 * 
 * @author Davide Heller
 *
 */
public class TestLoader extends PluginActionable {

	@Override
	public void run() {
		
		String test_file_name = "/Users/davide/data/neo/1/crop/skeletons_crop_t28-68_t0000.tif";
		File test_file = new File(test_file_name);
		File test_file_wkt = new File("/Users/davide/data/neo/1/crop_wkt/skeleton_000.wkt");
		if(!test_file.exists()){
			System.out.println("Test file not available on this machine");
			return;
		}
		
		Sequence sequence = Loader.loadSequence(test_file_name, 0, false);
		Icy.getMainInterface().addSequence(sequence);
		
		SpatioTemporalGraph test_stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						test_file_wkt, 
						1, InputType.WKT).getStGraph();
	
		sequence.addOverlay(new PolygonPainter(test_stGraph, Color.red));
		
		//Push to swimming pool TODO: ONLY REMOVE stGraphs
		Icy.getMainInterface().getSwimmingPool().removeAll();
		SwimmingObject swimmingObject = new SwimmingObject(test_stGraph,"stGraph");
		Icy.getMainInterface().getSwimmingPool().add( swimmingObject );
		
	}

}
