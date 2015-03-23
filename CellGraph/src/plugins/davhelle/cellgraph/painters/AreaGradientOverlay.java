/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;

import jxl.write.WritableSheet;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Overlay to highlight the area of cells in a gradient fashion  
 * 
 * @author Davide Heller
 *
 */
public class AreaGradientOverlay extends StGraphOverlay{
	
	public static final String DESCRIPTION = 
			"Overlay to color cells according to their area size in a gradient fashion";
	/**
	 * minimum and maximum area values to fix the gradient extremes
	 */
	private double min_area;
	private double max_area;
	
	
	/**
	 * HUE scaling factor of the gradient, dynamically passed from CellOverlay  
	 */
	private EzVarDouble gradientScalingFactor;
	
	/**
	 * Overlay constructor that takes a value from the calling GUI to 
	 * scale the color gradient dynamically
	 * 
	 * @param spatioTemporalGraph
	 * @param gradientScalingFactor
	 */
	public AreaGradientOverlay(SpatioTemporalGraph spatioTemporalGraph, EzVarDouble gradientScalingFactor){
		super("Cell area",spatioTemporalGraph);
		this.gradientScalingFactor = gradientScalingFactor; 
	
		min_area = Double.MAX_VALUE;
		max_area = Double.MIN_VALUE;
		
		for(int i=0; i < stGraph.size(); i++){
			for(Node node: stGraph.getFrame(i).vertexSet()){
				double node_area = node.getGeometry().getArea();
				if( node_area > max_area)
					max_area = node_area;
				if( node_area < min_area)
					min_area = node_area;
			}
		}
	
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		for(Node cell: frame_i.vertexSet()){
			
			double cell_area = cell.getGeometry().getArea();
			
			double h = (cell_area - min_area)/max_area;
			
			//adapt for certain color range
			//by multiplying with factor
			
			h = h * gradientScalingFactor.getValue();
			
			//revert
			//h = Math.abs(h - range_factor);
			
			Color hsbColor = Color.getHSBColor(
					(float)(h),
					1f,
					1f);
			
			g.setColor(hsbColor);
			g.fill((cell.toShape()));
		}
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Cell area");

		int row_no = 1;
		for(Node node: frame.vertexSet()){
			XLSUtil.setCellNumber(sheet, 0, row_no, node.getTrackID());
			XLSUtil.setCellNumber(sheet, 1, row_no, node.getGeometry().getArea());
			row_no++;
		}
	}
}
