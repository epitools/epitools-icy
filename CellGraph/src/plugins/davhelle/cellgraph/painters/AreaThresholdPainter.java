/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Painter to highlight all cells with a greater area
 * than the given threshold.  
 * 
 * @author Davide Heller
 *
 */
public class AreaThresholdPainter extends AbstractPainter{
	
	private SpatioTemporalGraph stGraph;
	private ArrayList<Node> toDraw;
	
	public AreaThresholdPainter(SpatioTemporalGraph spatioTemporalGraph, double areaThreshold){
		this.stGraph = spatioTemporalGraph;
		
		toDraw = new ArrayList<Node>();
		
		for(int i=0; i < stGraph.size(); i++){
			for(Node node: stGraph.getFrame(i).vertexSet()){
				if(node.getGeometry().getArea() > areaThreshold)
					toDraw.add(node);
			}
		}
	
	}

	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		if(time_point < stGraph.size()){
			//TODO include 3D information!
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.LIGHT_GRAY);
	
			for(Node cell: frame_i.vertexSet())
				if(toDraw.contains(cell))
					g.fill((cell.toShape()));

			
		}
    }
}
