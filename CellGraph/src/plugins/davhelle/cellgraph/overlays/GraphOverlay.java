package plugins.davhelle.cellgraph.overlays;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to visualize all the edges of a 
 * spatial-temporal graph as overlay in icy.
 * 
 * @author Davide Heller
 *
 */
public class GraphOverlay extends StGraphOverlay{

	/**
	 * Description string for GUI
	 */
	public static final String DESCRIPTION = 
			"Shows the connectivity (neighbors) of each cell;<br/><br/>" +
			"The XLS export [Layer Option Menu] contains the <br/>" +
			"vertex ids for every tracked edge in the graph.";
	
	/**
	 * JTS Geometry factory to generate segments to visualize connectivity
	 */
	private GeometryFactory factory;
	/**
	 * JTS to AWT shape converter
	 */
	private ShapeWriter writer;
	
	/**
	 * @param stGraph graph to analyze
	 */
	public GraphOverlay(SpatioTemporalGraph stGraph) {
		super("Graph edges",stGraph);
		this.factory = new GeometryFactory();
		this.writer = new ShapeWriter();
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i){

		g.setColor(Color.orange);

		//paint all the edges of the graph
		for(Edge edge: frame_i.edgeSet()){

			//extract points
			Point a = frame_i.getEdgeSource(edge).getCentroid();
			Point b = frame_i.getEdgeTarget(edge).getCentroid();

			//transform to JTS coordinates
			Coordinate[] edge_vertices = {a.getCoordinate(),b.getCoordinate()};

			//define line
			LineString edge_line = factory.createLineString(edge_vertices);

			//draw line
			g.draw(writer.toShape(edge_line));
			g.draw(writer.toShape(a));

		}
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		XLSUtil.setCellString(sheet, 0, 0, "Target id");
		XLSUtil.setCellString(sheet, 1, 0, "Source id");
		XLSUtil.setCellString(sheet, 2, 0, "Edge id (Cantor pairing)");

		int row_no = 1;

		for(Edge e: frame.edgeSet()){
			
			Node a = frame.getEdgeSource(e);
			Node b = frame.getEdgeTarget(e);
			
			if(a.getTrackID() != -1 && b.getTrackID() != -1){
				
				long edge_id = e.getPairCode(frame);
				
				XLSUtil.setCellNumber(sheet, 0, row_no, a.getTrackID());
				XLSUtil.setCellNumber(sheet, 1, row_no, b.getTrackID());
				XLSUtil.setCellNumber(sheet, 2, row_no, edge_id);
				
				row_no++;

			}
		}
		
	}

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		
		String s = "Neighborhood connectivity";
		Color c = Color.YELLOW;
		int offset = 0;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);
		
	}
	
	
}
