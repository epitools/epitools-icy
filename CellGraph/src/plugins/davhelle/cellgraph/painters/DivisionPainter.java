/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Painter to visualize all cells that will divide or be eliminated
 * over the length of the movie. To solve the ambiguity of cells
 * undergoing both events (i.e. a cell divides and then loses
 * one of the siblings by elimination) a different color scheme
 * is used (see below)
 * 
 * dividing cell - green
 * eliminated cell - red
 * both events - yellow
 * 
 * @author Davide Heller
 *
 */
public class DivisionPainter extends Overlay {

	SpatioTemporalGraph stGraph;
	
	public DivisionPainter(SpatioTemporalGraph stGraph){
		super("Divisions (green)");
		this.stGraph = stGraph;
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas){
		
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			
			for(Node cell: frame_i.vertexSet()){
				if(cell.getFirst() != null){
					
					if(cell.getFirst().hasObservedDivision()){
						g.setColor(Color.green);
						g.fill(cell.toShape());
					}
					
					if(cell.getFirst().hasObservedElimination()){
						if(cell.getFirst().hasObservedDivision())
							g.setColor(Color.yellow);
						else
							g.setColor(Color.red);
						
						g.fill(cell.toShape());
					}	
				}
			}	
		}
	}
}
