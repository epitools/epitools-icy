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
import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Iterator;

import jxl.write.WritableSheet;

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
public class CorrectionOverlay extends StGraphOverlay {
	
	private GeometryFactory factory;
	
	public static final String DESCRIPTION = "Overlay to evidence potential False positives (FP) and False Negatives (FN)\n\n" +
			"* [RED] FP, i.e. over-segmentation\n" + 
			"* [YELLOW] FN, i.e. under-segmentation\n" +
			"* Click on any mark to cancel the evidence\n\n"+
			"Useful in combination with CellEditor. Please note that it must be run on a different sequence than latter's [INPUT]" +
			" (i.e. to avoid event conflict between the clicking events)";
	
	public CorrectionOverlay(SpatioTemporalGraph stGraph) {
		super("Tracking Corrections",stGraph);
		this.factory = new GeometryFactory();
		
		System.out.println("Looking for potential False positives (FP) and negatives (FN)");
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
					if(cell.getNext().getBelongingFrame().getFrameNo() > time_point + 1){
						cell.setErrorTag(TrackingFeedback.FALSE_NEGATIVE.numeric_code);
						System.out.printf("\t Possible FN: %d @ frame %d\n",cell.getTrackID(),time_point + 1);
					}
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
					if(child1.getElimination().getTimePoint() == time_point){
						child1.setErrorTag(TrackingFeedback.FALSE_POSITIVE.numeric_code);
						System.out.printf("\t Possible FP: %d @ frame %d\n",child1.getTrackID(),time_point);
					}
				Node child2 = division.getChild2();
				if(child2.hasObservedElimination())
					if(child2.getElimination().getTimePoint() == time_point){
						child2.setErrorTag(TrackingFeedback.FALSE_POSITIVE.numeric_code);
						System.out.printf("\t Possible FP: %d @ frame %d\n",child1.getTrackID(),time_point);
					}
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
			 	if(cell.getGeometry().contains(point_geometry) && 
			 			cell.getErrorTag() == TrackingFeedback.FALSE_POSITIVE.numeric_code){
			 		cell.setErrorTag(TrackingFeedback.DEFAULT.numeric_code);
			 		System.out.println("Corrected potential FP: "+cell.getTrackID());
			 	}
			
			if(time_point > 0){
				//Help the user see a cell that went missing from the previous frame
				FrameGraph previous_frame = stGraph.getFrame(time_point - 1);
				for(Node cell: previous_frame.vertexSet())
					if(cell.getGeometry().contains(point_geometry) && 
							cell.getErrorTag() == TrackingFeedback.FALSE_NEGATIVE.numeric_code){
				 		cell.setErrorTag(TrackingFeedback.DEFAULT.numeric_code);
				 		System.out.println("Corrected potential FN: "+cell.getTrackID());
				 	}
			}
		}

	}
	
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		for(Node cell: frame_i.vertexSet())
		 	 if(cell.getErrorTag() == TrackingFeedback.FALSE_POSITIVE.numeric_code){
		 		g.setColor(Color.red);
		 		g.fill(cell.toShape());
		 	}
		
		int time_point = frame_i.getFrameNo();
		
		if(time_point > 0){
			//Help the user see a cell that went missing from the previous frame
			FrameGraph previous_frame = stGraph.getFrame(time_point - 1);
			for(Node cell: previous_frame.vertexSet())
				if(cell.getErrorTag() == TrackingFeedback.FALSE_NEGATIVE.numeric_code){
			 		g.setColor(Color.yellow);
			 		g.fill(cell.toShape());
			 	}
		}
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		//TODO currently errors are not synced with their frame. Should be +1
		
		XLSUtil.setCellString(sheet, 0, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 1, 0, "Centroid y");
		XLSUtil.setCellString(sheet, 2, 0, "Error Type");

		int row_no = 1;
		for(Node node: frame.vertexSet()){
			if(node.getErrorTag() == TrackingFeedback.FALSE_NEGATIVE.numeric_code){
				XLSUtil.setCellNumber(sheet, 0, row_no, node.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 1, row_no, node.getCentroid().getY());
				XLSUtil.setCellString(sheet, 2, row_no, "FN");
				row_no++;
			} else if(node.getErrorTag() == TrackingFeedback.FALSE_POSITIVE.numeric_code){
				XLSUtil.setCellNumber(sheet, 0, row_no, node.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 1, row_no, node.getCentroid().getY());
				XLSUtil.setCellString(sheet, 2, row_no, "FP");
				row_no++;
			}
		}
		
	}

}
