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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.PointShapeFactory.Triangle;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Point;

public class ArrowPainter extends Overlay {

	SpatioTemporalGraph stGraph;
	GeometryFactory factory;
	ShapeWriter writer;
	float displacement;
	
	public ArrowPainter(SpatioTemporalGraph stGraph, float displacement){
		super("Displacement arrows");
		this.stGraph = stGraph;
		this.factory = new GeometryFactory();
		this.writer = new ShapeWriter();
		this.displacement = displacement;
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			AffineTransform defaultTransform = g.getTransform();

			for(Node cell: frame_i.vertexSet()){
				if(cell.getNext() != null){
					g.rotate(0);
					Point next_cell_center = cell.getNext().getCentroid();
					LineSegment segment_to_successive_cell = 
							new LineSegment(
									cell.getCentroid().getCoordinate(),
									next_cell_center.getCoordinate()
							);
					
					double segment_angle = segment_to_successive_cell.angle() + Math.PI/2;
					
					Point2D tipCoordinate = new Point2D.Double(next_cell_center.getX(),next_cell_center.getY());
					
					
					//choose background color according to displacement length
					if(segment_to_successive_cell.getLength() > (double) displacement)
						g.setColor(Color.white);
					else
						g.setColor(Color.green);
					g.fill(cell.toShape());
					
					g.setStroke(new BasicStroke((float)0.5));
					g.setColor(Color.red);
					g.draw(writer.toShape(segment_to_successive_cell.toGeometry(factory)));
						
					Triangle tip = new Triangle(2);
					
					g.setStroke(new BasicStroke((float)0.2));
					g.rotate(segment_angle,tipCoordinate.getX(),tipCoordinate.getY());
					g.fill(tip.createPoint(tipCoordinate));
					g.setTransform(defaultTransform);
				}
			}
			
			
		}
	}


}
