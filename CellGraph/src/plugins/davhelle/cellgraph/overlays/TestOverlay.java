package plugins.davhelle.cellgraph.overlays;

import icy.gui.frame.progress.AnnounceFrame;
import icy.sequence.Sequence;

import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;

/**
 * Test Overlay to try out new ideas
 * 
 * @author Davide Heller
 *
 */
public class TestOverlay extends StGraphOverlay {
	
	public static final String DESCRIPTION = 
			"Test overlay to try out new ideas. " +
			"Modify cellgraph.overlays.TestOverlay.java" +
			"to work with this";
			
	/**
	 * @param stGraph graph object for which to create the overlay
	 * @param sequence sequence on which the overlay will be added
	 */
	public TestOverlay(SpatioTemporalGraph stGraph, Sequence sequence) {
		super("Test Overlay", stGraph);

		//Default action. Just displays a welcome message
		new AnnounceFrame("Executed cellgraph.overlays.TestOverlay successfully!",5);

	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#paintFrame(java.awt.Graphics2D, plugins.davhelle.cellgraph.graphs.FrameGraph)
	 */
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#specifyLegend(java.awt.Graphics2D, java.awt.geom.Line2D.Double)
	 */
	@Override
	public void specifyLegend(Graphics2D g, Double line) {

	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#writeFrameSheet(jxl.write.WritableSheet, plugins.davhelle.cellgraph.graphs.FrameGraph)
	 */
	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {

	}

}
