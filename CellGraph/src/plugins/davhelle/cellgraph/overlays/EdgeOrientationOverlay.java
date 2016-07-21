package plugins.davhelle.cellgraph.overlays;

import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import plugins.kernel.roi.roi2d.ROI2DArea;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;
import icy.util.XLSUtil;
import ij.process.EllipseFitter;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import jxl.write.WritableSheet;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarListener;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.IntensityReader;
import plugins.davhelle.cellgraph.io.IntensitySummaryType;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.misc.PolygonalCellTileGenerator;
import plugins.davhelle.cellgraph.misc.ShapeRoi;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.algorithm.MinimumBoundingCircle;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * EdgeOrientation overlay measures the orientation of the edges
 * and the orientation of the cells and additionally retrieves
 * the underlying intensity using a buffer geometry of the edge.
 * <br><br>
 * Custom overlay for experiment, main output is excel sheet.
 * 
 * @author Davide Heller
 *
 */
public class EdgeOrientationOverlay extends StGraphOverlay implements EzVarListener<Double> {

	public static final String DESCRIPTION = 
			"Computes edge orientation based on MinimumBoundingCircle<br/>" +
			"(method from JTS library); Currently only working on single<br/>" +
			"time points.";

	/**
	 * JTS to AWT shape writer
	 */
	private ShapeWriter writer;
	
	/**
	 * Width of the buffer geometry to retrieve intensity, can be interactively changed from EzGUI 
	 */
	private EzVarDouble bufferWidth;
	
	/**
	 * Channel from which to measure the intensity 
	 */
	private int channelNumber;
	
	/**
	 * Sequence used for the ellipse fit generation 
	 */
	private Sequence sequence;
	
	//private HashMap<Node,PolygonalCellTile> tiles;
	/**
	 * Container for 2D line representing the orientation of edges
	 */
	private HashMap<Edge,Line2D.Double> edgeOrientations;
	
	/**
	 * Container for containing the buffer shape for the edge measurement
	 */
	private HashMap<Edge,ROI> edgeROIs; 
	
	/**
	 * Container for containing the buffer shape for the edge measurement
	 */
	private HashMap<Edge,Shape> edgeShapes; 
	
	/**
	 * Container for 2D line representing the orientation of cells 
	 */
	private Map<Node, Line2D.Double> cellOrientation;

	private ROI2DArea nanAreaRoi;
	
	/**
	 * Intensity Summary type
	 */
	EzVarEnum<IntensitySummaryType> summary_type;

	private Map<Node, EllipseFitter> cell_ellipses;
	
	/**
	 * @param stGraph graph to be analyzed
	 * @param sequence sequence connected to the overlay
	 * @param plugin Ezplugin from which the overlay was launced
	 * @param buffer Width for the edge geometry used to measure the intensity
	 * @param varIntensityMeasure_EO 
	 */
	public EdgeOrientationOverlay(SpatioTemporalGraph stGraph,
			Sequence sequence, EzPlug plugin, EzVarDouble buffer,
			EzVarEnum<IntensitySummaryType> varIntensityMeasure_EO) {
		
		super("Edge Orientation", stGraph);
		this.sequence = sequence;
		this.bufferWidth = buffer;
		this.channelNumber = 0;
		this.summary_type = varIntensityMeasure_EO;
				
		PolygonalCellTileGenerator.createPolygonalTiles(stGraph,plugin);
		EllipseFitGenerator efg = new EllipseFitGenerator(stGraph,
				sequence.getWidth(),sequence.getHeight());
		this.cellOrientation = efg.getLongestAxes();
		this.cell_ellipses = efg.getFittedEllipses();
		
		this.edgeOrientations = computeEdgeOrientations();
		
		computeNanAreaROI(sequence);
		
		bufferWidth.addVarChangeListener(this);
		this.writer = new ShapeWriter();
		this.edgeROIs = new HashMap<Edge, ROI>();
		this.edgeShapes = new HashMap<Edge, Shape>();
		computeEdgeShapes();
		initializeTrackingIds();
		
	}

	/**
	 * @param sequence
	 */
	private void computeNanAreaROI(Sequence sequence) {
		//from icy's tutorials

		// consider first image and first channel only here
		double[] doubleArray = Array1DUtil.arrayToDoubleArray(
		     sequence.getDataXY(0, 0, 0), sequence.isSignedDataType());
		boolean[] mask = new boolean[doubleArray.length];
		
		double threshold = 0.0;
		
		for (int i = 0; i < doubleArray.length; i++)
		     mask[i] = !(doubleArray[i] > threshold);
		BooleanMask2D mask2d = new BooleanMask2D(sequence.getBounds2D(), mask); 
		
		nanAreaRoi = new ROI2DArea(mask2d);
	}
	
	/**
	 * initializes the tracking ids of both nodes and edges,
	 * i.e. increasing integers for nodes and long cantor pairing of vertices for edges
	 */
	private void initializeTrackingIds(){
		FrameGraph frame = stGraph.getFrame(0);
		
		if(!stGraph.hasTracking()){
			int cell_id = 0;
			for(Node n: frame.vertexSet())
				n.setTrackID(cell_id++);
		}
		
		for(Edge e: frame.edgeSet()){
			e.setTrackId(e.getPairCode(frame));
		}
	}
	
	/**
	 * computes the buffer geometries for all edges. See {@link Geometry} for specific buffer computation
	 */
	private void computeEdgeShapes() {
		
		for(Edge e: stGraph.getFrame(0).edgeSet()){
			Geometry g = e.getGeometry();
			Geometry buffer = g.buffer(bufferWidth.getValue());
			Shape s = writer.toShape(buffer);
			
			edgeShapes.put(e, s);
			
			//TODO possibly add a direct ROI field to edge class
			ShapeRoi edge_roi = null;
			try{
				edge_roi = new ShapeRoi(s);
			}catch(Exception ex){
				Point centroid = e.getGeometry().getCentroid();
				System.out.printf("Problems at %.2f %.2f",centroid.getX(),centroid.getY());
			}
			
			ROI edge_wo_nan = null;
			try{
				edge_wo_nan = ROIUtil.subtract(edge_roi, nanAreaRoi);
			}catch(UnsupportedOperationException ex){
				Point centroid = e.getGeometry().getCentroid();
	
				System.out.printf("Problems at %.2f %.2f: EdgeRoi area %.2f but intersection not available\n",
						centroid.getX(),centroid.getY(),
						buffer.getArea());
			}
			
			edgeROIs.put(e, edge_wo_nan);
		}
	}

	/**
	 * Computes the Orientation of edges using the {@link MinimumBoundingCircle} class from JTS.
	 * Edges orientation is approximated as the line connecting the two extreme points of the edge.
	 * 
	 * @return a map containing a 2D line for each edge representing it's orientation
	 */
	private HashMap<Edge, Line2D.Double> computeEdgeOrientations(){
		HashMap<Edge,Line2D.Double> edgeOrientations = new HashMap<Edge, Line2D.Double>();
		
		for(Edge e: super.stGraph.getFrame(0).edgeSet()){
			
			MinimumBoundingCircle mbc = new MinimumBoundingCircle(e.getGeometry());
			Coordinate[] edge_vertices = mbc.getExtremalPoints();
			
			if(edge_vertices.length > 1){
				Coordinate p0 = edge_vertices[0];
				Coordinate p1 = edge_vertices[1];
				double edge_orientation = Angle.angle(p0, p1);

				e.setValue(edge_orientation);
				edgeOrientations.put(e, new Line2D.Double(p0.x, p0.y, p1.x, p1.y));
			} else { 
				Coordinate p0 = edge_vertices[0];
				Coordinate p1 = new Coordinate(p0.x + 1, p0.y + 1);
				double edge_orientation = Angle.angle(p0, p1);

				e.setValue(edge_orientation);
				edgeOrientations.put(e, new Line2D.Double(p0.x, p0.y, p1.x, p1.y));
			}
				
			
		}
		
		return edgeOrientations;
	}
	
	/**
	 * Compute underlying intensity for edge using a ROI corresponding
	 * to the edge envelope (specified by bufferWidth)
	 * 
	 * @param e edge to measure
	 * @param frame_i frame to which the edge belongs
	 * @return mean intensity value of pixels within the edge envelope
	 */
	private double computeEdgeIntensity(Edge e, FrameGraph frame_i){
		
		ROI edge_wo_nan = edgeROIs.get(e);
		
		if(edge_wo_nan == null){
			Point centroid = e.getGeometry().getCentroid();
			System.out.printf("Could not compute intensity for edge @[%.2f,%.2f]\n",
					centroid.getX(),centroid.getY());
			return -1;
		}
		
		int z=0;
		int t=frame_i.getFrameNo();
		int c=channelNumber;
		
		//TODO possibly use getIntensityInfo here
		double mean_intensity = 
				IntensityReader.measureRoiIntensity(
						sequence, edge_wo_nan, z, t, c, summary_type.getValue());

		return mean_intensity;
		
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		if(frame_i.getFrameNo() != 0)
			return;
		
		int fontSize = 2;
		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
		
		//Draw orientation lines
		
		for(Edge e: frame_i.edgeSet()){
			Line2D.Double line1 = edgeOrientations.get(e);

			g.setColor(Color.red);
			g.draw(line1);
			
			g.setColor(new Color(1.0f, 0.0f, 0.0f, 0.5f));
			g.fill(edgeShapes.get(e));
			//sequence.addROI(edgeROIs.get(e)); //debug only
		}
		
		
		for(Node n: frame_i.vertexSet()){
			Line2D.Double line2 = cellOrientation.get(n);

			g.setColor(Color.green);
			g.draw(line2);

		}
		
		//add labels in foreground
		
		for(Edge e: frame_i.edgeSet()){
			Line2D.Double line1 = edgeOrientations.get(e);
			Geometry geometry = e.getGeometry();
			
			int x = (int)geometry.getCentroid().getX();
			int y = (int)geometry.getCentroid().getY();
			String str = String.format("[e%d:%.0f\u00b0]",e.getTrackId(),computeAngle(line1));
			
			drawTextWithBackground(g, x, y, str, Color.red, Color.black);
		}
		
		for(Node n: frame_i.vertexSet()){
			Line2D.Double line2 = cellOrientation.get(n);
			
			int x = (int)n.getCentroid().getX();
			int y = (int)n.getCentroid().getY();
			String str = String.format("[c%d:%.0f\u00b0]",n.getTrackID(),computeAngle(line2));
			
			drawTextWithBackground(g, x, y, str, Color.green, Color.black);
		}
		
	}

	/**
	 * draws a label with a background color 
	 * 
	 * @param g graphics handle
	 * @param x x coordinate to start drawing
	 * @param y y coordinate to start drawing
	 * @param str string to print in foreground color
	 * @param bgColor background color
	 * @param textColor color of the text
	 */
	private void drawTextWithBackground(Graphics2D g, int x,
			int y, String str, Color bgColor, Color textColor) {
		
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D rect = fm.getStringBounds(str, g);
		
		g.setColor(bgColor);
		g.fillRect(x,
		           y - fm.getAscent(),
		           (int) rect.getWidth(),
		           (int) rect.getHeight());
		g.setColor(textColor);
		g.drawString(str, x, y);
	}

	/**
	 * Computes the angle of a 2D lines in degrees
	 * @param line1 line to compute the angle for
	 * @return angle of input line in degrees
	 */
	private double computeAngle(Line2D.Double line) {
		
		double angle = Math.PI;
		
		if(line.x1 > line.x2)
			angle = Math.atan2(line.y1 - line.y2, line.x1 - line.x2);
		else
			angle = Math.atan2(line.y2 - line.y1, line.x2 - line.x1);
		
		//convert to degrees and invert axis (i.e. positive anti-clock-wise)
		angle = Angle.toDegrees(angle) * - 1;
		
		return angle;
	}

	@Override
	public void specifyLegend(Graphics2D g, Line2D.Double line) {
		// TODO Auto-generated method stub
		
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		if(frame.getFrameNo() != 0)
			return;

		int c = 0;
		int r = 0;
		XLSUtil.setCellString(sheet, c++, r, "Cell number");
		XLSUtil.setCellString(sheet, c++, r, "Cell Size (area)");
		XLSUtil.setCellString(sheet, c++, r, "Cell Orientation");
		XLSUtil.setCellString(sheet, c++, r, "Cell Long Axis (length)");
		XLSUtil.setCellString(sheet, c++, r, "Cell Short Axis (length)");
		
		XLSUtil.setCellString(sheet, c++, r, "Edge number");
		XLSUtil.setCellString(sheet, c++, r, "Edge Size (length)");
		XLSUtil.setCellString(sheet, c++, r, String.format(
				"Edge intensity (%s)",summary_type.getValue().getDescription()));
		XLSUtil.setCellString(sheet, c++, r, "Edge orientation");
		
		
		for(Node n: frame.vertexSet()){
			for(Node neighbor: n.getNeighbors()){
				//reset column and increment row
				c=0;
				r++;
				
				//write cell statistics
				XLSUtil.setCellNumber(sheet, c++, r, n.getTrackID());
				XLSUtil.setCellNumber(sheet, c++, r, n.getGeometry().getArea());
				double cell_orientation = computeAngle(cellOrientation.get(n));
				//XLSUtil.setCellString(sheet, c++, r, String.format("%.2f",cell_orientation));
				XLSUtil.setCellNumber(sheet, c++, r, cell_orientation);
				
				double long_axis = this.cell_ellipses.get(n).major;
				double short_axis = this.cell_ellipses.get(n).minor;
				XLSUtil.setCellNumber(sheet, c++, r, long_axis);
				XLSUtil.setCellNumber(sheet, c++, r, short_axis);
				
				//write edge statistics
				Edge e = frame.getEdge(n, neighbor);
				XLSUtil.setCellNumber(sheet, c++, r, e.getTrackId());
				XLSUtil.setCellNumber(sheet, c++, r, frame.getEdgeWeight(e));
				XLSUtil.setCellNumber(sheet, c++, r, computeEdgeIntensity(e, frame));
				double edge_orientation = computeAngle(edgeOrientations.get(e));
				//XLSUtil.setCellString(sheet, c++, r, String.format("%.2f",edge_orientation));
				XLSUtil.setCellNumber(sheet, c++, r, edge_orientation);
				
			}
		}
	}

	@Override
	public void variableChanged(EzVar<Double> source, Double newValue) {
		computeEdgeShapes();
		painterChanged();
	}

}
