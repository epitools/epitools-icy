package plugins.davhelle.cellgraph.overlays;

import icy.sequence.Sequence;
import icy.util.XLSUtil;
import ij.process.EllipseFitter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Overlay to show the elongation ratio (major/minor axis of estimated elilpse) of every cell.
 * 
 * @author Davide Heller
 *
 */
public class ElongationRatioOverlay extends StGraphOverlay{

	/**
	 * Description string for GUI
	 */
	public static final String DESCRIPTION = "Color codes the cell according to their elongation ratio and "+
			"writes the elongation factor within every cell";
	/**
	 * Elongation ratio for each node
	 */
	private Map<Node, java.lang.Double> elongationRatios;
	
	/**
	 * Maximum elongation ratio found
	 */
	private double min;
	/**
	 * Minimal elongation ratio found
	 */
	private double max;

	/**
	 * @param spatioTemporalGraph graph to be analyzed
	 * @param sequence image to be used for ellipse estimation
	 */
	public ElongationRatioOverlay(SpatioTemporalGraph spatioTemporalGraph, Sequence sequence) {
		super("EllipseRatio Coloring",spatioTemporalGraph);

		Map<Node, EllipseFitter> fittedEllipses = new EllipseFitGenerator(stGraph,sequence).getFittedEllipses();
	
		this.min = java.lang.Double.MAX_VALUE;
		this.max = java.lang.Double.MIN_VALUE;
		
		this.elongationRatios = new HashMap<Node, java.lang.Double>();
		
		for(Node n: fittedEllipses.keySet()){
			EllipseFitter ef = fittedEllipses.get(n);
			double elongation_ratio = ef.major/ef.minor;
			
			elongationRatios.put(n, elongation_ratio);

			if(elongation_ratio > max)
				max = elongation_ratio;
			else if(elongation_ratio < min)
				min = elongation_ratio;
		}

	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i)
	{

		int fontSize = 3;
		boolean show_only_divsion=false;

		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));

		for(Node n: frame_i.vertexSet()){

			if(show_only_divsion)
				if(!n.hasObservedDivision())
					continue;

			double elongation_ratio = elongationRatios.get(n);

			double normalized_ratio = (elongation_ratio - min)/max;

			Color hsbColor = Color.getHSBColor(
					(float)(normalized_ratio*0.9 + 0.4),
					1f,
					1f);

			g.setColor(hsbColor);

			if(show_only_divsion)
				g.draw((n.toShape()));
			else
				g.fill((n.toShape()));

			g.setColor(Color.black);
			g.drawString(String.format("%.1f",
					elongation_ratio), 
					(float)n.getCentroid().getX(), 
					(float)n.getCentroid().getY());
		}

	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 2, 0, "Centroid y");
		XLSUtil.setCellString(sheet, 3, 0, "Elongation Ratio");

		int row_no = 1;

		for(Node n: frame.vertexSet()){
			if(elongationRatios.containsKey(n)){
				
				double elongation_ratio = elongationRatios.get(n);
			
				XLSUtil.setCellNumber(sheet, 0, row_no, n.getTrackID());
				XLSUtil.setCellNumber(sheet, 1, row_no, n.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 2, row_no, n.getCentroid().getY());
				XLSUtil.setCellNumber(sheet, 3, row_no, elongation_ratio);
				
				row_no++;

			}
		}
		
	}

	@Override
	public void specifyLegend(Graphics2D g, java.awt.geom.Line2D.Double line) {
		
		int binNo = 50;
		double scaling_factor = 0.9;
		double shift_factor = 0.4;
		
		OverlayUtils.gradientColorLegend_1Digit(g, line, min, max, binNo,
				scaling_factor, shift_factor);
	}
}