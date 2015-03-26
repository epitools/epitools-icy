/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.util.XLSUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import jxl.write.WritableSheet;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.PointShapeFactory.Triangle;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.Point;

/**
 * Overlay to display the displacement of the cells in the sample over time.
 * An arrow image is displayed over each cell visualizing the position shift
 * to the next frame. The arrows are colored differently according to the
 * magnitude of the shift. The displacement variable sets this threshold.
 * 
 * 
 * @author Davide Heller
 *
 */
public class DisplacementOverlay extends StGraphOverlay {

	public static final String DESCRIPTION = "Overlay to highlight the spatial displacement from one frame to the next.\n\n" +
			"Displacement Color code:\n" +
			"* [white] cell centroid moves > [x] px\n" +
			"* [green] cell centroid moves < [x] px\n" +
			"* [none] next frame info not available\n";
	
	GeometryFactory factory;
	ShapeWriter writer;
	float displacement;
	
	/**
	 * @param stGraph
	 * @param displacement the threshold of movement to color the cell differently
	 */
	public DisplacementOverlay(SpatioTemporalGraph stGraph, float displacement){
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


	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Distance to cell in next frame");
		
		int row_no = 1;
		for(Node cell: frame.vertexSet()){
					
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

			//write out the distance to the successive cell position for every frame
			XLSUtil.setCellNumber(sheet, 0, row_no, cell.getTrackID());
			XLSUtil.setCellNumber(sheet, 1, row_no, segment_to_successive_cell.getLength());
			row_no++;
		}
			
	}

}
