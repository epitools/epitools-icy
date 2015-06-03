package plugins.davhelle.cellgraph.overlays;

import icy.roi.ROIUtil;
import icy.sequence.Sequence;
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

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.algorithm.MinimumBoundingCircle;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import jxl.write.WritableSheet;
import plugins.adufour.ezplug.EzPlug;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.misc.PolygonalCellTileGenerator;
import plugins.davhelle.cellgraph.misc.ShapeRoi;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * EdgeOrientation overlay measures the orientation of the edges
 * and the orientation of the cell as well as the divisions
 * 
 * @author Davide Heller
 *
 */
public class EdgeOrientationOverlay extends StGraphOverlay {

	public static final String DESCRIPTION = "compute edge orientation based on" +
			"MinimumBoundingCircle method from JTS library";

	/**
	 * JTS to AWT shape writer
	 */
	private ShapeWriter writer;
	
	private double bufferWidth;
	private int channelNumber;
	private Sequence sequence;
	
	private HashMap<Node,PolygonalCellTile> tiles;
	private HashMap<Edge,Line2D.Double> edgeOrientations;
	private HashMap<Edge,Shape> edgeShapes; 
	private Map<Node, Line2D.Double> fittedEllipses;
	
	public EdgeOrientationOverlay(SpatioTemporalGraph stGraph, Sequence sequence, EzPlug plugin) {
		super("Edge Orientation", stGraph);
		this.sequence = sequence;
		this.tiles = PolygonalCellTileGenerator.createPolygonalTiles(stGraph,plugin);
		EllipseFitGenerator efg = new EllipseFitGenerator(stGraph,sequence.getWidth(),sequence.getHeight());
		this.fittedEllipses = efg.getLongestAxes();
		this.edgeOrientations = computeEdgeOrientations();
		
		this.bufferWidth = 3;
		this.writer = new ShapeWriter();
		this.edgeShapes = new HashMap<Edge, Shape>();
		computeEdgeShapes();
		initializeTrackingIds();

		
		
		
	}
	
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
	
	private void computeEdgeShapes() {
		
		for(Edge e: stGraph.getFrame(0).edgeSet()){
			Geometry g = e.getGeometry();
			Shape s = writer.toShape(g.buffer(bufferWidth));
			edgeShapes.put(e, s);
		}
	}

	private HashMap<Edge, Line2D.Double> computeEdgeOrientations(){
		HashMap<Edge,Line2D.Double> edgeOrientations = new HashMap<Edge, Line2D.Double>();
		
		for(Edge e: super.stGraph.getFrame(0).edgeSet()){
			
			MinimumBoundingCircle mbc = new MinimumBoundingCircle(e.getGeometry());
			Coordinate[] edge_vertices = mbc.getExtremalPoints();
			
			Coordinate p0 = edge_vertices[0];
			Coordinate p1 = edge_vertices[1];
			double edge_orientation = Angle.angle(p0, p1);
			
			e.setValue(edge_orientation);
			edgeOrientations.put(e, new Line2D.Double(p0.x, p0.y, p1.x, p1.y));
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
		
		Geometry edge_geo = e.getGeometry();
		
		Geometry edge_buffer = edge_geo.buffer(bufferWidth);
		
		Shape egde_shape = writer.toShape(edge_buffer);
		
		//TODO possibly add a direct ROI field to edge class
		ShapeRoi edge_roi = null;
		try{
			edge_roi = new ShapeRoi(egde_shape);
		}catch(Exception ex){
			Point centroid = e.getGeometry().getCentroid();
			System.out.printf("Problems at %.2f %.2f",centroid.getX(),centroid.getY());
			return 0.0;
		}
		
		int z=0;
		int t=frame_i.getFrameNo();
		int c=channelNumber;
		
		//TODO possibly use getIntensityInfo here
		double mean_intensity = 
				ROIUtil.getMeanIntensity(
						sequence,
						edge_roi,
						z, t, c);

		e.setValue(mean_intensity);
		
		return mean_intensity;
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		int fontSize = 2;
		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
		
		//Draw orientation lines
		
		for(Edge e: frame_i.edgeSet()){
			Line2D.Double line1 = edgeOrientations.get(e);

			g.setColor(Color.red);
			g.draw(line1);
			
			g.setColor(new Color(1.0f, 0.0f, 0.0f, 0.5f));
			g.fill(edgeShapes.get(e));
		}
		
		
		for(Node n: frame_i.vertexSet()){
			Line2D.Double line2 = fittedEllipses.get(n);

			g.setColor(Color.green);
			g.draw(line2);

		}
		
		//add labels in foreground
		
		for(Edge e: frame_i.edgeSet()){
			Line2D.Double line1 = edgeOrientations.get(e);
			Geometry geometry = e.getGeometry();
			
			int x = (int)geometry.getCentroid().getX();
			int y = (int)geometry.getCentroid().getY();
			String str = String.format("[e%d:%.0f]",e.getTrackId(),computeAngle(line1));
			
			drawTextWithBackground(g, x, y, str, Color.red, Color.black);
		}
		
		for(Node n: frame_i.vertexSet()){
			Line2D.Double line2 = fittedEllipses.get(n);
			
			int x = (int)n.getCentroid().getX();
			int y = (int)n.getCentroid().getY();
			String str = String.format("[c%d:%.0f]",n.getTrackID(),computeAngle(line2));
			
			drawTextWithBackground(g, x, y, str, Color.green, Color.black);
		}
		
	}

	/**
	 * @param g
	 * @param fm
	 * @param x
	 * @param y
	 * @param str
	 * @param bgColor
	 * @param textColor
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
		// cell number, Cell Size (area), Orientation (relative to x-axis in degrees), edge number, 
		// edge intensity, edge orientation (relative to x-axis in degree)

		
	}

}
