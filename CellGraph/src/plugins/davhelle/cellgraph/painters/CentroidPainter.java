/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import java.awt.Color;
import java.awt.Graphics2D;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Point;

public class CentroidPainter extends AbstractGraphPainter{
	
	public CentroidPainter(SpatioTemporalGraph spatioTemporalGraph){
		super("Cell centroids", spatioTemporalGraph);
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
