package plugins.davhelle.cellgraph.overlays;

import icy.roi.ROIUtil;
import icy.sequence.Sequence;
import ij.process.EllipseFitter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
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
	private Map<Node, EllipseFitter> fittedEllipses;
	
	public EdgeOrientationOverlay(SpatioTemporalGraph stGraph, Sequence sequence, EzPlug plugin) {
		super("Edge Orientation", stGraph);
		this.sequence = sequence;
		this.tiles = PolygonalCellTileGenerator.createPolygonalTiles(stGraph,plugin);
		this.fittedEllipses = new EllipseFitGenerator(stGraph,sequence.getWidth(),sequence.getHeight()).getFittedEllipses();
		this.edgeOrientations = computeEdgeOrientations();
		
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
		
		g.setColor(Color.red);
		
		for(Edge e: frame_i.edgeSet()){
			Line2D.Double l = edgeOrientations.get(e);
			g.draw(l);
		}
		
		g.setColor(Color.green);
		for(Node n: frame_i.vertexSet()){
			EllipseFitter ef = fittedEllipses.get(n);
			double cX = n.getGeometry().getCentroid().getX();
			double cY = n.getGeometry().getCentroid().getY();
			double length = ef.major / 2.0;
			if(length > 10)
				length -= 5;
			
			double x0 = cX - Math.cos(ef.theta) * length;
	        double y0 = cY + Math.sin(ef.theta) * length;
	        double x1 = cX + Math.cos(ef.theta) * length;
	        double y1 = cY - Math.sin(ef.theta) * length;
			
			g.draw(new Line2D.Double(x0, y0, x1, y1));
		}
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
