/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Painter to highlight the cells that lie on the boundary. 
 * 
 * @author Davide Heller
 *
 */
public class BorderOverlay extends StGraphOverlay{

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
//				else
//					g.setColor(Color.green);

				//Fill cell shape
				//if(!cell.onBoundary())
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
}
