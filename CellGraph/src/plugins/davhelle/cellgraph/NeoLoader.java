package plugins.davhelle.cellgraph;

import java.io.File;

import headless.LoadNeoWktFiles;
import icy.file.Loader;
import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import icy.swimmingPool.SwimmingPool;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.overlays.TrackIdOverlay;
import plugins.davhelle.cellgraph.overlays.TrackingOverlay;

/**
 * Helper plugin to quickly load the {@link SpatioTemporalGraph}
 * from the samples in the neo analysis.<br><br>
 *
 * I.e. Loads neo samples using wkt & trackign files
 * 
 * @author Davide Heller
 *
 */
public class NeoLoader extends EzPlug {

	/**
	 * Sequence to display the neo stGraph on 
	 */
	private EzVarSequence varSequence;
	/**
	 * Series number of neo: 0|1|2
	 */
	private EzVarInteger varNeo;

	@Override
	public void clean() {
		
	}

	@Override
	protected void execute() {
		Sequence sequence = varSequence.getValue();
		
		if(sequence == null){
			String test_file_name = String.format(
					"/Users/davide/data/neo/%d/neo%d_RegIm_Clahe_8bit.tif",
					varNeo.getValue(),varNeo.getValue());
			File test_file_path = new File(test_file_name);
			if(test_file_path.exists()){
				sequence = Loader.loadSequence(test_file_name, 0, true);
				Icy.getMainInterface().addSequence(sequence);
			}else{
				new AnnounceFrame("Plugin requires active sequence! Please open an image on which to display results");
				return;
			}
		}
		
		//Load stGraph
		SpatioTemporalGraph wing_disc_movie = LoadNeoWktFiles.loadNeo(varNeo.getValue());
		
		//Overlay tracking view
		Overlay trackID = new TrackIdOverlay(wing_disc_movie);
		sequence.addOverlay(trackID);
		Overlay correspondence = new TrackingOverlay(wing_disc_movie,true);
		sequence.addOverlay(correspondence);
		
		//remove all formerly present stGraph objects 
		SwimmingPool icySP = Icy.getMainInterface().getSwimmingPool();
		for(SwimmingObject swimmingObject: icySP.getObjects())
			if ( swimmingObject.getObject() instanceof SpatioTemporalGraph )
				icySP.remove(swimmingObject);
		
		//Add the neo stGraph
		SwimmingObject swimmingObject = new SwimmingObject(wing_disc_movie,"stGraph");
		icySP.add( swimmingObject );
	}

	@Override
	protected void initialize() {
		
		this.getUI().setRunButtonText("Load Neo");
		this.getUI().setParametersIOVisible(false);
		
		varSequence = new EzVarSequence("Input sequence");
		super.addEzComponent(varSequence);
		
		varNeo = new EzVarInteger("Neo sample to load",0,2,1);
		super.addEzComponent(varNeo);
		
	}

}
