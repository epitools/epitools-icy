/**
 * 
 */
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.gui.dialog.SaveDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.system.IcyExceptionHandler;
import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.lang.reflect.Field;

import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

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
		
		Edge oldEdge = edge;
		FrameGraph oldFrame = frame;
		for(int i=frame.getFrameNo(); i<stGraph.size(); i++){
			
			FrameGraph futureFrame = stGraph.getFrame(i);
			long edgeTrackId = oldEdge.getPairCode(oldFrame);
			Edge futureEdge = null;

			if(futureFrame.hasEdgeTrackId(edgeTrackId)){
				futureEdge = futureFrame.getEdgeWithTrackId(edgeTrackId);
			} else {
				//did source or target divide? => changing the ids
				Node target = frame.getEdgeTarget(edge);
				Node source = frame.getEdgeSource(edge);
				
				if(target.hasObservedDivision())
					futureEdge = findFutureEdge(futureFrame, target, source);
				else if(source.hasObservedDivision())
					futureEdge = findFutureEdge(futureFrame, source, target);
			}
			
			if(futureEdge != null){
				//set
				futureEdge.computeGeometry(futureFrame);
				futureEdge.setColorTag(colorTag);
				
				//link
				futureEdge.setPrevious(oldEdge);
				oldEdge.setNext(futureEdge);

				//update
				oldEdge = futureEdge;
				oldFrame = futureFrame;
			}
		}
	}

	/**
	 * @param futureFrame
	 * @param target
	 * @param source
	 * @return
	 */
	private Edge findFutureEdge(FrameGraph futureFrame, Node target, Node source) {
		Division d = target.getDivision();
		Node child1 = d.getChild1();
		Node child2 = d.getChild2();
		
		long code1 = Edge.computePairCode(child1.getTrackID(), source.getTrackID());
		long code2 = Edge.computePairCode(child2.getTrackID(), source.getTrackID());

		Edge futureEdge = null;
		if(futureFrame.hasEdgeTrackId(code1) &&
				!futureFrame.hasEdgeTrackId(code2)){
			futureEdge = futureFrame.getEdgeWithTrackId(code1);
		} 
		else if(futureFrame.hasEdgeTrackId(code2) &&
				!futureFrame.hasEdgeTrackId(code1)){
			futureEdge = futureFrame.getEdgeWithTrackId(code2);
		}
		return futureEdge;
	}


	/**
	 * @param oldEdge
	 * @param futureEdge
	 * @return
	 */
	private void linkAndUpdate(Edge oldEdge, Edge futureEdge) {
		futureEdge.setPrevious(oldEdge);
		oldEdge.setNext(futureEdge);
		oldEdge = futureEdge;
	}	

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			String file_name = SaveDialog.chooseFile(
					"Please choose where to save the excel Sheet",
					"/Users/davide/",
					"test_file", XLSUtil.FILE_DOT_EXTENSION);
			
			if(file_name == null)
				return;
				
			WritableWorkbook wb = XLSUtil.createWorkbook(file_name);
			
			
			String sheetName = String.format("Edge Length");
			WritableSheet sheet = XLSUtil.createNewPage(wb, sheetName);
			int col_no = 0;
			int row_no = 0;
			FrameGraph frame0 = stGraph.getFrame(0);
			for(Edge edge: frame0.edgeSet()){
				if(edge.hasColorTag()){
					
					//Column header: [colorString] ([EdgeSourceID],[EdgeTargetID])
					String header = getColorName(edge.getColorTag());
					header += " (" + frame0.getEdgeSource(edge).getTrackID();
					header += " ," + frame0.getEdgeTarget(edge).getTrackID() + ")";
					XLSUtil.setCellString(sheet, col_no, row_no++, header);
					
					//For every row write the length of a tagged edge
					XLSUtil.setCellNumber(sheet, col_no, row_no++, edge.getGeometry().getLength());
					Edge next = edge;
					while(next.hasNext()){
						next = next.getNext();
						XLSUtil.setCellNumber(sheet, col_no, row_no++, next.getGeometry().getLength());
					}
					
					//update counters
					col_no++;
					row_no=0;
				}
			}
			
			XLSUtil.saveAndClose(wb);
			
			new AnnounceFrame("XLS file exported successfully to: "+file_name,10);
			
		} catch (WriteException writeException) {
			IcyExceptionHandler.showErrorMessage(writeException, true, true);
		} catch (IOException ioException) {
			IcyExceptionHandler.showErrorMessage(ioException, true, true);
		}
	}
	
	/**
	 * source: http://stackoverflow.com/a/12828811
	 * 
	 * convert the color into a string if possible
	 * 
	 * @param c
	 * @return
	 */
	public static String getColorName(Color c) {
	    for (Field f : Color.class.getFields()) {
	        try {
	            if (f.getType() == Color.class && f.get(null).equals(c)) {
	                return f.getName();
	            }
	        } catch (java.lang.IllegalAccessException e) {
	            // it should never get to here
	        } 
	    }
	    return "unknown";
	}

}
