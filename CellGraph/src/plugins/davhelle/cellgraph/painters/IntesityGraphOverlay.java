/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.roi.ROI;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashMap;

import plugins.adufour.ezplug.EzGUI;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.ShapeRoi;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to visualize the intensity underlying edges of the
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
	
	private HashMap<Edge,ROI> buffer_roi;
	private HashMap<Edge,Shape> buffer_shape;
	private HashMap<Node,Double> buffer_background;
	
	public IntesityGraphOverlay(SpatioTemporalGraph stGraph, Sequence sequence, EzGUI gui) {
		super("Graph edges");
		this.stGraph = stGraph;
		this.factory = new GeometryFactory();
		this.writer = new ShapeWriter();
		this.buffer_shape = new	HashMap<Edge, Shape>();
		this.buffer_roi = new HashMap<Edge, ROI>();
		this.buffer_background = new HashMap<Node, Double>();
		this.sequence = sequence;
		
		gui.setProgressBarMessage("Computing Edge Intensities");
		for(int i = 0; i < stGraph.size(); i++){
		
		//Todo loop over all frames
		FrameGraph frame_i = stGraph.getFrame(i);
		
		double sum_mean_cell_background = 0;
		
		for(Edge e: frame_i.edgeSet()){
			e.computeGeometry(frame_i);
			double cell_background = computeEdgeIntensity(e,i);
			sum_mean_cell_background += cell_background;
		}
		double overall_mean_cell_background = sum_mean_cell_background / frame_i.edgeSet().size();
		
		for(Node n: frame_i.vertexSet())
			//reduce geometry of the cell areas to an exclusion with
			//the expanded edges
			reduce(n,i);
		
		//normalization should be done through
		//cell intensity (see zallen paper)
		int counter = 0;
		gui.setProgressBarValue(counter);
		gui.setProgressBarMessage("Computing Vertex Intensities");
		
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		
		for(Edge e: frame_i.edgeSet()){
			
			double org_value = e.getValue();
			
			normalize_edge(e,i);
			
			double rel_value = e.getValue();
			
			double norm_value = rel_value/overall_mean_cell_background;
			
//			System.out.printf("%d:\t%.2f\t%.2f\t%.2f\n",
//					counter++,org_value,rel_value,norm_value);
			
			e.setValue(norm_value);
			
			if(norm_value > max)
				max = norm_value;
			else if(norm_value < min)
				min = norm_value;
			

		}
		
		gui.setProgressBarValue(i/(double)(stGraph.size()));
		
		System.out.printf("Overall background correction is: %.2f\n",overall_mean_cell_background);
		System.out.printf("Min/max relative value is: %.2f\t%.2f\n",min,max);
		}
	}
	
	private double computeEdgeIntensity(Edge e, int frame_no){
		
		Geometry edge_geo = e.getGeometry();
		
		//taking 3px buffer distance from edge
		Geometry edge_buffer = edge_geo.buffer(3.0);
		
		Shape egde_shape = writer.toShape(edge_buffer);
		
		this.buffer_shape.put(e, egde_shape);
		
		//TODO possibly add a direct ROI field to edge class
		ShapeRoi edge_roi = null;
		try{
			edge_roi = new ShapeRoi(egde_shape);
		}catch(Exception ex){
			Point centroid = e.getGeometry().getCentroid();
			System.out.printf("Problems at %.2f %.2f",centroid.getX(),centroid.getY());
			return 0.0;
		}
		
		buffer_roi.put(e, edge_roi);
		
		int z=0;
		int t=0;
		int c=0;
		
		//TODO possibly use getIntensityInfo here
		double mean_intensity = 
				ROIUtil.getMeanIntensity(
						sequence,
						edge_roi,
						z, t, c);

		//TODO re-think name of value
		e.setValue(mean_intensity);
		
		return mean_intensity;
	}
	
	private void reduce(Node s,int frame_no){
		
		//who are the flanking cells?
		FrameGraph frame = stGraph.getFrame(frame_no);
		
		if(s.getNeighbors().isEmpty())
			return;
		
		ArrayList<ROI> rois = new ArrayList<ROI>();
		for(Node t: frame.getNeighborsOf(s)){
			Edge e = frame.getEdge(s, t);
			rois.add(buffer_roi.get(e));
		}
			
		ROI ring =	ROIUtil.getIntersection(rois);
		ShapeRoi s_roi = new ShapeRoi(writer.toShape(s.getGeometry()));
		
		ROI s_minimal = ROIUtil.subtract(s_roi, ring);
		int z=0;
		int t=0;
		int c=0;
		double s_mean = ROIUtil.getMeanIntensity(sequence, s_minimal, z, t, c);

		buffer_background.put(s,s_mean);
		
	}
	
	private void normalize_edge(Edge e, int frame_no){
		
		FrameGraph frame = stGraph.getFrame(frame_no);
		
		Node source = frame.getEdgeSource(e);
		Node target = frame.getEdgeTarget(e);
		
		double source_mean = buffer_background.get(source);
		double target_mean = buffer_background.get(target);

		//dubious definition
		double mean_cell_background = (source_mean + target_mean)/2;
		
		double final_edge_value = e.getValue() - mean_cell_background;
		
		e.setValue(final_edge_value);		
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
						(float)(edge.getValue() * 0.8 + 0.5),
						1f,
						1f);
				
				g.setColor(hsbColor);
				g.draw(egde_shape);
				
				
				
			}
		}
	}
	
	
}
