/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import plugins.adufour.ezplug.EzVarDouble;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Painter to highlight all cells with a greater area
 * than the given threshold.  
 * 
 * @author Davide Heller
 *
 */
public class AreaThresholdPainter extends Overlay{
	
	private SpatioTemporalGraph stGraph;
	private ArrayList<Node> toDraw;
	
	private double min_area;
	private double max_area;
	private EzVarDouble areaThreshold;
	
	public AreaThresholdPainter(SpatioTemporalGraph spatioTemporalGraph, EzVarDouble varAreaThreshold){
		super("Cells colored according to area");
		this.stGraph = spatioTemporalGraph;
		this.areaThreshold = varAreaThreshold; 
	
		toDraw = new ArrayList<Node>();
		
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
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			//TODO include 3D information!
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			
	
			for(Node cell: frame_i.vertexSet()){
				//if(toDraw.contains(cell)){
				
				double cell_area = cell.getGeometry().getArea();
				
				double h = (cell_area - min_area)/max_area;
				
				//adapt for certain color range
				//by multiplying with factor
				//double range_factor = 0.9;
				
				h = h * areaThreshold.getValue();
				
				//revert
				//h = Math.abs(h - range_factor);
				
				//scale to use the color range of interest
				
				Color hsbColor = Color.getHSBColor(
						(float)(h),
						1f,
						1f);
				
				g.setColor(hsbColor);
				g.fill((cell.toShape()));
				//}
			}
		}
    }
}
