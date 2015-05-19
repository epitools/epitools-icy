package plugins.davhelle.cellgraph.overlays;

import icy.roi.ROI;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;
import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import jxl.write.WritableSheet;
import plugins.adufour.ezplug.EzGUI;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.ShapeRoi;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Class to visualize the intensity underlying the edges of the
 * spatial-temporal graph as overlay in icy.
 * 
 * @author Davide Heller
 *
 */
public class EdgeIntensityOverlay extends StGraphOverlay{
	
	/**
	 * Description string for GUI
	 */
	public static final String DESCRIPTION = 
			"Transforms the edge geometries into ROIs and displays " +
			"the underlying intensity (I) of the first frame." +
			"(Current method limitation)\n\n" +
					
			"1. The mean I is computed from a 5px area buffer" +
			" of the edge geometry\n\n" +
			
			"2. The relative I is computed by correcting for" +
			" the avg cell background(bg) and the avg intensity" +
			" of the neighbor edges(ne) in the 1st order" +
			" neighborhood:\n" +
			"    rel_I = (edge_I - bg_I) / (ne_I - bg_I)\n\n" +
			
			"3. The noralized I is computed considering the" +
			" maximum and minimun relative I in the frame.";

	/**
	 * JTS to AWT shape writer
	 */
	private ShapeWriter writer;
	/**
	 * Connected Icy sequence to retrieve the intensity from
	 */
	private Sequence sequence;
	/**
	 * Connected GUI for Progress and Parameter retrieval
	 */
	private EzGUI gui;
	/**
	 * Visualization parameter for scaling the color gradient reflecting the intensity values
	 */
	private EzVarDouble slider;
	/**
	 * Visualization flag for filling or drawing the edge envelope
	 */
	private EzVarBoolean fillEdgeCheckbox;
	
	//Containers
	/**
	 * ROI representation for each edge
	 */
	private HashMap<Edge,ROI> buffer_roi;
	/**
	 * AWT Shape representation for each edge 
	 */
	private HashMap<Edge,Shape> buffer_shape;
	/**
	 * Relative intensity value for each edge
	 */
	private HashMap<Edge,Double> relativeEdgeIntensity;
	/**
	 * Normalized intensity value for each edge
	 */
	private HashMap<Edge,Double> normalizedEdgeIntensity;
	/**
	 * Background intensity of every cell (reverse selection of edges)
	 */
	private HashMap<Node,Double> cell_background;
	/**
	 * Mean Edge Intensities for every cell 
	 */
	private HashMap<Node,Double> cell_edges;
	
	/**
	 * Minimal displayed edge intensity
	 */
	private double min;
	/**
	 * Maximal displayed edge intensity
	 */
	private double max;

	/**
	 * Buffer width of the edge envelope with which to retrieve the intensities
	 */
	private double bufferWidth;

	/**
	 * Flag whether to normalize the intensities
	 */
	private boolean normalize_intensities;
	/**
	 * Image channel from which to retrieve the intensities
	 */
	private int channelNumber;
	
	/**
	 * @param stGraph graph to display
	 * @param sequence image from which to measure the intensities
	 * @param gui connected GUI
	 * @param varIntensitySlider scaling of color gradient
	 * @param varFillingCheckbox visualization of coloring as filling or outline
	 * @param varBufferWidth edge envelope thickness for intensity retrieval
	 * @param normalize_intensities flag to normalize or not the intensities
	 * @param channelNumber image channel to measure
	 */
	public EdgeIntensityOverlay(SpatioTemporalGraph stGraph, Sequence sequence,
			EzGUI gui, EzVarDouble varIntensitySlider, EzVarBoolean varFillingCheckbox,
			EzVarInteger varBufferWidth,
			boolean normalize_intensities,
			int channelNumber) {
		super("Edge Intensities",stGraph);
		
		this.gui = gui;
		this.slider = varIntensitySlider;
		this.fillEdgeCheckbox = varFillingCheckbox;
		this.writer = new ShapeWriter();
		this.buffer_shape = new	HashMap<Edge, Shape>();
		this.buffer_roi = new HashMap<Edge, ROI>();
		this.normalizedEdgeIntensity = new HashMap<Edge, Double>();
		this.relativeEdgeIntensity = new HashMap<Edge, Double>();
		this.cell_background = new HashMap<Node, Double>();
		this.cell_edges = new HashMap<Node, Double>();
		this.sequence = sequence;
		this.bufferWidth = varBufferWidth.getValue();
		this.normalize_intensities = normalize_intensities;
		this.channelNumber = channelNumber;
		
		
		for(int i = 0; i < 1; i++){
			FrameGraph frame_i = stGraph.getFrame(i);
			computeFrameIntensities(frame_i);
		}
	}

	/**
	 * Computes the edge intensities for all edges in the graph
	 * 
	 * @param frame_i frame to measure
	 */
	private void computeFrameIntensities(FrameGraph frame_i) {
		
		//double sum_mean_cell_background = 0;
		int gui_counter = 0;
		
		//Compute individual edge intensities
		gui.setProgressBarMessage("Computing Edge Intensities...");
		for(Edge e: frame_i.edgeSet()){
			if(!e.hasGeometry())
				e.computeGeometry(frame_i);
			
			computeEdgeIntensity(e,frame_i);
			//double edge_intensity = computeEdgeIntensity(e,frame_i);
			//sum_mean_cell_background += edge_intensity;
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

		
		if(normalize_intensities){
			gui.setProgressBarValue(0);
			gui_counter = 0;
			gui.setProgressBarMessage("Normalizing Intensities...");

			for(Edge e: frame_i.edgeSet()){

				double rel_value = normalizeEdgeIntensity(e,frame_i);
				relativeEdgeIntensity.put(e, rel_value);

				if(rel_value > max)
					max = rel_value;
				else if(rel_value < min)
					min = rel_value;

				gui.setProgressBarValue(gui_counter++/(double)frame_i.edgeSet().size());

			}
		}
		else{
			for(Edge e: frame_i.edgeSet()){

				double rel_value = e.getValue();
				//put same raw values in data fieds
				relativeEdgeIntensity.put(e, rel_value);

				if(rel_value > max)
					max = rel_value;
				else if(rel_value < min)
					min = rel_value;
			}
		}
		
		//Normalize
		for(Edge e: frame_i.edgeSet()){
			//update from relative to normalized
			double rel_value = relativeEdgeIntensity.get(e);
			double normalized_value = (rel_value - min)/max;
			normalizedEdgeIntensity.put(e,normalized_value);

		}
		
		if(normalize_intensities){
			min = 0;
			max = 1;
		}
			
	}
	
	/**
	 * Compute underlying intensity for edge using a ROI corresponding
	 * to the edge envelope (specified by bufferWidth)
	 * 
	 * @param e edge to measure
	 * @param frame_i frame to which the edge belongs
	 * @return mean intensity value of pixels within the edge envelope
	 */
	private double computeEdgeIntensity(Edge e, FrameGraph frame_i){
		
		Geometry edge_geo = e.getGeometry();
		
		Geometry edge_buffer = edge_geo.buffer(bufferWidth);
		
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
		int c=channelNumber;
		
		//TODO possibly use getIntensityInfo here
		double mean_intensity = 
				ROIUtil.getMeanIntensity(
						sequence,
						edge_roi,
						z, t, c);

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
		int c=channelNumber;
		double s_mean = ROIUtil.getMeanIntensity(sequence, s_minimal, z, t, c);
		double mean_edge_intensity = ROIUtil.getMeanIntensity(sequence, edge_union, z, t, c);

		//Save results
		cell_background.put(s,s_mean);
		cell_edges.put(s, mean_edge_intensity);
	}
	
	/**
	 * Every edge is normalized by taking into account the first order neighborhood
	 * 
	 * The edge intensity is first made relative by subtracting the average cell intensity
	 * of the neighborhood and than divided by the relative average intensity of neighboring
	 * edges (1st neighborhood cells).
	 * 
	 * Int = intensity
	 * Bg = Background
	 * 1stOrderN = 1st Order Neighborhood (i.e. the neighborhood of the edge's cells)
	 * 
	 * adapted from Zallen et al.
	 * 
	 * normalizedInt = (edgeInt - 1stOrderNBgInt)/(1stOrderNEdgeInt - 1stOrderNCellBgInt)
	 * 
	 * @param e
	 * @param frame
	 * @return
	 */
	private double normalizeEdgeIntensity(Edge e, FrameGraph frame){
		
		Node source = frame.getEdgeSource(e);
		Node target = frame.getEdgeTarget(e);
		
		//dubious definition
		//what about including 1 order of neigborhood?
		//i.e. 
		
		HashSet<Node> firstOrderNeighbors = new HashSet<Node>();
		firstOrderNeighbors.addAll(source.getNeighbors());
		firstOrderNeighbors.addAll(target.getNeighbors());
		
		double sum_cell_background = 0;
		double sum_cell_edges = 0;
		
		for(Node n: firstOrderNeighbors){
			sum_cell_background += cell_background.get(n);
			sum_cell_edges += cell_edges.get(n);
		}
		
		double mean_cell_background = sum_cell_background/firstOrderNeighbors.size();
		double mean_cell_edges = sum_cell_edges/firstOrderNeighbors.size();
		
		double rel_edge_value = e.getValue() - mean_cell_background;
		double rel_neighborEdge_value = mean_cell_edges	 - mean_cell_background;
		
		double normalized_value = rel_edge_value / rel_neighborEdge_value;
		
		return normalized_value;
	}

	@Override
    public void paintFrame(Graphics2D g, FrameGraph frame_i){
		
		if(frame_i.getFrameNo() != 0)
			return;
		
		g.setColor(Color.blue);
		
		double scaling_factor = - slider.getValue();
		double shift_factor = 0.8;

		//paint all the edges of the graph
		for(Edge edge: frame_i.edgeSet()){

			assert(buffer_shape.containsKey(edge));

			Shape egde_shape = buffer_shape.get(edge);


			Color hsbColor = Color.getHSBColor(
					(float)(normalizedEdgeIntensity.get(edge) * scaling_factor + shift_factor),
					1f,
					1f);

			g.setColor(hsbColor);

			if(fillEdgeCheckbox.getValue())
				g.fill(egde_shape);
			else
				g.draw(egde_shape);

		}

	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		if(frame.getFrameNo() != 0)
			return;
		
		XLSUtil.setCellString(sheet, 0, 0, "Edge id");
		XLSUtil.setCellString(sheet, 1, 0, "Edge x");
		XLSUtil.setCellString(sheet, 2, 0, "Edge y");
		XLSUtil.setCellString(sheet, 3, 0, "Mean Edge intensity");
		XLSUtil.setCellString(sheet, 4, 0, "Relative Edge intensity");
		XLSUtil.setCellString(sheet, 5, 0, "Normalized Edge intensity");

		int row_no = 1;

		for(Edge e: frame.edgeSet()){
			
			Point centroid = e.getGeometry().getCentroid();
			long edge_id = e.getPairCode(frame);
			
			double relative_value = relativeEdgeIntensity.get(e);
			double normalized_value = normalizedEdgeIntensity.get(e);

			XLSUtil.setCellNumber(sheet, 0, row_no, edge_id);
			XLSUtil.setCellNumber(sheet, 1, row_no, centroid.getX());
			XLSUtil.setCellNumber(sheet, 2, row_no, centroid.getY());
			XLSUtil.setCellNumber(sheet, 3, row_no, e.getValue());
			XLSUtil.setCellNumber(sheet, 4, row_no, relative_value);
			XLSUtil.setCellNumber(sheet, 5, row_no, normalized_value);

			row_no++;
		}

	}

	@Override
	public void specifyLegend(Graphics2D g, java.awt.geom.Line2D.Double line) {
		
		int binNo = 50;
		double scaling_factor = - slider.getValue();
		double shift_factor = 0.8;
		
		OverlayUtils.gradientColorLegend(g, line, min, max, binNo,
				scaling_factor, shift_factor);
		
	}
	
	
}
