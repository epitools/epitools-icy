package plugins.davhelle.cellgraph.overlays;

import icy.sequence.Sequence;
import icy.util.XLSUtil;
import ij.process.EllipseFitter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Double;
import java.util.Map;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Overlay to display the fitted ellipses for each polygon as segment representing the longest axis
 * 
 * @author Davide Heller
 *
 */
public class EllipseFitterOverlay extends StGraphOverlay {

	/**
	 * Description string for GUI
	 */
	public static final String DESCRIPTION = 
			"Fits an ellipse to each cell geometry and displays the<br/>" +
			" longest axis using the ImageJ(R) EllipseFitter Macro.";
	
	/**
	 * Ellipse fit for every node
	 */
	private Map<Node, EllipseFitter> fittedEllipses;
	
	/**
	 * @param spatioTemporalGraph graph to be analyzed
	 * @param sequence sequence to use for the ellipseFit (dimensions must agree with input skeletons)
	 */
	public EllipseFitterOverlay(SpatioTemporalGraph spatioTemporalGraph, Sequence sequence) {
		super("Ellipse Fitter",spatioTemporalGraph);
		fittedEllipses = new EllipseFitGenerator(stGraph,sequence.getWidth(),sequence.getHeight()).getFittedEllipses();
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		Color old = g.getColor();
		
		Color ellipseColor = Color.green;
		g.setColor(ellipseColor);
		int fontSize = 3;
		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
		
		for(Node n: frame_i.vertexSet()){
			if(fittedEllipses.containsKey(n)){
				EllipseFitter ef = fittedEllipses.get(n);
				/* 
				 * http://rsb.info.nih.gov/ij/developer/source/ij/process/EllipseFitter.java.html
				 * 
				 * ef.major: major axis 
				 * ef.minor: minor axis
				 * ef.theta:, angle of major axis, clockwise with respect to x axis
				 * 
				 * for complete ellipse drawing see
				 * Draws the ellipse on the specified image.
				 * public void drawEllipse(ImageProcessor ip) 
				 * 
				 * */
				
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
		
		g.setColor(old);
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 2, 0, "Centroid y");
		XLSUtil.setCellString(sheet, 3, 0, "Ellipse center x");
		XLSUtil.setCellString(sheet, 4, 0, "Ellipse center y");
		XLSUtil.setCellString(sheet, 5, 0, "Ellipse major axis length");
		XLSUtil.setCellString(sheet, 6, 0, "Ellipse minor axis length");
		XLSUtil.setCellString(sheet, 7, 0, "Ellipse major axis angle");

		int row_no = 1;

		for(Node n: frame.vertexSet()){
			if(fittedEllipses.containsKey(n)){
			
				EllipseFitter ef = fittedEllipses.get(n);
				
				XLSUtil.setCellNumber(sheet, 0, row_no, n.getTrackID());
				XLSUtil.setCellNumber(sheet, 1, row_no, n.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 2, row_no, n.getCentroid().getY());
				XLSUtil.setCellNumber(sheet, 3, row_no, ef.xCenter);
				XLSUtil.setCellNumber(sheet, 4, row_no, ef.yCenter);
				XLSUtil.setCellNumber(sheet, 5, row_no, ef.major);
				XLSUtil.setCellNumber(sheet, 6, row_no, ef.minor);
				XLSUtil.setCellNumber(sheet, 7, row_no, ef.angle);
				
				row_no++;

			}
		}
		
	}

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		String s = "EllipseFit: longest axis";
		Color c = Color.GREEN;
		int offset = 0;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);
		
	}

}
