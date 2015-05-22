package plugins.davhelle.cellgraph;

import icy.file.Loader;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.swimmingPool.SwimmingPool;

import java.io.File;

import javax.swing.JFileChooser;

import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.io.CsvTrackReader;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.overlays.TrackIdOverlay;
import plugins.davhelle.cellgraph.overlays.TrackingOverlay;

/**
 * Test plugin to automatically load a test image and the connected graph.
 * The user will be asked to identify the location of the test files.
 * 
 * @author Davide Heller
 *
 */
public class TestLoader extends PluginActionable {

	@Override
	public void run() {

		//Choose location of test folder
		JFileChooser dialog = new JFileChooser();
		dialog.setDialogTitle("Please choose [test folder] location");
		dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		//Only proceed if the user puts in a valid directory
		if(dialog.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return;
		
		final File f = dialog.getSelectedFile();
		System.out.println(f.getAbsolutePath());
		String test_folder = f.getAbsolutePath();
		
		//Image on which the spatio-temporal graph (stGraph) will be generated
		String test_file_name = test_folder+"/skeletons.tif";
		File test_file = new File(test_file_name);
		//First input skeleton for the stGraph object creation
		File test_file_wkt = new File(test_folder+"/skeleton_000.wkt");
		
		if(!test_file.exists()){
			System.out.printf("Test file not available on this folder:\n%s\n",test_file.getAbsolutePath());
			return;
		}
		
		Sequence sequence = Loader.loadSequence(test_file_name, 0, false);
		Icy.getMainInterface().addSequence(sequence);
		
		//create Spatio-temporal graph by using the wkt skeletons in the test folder
		SpatioTemporalGraph test_stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						test_file_wkt, 
						10, InputType.WKT).getStGraph();
		
		//Apply border conditions by reading the wkt border files
		new BorderCells(test_stGraph).markBorderCellsWKT(test_folder);
		
		//Apply tracking by reading the saved tracking information in the CSV files 
		new CsvTrackReader(test_stGraph, test_folder).track();
		
		//Add the tracking overlay to the input image
		sequence.addOverlay(new TrackIdOverlay(test_stGraph));
		sequence.addOverlay(new TrackingOverlay(test_stGraph, true));
		
		//remove all formerly present stGraph objects 
		SwimmingPool icySP = Icy.getMainInterface().getSwimmingPool();
		for(SwimmingObject swimmingObject: icySP.getObjects())
			if ( swimmingObject.getObject() instanceof SpatioTemporalGraph )
				icySP.remove(swimmingObject);
		
		SwimmingObject swimmingObject = new SwimmingObject(test_stGraph,"stGraph");
		icySP.add( swimmingObject );
		
	}

}
