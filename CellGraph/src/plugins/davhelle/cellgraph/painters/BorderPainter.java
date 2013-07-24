/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Painter to highlight the cells that lie on the boundary. 
 * 
 * @author Davide Heller
 *
 */
public class BorderPainter extends Overlay{

		private SpatioTemporalGraph stGraph;
		//TODO maybe speedup with private ShapeWriter writer;
		
		public BorderPainter(SpatioTemporalGraph spatioTemporalGraph){
			super("Border cells");
			this.stGraph = spatioTemporalGraph;
			

		}

		public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas){
			int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

			if(time_point < stGraph.size()){

				FrameGraph frame_i = stGraph.getFrame(time_point);
				
				for(Node cell: frame_i.vertexSet()){

					if(cell.onBoundary()){
						g.setColor(Color.white);
						g.fill(cell.toShape());
					}
//					else
//						g.setColor(Color.green);

					//Fill cell shape
					//if(!cell.onBoundary())
					

				}
			}
		}
}
