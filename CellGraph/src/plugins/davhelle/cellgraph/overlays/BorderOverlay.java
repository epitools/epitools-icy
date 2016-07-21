package plugins.davhelle.cellgraph.overlays;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Painter to highlight the cells that lie on the boundary in white 
 * 
 * @author Davide Heller
 *
 */
public class BorderOverlay extends StGraphOverlay{

		/**
		 * Descriptor String for GUI use
		 */
		public static final String DESCRIPTION = 
				"Overlay to show where the border of the segmentation <br/>" +
				"was identified";

		/**
		 * @param spatioTemporalGraph graph to be analyzed
		 */
		public BorderOverlay(SpatioTemporalGraph spatioTemporalGraph){
			super("Border cells",spatioTemporalGraph);
		}

		@Override
		public void paintFrame(Graphics2D g, FrameGraph frame_i) {
			
			for(Node cell: frame_i.vertexSet()){
				if(cell.onBoundary()){
					g.setColor(Color.white);
					g.fill(cell.toShape());
				}
			}			
		}

		@Override
		void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
			
			XLSUtil.setCellString(sheet, 0, 0, "Cell id");
			XLSUtil.setCellString(sheet, 1, 0, "On Border");

			int row_no = 1;
			for(Node node: frame.vertexSet()){
				XLSUtil.setCellNumber(sheet, 0, row_no, node.getTrackID());
				
				String booleanString = String.valueOf(node.onBoundary()).toUpperCase();
				XLSUtil.setCellString(sheet, 1, row_no, booleanString);

				row_no++;
			}
		}

		@Override
		public void specifyLegend(Graphics2D g, Double line) {
			
			String s = "Border Cells";
			Color c = Color.white;
			int offset = 0;
			
			OverlayUtils.stringColorLegend(g, line, s, c, offset);
			
		}
}
