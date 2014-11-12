/**
 * 
 */
package plugins.davhelle.cellgraph;

import icy.canvas.IcyCanvas2D;
import icy.gui.frame.progress.AnnounceFrame;
import icy.sequence.Sequence;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;

/**
 * Class to quickly find the location of an event
 * 
 * @author Davide Heller
 *
 */
public class CellFinder extends EzPlug {

	private EzVarSequence varSequence;
	private EzVarInteger varX;
	private EzVarInteger varY;
	
	
	@Override
	protected void initialize() {
		varSequence = new EzVarSequence("Input sequence");
		super.addEzComponent(varSequence);
		
		varX = new EzVarInteger("X location [px]");
		super.addEzComponent(varX);
		varY = new EzVarInteger("Y location [px]");
		super.addEzComponent(varY);
	}

	@Override
	protected void execute() {
		Sequence sequence = varSequence.getValue();
		
		if(sequence == null){
			new AnnounceFrame("Plugin requires active sequence! Please open an image on which to display results");
			return;
		}
		
		IcyCanvas2D canvas2d = (IcyCanvas2D)sequence.getFirstViewer().getCanvas();
		canvas2d.centerOnImage(varX.getValue(), varY.getValue());
		
	}

	
	@Override
	public void clean() {
		// TODO Auto-generated method stub
		
	}
	
	

}
