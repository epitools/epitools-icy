package plugins.davhelle.cellgraph;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.file.Saver;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.painter.Painter;
import icy.plugin.PluginDescriptor;
import icy.plugin.PluginLauncher;
import icy.plugin.PluginLoader;
import icy.plugin.abstract_.Plugin;
import icy.sequence.Sequence;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import loci.formats.FormatException;

import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.io.FileNameGenerator;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.SegmentationProgram;
import plugins.tprovoost.painting.Painting;

/**
 * Tool to apply manual modifications to skeleton files
 * use white to add and black to 
 * 
 * TODO apply white and black default!
 * 
 * 
 * @author Davide Heller
 *
 */
public class CellEditor extends EzPlug{
	
	EzVarSequence				varInputSeq;
	EzVarSequence				varOutputSeq;
	EzVarEnum<SegmentationProgram>  varTool;
	EzVarBoolean				varSaveChanges;
	
	@Override
	protected void initialize() {
		
		//Open Painting plugin
		//TODO check that it is available
		String class_name = "plugins.tprovoost.painting.Painting";
		//TODO possibly set preferences of colors and widith in advance?
		Plugin painting_plugin = PluginLauncher.start(class_name);
		
		varInputSeq = new EzVarSequence("Input sequence");
		varOutputSeq = new EzVarSequence("Output sequence");
		varTool = new EzVarEnum<SegmentationProgram>(
				"Seg.Tool used",SegmentationProgram.values(), 
				SegmentationProgram.MatlabLabelOutlines);
		varSaveChanges = new EzVarBoolean("Save changes", false );
		
		super.addEzComponent(varInputSeq);
		super.addEzComponent(varOutputSeq);
		super.addEzComponent(varTool);
		super.addEzComponent(varSaveChanges);
	}

	@Override
	protected void execute() {
		
		ArrayList<Viewer> input_viewers = Icy.getMainInterface().getViewers(varInputSeq.getValue());
		ArrayList<Viewer> output_viewers = Icy.getMainInterface().getViewers(varOutputSeq.getValue());

		if(input_viewers.size() < 1 || output_viewers.size() < 1){
			System.out.println("No viewers attached to input/output sequence, abort");
			return;
		}

		Viewer input_viewer = input_viewers.get(0);
		Viewer output_viewer = output_viewers.get(0);
		
		Painter modifications = extractPaintingOverlay(input_viewer);
		if(modifications == null){
			System.out.println("No Painting overlay found");
			return;
		}

		applyModifications(modifications, output_viewer);
		removeModifications(modifications, input_viewer);
	}

	/**
	 * Remove modifications form viewer/sequence
	 * 
	 * @param modifications
	 * @param viewer
	 */
	private void removeModifications(Painter modifications, Viewer viewer) {
		Sequence seq = viewer.getSequence();
		seq.removePainter(modifications);
		seq.painterChanged(modifications);
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
		
		int skleton_image_type;
		switch(varTool.getValue()){
		case MatlabLabelOutlines:
			//yet to find a working solution! Also this fails
			skleton_image_type = BufferedImage.TYPE_BYTE_GRAY;
			break;
		case PackingAnalyzer:
			skleton_image_type = BufferedImage.TYPE_BYTE_GRAY;
			break;
		case SeedWater:
			skleton_image_type = BufferedImage.TYPE_BYTE_GRAY;
			break;
		default:
			skleton_image_type = BufferedImage.TYPE_BYTE_GRAY;
			break;
		}

		//TODO check TYPE_BYTE_GRAY suitability
		BufferedImage imgBuff = IcyBufferedImageUtil.toBufferedImage(img, new BufferedImage(img.getWidth(),
				img.getHeight(), skleton_image_type));

		Graphics2D g2d = imgBuff.createGraphics();

		//Apply painting to real canvas
		modifications.paint(g2d, varOutputSeq.getValue(), output_canvas);

		//close graphics devices and update sequence
		g2d.dispose();

		img = IcyBufferedImage.createFrom(imgBuff);
		output_canvas.getCurrentImage().setDataXY(0, img.getDataXY(0));

		output_canvas.getSequence().dataChanged();
		
		if(varSaveChanges.getValue()){
			String file_name = varOutputSeq.getValue().getFilename();
			File skeleton_file = new File(file_name);
			FileNameGenerator skeleton_file_name_generator = 
					new FileNameGenerator(
							skeleton_file,
							InputType.SKELETON,
							true, 
							SegmentationProgram.SeedWater);
			
			int current_time_point = output_viewer.getPositionT();
			String current_file_name = skeleton_file_name_generator.getFileName(current_time_point);
			
			System.out.println("Saving changes to:"+current_file_name);
			try {
				
				//check existence!
				File current_file = new File(current_file_name);
				if(!current_file.exists())
					throw new IOException("File doesn't exists");
				
				//attempt saving
				Saver.saveImage(img, current_file, true);
				
			} catch (FormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

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
