package plugins.davhelle.cellgraph.overlays;

import icy.gui.frame.progress.AnnounceFrame;
import icy.sequence.Sequence;
import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;

import com.vividsolutions.jts.geom.Point;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Test Overlay to try out new ideas
 * 
 * @author Davide Heller
 *
 */
public class TestOverlay extends StGraphOverlay {
	
	public static final String DESCRIPTION = 
			"Test overlay to try out new ideas.<br/>" +
			"Modify cellgraph.overlays.TestOverlay.java<br/>" +
			"to change the default behavior.";
			
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
		
		//Example how to go through all the cells in FrameGraph
		//and put a green oval at the centroid position.
		
		g.setColor(Color.green);

		for(Node cell: frame_i.vertexSet()){
			
			Point centroid = cell.getCentroid();
			
			int oval_x = (int)centroid.getX();
			int oval_y = (int)centroid.getY();
			int oval_h = 2;
			int oval_w = 2;
			
			g.fillOval(
					oval_x,
					oval_y,
					oval_w,
					oval_h);
		}		
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#specifyLegend(java.awt.Graphics2D, java.awt.geom.Line2D.Double)
	 */
	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		String s = "Test legend";
		Color c = Color.green;
		int offset = 0;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#writeFrameSheet(jxl.write.WritableSheet, plugins.davhelle.cellgraph.graphs.FrameGraph)
	 */
	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame_i) {
		
		//Simple excel sheet output containing the centroid coordinates
		//for every cell in the corresponding frame sheet.
		
		int row_no = 0;
		
		XLSUtil.setCellString(sheet, 0, row_no, "Centroid x");
		XLSUtil.setCellString(sheet, 1, row_no, "Centroid y");
		
		for(Node cell: frame_i.vertexSet()){
			row_no++;
			XLSUtil.setCellNumber(sheet, 0, row_no, cell.getCentroid().getX());
			XLSUtil.setCellNumber(sheet, 1, row_no, cell.getCentroid().getY());
		}
	}

}
