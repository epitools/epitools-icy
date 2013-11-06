package plugins.davhelle.cellgraph;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.file.Saver;
import icy.gui.frame.IcyFrame;
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
import icy.swimmingPool.SwimmingObject;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import loci.formats.FormatException;

import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.graphs.FrameGenerator;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.FileNameGenerator;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.SegmentationProgram;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.misc.SmallCellRemover;
import plugins.davhelle.cellgraph.tracking.NearestNeighborTracking;
import plugins.davhelle.cellgraph.tracking.TrackingAlgorithm;
import plugins.tprovoost.painting.Painting;

/**
 * Tool to apply manual modifications to skeleton files
 * use white to add and black to add or remove membrane.
 * 
 * Requires both the raw image (preferred) and the
 * skeleton file (must) to be open.
 * 
 * current state: EXPERIMENTAL! 
 * WARNING: This plugin can permanently modify your
 * output data. Please make sure to back it up before
 * trying this plugin!
 * 
 * TODO apply white and black default!
 * TODO avoid that painter tip is also applied to img
 * 
 * 
 * @author Davide Heller
 *
 */
public class CellEditor extends EzPlug{
	
	private EzVarSequence				varInputSeq;
	private EzVarSequence				varOutputSeq;
	private EzVarEnum<SegmentationProgram>  varTool;
	private EzVarBoolean				varSaveChanges;
	private EzVarBoolean				varRegenerateGraph;
	private Plugin 						painting_plugin;
	private FrameGenerator				frame_generator;
	
	@Override
	protected void initialize() {
		
		this.frame_generator = new FrameGenerator(
				InputType.SKELETON,
				true,
				SegmentationProgram.SeedWater);
		
		//Open Painting plugin
		//TODO check that it is available
		String class_name = "plugins.tprovoost.painting.Painting";
		painting_plugin = PluginLauncher.start(class_name);
		
		varInputSeq = new EzVarSequence("Input sequence");
		varOutputSeq = new EzVarSequence("Output sequence");
		varTool = new EzVarEnum<SegmentationProgram>(
				"Seg.Tool used",SegmentationProgram.values(), 
				SegmentationProgram.MatlabLabelOutlines);
		varSaveChanges = new EzVarBoolean("Save changes", false );
		varRegenerateGraph = new EzVarBoolean("Regenerate graph (default only!)", false);
		
		super.addEzComponent(varInputSeq);
		super.addEzComponent(varOutputSeq);
		super.addEzComponent(varTool);
		super.addEzComponent(varSaveChanges);
		super.addEzComponent(varRegenerateGraph);
	}

	@Override
	protected void execute() {
		
		if(varInputSeq.getValue().equals(varOutputSeq.getValue())){
			System.out.println("Input and Output sequence should be different!");
			return;
		}
		
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

		IcyBufferedImage img = applyModifications(modifications, output_viewer);
		if(varSaveChanges.getValue()){
			String file_name = saveModifications(output_viewer, img);
		}
		
		removeModifications(modifications, input_viewer);
		
		//restartPainterPlugin();
		
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
	 * Optional method to eventually restart the paiter plugin in order to avoid user 
	 * action gap after the modification layer is removed. Downside is reset of color  
	 * and the painter size which is more manual work than just clicking on the output
	 * image. 
	 * 
	 * TODO save closure
	 * TODO open Painter with specific option (color w/b, painter size 2)
	 * 
	 */
	private void restartPainterPlugin() {
		
		//close current painter
		ArrayList<IcyFrame> all_open_frames = IcyFrame.getAllFrames();
		System.out.println("All open IcyFrames:");
		for(IcyFrame frame: all_open_frames){	
			String frame_name = frame.getTitle();
			//System.out.println("\t "+frame_name);
			if(frame_name.equals("Tools")){
				frame.close();
				break;
			}
		}
		
		//restart painter
		Icy.getMainInterface().unRegisterPlugin(painting_plugin);
		Icy.getMainInterface().setFocusedViewer(varInputSeq.getValue().getFirstViewer());
		String class_name = "plugins.tprovoost.painting.Painting";
		painting_plugin = PluginLauncher.start(class_name);
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
	 * @return 
	 */
	private IcyBufferedImage applyModifications(Painter modifications, Viewer output_viewer) {

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
		
		return img;

		//TODO the copied painting modification must be separately saved yet.
	}

	private String saveModifications(Viewer output_viewer, IcyBufferedImage img) {
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
		
		if(varRegenerateGraph.getValue()){
			substituteFrame(current_time_point, current_file_name);
		}
		
		return current_file_name;
	}

	private void substituteFrame(int time_point, String substitute_file_name) {
		//if swimming pool object is present also apply changes to graph
		if(Icy.getMainInterface().getSwimmingPool().hasObjects("stGraph", true))
			for ( SwimmingObject swimmingObject : 
				
				Icy.getMainInterface().getSwimmingPool().getObjects(
						"stGraph", true) ){

				if ( swimmingObject.getObject() instanceof SpatioTemporalGraph ){

					long startTime = System.currentTimeMillis();
					
					SpatioTemporalGraph wing_disc_movie = (SpatioTemporalGraph) swimmingObject.getObject();	

					System.out.println("Rereading frame "+time_point);
					
					//Read and substitute the frame
					FrameGraph substitution_frame = frame_generator.generateFrame(time_point, substitute_file_name); 
					wing_disc_movie.setFrame(substitution_frame, time_point);
					
					//Apply default conditions from CellGraph plugin 
					//TODO make flexible!
					BorderCells borderUpdater = new BorderCells(wing_disc_movie);
					borderUpdater.applyBoundaryConditionsToFrame(time_point);
					if(wing_disc_movie.hasTracking() && time_point == 0)
						borderUpdater.removeOneBoundaryLayerFromFrame(0);
					
					new SmallCellRemover(wing_disc_movie).removeCellsOnFrame(time_point, 10.0);
					
					if(wing_disc_movie.hasTracking()){
						
						//TODO need to reset tracking 
						TrackingAlgorithm tracker = new NearestNeighborTracking(
								wing_disc_movie, 
								5,
								1,
								1);
						
						tracker.track();
						
					}
					
					long endTime = System.currentTimeMillis();
					System.out.println("Completed substitution in " + (endTime - startTime) + " milliseconds");
				}
			}
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
		List<Layer> layer_list = first_canvas.getLayers();
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
