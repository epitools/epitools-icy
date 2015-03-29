/**
 * 
 */
package plugins.davhelle.cellgraph;

import headless.LoadNeoWtkFiles;
import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.painters.TrackIdOverlay;
import plugins.davhelle.cellgraph.painters.TrackingOverlay;

/**
 * Loads neo samples using wkt
 * 
 * @author Davide Heller
 *
 */
public class NeoLoader extends EzPlug {

	private EzVarSequence varSequence;
	private EzVarInteger varNeo;

	@Override
	public void clean() {
		
	}

	@Override
	protected void execute() {
		Sequence sequence = varSequence.getValue();
		if(sequence == null){
			new AnnounceFrame("Plugin requires active sequence! Please open an image on which to display results");
			return;
		}
		
		//Load stGraph
		SpatioTemporalGraph wing_disc_movie = LoadNeoWtkFiles.loadNeo(varNeo.getValue());
		
		//Overlay tracking view
		Overlay trackID = new TrackIdOverlay(wing_disc_movie);
		sequence.addOverlay(trackID);
		Overlay correspondence = new TrackingOverlay(wing_disc_movie,true);
		sequence.addOverlay(correspondence);
		
		//Push to swimming pool TODO: ONLY REMOVE stGraphs
		Icy.getMainInterface().getSwimmingPool().removeAll();
		SwimmingObject swimmingObject = new SwimmingObject(wing_disc_movie,"stGraph");
		Icy.getMainInterface().getSwimmingPool().add( swimmingObject );
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
