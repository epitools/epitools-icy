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
import java.util.Map;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;;

public class VoronoiPainter extends Overlay {

	private Map<Node, Geometry> nodeVoronoiMap;
	private ShapeWriter writer;
	private SpatioTemporalGraph stGraph;
	
	
	public VoronoiPainter(SpatioTemporalGraph stGraph, Map<Node,Geometry> nodeVoronoiMap) {
		// TODO Auto-generated constructor stub
		super("Voronoi Diagram");
		this.stGraph = stGraph;
		this.nodeVoronoiMap = nodeVoronoiMap;
		this.writer = new ShapeWriter();

	}
	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.green);
	
			for(Node cell: frame_i.vertexSet())
				if(nodeVoronoiMap.containsKey(cell))
					g.draw((writer.toShape(nodeVoronoiMap.get(cell))));
			
		}
    }

}
