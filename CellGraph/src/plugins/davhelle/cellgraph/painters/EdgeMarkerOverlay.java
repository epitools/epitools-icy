/**
 * 
 */
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * @author Davide Heller
 *
 */
public class EdgeMarkerOverlay extends StGraphOverlay {

	private ShapeWriter writer;
	private GeometryFactory factory;
	
	
	public EdgeMarkerOverlay(SpatioTemporalGraph stGraph) {
		super("Edge Color Tag", stGraph);
		
		this.writer = new ShapeWriter();
		this.factory = new GeometryFactory();
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.painters.StGraphOverlay#paintFrame(java.awt.Graphics2D, plugins.davhelle.cellgraph.graphs.FrameGraph)
	 */
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		for(Edge edge: frame_i.edgeSet()){
			if(edge.hasColorTag()){
				drawEdge(g, edge, edge.getColorTag());
			}
		}

	}
	
	public void drawEdge(Graphics2D g, Edge e, Color color){
		g.setColor(color);
		g.draw(writer.toShape(e.getGeometry().buffer(1.0)));
	}
	
	@Override
	public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas){
		int time_point = canvas.getPositionT();
		Color colorTag = Color.cyan;
		
		if(time_point < stGraph.size()){
			
			//create point Geometry
			Coordinate point_coor = new Coordinate(imagePoint.getX(), imagePoint.getY());
			Point point_geometry = factory.createPoint(point_coor);			
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			for(Node cell: frame_i.vertexSet()){
			 	Geometry cellGeometry = cell.getGeometry();
				
			 	//if cell contains click search it's edges
			 	if(cellGeometry.contains(point_geometry)){
			 		for(Node neighbor: cell.getNeighbors()){
			 			Edge edge = frame_i.getEdge(cell, neighbor);
			 			if(edge.hasColorTag()){
			 				Geometry envelope = edge.getGeometry().buffer(1.0);
			 				if(envelope.contains(point_geometry)){
			 					if(edge.getColorTag() == colorTag)
			 						propagateTag(edge,null,frame_i);
			 					else
			 						propagateTag(edge,colorTag,frame_i);
			 				}
			 			}
			 			else{
			 				if(!edge.hasGeometry())
			 					edge.computeGeometry(frame_i);
			 					
			 				Geometry intersection = edge.getGeometry();
			 				
			 				Geometry envelope = intersection.buffer(1.0);
			 				if(envelope.contains(point_geometry))
			 					propagateTag(edge,colorTag,frame_i);
			 			}
			 		}
			 	}
			}
		}

	}

	private void propagateTag(Edge edge, Color colorTag, FrameGraph frame) {
		edge.setColorTag(colorTag);
		
		long edgeTrackId = edge.getPairCode(frame);
		for(int i=frame.getFrameNo(); i<stGraph.size(); i++){
			
			FrameGraph futureFrame = stGraph.getFrame(i);
			if(futureFrame.hasEdgeTrackId(edgeTrackId)){
				Edge futureEdge = futureFrame.getEdgeWithTrackId(edgeTrackId);
				futureEdge.computeGeometry(futureFrame);
				futureEdge.setColorTag(colorTag);
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.painters.StGraphOverlay#writeFrameSheet(jxl.write.WritableSheet, plugins.davhelle.cellgraph.graphs.FrameGraph)
	 */
	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		// TODO Auto-generated method stub

	}

}
