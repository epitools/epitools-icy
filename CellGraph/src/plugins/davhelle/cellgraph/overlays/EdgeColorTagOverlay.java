package plugins.davhelle.cellgraph.overlays;

import icy.canvas.IcyCanvas;
import icy.gui.dialog.SaveDialog;
import icy.gui.frame.progress.AnnounceFrame;
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
import java.util.HashMap;

import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.IntensityReader;
import plugins.davhelle.cellgraph.io.IntensitySummaryType;
import plugins.davhelle.cellgraph.misc.CellColor;
import plugins.davhelle.cellgraph.misc.ShapeRoi;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

/**
 * Interactive overlay to mark individual edges by clicking actions.
 * Export allows to extract the length of the tagged edges over time
 * as well as the intensity.
 * 
 * @author Davide Heller
 *
 */
public class EdgeColorTagOverlay extends StGraphOverlay implements EzVarListener<Integer> {

	/**
	 * Description String for GUI
	 */
	public static final String DESCRIPTION = 
			" Interactive overlay to mark edges/junctions in the graph<br/>" +
			" and follow them over time. See CELL_COLOR_TAG for usage help.<br/>" +
			" All parameters are interactive and can be changed also after<br/>" +
			" the overlay is added<br/><br>" +
			" NOTE: Header in the Export XLS identifies the edge:<br/>" +
			" [colorString] [tStart,xStart,yStart]";
	
	/**
	 * JTS to AWT writer
	 */
	private ShapeWriter writer;
	/**
	 * Geometry creator for Edge Envelope
	 */
	private GeometryFactory factory;
	/**
	 * Sequence from which to retrieve the intensities
	 */
	private Sequence sequence;
	/**
	 * Current tagging color handle
	 */
	private EzVarEnum<CellColor> tag_color;
	/**
	 * Current Envelope width handle
	 */
	private EzVarInteger envelope_buffer;
	/**
	 * Flag to see whether current tags exist
	 */
	private boolean tags_exist;
	
	/**
	 * Intensity Summary type
	 */
	EzVarEnum<IntensitySummaryType> summary_type;
	
	/**
	 * Geometries used to measure the image intensity
	 */
	private HashMap<Edge,Geometry> measurement_geometries;
	
	/**
	 * Exclude vertex geometry 
	 */
	private EzVarInteger roi_mode;
	
	/**
	 * Exclude vertex geometry 
	 */
	private EzVarInteger envelope_vertex_buffer;	
	
	/**
	 * Intensity channel to measure
	 */
	private EzVarInteger intensity_channel;
	
	private EzVarDouble top_percent;
	
	private EzVarBoolean add_roi;
	
	
	/**
	 * @param stGraph graph to analyze
	 * @param varEdgeColor color choice EzGUI handle
	 * @param varEnvelopeBuffer envelope width EzGUI handle
	 * @param sequence image from which to retrieve the intensities for export
	 * @param varEdgeChannel 
	 */
	public EdgeColorTagOverlay(SpatioTemporalGraph stGraph,
			EzVarEnum<CellColor> varEdgeColor,
			EzVarInteger varEnvelopeBuffer,
			EzVarInteger varVertexBuffer,
			EzVarInteger varVertexMode,
			Sequence sequence,
			EzVarEnum<IntensitySummaryType> intensitySummaryType, 
			EzVarInteger varEdgeChannel,
			EzVarDouble varTopPercent,
			EzVarBoolean varAddRoi) {
		super("Edge Color Tag", stGraph);
	
		this.tag_color = varEdgeColor;
		
		this.envelope_buffer = varEnvelopeBuffer;
		this.envelope_buffer.addVarChangeListener(this);
		
		this.roi_mode = varVertexMode;
		this.roi_mode.addVarChangeListener(this);
		
		this.envelope_vertex_buffer = varVertexBuffer;
		this.envelope_vertex_buffer.addVarChangeListener(this);
		
		this.intensity_channel = varEdgeChannel;
		
		this.top_percent = varTopPercent;
		this.add_roi = varAddRoi;
		
		this.writer = new ShapeWriter();
		this.factory = new GeometryFactory();
		this.sequence = sequence;
		this.summary_type = intensitySummaryType;
		this.measurement_geometries = new HashMap<Edge, Geometry>();
		
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
	
	/**
	 * Draw envelope of edge
	 * 
	 * @param g graphics handle
	 * @param e edge object to color
	 * @param color color of the envelope
	 */
	public void drawEdge(Graphics2D g, Edge e, Color color){
		
		if(measurement_geometries.containsKey(e)){
			g.setColor(color);
			Geometry measurement_geometry = measurement_geometries.get(e);
			g.draw(writer.toShape(measurement_geometry));
		}
		else{
			propagateTag(e,null);
		}
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
			 				
			 			if(!measurement_geometries.containsKey(edge)){
			 				Geometry measurement_geometry = computeMeasurementGeometry(edge,frame_i);
			 				measurement_geometries.put(edge, measurement_geometry);
			 			}
			 			
			 			Geometry intersection = measurement_geometries.get(edge);
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

	/**
	 * Computes the geometry that will be used to measure the intensity
	 * in the underlying image. According to user decision [envelope_buffer]
	 * and [excludeVertex] the geometry will be differently shaped.
	 * 
	 * roi_modes:
	 * 0 - CAP_ROUND: normal round cap buffer around the edge geometry
	 * 1 - inner edge by intersecting the vertex geoemtry
	 * 2 - vertex geometry
	 * 3 - CAP_FLAT by computing the lineString of all elements in the edge_geo
	 * 
	 * Warning: Intersection 3 is still unstable
	 * 
	 * @param e Edge to compute the geometry of
	 * @param frame_i Frame Graph of the edge
	 * @return
	 */
	private Geometry computeMeasurementGeometry(Edge e, FrameGraph frame_i) {
		
		if(!e.hasGeometry())
			e.computeGeometry(frame_i);
		
		Geometry e_geo = e.getGeometry();
		
		Geometry edge_buffer = e_geo.buffer(envelope_buffer.getValue());
		//Mode 0: Round cap
		if(roi_mode.getValue() == 0)
			return edge_buffer;
		
		//Mode 3: Flat cap
		if(roi_mode.getValue() == 3){
			
			int quadrantSegments = 2;
			int capGeometry = BufferParameters.CAP_FLAT;
		
			//transformation to coordinate array to combine all line segments
			//to produce a single line string geometry
			//http://stackoverflow.com/a/27815428
			Coordinate list[] = e_geo.getCoordinates();
			CoordinateArraySequence cas = new CoordinateArraySequence(list);
		
			LineString ls = new LineString(cas, factory);
		
			//Possibly interesting for future geometry applications
			//Geometry edge_buffer = DouglasPeuckerSimplifier.simplify(ls,0.0001);		

			return ls.buffer(envelope_buffer.getValue(),
				quadrantSegments,capGeometry);
		}
		
		//Mode 1 & 2: Inner edge or vertex intersection
		Node s = frame_i.getEdgeSource(e);

		double final_vertex_x = java.lang.Double.MAX_VALUE;
		Geometry final_vertex_geo = null;
		
		for(Node t: frame_i.getNeighborsOf(s)){
			Edge e2 = frame_i.getEdge(s, t);

			if(e2 == e)
				continue;
			
			if(!e2.hasGeometry())
				e2.computeGeometry(frame_i);
			
			Geometry e_geo2 = e2.getGeometry();
			
			if(e_geo2.intersects(e_geo)){
				Geometry e_vertexGeo = e_geo.intersection(e_geo2);
				
				double vertex_x = e_vertexGeo.getCentroid().getX();
				
				Geometry vertex_buffer = e_vertexGeo.buffer(
						envelope_vertex_buffer.getValue());
				
				if(roi_mode.getValue() == 2)
					if(vertex_x < final_vertex_x){
						final_vertex_x = vertex_x;
						final_vertex_geo = vertex_buffer;
					}
				
				edge_buffer = edge_buffer.difference(vertex_buffer);
			}
		}
			
		if(roi_mode.getValue() == 2)
			return final_vertex_geo;
		else
			return edge_buffer;
		
		
	}

	/**
	 * Propagate the Tag of the edge in time when possible
	 * 
	 * @param edge edge for which to propagate the tag
	 * @param colorTag tag to propagate
	 */
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

	/**
	 * Initialize the edge Tag
	 * 
	 * @param edge edge to be tagged
	 * @param colorTag color tag
	 * @param frame frame in which the edge is tagged
	 */
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
				
				//add the measurement shape if not yet present
				if(!measurement_geometries.containsKey(futureEdge)){
	 				Geometry measurement_geometry = 
	 						computeMeasurementGeometry(
	 								futureEdge,futureFrame);
	 				measurement_geometries.put(
	 						futureEdge, measurement_geometry);
	 			}
				
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
	 * @return the edge in a future frame
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
	 * @param wb workbook into which to write the edges 
	 */
	private void writeEdges(WritableWorkbook wb) {
		
		WritableSheet sheet = null; 
		if(roi_mode.getValue() != 0)
			sheet = XLSUtil.createNewPage(wb, "Edge ROI Length Mode=0");
		
		String roiLengthSheetName = String.format("Edge ROI Length Mode=%d",
				roi_mode.getValue());
		WritableSheet roi_sheet = XLSUtil.createNewPage(wb, roiLengthSheetName);
		
		String roiAreaSheetName = String.format("Edge ROI Area Mode=%d",
				roi_mode.getValue());
		WritableSheet area_sheet = XLSUtil.createNewPage(wb, roiAreaSheetName);
		
		String intensitySheetName = String.format("Edge %s Intensity Top=%.2f",
				summary_type.getValue().getDescription(),
				top_percent.getValue());
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
					
					if(roi_mode.getValue() != 0)
						XLSUtil.setCellString(sheet, col_no, row_no, header);
					
					XLSUtil.setCellString(roi_sheet, col_no, row_no, header);
					XLSUtil.setCellString(area_sheet, col_no, row_no, header);
					XLSUtil.setCellString(intensitySheet, col_no, row_no, header);

					//For every row write the length of a tagged edge
					//leave an empty row for a time point where the edge is not present
					row_no = tStart + 1;
					
					if(roi_mode.getValue() != 0){
						Geometry edgeBuffer = edge.getGeometry().buffer(envelope_buffer.getValue());
						XLSUtil.setCellNumber(sheet, col_no, row_no, edgeBuffer.getLength());
					}
					
					Geometry edgeRoi = measurement_geometries.get(edge);
					XLSUtil.setCellNumber(roi_sheet, col_no, row_no, edgeRoi.getLength());
					
					XLSUtil.setCellNumber(area_sheet, col_no, row_no, edgeRoi.getArea());
					
					double meanIntensity = computeIntensity(edge);
					XLSUtil.setCellNumber(intensitySheet, col_no, row_no, meanIntensity);
					
					//Fill all linked time points
					Edge next = edge;
					while(next.hasNext()){
						
						next = next.getNext();
						row_no = next.getFrame().getFrameNo() + 1;
						
						if(roi_mode.getValue() != 0){
							Geometry nextBuffer = next.getGeometry().buffer(envelope_buffer.getValue());
							XLSUtil.setCellNumber(sheet, col_no, row_no, nextBuffer.getLength());
						}
						
						Geometry nextRoi = measurement_geometries.get(next);
						XLSUtil.setCellNumber(roi_sheet, col_no, row_no, nextRoi.getLength());
						XLSUtil.setCellNumber(area_sheet, col_no, row_no, nextRoi.getArea());

						meanIntensity = computeIntensity(next);
						XLSUtil.setCellNumber(intensitySheet, col_no, row_no, meanIntensity);
						
					}

					//update counter
					col_no++;
					
				}
			}
		}
	}
	
	/**
	 * Compute mean intensity underlying the edge envelope
	 * 
	 * @param edge fow which to compute the intensity
	 * @return mean intensity underlying the envelope
	 */
	private double computeIntensity(Edge edge){
		
		Geometry envelope = measurement_geometries.get(edge);
		
		ShapeRoi edgeEnvelopeRoi = new ShapeRoi(writer.toShape(envelope));
		
		int z=0;
		int t=edge.getFrame().getFrameNo();
		int c=intensity_channel.getValue();
		
		double envelopeMeanIntenisty = 0;
		try{
			envelopeMeanIntenisty = IntensityReader.measureRoiIntensity(
				sequence, edgeEnvelopeRoi, z, t, c, summary_type.getValue(),
				top_percent.getValue(),add_roi.getValue());
			
			if(java.lang.Double.isNaN(envelopeMeanIntenisty)){
				envelopeMeanIntenisty = -1;
				System.out.printf(
						"Edge intensity @[%.0f,%.0f,%d] N.A.(-1) because area too small: %.2f\n",
						edge.getGeometry().getCentroid().getX(),
						edge.getGeometry().getCentroid().getY(),
						t,
						envelope.getArea());
			}
		}
		catch(java.lang.UnsupportedOperationException e){
			System.out.printf(
					"Edge intensity @[%.0f,%.0f,%d] N.A.(-1) because area too small: %.2f\n",
					edge.getGeometry().getCentroid().getX(),
					edge.getGeometry().getCentroid().getY(),
					t,
					envelope.getArea());
			envelopeMeanIntenisty = -1;
		}
		
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

	@Override
	public void variableChanged(EzVar<Integer> source, Integer newValue) {
		
		for(Edge e: measurement_geometries.keySet())
			for(int i=0; i<stGraph.size(); i++)
				if(stGraph.getFrame(i).containsEdge(e))
					measurement_geometries.put(e, 
							computeMeasurementGeometry(e,stGraph.getFrame(i)));
		
		painterChanged();
		
	}

}
