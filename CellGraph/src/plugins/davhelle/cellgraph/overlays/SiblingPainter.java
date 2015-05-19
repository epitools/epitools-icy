/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.overlays;

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

public class SiblingPainter extends Overlay {

	SpatioTemporalGraph stGraph;
	
	public SiblingPainter(SpatioTemporalGraph stGraph){
		super("Common parent highlighter (Green)");
		
		this.stGraph = stGraph;
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			
			//mark in green all cells that have a neighbor with the same parent
			g.setColor(Color.green);
			
			int division_no = 0;
			for(Node cell: frame_i.vertexSet())	
				if(cell.getFirst() != null)
					for(Node neighbor: cell.getNeighbors())
						if(neighbor.getFirst() != null)
							if(neighbor.getFirst() == cell.getFirst()){
								g.fill(cell.toShape());
								g.fill(neighbor.toShape());
								division_no++;
							}
			
			
			//g.setColor(Color.white);
			g.setFont(new Font("TimesRoman", Font.PLAIN, 10));
			g.drawString("Automatically identified: "+division_no, 10 , 20);
		}
	}
}
