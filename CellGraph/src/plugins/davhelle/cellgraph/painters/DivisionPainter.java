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
import java.awt.Font;
import java.awt.Graphics2D;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

public class DivisionPainter extends Overlay {

	SpatioTemporalGraph stGraph;
	
	public DivisionPainter(SpatioTemporalGraph stGraph){
		super("Divisions (green)");
		this.stGraph = stGraph;
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			
			int division_no = 0;
			for(Node cell: frame_i.vertexSet()){
				if(cell.getFirst() != null){
					
					if(cell.getFirst().hasObservedDivision()){
						division_no++;
						g.setColor(Color.green);
//						if(!cell.onBoundary()) TODO: apply correspondence to first frame!
						g.fill(cell.toShape());
						g.fillOval((int)cell.getCentroid().getX(), (int)cell.getCentroid().getY(), 2, 2);
					}
					
					if(cell.getFirst().hasObservedElimination()){
						g.setColor(Color.red);
						g.fill(cell.toShape());
					}
				
//					else{
//						g.setColor(Color.white);
//						g.fill(cell.toShape());
//						g.setColor(Color.red);
//						g.draw(cell.toShape());
//					}
				}
			}
			
			//g.setColor(Color.white);
			g.setFont(new Font("TimesRoman", Font.PLAIN, 10));
			//g.drawString("Manually identified: "+division_no, 10 , 30);
		}
	}

}
