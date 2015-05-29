package plugins.davhelle.cellgraph.overlays;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;

import jxl.write.WritableSheet;
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
	
	/**
	 * Description string for GUI use
	 */
	public static final String DESCRIPTION = 
			"Overlay to color cells according to their area size in a gradient fashion. " +
			"Adjust the color scheme in the OptionPanel of the Layer menu.";
	
	/**
	 * Overlay constructor that takes a value from the calling GUI to 
	 * scale the color gradient dynamically from the OptionPanel
	 * 
	 * @param spatioTemporalGraph
	 */
	public AreaGradientOverlay(SpatioTemporalGraph spatioTemporalGraph){
		super("Cell area",spatioTemporalGraph);
	
		//define the gradient color scheme
		double min_area = Double.MAX_VALUE;
		double max_area = Double.MIN_VALUE;
		
		for(int i=0; i < stGraph.size(); i++){
			for(Node node: stGraph.getFrame(i).vertexSet()){
				double node_area = node.getGeometry().getArea();
				if( node_area > max_area)
					max_area = node_area;
				if( node_area < min_area)
					min_area = node_area;
			}
		}
		
		super.setMaximumGradient(max_area);
		super.setMinimumGradient(min_area);
		
		//default blue -> red color scheme
		super.setScaleGradient(0.5);
		super.setShiftGradient(0.5);
	
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		for(Node cell: frame_i.vertexSet()){
			
			double cell_area = cell.getGeometry().getArea();
			
			double h = 100;
			if(cell_area < super.getMinimumGradient())
				h = 0.0;
			else if(cell_area > super.getMaximumGradient())
				h = 1.0;
			else
				h = (cell_area - super.getMinimumGradient())/(super.getMaximumGradient() - super.getMinimumGradient());
			
			//define the HUE color
			h = h * super.getScaleGradient() + super.getShiftGradient();
			
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

	@Override
	public void specifyLegend(Graphics2D g, java.awt.geom.Line2D.Double line) {
		
		int bin_no = 50;
		
		String min_value = String.format("%.1f",super.getMinimumGradient());
		String max_value = String.format("%.1f",super.getMaximumGradient());
		
		OverlayUtils.gradientColorLegend_ZeroOne(g, line, min_value, max_value, 
				bin_no,super.getScaleGradient(),super.getShiftGradient());
		
	}
}
