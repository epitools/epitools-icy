package plugins.davhelle.cellgraph;

import icy.canvas.IcyCanvas2D;
import icy.gui.frame.progress.AnnounceFrame;
import icy.sequence.Sequence;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;

/**
 * Class to quickly find the location of a coordinate in the image
 * 
 * @author Davide Heller
 *
 */
public class CellFinder extends EzPlug {

	/**
	 * Sequence to find the coordiante in
	 */
	private EzVarSequence varSequence;
	/**
	 * X coordinate set by the user
	 */
	private EzVarInteger varX;
	/**
	 * Y coordinate set by the user
	 */
	private EzVarInteger varY;
	
	@Override
	protected void initialize() {
		
		this.getUI().setRunButtonText("Center viewer on [X,Y]");
		this.getUI().setParametersIOVisible(false);
		
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
		
		int x = varX.getValue();
		int y = varY.getValue();
		
		IcyCanvas2D canvas2d = (IcyCanvas2D)sequence.getFirstViewer().getCanvas();
		canvas2d.centerOnImage(x, y);
		
	}

	
	@Override
	public void clean() {
	}
	
}
