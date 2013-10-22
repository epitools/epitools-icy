/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

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

public class ArrowPainter extends AbstractGraphPainter {

	GeometryFactory factory;
	ShapeWriter writer;
	float displacement;
	
	public ArrowPainter(SpatioTemporalGraph stGraph, float displacement){
		super("Displacement arrows", stGraph);
		this.factory = new GeometryFactory();
		this.writer = new ShapeWriter();
		this.displacement = displacement;
	}
	
		
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		for(Node cell: frame_i.vertexSet()){
			
			//skipping cells which haven't been tracked
			if(!cell.hasNext())
				continue;	

			//create segment line to next
			Point next_cell_center = cell.getNext().getCentroid();
			LineSegment segment_to_successive_cell = 
					new LineSegment(
							cell.getCentroid().getCoordinate(),
							next_cell_center.getCoordinate()
							);

			Point2D tipCoordinate = new Point2D.Double(next_cell_center.getX(),next_cell_center.getY());				

			draw(g, cell, segment_to_successive_cell, tipCoordinate);
		}
	}


	private void draw(Graphics2D g, Node cell,
			LineSegment segment_to_successive_cell, 
			Point2D tipCoordinate) {
		
		g.rotate(0);
		drawBackground(g, cell, segment_to_successive_cell);		
		drawRotatedArrow(g, segment_to_successive_cell, tipCoordinate);
	}


	private Color getSegmentBackgroundColor(LineSegment segment_to_successive_cell) {
		//choose background color according to displacement length
		if(segment_to_successive_cell.getLength() > (double) displacement)
			return Color.white;
		else
			return Color.green;
	}
	
	private void drawBackground(Graphics2D g, Node cell,
			LineSegment segment_to_successive_cell) { 
		g.setColor(getSegmentBackgroundColor(segment_to_successive_cell));
		g.fill(cell.toShape());
	}


	private void drawRotatedArrow(Graphics2D g,
			LineSegment segment_to_successive_cell, Point2D tipCoordinate) {
		//draw a line segment
		g.setStroke(new BasicStroke((float)0.5));
		g.setColor(Color.red);
		g.draw(writer.toShape(segment_to_successive_cell.toGeometry(factory)));
			
		//draw a tip
		Triangle tip = new Triangle(2);
		AffineTransform defaultTransform = g.getTransform();
		
		g.setStroke(new BasicStroke((float)0.2));
		double segment_angle = segment_to_successive_cell.angle() + Math.PI/2;
		g.rotate(segment_angle,tipCoordinate.getX(),tipCoordinate.getY());
		g.fill(tip.createPoint(tipCoordinate));
		g.setTransform(defaultTransform);
	}
}
