/*=========================================================================
 *
 *  (C) Copyright (2012-2014) Basler Group, IMLS, UZH
 *  
 *  All rights reserved.
 *	
 *  author:	Davide Heller
 *  email:	davide.heller@imls.uzh.ch
 *  
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Iterator;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.tracking.TrackingFeedback;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Interactive painter to mark cell with a certain
 * color tag set in the UI. Current behavior tags
 * a cell is the default color is present (black)
 * or if another color is present. If the user
 * tries to repaint a cell with the same color it 
 * already has, the cell is put back to default.
 * 
 * 
 * @author Davide Heller
 *
 */
public class CorrectionOverlay extends Overlay {
	
	private SpatioTemporalGraph stGraph;
	private GeometryFactory factory;
	
	public CorrectionOverlay(SpatioTemporalGraph stGraph) {
		super("Tracking Corrections");
		this.stGraph = stGraph;
		this.factory = new GeometryFactory();
		
		markFalsePositives();
		markFalseNegatives();
	}
	
	private void markFalseNegatives() {
		for(int time_point = 0; time_point < stGraph.size(); time_point++){
					
			FrameGraph frame = stGraph.getFrame(time_point);

			Iterator<Node> node_it = frame.iterator();

			while(node_it.hasNext()){
				Node cell = node_it.next();
				if(cell.hasNext())
					if(cell.getNext().getBelongingFrame().getFrameNo() > time_point + 1)
						cell.setErrorTag(TrackingFeedback.FALSE_NEGATIVE.numeric_code);
			}
					
		}
		
	}

	/**
	 * Method that marks cells which are most likely false 
	 * positives due to a non existent cell boundary  
	 */
	private void markFalsePositives() {
		
		for(int time_point = 0; time_point < stGraph.size(); time_point++){
			
			FrameGraph frame = stGraph.getFrame(time_point);
			Iterator<Division> division_it = frame.divisionIterator();
					
			while(division_it.hasNext()){
				Division division = division_it.next();
				
				Node child1 = division.getChild1();
				if(child1.hasObservedElimination())
					if(child1.getElimination().getTimePoint() == time_point + 1)
						child1.setErrorTag(TrackingFeedback.FALSE_POSITIVE.numeric_code);
				
				Node child2 = division.getChild2();
				if(child2.hasObservedElimination())
					if(child2.getElimination().getTimePoint() == time_point + 1)
						child2.setErrorTag(TrackingFeedback.FALSE_POSITIVE.numeric_code);
			}
		}
	}

	@Override
	public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas){
		int time_point = canvas.getPositionT();
		
		if(time_point < stGraph.size()){
			
			//create point Geometry
			Coordinate point_coor = new Coordinate(imagePoint.getX(), imagePoint.getY());
			Point point_geometry = factory.createPoint(point_coor);			
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			for(Node cell: frame_i.vertexSet())
			 	if(cell.getGeometry().contains(point_geometry)){
			 		cell.setErrorTag(TrackingFeedback.DEFAULT.numeric_code);
			 	}
		}

	}
	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			FrameGraph frame_i = stGraph.getFrame(time_point);
			for(Node cell: frame_i.vertexSet())
			 	if(cell.getErrorTag() == TrackingFeedback.FALSE_NEGATIVE.numeric_code){
			 		g.setColor(Color.yellow);
			 		g.fill(cell.toShape());
			 	}
			 	else if(cell.getErrorTag() == TrackingFeedback.FALSE_POSITIVE.numeric_code){
			 		g.setColor(Color.red);
			 		g.fill(cell.toShape());
			 	}
		}
    }

}
