package plugins.davhelle.cellgraph;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.painter.Painter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarSequence;

public class CellEditor extends EzPlug{
	
	EzVarSequence				varInputSeq;
	EzVarSequence				varOutputSeq;
	
	@Override
	protected void initialize() {
		varInputSeq = new EzVarSequence("Input sequence");
		varOutputSeq = new EzVarSequence("Output sequence");
		super.addEzComponent(varInputSeq);
		super.addEzComponent(varOutputSeq);
	}

	@Override
	protected void execute() {
		
		ArrayList<Viewer> input_viewers = Icy.getMainInterface().getViewers(varInputSeq.getValue());
		ArrayList<Viewer> output_viewers = Icy.getMainInterface().getViewers(varOutputSeq.getValue());

		if(input_viewers.size() < 1 || output_viewers.size() < 1){
			System.out.println("No viewers attached to input/output sequence, abort");
			return;
		}

		Painter modifications = extractPaintingOverlay(input_viewers.get(0));

		if(modifications == null){
			System.out.println("No Painting overlay found");
			return;
		}

		applyModifications(modifications, output_viewers.get(0));
		
	}

	/**
	 * Method to permanently apply the painting overlay of the input sequence
	 * to the output sequence.
	 * 
	 * Based on the Painting plugin by
	 * Thomas Provoost, http://icy.bioimageanalysis.org/plugin/Painting
	 * 
	 * specifically the PaintingPainter.onMouseRelease() method
	 * 
	 * The implementing idea is to apply the paint method
	 * of the input overlay directly to the raw data of 
	 * the output data by extracting the needed data objects.
	 * I.e. Graphics device, Sequence and IcyCanvas 
	 * <-> paint(g, sequence, canvas)
	 * 
	 * @param modifications Painter/Overlay of the input sequence to be permanently applied
	 * @param output_viewer Viewer associated with the output sequence (by default the 1st viewer is chosen)
	 */
	private void applyModifications(Painter modifications, Viewer output_viewer) {

		IcyCanvas output_canvas = output_viewer.getCanvas();
		IcyBufferedImage img = output_canvas.getCurrentImage();

		//TODO check TYPE_BYTE_GRAY suitability
		BufferedImage imgBuff = IcyBufferedImageUtil.toBufferedImage(img, new BufferedImage(img.getWidth(),
				img.getHeight(), BufferedImage.TYPE_BYTE_GRAY));

		Graphics2D g2d = imgBuff.createGraphics();

		//Apply painting to real canvas
		modifications.paint(g2d, varOutputSeq.getValue(), output_canvas);

		//close graphics devices and update sequence
		g2d.dispose();

		img = IcyBufferedImage.createFrom(imgBuff);
		output_canvas.getCurrentImage().setDataXY(0, img.getDataXY(0));

		output_canvas.getSequence().dataChanged();

		//TODO the copied painting modification must be separately saved yet.
	}

	/**
	 * Extract the Painting Overlay created by Thomas Provoost
	 * Painting plugin, called "Painting"
	 * 
	 * @param viewer first viewer attached to the input sequence
	 * @return
	 */
	private Painter extractPaintingOverlay(Viewer viewer) {
		
		//Prepare input
		//i.e. get painter
		Viewer first = viewer;
		IcyCanvas first_canvas = first.getCanvas();
		ArrayList<Layer> layer_list = first_canvas.getLayers();
		Painter modifications = null;

		for(Layer l: layer_list){
			//System.out.println("\t"+l.getName());
			if(l.getName().equals("Painting")){
				modifications = l.getPainter();

			}
		}
		return modifications;
	}
	
	@Override
	public void clean() {
		// TODO Auto-generated method stub
		
	}

}
