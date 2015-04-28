/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Line2D.Double;

import com.vividsolutions.jts.io.WKTWriter;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.util.XLSUtil;
import ij.process.EllipseFitter;

/**
 * PolygonPainter depicts Polygons detected by JTS Polygonizer
 * 
 * @author Davide Heller
 *
 */
public class PolygonOverlay extends StGraphOverlay{
	
	public static final String DESCRIPTION = 
			"Simple Overlay to show cells and their outlines in a color of choice";
	
	private Color painter_color;
	
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
		// TODO Auto-generated method stub
		
	}
	
	
}
