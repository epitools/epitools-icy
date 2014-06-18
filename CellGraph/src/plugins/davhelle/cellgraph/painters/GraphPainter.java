/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import java.awt.Color;
import java.awt.Graphics2D;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;

/**
 * Class to visualize all the edges of a 
 * spatial-temporal graph as overlay in icy.
 * 
 * @author Davide Heller
 *
 */
public class GraphPainter extends Overlay{

	private SpatioTemporalGraph stGraph;
	private GeometryFactory factory;
	private ShapeWriter writer;
	
	public GraphPainter(SpatioTemporalGraph stGraph) {
		super("Graph edges");
		this.stGraph = stGraph;
		this.factory = new GeometryFactory();
		this.writer = new ShapeWriter();
	}

	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas){
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
		
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.orange);
			
			//paint all the edges of the graph
			for(DefaultWeightedEdge edge: frame_i.edgeSet()){
				
				//extract points
				Point a = frame_i.getEdgeSource(edge).getCentroid();
				Point b = frame_i.getEdgeTarget(edge).getCentroid();
				
				//transform to JTS coordinates
				Coordinate[] edge_vertices = {a.getCoordinate(),b.getCoordinate()};

				//define line
				LineString edge_line = factory.createLineString(edge_vertices);
				
				//draw line
				g.draw(writer.toShape(edge_line));
				g.draw(writer.toShape(a));
				
			}
		}
	}
	
	
}
