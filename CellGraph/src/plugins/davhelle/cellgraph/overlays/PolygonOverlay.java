package plugins.davhelle.cellgraph.overlays;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.io.WKTWriter;

/**
 * PolygonPainter depicts polygonal geometries within the stGraph
 * 
 * @author Davide Heller
 *
 */
public class PolygonOverlay extends StGraphOverlay{
	
	/**
	 * Description string for GUI
	 */
	public static final String DESCRIPTION = 
			"Simple Overlay to show cells and their outlines<br/>" +
			" in a color of choice";
	
	/**
	 * Color for polygon outlines
	 */
	private Color painter_color;
	
	/**
	 * @param spatioTemporalGraph graph to analyze
	 * @param painter_color color to paint the polygons with
	 */
	public PolygonOverlay(SpatioTemporalGraph spatioTemporalGraph,Color painter_color){
		super("Polygons",spatioTemporalGraph);
		this.painter_color = painter_color;
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		Color old = g.getColor();
		g.setColor(painter_color);

		for(Node cell: frame_i.vertexSet())
			g.draw((cell.toShape()));

		g.setColor(old);
		
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		WKTWriter writer = new WKTWriter(2);
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 2, 0, "Centroid y");
		XLSUtil.setCellString(sheet, 3, 0, "WKT String");

		int row_no = 1;

		for(Node n: frame.vertexSet()){
			
				XLSUtil.setCellNumber(sheet, 0, row_no, n.getTrackID());
				XLSUtil.setCellNumber(sheet, 1, row_no, n.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 2, row_no, n.getCentroid().getY());
				XLSUtil.setCellString(sheet, 3, row_no, writer.write(n.getGeometry()));
				row_no++;

		}
		
	}

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		
		String s = "Cell Outlines";
		Color c = painter_color;
		int offset = 0;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);
		
	}
	
	
}
