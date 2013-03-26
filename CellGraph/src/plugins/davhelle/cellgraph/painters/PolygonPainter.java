package plugins.davhelle.cellgraph.painters;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import be.humphreys.simplevoronoi.GraphEdge;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

/**
 * PolygonPainter depicts Polygons detected by JTS Polygonizer
 * 
 * @author Davide Heller
 *
 */
public class PolygonPainter extends AbstractPainter{
	
	private SpatioTemporalGraph stGraph;
	
	public PolygonPainter(SpatioTemporalGraph spatioTemporalGraph){
		this.stGraph = spatioTemporalGraph;
	}

	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		if(time_point < stGraph.size()){
			//TODO include 3D information!
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.red);
	
			for(Node cell: frame_i.vertexSet())
				g.draw((cell.toShape()));

			
		}
    }
}
