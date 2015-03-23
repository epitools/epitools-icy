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
import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import jxl.write.WritableSheet;

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
public class IntesityGraphOverlay extends StGraphOverlay{
	
	public static final String DESCRIPTION = 
			"Transforms the edge geometries into ROIs and displays " +
			"the underlying intensity of the image [time consuming!].\n\n" +
			"The mean intensity is computed over a 5px wide buffer area " +
			"around the edge geometry. The intensity is normalized" +
			"by correcting for the avg. cell background and the" +
			"avg. intensity of the neighbors";

	private GeometryFactory factory;
	private ShapeWriter writer;
	private Sequence sequence;
	private EzGUI gui;
	
	private HashMap<Edge,ROI> buffer_roi;
	private HashMap<Edge,Shape> buffer_shape;
	private HashMap<Edge,Double> normalizedEdgeIntensity;
	private HashMap<Node,Double> cell_background;
	private HashMap<Node,Double> cell_edges;
	private double min;
	private double max;
	private double[] heat_map;
	
	public IntesityGraphOverlay(SpatioTemporalGraph stGraph, Sequence sequence, EzGUI gui) {
		super("Edge Intensities",stGraph);
		
		this.gui = gui;
		this.factory = new GeometryFactory();
		this.writer = new ShapeWriter();
		this.buffer_shape = new	HashMap<Edge, Shape>();
		this.buffer_roi = new HashMap<Edge, ROI>();
		this.normalizedEdgeIntensity = new HashMap<Edge, Double>();
		this.cell_background = new HashMap<Node, Double>();
		this.cell_edges = new HashMap<Node, Double>();
		this.sequence = sequence;
		
		
		for(int i = 0; i < 1; i++){
			FrameGraph frame_i = stGraph.getFrame(i);
			computeFrameIntensities(frame_i);
		}
	}

	/**
	 * Computes the edge intensities for all edges in the graph
	 * 
	 * @param frame_i
	 */
	private void computeFrameIntensities(FrameGraph frame_i) {
		
		double sum_mean_cell_background = 0;
		int gui_counter = 0;
		
		//Compute individual edge intensities
		gui.setProgressBarMessage("Computing Edge Intensities...");
		for(Edge e: frame_i.edgeSet()){
			e.computeGeometry(frame_i);
			double cell_background = computeEdgeIntensity(e,frame_i);
			sum_mean_cell_background += cell_background;
			gui.setProgressBarValue(gui_counter++/(double)frame_i.edgeSet().size());
		}

		gui.setProgressBarValue(0);
		gui_counter = 0;
		gui.setProgressBarMessage("Computing Cell Intensities...");
		for(Node n: frame_i.vertexSet()){
			computeCellIntensity(n);
			gui.setProgressBarValue(gui_counter++/(double)frame_i.vertexSet().size());
		}

		//normalization should be done through
		//cell intensity (see zallen paper)

		this.min = Double.MAX_VALUE;
		this.max = Double.MIN_VALUE;

		int edge_no = frame_i.edgeSet().size();
		double[] intensity_values = new double[edge_no];
		int e_i = 0;

		gui.setProgressBarValue(0);
		gui_counter = 0;
		gui.setProgressBarMessage("Normalizing Intensities...");
		
		for(Edge e: frame_i.edgeSet()){

			double rel_value = normalizeEdgeIntensity(e,frame_i);

			if(rel_value > max)
				max = rel_value;
			else if(rel_value < min)
				min = rel_value;

			intensity_values[e_i++] = rel_value;
			
			gui.setProgressBarValue(gui_counter++/(double)frame_i.edgeSet().size());

		}

		//Find out the color map
		Arrays.sort(intensity_values);
		int step_no = 10; 
		int step = edge_no / step_no;
		this.heat_map = new double[step_no];

		for(e_i = 0; e_i < 10; e_i++){
			int index = e_i * step;

			if(index >= edge_no)
				index = edge_no - 1;

			heat_map[e_i] = intensity_values[index];
		}
		
		double overall_mean_cell_background = sum_mean_cell_background / frame_i.edgeSet().size();
//		System.out.printf("Overall background correction is: %.2f\n",overall_mean_cell_background);
//		System.out.printf("Min/max relative value is: %.2f\t%.2f\n",min,max);
	}
	
	private double computeEdgeIntensity(Edge e, FrameGraph frame_i){
		
		Geometry edge_geo = e.getGeometry();
		
		//taking 3px buffer distance from edge
		Geometry edge_buffer = edge_geo.buffer(5.0);
		
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
		int t=frame_i.getFrameNo();
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
	
	/**
	 * Split the cell geometry in two between edges and inside
	 * and compute the separate intensity. 
	 * 
	 * Populate cell_background and cell_edges fields
	 * 
	 * @param s cell to be computed
	 */
	private void computeCellIntensity(Node s){
		
		//who are the flanking cells?
		FrameGraph frame = s.getBelongingFrame();
		
		if(s.getNeighbors().isEmpty())
			return;
		
		//combine edge rois
		ArrayList<ROI> rois = new ArrayList<ROI>();
		for(Node t: frame.getNeighborsOf(s)){
			Edge e = frame.getEdge(s, t);
			rois.add(buffer_roi.get(e));
		}
		
		//Define Edge Roi region
		ROI edge_union = ROIUtil.getUnion(rois);
		
		//Define Interior Roi region
		ROI ring =	ROIUtil.getIntersection(rois);
		ShapeRoi s_roi = new ShapeRoi(writer.toShape(s.getGeometry()));
		ROI s_minimal = ROIUtil.subtract(s_roi, ring);
		
		//Compute intensities
		int z=0;
		int t=frame.getFrameNo();
		int c=0;
		double s_mean = ROIUtil.getMeanIntensity(sequence, s_minimal, z, t, c);
		double mean_edge_intensity = ROIUtil.getMeanIntensity(sequence, edge_union, z, t, c);

		//Save results
		cell_background.put(s,s_mean);
		cell_edges.put(s, mean_edge_intensity);
	}
	
	private double normalizeEdgeIntensity(Edge e, FrameGraph frame){
		
		Node source = frame.getEdgeSource(e);
		Node target = frame.getEdgeTarget(e);
		
		//retrieve intensities
		double source_mean = cell_background.get(source);
		double target_mean = cell_background.get(target);
		double source_edges = cell_edges.get(source);
		double target_edges = cell_edges.get(target);

		//dubious definition
		//what about including 1 order of neigborhood?
		//i.e. nInt = (eInt - 1stOrderCellBg)/(1stOrderEdgeInt - 1stOrderCellBg)
		
		double mean_cell_background = (source_mean + target_mean)/2;
		double mean_cell_edges = (source_edges + target_edges)/2;
		
		double abs_edge_value = e.getValue() - mean_cell_background;
		
		double final_edge_value = abs_edge_value / mean_cell_edges;
		
		normalizedEdgeIntensity.put(e, final_edge_value);	
		
		return final_edge_value;
	}

	@Override
    public void paintFrame(Graphics2D g, FrameGraph frame_i){
		
		g.setColor(Color.blue);

		//paint all the edges of the graph
		for(Edge edge: frame_i.edgeSet()){

			assert(buffer_shape.containsKey(edge));

			Shape egde_shape = buffer_shape.get(edge);


			Color hsbColor = Color.getHSBColor(
					(float)(normalizedEdgeIntensity.get(edge) * 0.8 + 0.5),
					1f,
					1f);

			g.setColor(hsbColor);
			//add switch here to use either fill or draw via ezPlug
			g.draw(egde_shape);

		}

		//draw scale bar
		for(int i=0; i<heat_map.length; i++){

			Color hsbColor = Color.getHSBColor(
					(float)(heat_map[i] * 0.8 + 0.5),
					1f,
					1f);

			g.setColor(hsbColor);

			g.fillRect(20*i + 30,30,20, 20);

		}
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		if(frame.getFrameNo() == 0){
		
		XLSUtil.setCellString(sheet, 0, 0, "Edge id");
		XLSUtil.setCellString(sheet, 1, 0, "Edge x");
		XLSUtil.setCellString(sheet, 2, 0, "Edge y");
		XLSUtil.setCellString(sheet, 3, 0, "Mean Edge intensity");
		XLSUtil.setCellString(sheet, 4, 0, "Relative Edge intensity");

		int row_no = 1;

		for(Edge e: frame.edgeSet()){
			
			Point centroid = e.getGeometry().getCentroid();
			long edge_id = e.getPairCode(frame);
			
			double normalized_value = normalizedEdgeIntensity.get(e);

			XLSUtil.setCellNumber(sheet, 0, row_no, edge_id);
			XLSUtil.setCellNumber(sheet, 1, row_no, centroid.getX());
			XLSUtil.setCellNumber(sheet, 2, row_no, centroid.getY());
			XLSUtil.setCellNumber(sheet, 3, row_no, e.getValue());
			XLSUtil.setCellNumber(sheet, 4, row_no, normalized_value);

			row_no++;
		}

		}
		
	}
	
	
}
