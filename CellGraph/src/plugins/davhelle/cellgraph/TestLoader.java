/**
 * 
 */
package plugins.davhelle.cellgraph;

import icy.file.Loader;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;

import java.io.File;
import java.util.ArrayList;

import javax.swing.JFileChooser;

import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.io.CsvTrackReader;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.WktPolygonImporter;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.painters.TrackIdOverlay;
import plugins.davhelle.cellgraph.painters.TrackingOverlay;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Test class to automatically load a test image and the connected graph
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
		
		String test_file_name = test_folder+"/skeletons.tif";
		File test_file = new File(test_file_name);
		File test_file_wkt = new File(test_folder+"/skeleton_000.wkt");
		
		if(!test_file.exists()){
			System.out.printf("Test file not available on this folder:\n%s\n",test_file.getAbsolutePath());
			return;
		}
		
		Sequence sequence = Loader.loadSequence(test_file_name, 0, false);
		Icy.getMainInterface().addSequence(sequence);
		
		SpatioTemporalGraph test_stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						test_file_wkt, 
						10, InputType.WKT).getStGraph();
	
		
		
		//load border
		WktPolygonImporter wkt_importer = new WktPolygonImporter();
		BorderCells border = new BorderCells(test_stGraph);
		for(int i=0; i<test_stGraph.size();i++){
			String border_file_name = String.format("%s/border_%03d.wkt",test_folder,i);
			ArrayList<Geometry> boundaries = wkt_importer.extractGeometries(border_file_name);
			border.markBorderCells(test_stGraph.getFrame(i), boundaries.get(0));
		}
		
		//track cells
		new CsvTrackReader(test_stGraph, test_folder).track();
		
		sequence.addOverlay(new TrackIdOverlay(test_stGraph));
		sequence.addOverlay(new TrackingOverlay(test_stGraph, true));
		
		//Push to swimming pool TODO: ONLY REMOVE stGraphs
		Icy.getMainInterface().getSwimmingPool().removeAll();
		SwimmingObject swimmingObject = new SwimmingObject(test_stGraph,"stGraph");
		Icy.getMainInterface().getSwimmingPool().add( swimmingObject );
		
	}

}
