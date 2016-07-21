package plugins.davhelle.cellgraph.overlays;

import icy.util.XLSUtil;

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
			"Overlay to color cells according to their area size in a<br/>" +
			"gradient fashion.<br/><br/>" +
			"NOTE: Color scheme can be changed in the OptionPanel<br/>" +
			"of the Layer menu.";
	
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
		
		super.setGradientMaximum(max_area);
		super.setGradientMinimum(min_area);
		
		//default blue -> red color scheme
		super.setGradientScale(0.5);
		super.setGradientShift(0.5);
	
		super.setGradientControlsVisibility(true);
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		for(Node cell: frame_i.vertexSet()){
			
			double cell_area = cell.getGeometry().getArea();
			
			g.setColor(super.getScaledColor(cell_area));
			
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
		
		String min_value = String.format("%.1f",super.getGradientMinimum());
		String max_value = String.format("%.1f",super.getGradientMaximum());
		
		OverlayUtils.gradientColorLegend_ZeroOne(g, line, min_value, max_value, 
				bin_no,super.getGradientScale(),super.getGradientShift());
		
	}
}
