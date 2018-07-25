package plugins.davhelle.cellgraph.overlays;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.Map;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * AreaDifferencePainter depicts the difference between 
 * cell areas and the correspondent Voronoi cell areas. 
 * 
 * @author Davide Heller
 *
 */
public class VoronoiAreaDifferenceOverlay extends StGraphOverlay{
	
	/**
	 * Difference between voronoi tile and orignal tile for each cell
	 */
	private Map<Node, Double> area_difference_map;
	
	public VoronoiAreaDifferenceOverlay(SpatioTemporalGraph stGraph, Map<Node,Double> area_difference_map) {
		super("Voronoi Area Difference",stGraph);
		this.area_difference_map = area_difference_map;

		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		for(Node n: area_difference_map.keySet()){
			if(!n.onBoundary()){
				double area_difference = area_difference_map.get(n);
				
				if(area_difference > max)
					max = area_difference;
				
				if(area_difference < min)
					min = area_difference;
			}
		}
		
		super.setGradientMaximum(max);
		super.setGradientMinimum(min);
		super.setGradientScale(0.6);
		super.setGradientShift(0.4);
		super.setGradientControlsVisibility(true);
	}
	
	@Override
    public void paintFrame(Graphics2D g, FrameGraph frame_i){
		
		int fontSize = 3;
		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
		
		for(Node n: frame_i.vertexSet()){
			if(!n.onBoundary()){
				if(area_difference_map.containsKey(n)){
					double area_difference = area_difference_map.get(n);
					
					g.setColor(super.getScaledColor(area_difference));
					g.fill((n.toShape()));

					g.setColor(Color.black);
					g.drawString(String.format("%.0f",
							area_difference), 
					(float)n.getCentroid().getX(), 
					(float)n.getCentroid().getY());
				}
			}
		}
    }

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Cell x");
		XLSUtil.setCellString(sheet, 2, 0, "Cell y");
		XLSUtil.setCellString(sheet, 3, 0, "Cell area");
		XLSUtil.setCellString(sheet, 4, 0, "Voronoi area difference");

		int row_no = 1;

		for(Node n: frame.vertexSet()){
			
			if(!n.onBoundary() && area_difference_map.containsKey(n)){
			
				Double voronoiAreaDifference = area_difference_map.get(n);
				
				XLSUtil.setCellNumber(sheet, 0, row_no, n.getTrackID());
				XLSUtil.setCellNumber(sheet, 1, row_no, n.getGeometry().getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 2, row_no, n.getGeometry().getCentroid().getY());
				XLSUtil.setCellNumber(sheet, 3, row_no, n.getGeometry().getArea());
				XLSUtil.setCellNumber(sheet, 4, row_no, voronoiAreaDifference);
				row_no++;
			}
		}
		
	}

	@Override
	public void specifyLegend(Graphics2D g, Line2D line) {
		
		String min_value = String.format("%.0f", super.getGradientMinimum());
		String max_value = String.format("%.0f", super.getGradientMaximum());
		
		OverlayUtils.gradientColorLegend_ZeroOne(g, line, min_value, max_value,
				50, super.getGradientScale(), super.getGradientShift());
	}
}

