/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.HashMap;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.EdgeRoi;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.kernel.roi.roi2d.ROI2DShape;
import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;

/**
 * Class to visualize all the edges of a 
 * spatial-temporal graph as overlay in icy.
 * 
 * @author Davide Heller
 *
 */
public class IntesityGraphOverlay extends Overlay{

	private SpatioTemporalGraph stGraph;
	private GeometryFactory factory;
	private ShapeWriter writer;
	private Sequence sequence;
	
	private HashMap<Edge,Shape> buffer_shape;
	
	public IntesityGraphOverlay(SpatioTemporalGraph stGraph, Sequence sequence) {
		super("Graph edges");
		this.stGraph = stGraph;
		this.factory = new GeometryFactory();
		this.writer = new ShapeWriter();
		this.buffer_shape = new	HashMap<Edge, Shape>();
		this.sequence = sequence;
		
		int i = 0;
		
		//Todo loop over all frames
		FrameGraph frame_i = stGraph.getFrame(i);
		
		double sum_mean_cell_background = 0;
		
		for(Edge e: frame_i.edgeSet()){
			e.computeGeometry(frame_i);
			double cell_background = this.computeEdgeIntensity(e,i);
			sum_mean_cell_background += cell_background;
		}
		
		double overall_mean_cell_background = sum_mean_cell_background / frame_i.edgeSet().size();
		
		//normalization should be done through
		//cell intensity (see zallen paper)
		int counter = 1;
		for(Edge e: frame_i.edgeSet()){
			double rel_value = e.getValue();
			double norm_value = rel_value/overall_mean_cell_background;
			System.out.printf("%d:\t%.2f\t%.2f\n",counter++,rel_value,norm_value);
			e.setValue(rel_value);
		}
		
		
		System.out.printf("Overall background correction is: %.2f\n",overall_mean_cell_background);
	}
	
	private double computeEdgeIntensity(Edge e, int frame_no){
		
		Geometry edge_geo = e.getGeometry();
		
		//taking 3px buffer distance from edge
		Geometry edge_buffer = edge_geo.buffer(3.0);
		
		Shape egde_shape = writer.toShape(edge_buffer);
		
		this.buffer_shape.put(e, egde_shape);
		
		//TODO possibly add a direct ROI field to edge class
		EdgeRoi edge_roi = new EdgeRoi(egde_shape);
		
		int z=0;
		int t=0;
		int c=0;
		
		//TODO possibly use getIntensityInfo here
		double mean_intensity = 
				ROIUtil.getMeanIntensity(
						sequence,
						edge_roi,
						z, t, c);

		//retrieve avg intensity of flanking cells
		
		//who are the flanking cells?
		FrameGraph frame = stGraph.getFrame(frame_no);
		Node source = frame.getEdgeSource(e);
		Node target = frame.getEdgeTarget(e);
		
		EdgeRoi source_roi = new EdgeRoi(writer.toShape(source.getGeometry()));
		EdgeRoi target_roi = new EdgeRoi(writer.toShape(target.getGeometry()));
		
		double source_mean = ROIUtil.getMeanIntensity(sequence, source_roi, z, t, c);
		double target_mean = ROIUtil.getMeanIntensity(sequence, target_roi, z, t, c);
		
		//dubious definition
		double mean_cell_background = (source_mean + target_mean)/2;
		
		double final_edge_value = mean_intensity - mean_cell_background;
		
		//TODO re-think name of value
		e.setValue(final_edge_value);
		
		return mean_cell_background;
	}

	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas){
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
		
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.blue);
			
			//paint all the edges of the graph
			for(Edge edge: frame_i.edgeSet()){
				
				assert(buffer_shape.containsKey(edge));
				
				Shape egde_shape = buffer_shape.get(edge);
				
				
				Color hsbColor = Color.getHSBColor(
						(float)(edge.getValue()),
						1f,
						1f);
				
				g.setColor(hsbColor);
				g.draw(egde_shape);
				
			}
		}
	}
	
	
}
