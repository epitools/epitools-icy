/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * AreaDifferencePainter computes and depicts the difference between 
 * cell areas and the correspondent Voronoi cell areas. 
 * 
 * Currently the cells with a positive difference are shaded in green 
 * (i.e. bigger than voronoi) and the ones with a negative difference
 * in red. Neutral (+/- THRESHOLD) are left white. 
 * 
 *  TODO Bug in the double int conversion, check area computation as well
 * 
 * @author Davide Heller
 *
 */
public class VoronoiAreaDifferenceOverlay extends StGraphOverlay{
	
	private final int DIFFERENCE_THRESHOLD = 10;
	
	private Map<Node, Double> area_difference_map;
	private Map<Node, Color> color_map;

	private double color_amplification;
	private int color_scheme;
	
	public VoronoiAreaDifferenceOverlay(SpatioTemporalGraph stGraph, Map<Node,Double> area_difference_map) {
		super("Voronoi Area Difference",stGraph);
		this.area_difference_map = area_difference_map;

		this.color_scheme = 2;
		this.color_amplification = 5;
		
		this.color_map = new HashMap<Node, Color>();
		defineColorMap();
		
	}

	//Color scheme generation
	private void defineColorMap(){
		
		//for every frame and node
		for(int i=0; i<stGraph.size(); i++){

			for(Node cell: stGraph.getFrame(i).vertexSet()){
				
				if(area_difference_map.containsKey(cell)){

					double area_difference = area_difference_map.get(cell);

					if(color_scheme == 1){			
						//Color cells according to threshold into three categories
						double area_threshold = DIFFERENCE_THRESHOLD;
						if(area_difference > area_threshold)
							color_map.put(cell,Color.GREEN);
						else if(area_difference < area_threshold*(-1))
							color_map.put(cell,Color.RED);
						else
							color_map.put(cell,Color.WHITE);
					}
					else{		
						//color scheme which allows +/- 255 differences (255 being the MAX_DIFF)
						//magenta (negative diff.) to white (neutral) to light blue (positive)
						int intensity = 255 - (int)Math.min(
								color_amplification*Math.abs(area_difference), 255);
						if(area_difference > 0)
							color_map.put(cell,new Color(intensity,255,255));
						else
							color_map.put(cell,new Color(255,intensity,255));
					}
				}
			}
		}
	}
	
	@Override
    public void paintFrame(Graphics2D g, FrameGraph frame_i)
    {
		for(Node cell: frame_i.vertexSet()){
			if(!cell.onBoundary()){
				if(color_map.containsKey(cell)){
					g.setColor(color_map.get(cell));
					g.fill(cell.toShape());
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
}

