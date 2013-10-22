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

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Point;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

public class CentroidPainter extends AbstractGraphPainter{

	private ShapeWriter writer;
	
	public CentroidPainter(SpatioTemporalGraph spatioTemporalGraph){
		super("Cell centroids", spatioTemporalGraph);
		this.writer = new ShapeWriter();	
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		//TODO include 3D information!
		//TODO possible performance improvement if map<Node,Point> is created
		g.setColor(Color.red);
		
		for(Node cell: frame_i.vertexSet()){
			Point centroid = cell.getCentroid();
			g.fillOval((int)centroid.getX(), (int)centroid.getY(), 2, 2);
		}		
	}
}
