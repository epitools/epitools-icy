/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import java.awt.Graphics2D;
import java.awt.Color;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.painter.Overlay;
import icy.sequence.Sequence;

/**
 * PolygonPainter depicts Polygons detected by JTS Polygonizer
 * 
 * @author Davide Heller
 *
 */
public class PolygonPainter extends Overlay{
	
	private SpatioTemporalGraph stGraph;
	
	public PolygonPainter(SpatioTemporalGraph spatioTemporalGraph){
		super("Polygons");
		this.stGraph = spatioTemporalGraph;
	}

	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			//TODO include 3D information!
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.red);
	
			for(Node cell: frame_i.vertexSet())
				g.draw((cell.toShape()));

			
		}
    }
}
