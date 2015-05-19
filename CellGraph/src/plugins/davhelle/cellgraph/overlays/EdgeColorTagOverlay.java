/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.overlays;

import icy.canvas.IcyCanvas;
import icy.gui.dialog.SaveDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;
import icy.system.IcyExceptionHandler;
import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D.Double;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.lang.reflect.Field;

import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.CellColor;
import plugins.davhelle.cellgraph.misc.ShapeRoi;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Interactive overlay to mark individual edges by clicking actions.
 * Export allows to extract the length of the tagged edges over time
 * as well as the intensity.
 * 
 * @author Davide Heller
 *
 */
public class EdgeColorTagOverlay extends StGraphOverlay {

	public static final String DESCRIPTION = "Interactive overlay to mark edges/junctions" +
			" in the graph and follow them over time. See CELL_COLOR_TAG for usage help.\n\n" +
			" [color]  = color to tag the edge with\n" +
			" [buffer] = width for intensity measurement\n" +
			" both options can be changed at runtime.\n\n" +
			" The XLS export option in the layer panel saves" +
			" for every selected edge the length over time in the first sheet and the mean" +
			" intensity of the image underneath the expanded edge geometry. The column" +
			" header provides a identification for the edge:\n\n" +
			" [colorString] [tStart,xStart,yStart]";
	
	private ShapeWriter writer;
	private GeometryFactory factory;
	private Sequence sequence;
	private EzVarEnum<CellColor> tag_color;
	private EzVarInteger envelope_buffer;
	private boolean tags_exist;
	
	public EdgeColorTagOverlay(SpatioTemporalGraph stGraph,
			EzVarEnum<CellColor> varEdgeColor,
			EzVarInteger varEnvelopeBuffer, Sequence sequence) {
		super("Edge Color Tag", stGraph);
	
		this.tag_color = varEdgeColor;
		this.envelope_buffer = varEnvelopeBuffer;
		this.writer = new ShapeWriter();
		this.factory = new GeometryFactory();
		this.sequence = sequence;
		
		this.tags_exist = false;
		for(Edge edge: stGraph.getFrame(0).edgeSet()){
			if(edge.hasColorTag()){
				this.tags_exist = true;
				break;
			}
		}
		
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#paintFrame(java.awt.Graphics2D, plugins.davhelle.cellgraph.graphs.FrameGraph)
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
		g.draw(writer.toShape(e.getGeometry().buffer(envelope_buffer.getValue())));
	}
	
	@Override
	public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas){
		
		final double CLICK_BUFFER_WIDTH = 2.0;
		
		int time_point = canvas.getPositionT();
		Color colorTag = tag_color.getValue().getColor();
		
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
			 			
			 			//get edge geometry
			 			if(!edge.hasGeometry())
			 				edge.computeGeometry(frame_i);
			 			Geometry intersection = edge.getGeometry();
			 			Geometry envelope = intersection.buffer(CLICK_BUFFER_WIDTH);
			 			
			 			//check if click falls into envelope
			 			if(envelope.contains(point_geometry)){
			 				tags_exist = true;
			 				
			 				if(edge.hasColorTag()){
			 					if(edge.getColorTag() == colorTag)
			 						propagateTag(edge,null);
			 					else
			 						propagateTag(edge,colorTag);
			 				}
			 				else
			 					initializeTag(edge,colorTag,frame_i);
			 			}
			 		}
			 	}
			}
		}
	}

	private void propagateTag(Edge edge, Color colorTag) {
		//change current edge
		edge.setColorTag(colorTag);
		
		//change previous edges
		Edge old = edge;
		while(old.hasPrevious()){
			old = old.getPrevious();
			old.setColorTag(colorTag);
		}
		
		//change next edges
		Edge next = edge;
		while(next.hasNext()){
			next = next.getNext();
			next.setColorTag(colorTag);
		}
	}

	private void initializeTag(Edge edge, Color colorTag, FrameGraph frame) {
		edge.setColorTag(colorTag);
		
		Edge oldEdge = edge;
		FrameGraph oldFrame = frame;
		int nextFrameNo = frame.getFrameNo() + 1;
		for(int i=nextFrameNo; i<stGraph.size(); i++){
			
			FrameGraph futureFrame = stGraph.getFrame(i);
			long edgeTrackId = oldEdge.getPairCode(oldFrame);
			Edge futureEdge = null;

			if(futureFrame.hasEdgeTrackId(edgeTrackId)){
				futureEdge = futureFrame.getEdgeWithTrackId(edgeTrackId);
			} else {
				//did source or target divide? => changing the ids
				Node target = oldFrame.getEdgeTarget(oldEdge);
				Node source = oldFrame.getEdgeSource(oldEdge);
				
				//1. Both cells divide
				if(target.hasObservedDivision() && source.hasObservedDivision()){
					Division d1 = target.getDivision();
					Division d2 = source.getDivision();
					
					int t1 = d1.getTimePoint();
					int t2 = d2.getTimePoint();
					
					//always choose the event closest to the current frame
					if(t1 < t2){
						if (t2 <= i)
							futureEdge = findFutureEdge(futureFrame, source, target);
						else if(t1 <= i)
							futureEdge = findFutureEdge(futureFrame, target, source);
					} else if (t1 > t2) {
						if (t1 <= i)
							futureEdge = findFutureEdge(futureFrame, target, source);
						else if(t2 <= i)
							futureEdge = findFutureEdge(futureFrame, source, target);
					} else  //(t1==t2)
						continue;
				}
				else if(target.hasObservedDivision()) // only target divides
					futureEdge = findFutureEdge(futureFrame, target, source);
				else if(source.hasObservedDivision()) // only source divides
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
	 * @param futureFrame the currently analyzed frame
	 * @param dividingCell the dividing cell in a previous frame
	 * @param neighbor the neighboring cell in a previous frame
	 * @return
	 */
	private Edge findFutureEdge(FrameGraph futureFrame, Node dividingCell, Node neighbor) {
		Division d = dividingCell.getDivision();
		
		//if the current time point is prior to the division this event should be ignored
		if(futureFrame.getFrameNo() < d.getTimePoint())
			return null;
		
		//if the dividing cell is a daughter cell the id change already happened
		if(!d.isMother(dividingCell))
			return null;
		
		Node child1 = d.getChild1();
		Node child2 = d.getChild2();
		
		long code1 = Edge.computePairCode(child1.getTrackID(), neighbor.getTrackID());
		long code2 = Edge.computePairCode(child2.getTrackID(), neighbor.getTrackID());

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
			
			writeEdges(wb);
			
			XLSUtil.saveAndClose(wb);
			
			new AnnounceFrame("XLS file exported successfully to: "+file_name,10);
			
		} catch (WriteException writeException) {
			IcyExceptionHandler.showErrorMessage(writeException, true, true);
		} catch (IOException ioException) {
			IcyExceptionHandler.showErrorMessage(ioException, true, true);
		}
	}
	
	/**
	 * write sheet with edge length and intensities
	 * 
	 * Edge header (column): [colorString] [tStart,xStart,yStart]
	 * 
	 * @param wb
	 */
	private void writeEdges(WritableWorkbook wb) {
		String sheetName = String.format("Edge Length");
		WritableSheet sheet = XLSUtil.createNewPage(wb, sheetName);
		
		String intensitySheetName = "Edge Intensity";
		WritableSheet intensitySheet = XLSUtil.createNewPage(wb, intensitySheetName);
		
		int col_no = 0;
		for(int i=0; i<stGraph.size(); i++){
			for(Edge edge: stGraph.getFrame(i).edgeSet()){
				if(edge.hasColorTag()){
					
					//avoid writing the same edge again
					if(edge.hasPrevious())
						if(edge.getPrevious().hasColorTag())
							continue; 

					//Column header: [colorString] [tStart,xStart,yStart]
					int row_no = 0;

					double xStart = edge.getGeometry().getCentroid().getX();
					double yStart = edge.getGeometry().getCentroid().getY();
					int tStart = edge.getFrame().getFrameNo();

					String header = String.format("%s [t%d,x%.0f,y%.0f]",
							getColorName(edge.getColorTag()),
							tStart,
							xStart,
							yStart);
					XLSUtil.setCellString(sheet, col_no, row_no, header);
					XLSUtil.setCellString(intensitySheet, col_no, row_no, header);

					//For every row write the length of a tagged edge
					//leave an empty row for a time point where the edge is not present
					row_no = tStart + 1;
					XLSUtil.setCellNumber(sheet, col_no, row_no, edge.getGeometry().getLength());

					double meanIntensity = computeIntensity(edge);
					XLSUtil.setCellNumber(intensitySheet, col_no, row_no, meanIntensity);
					
					//Fill all linked time points
					Edge next = edge;
					while(next.hasNext()){
						
						next = next.getNext();
						row_no = next.getFrame().getFrameNo() + 1;
						XLSUtil.setCellNumber(sheet, col_no, row_no, next.getGeometry().getLength());
						
						meanIntensity = computeIntensity(next);
						XLSUtil.setCellNumber(intensitySheet, col_no, row_no, meanIntensity);
						
					}

					//update counter
					col_no++;
					
				}
			}
		}
	}
	
	private double computeIntensity(Edge edge){
		
		Geometry envelope = edge.getGeometry().buffer(envelope_buffer.getValue());
		
		ShapeRoi edgeEnvelopeRoi = new ShapeRoi(writer.toShape(envelope));
		
		int z=0;
		int t=edge.getFrame().getFrameNo();
		int c=0;
		double envelopeMeanIntenisty = ROIUtil.getMeanIntensity(sequence, edgeEnvelopeRoi, z, t, c);

		return envelopeMeanIntenisty;
		
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

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		if(!tags_exist){
			String s = "Click on a junction to color-tag it";
			Color c = Color.WHITE;
			int offset = 0;

			OverlayUtils.stringColorLegend(g, line, s, c, offset);
		}
	}

}
