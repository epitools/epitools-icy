package plugins.davhelle.cellgraph;

import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.viewer.Viewer;
import icy.main.Icy;
import icy.sequence.Sequence;

import java.util.ArrayList;

import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.overlays.SkeletonModifier;

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
	
	private Sequence inputSequence;
	
	private EzVarSequence				varInputSeq;
	private EzVarSequence				varOutputSeq;
	private EzVarBoolean				varSaveChanges;
	private EzVarBoolean				varSync;
	
	private SkeletonModifier			modificationOverlay;
	
	@Override
	protected void initialize() {
		
		this.getUI().setRunButtonText("Start Editing");
		this.getUI().setParametersIOVisible(false);
		
		modificationOverlay = null;
		inputSequence = null;
		
		varInputSeq = new EzVarSequence("Input sequence");
		varOutputSeq = new EzVarSequence("Output sequence");
		
		varSaveChanges = new EzVarBoolean("Save changes permanently to Output", false );
		varSync = new EzVarBoolean("Sync [Input] and [Output] viewers",false);
		
		EzLabel description = new EzLabel(
				"CellEditor allows to edit skeletons in tiff format by\n" +
				"drawing on a reference image [Input] and applies \n" +
				"automatically changes to the skeleton [Output]:\n\n"+
				"INSTRUCTIONS:\n"+
				"1. Set [Input] to the image sequence to draw on\n" +
				"2. Set [Output] to the skeleton sequence to edit\n"+
				"3. Run [>] to start the modification Overlay on [Input]\n\n"+
				"   a. Click on [Input] to paint a new junction (white)\n" +
				"   b. Press [Space-Bar] to toggle to remove (black)\n"+
				"   c. Press [CTRL]+[Z] to undo (+[SHIFT] to redo)\n"+
				"   d. Always modify only one frame at a time\n"+
				"   e. Press [Enter] to apply changes on [Output]\n\n" +
				"4. Close this plugin to remove the painter from [Input]\n\n"+
				"PLEASE NOTE:\n" +
				"* Before selecting [Save changes] do a test run\n"+
				"* Backup your original skeletons before editing\n"+
				"* Only 8-bit binary TIFFs can be edited currently\n"+
				"* Use [Sync] to syncronize [input]&[output] viewer\n"+
				"* Use CorrectionOverlay from CellOverlay to visualize\n" +
				"  potential segmentation errors.\n");
		
		EzGroup descriptionGroup = new EzGroup("Plugin Description",description);
		
		super.addEzComponent(descriptionGroup);
		super.addEzComponent(varInputSeq);
		super.addEzComponent(varOutputSeq);
		super.addEzComponent(varSaveChanges);
		super.addEzComponent(varSync);
	}

	@Override
	protected void execute() {
		
		if(inputSequence != null){
			new AnnounceFrame(
					String.format("CellEditor is already running on %s. To rerun please close this plugin first",
							inputSequence.getName()),5);
			return;
		}
		
		if(varInputSeq.getValue() == null || varOutputSeq.getValue() == null){
			new AnnounceFrame("Input and Output must be specified!",5);
			return;
		}
		
		
		if(varInputSeq.getValue().equals(varOutputSeq.getValue())){
			new AnnounceFrame("Input and Output sequence must be different!",5);
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

		if(varSync.getValue()){
			input_viewer.getCanvas().setSyncId(1);
			output_viewer.getCanvas().setSyncId(1);
			//return;
		}
		
		inputSequence = varInputSeq.getValue();
		Sequence outputSequence = varOutputSeq.getValue();
		modificationOverlay = new SkeletonModifier(outputSequence,varSaveChanges.getValue());
		inputSequence.addOverlay(modificationOverlay);
		
		
	}

	//Future Idea for substituting a frame live
//	private void substituteFrame(int time_point, String substitute_file_name) {
//		//if swimming pool object is present also apply changes to graph
//		if(Icy.getMainInterface().getSwimmingPool().hasObjects("stGraph", true))
//			for ( SwimmingObject swimmingObject : 
//				
//				Icy.getMainInterface().getSwimmingPool().getObjects(
//						"stGraph", true) ){
//
//				if ( swimmingObject.getObject() instanceof SpatioTemporalGraph ){
//
//					long startTime = System.currentTimeMillis();
//					
//					SpatioTemporalGraph wing_disc_movie = (SpatioTemporalGraph) swimmingObject.getObject();	
//
//					System.out.println("Rereading frame "+time_point);
//					
//					//Read and substitute the frame
//					FrameGraph substitution_frame = frame_generator.generateFrame(time_point, substitute_file_name); 
//					wing_disc_movie.setFrame(substitution_frame, time_point);
//					
//					//Apply default conditions from CellGraph plugin 
//					//TODO make flexible!
//					BorderCells borderUpdater = new BorderCells(wing_disc_movie);
//					borderUpdater.removeOneBoundaryLayerFromFrame(time_point);
//					if(wing_disc_movie.hasTracking() && time_point == 0)
//						borderUpdater.removeOneBoundaryLayerFromFrame(0);
//					
//					new SmallCellRemover(wing_disc_movie).removeCellsOnFrame(time_point, 10.0);
//					
//					if(wing_disc_movie.hasTracking()){
//						
//						//TODO need to reset tracking 
//						TrackingAlgorithm tracker = new NearestNeighborTracking(
//								wing_disc_movie, 
//								5,
//								1,
//								1);
//						
//						tracker.track();
//						
//					}
//					
//					long endTime = System.currentTimeMillis();
//					System.out.println("Completed substitution in " + (endTime - startTime) + " milliseconds");
//				}
//			}
//	}
	
	@Override
	public void clean() {
		//System.out.println(System.currentTimeMillis());
		
		if(modificationOverlay != null){
			if(varInputSeq.getValue() != null){
				Sequence input = varInputSeq.getValue();
				
				if(input.hasOverlay(SkeletonModifier.class))
					input.removeOverlay(modificationOverlay);
			}
		}
		
	}

}
