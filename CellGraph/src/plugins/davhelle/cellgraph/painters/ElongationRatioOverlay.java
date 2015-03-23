/*=========================================================================
 *
 *  (C) Copyright (2012-2015) Basler Group, IMLS, UZH
 *  
 *  author:	Davide Heller
 *  email:	davide.heller@imls.uzh.ch
 *  
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.util.XLSUtil;
import ij.process.EllipseFitter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.HashMap;

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

	public static final String DESCRIPTION = "Color codes the cell according to their elongation ratio and "+
			"writes the elongation factor within every cell";
	private HashMap<Node, EllipseFitter> fittedEllipses;

	
	public ElongationRatioOverlay(SpatioTemporalGraph spatioTemporalGraph) {
		super("EllipseRatio Coloring",spatioTemporalGraph);

		fittedEllipses = new EllipseFitGenerator(stGraph).getFittedEllipses();
	
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i)
	{
		double[] heat_map = {0.0,0.25,0.5,0.75,1.0};

		int fontSize = 3;
		boolean show_only_divsion=false;

		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));

		for(Node n: frame_i.vertexSet()){

			if(show_only_divsion)
				if(!n.hasObservedDivision())
					continue;

			EllipseFitter ef = fittedEllipses.get(n);
			double elongation_ratio = ef.major/ef.minor;

			double normalized_ratio = (elongation_ratio - 1.0)/3.0;

			Color hsbColor = Color.getHSBColor(
					(float)(normalized_ratio*0.9 + 0.4		),
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
			if(fittedEllipses.containsKey(n)){
				
				EllipseFitter ef = fittedEllipses.get(n);
				double elongation_ratio = ef.major/ef.minor;
			
				XLSUtil.setCellNumber(sheet, 0, row_no, n.getTrackID());
				XLSUtil.setCellNumber(sheet, 1, row_no, n.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 2, row_no, n.getCentroid().getY());
				XLSUtil.setCellNumber(sheet, 3, row_no, elongation_ratio);
				
				row_no++;

			}
		}
		
	}
}