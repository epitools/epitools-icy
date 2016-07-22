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
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.IntensityReader;
import plugins.davhelle.cellgraph.io.IntensitySummaryType;
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
			"Transforms the edge geometries into ROIs and displays<br/>" +
			" the underlying intensity (I). Save the intensities via<br/>" +
			" the layer option menu as XLS.<br/><br/>" +
			
			"(i) [Measure relative intensity] computes a ratio by<br/>" +
			" dividing the original value through intensities of<br/>" +
			" neighboring edges and cells:<br/><br/>" +
			"  relative_edge_intensity = <br/>" +
			"    (edge - avg_cell_background) / <br/>" +
			"    (avg_neighbor_edges - avg_cell_background)<br/><br/>" +
			
			"(i) The color gradient is normalized by frame.<br/><br/>" + 
			
			"(w) [Measure all frames] can take very long!";

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
	private double[] min;
	/**
	 * Maximal displayed edge intensity
	 */
	private double[] max;

	/**
	 * Buffer width of the edge envelope with which to retrieve the intensities
	 */
	private double bufferWidth;

	/**
	 * Flag whether to normalize the intensities
	 * with the method inspired by 
	 * Fernandez-Gonzalez et al. Dev.Cell (2009)
	 */
	private boolean zallen_normalization;
	/**
	 * Image channel from which to retrieve the intensities
	 */
	private int channelNumber;
	
	/**
	 * Flag whether to analyze all frames or just one
	 */
	private boolean analyzeAllFrames;
	
	/**
	 * Intensity Summary type
	 */
	EzVarEnum<IntensitySummaryType> summary_type;
	
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
			EzGUI gui, EzVarBoolean varFillingCheckbox,
			EzVarInteger varBufferWidth,
			EzVarEnum<IntensitySummaryType> intensitySummaryType,
			boolean varMeasureAllFrames,
			boolean normalize_intensities,
			int channelNumber) {
		super("Edge Intensities",stGraph);
		
		this.gui = gui;
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
		this.zallen_normalization = normalize_intensities;
		this.channelNumber = channelNumber;
		this.summary_type = intensitySummaryType;
		this.analyzeAllFrames = varMeasureAllFrames;
		int frame_no = 1;
		
		if(analyzeAllFrames)
			frame_no = stGraph.size();
		
		min = new double[frame_no];
		max = new double[frame_no];
		
		for(int i = 0; i < frame_no; i++){
			FrameGraph frame_i = stGraph.getFrame(i);
			computeFrameIntensities(frame_i);
		}
		
		super.setGradientScale(-0.4);
		super.setGradientShift(0.8);
		super.setGradientControlsVisibility(true);
	}

	/**
	 * Computes the edge intensities for all edges in the graph
	 * 
	 * @param frame_i frame to measure
	 */
	private void computeFrameIntensities(FrameGraph frame_i) {
		
		//Initialize Progress bar
		gui.setProgressBarValue(0.01);
		int gui_counter = 0;
		
		double total_count = 2 * frame_i.edgeSet().size();
		if(zallen_normalization)
			total_count += frame_i.vertexSet().size();
		
		int frameNo = frame_i.getFrameNo();
		String frame_str = String.format(" in frame %d...",
				frameNo);
		
		//Compute individual edge intensities
		gui.setProgressBarMessage("Computing Edge Geometries"+frame_str);
		for(Edge e: frame_i.edgeSet()){
			if(!e.hasGeometry())
				e.computeGeometry(frame_i);
			gui.setProgressBarValue(gui_counter++/total_count);
		}
		
		gui.setProgressBarMessage("Computing Edge Intensities"+frame_str);
		for(Edge e: frame_i.edgeSet()){
			computeEdgeIntensity(e,frame_i);
			gui.setProgressBarValue(gui_counter++/total_count);
		}

		//Edge Normalization and limit identification
		
		this.min[frameNo] = Double.MAX_VALUE;
		this.max[frameNo] = Double.MIN_VALUE;

		if(zallen_normalization){
			
			//normalization through nearby
			//cell intensities (see zallen paper)
			
			gui.setProgressBarMessage("Computing Cell Intensities"+frame_str);
			for(Node n: frame_i.vertexSet()){
				computeCellIntensity(n);
				gui.setProgressBarValue(gui_counter++/total_count);
			}

			for(Edge e: frame_i.edgeSet()){

				double rel_value = computeRelativeEdgeIntensity(e,frame_i);
				relativeEdgeIntensity.put(e, rel_value);

				if(rel_value > max[frameNo])
					max[frameNo] = rel_value;
				else if(rel_value < min[frameNo])
					min[frameNo] = rel_value;
			}
		}
		else{
			for(Edge e: frame_i.edgeSet()){

				double rel_value = e.getValue();
				//put same raw values in data fields
				relativeEdgeIntensity.put(e, rel_value);

				if(rel_value > max[frameNo])
					max[frameNo] = rel_value;
				else if(rel_value < min[frameNo])
					min[frameNo] = rel_value;
			}
		}
		
		//Normalize
		for(Edge e: frame_i.edgeSet()){
			//update from relative to normalized
			double rel_value = relativeEdgeIntensity.get(e);
			double normalized_value = 
					(rel_value - min[frameNo])/(max[frameNo] - min[frameNo]);
			normalizedEdgeIntensity.put(e,normalized_value);

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
			return -1.0;
		}
		
		buffer_roi.put(e, edge_roi);
		
		int z=0;
		int t=frame_i.getFrameNo();
		int c=channelNumber;
		
		//TODO possibly use getIntensityInfo here
		
		double mean_intensity = 
				IntensityReader.measureRoiIntensity(
						sequence, edge_roi, z, t, c, summary_type.getValue());

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
		ShapeRoi s_roi = new ShapeRoi(writer.toShape(s.getGeometry()));
		s_roi.setC(0);
		s_roi.setT(0);
		s_roi.setZ(0);
		ROI s_minimal = ROIUtil.subtract(s_roi, edge_union);
		
		//Compute intensities
		int z=0;
		int t=frame.getFrameNo();
		int c=channelNumber;
		double s_mean = IntensityReader.measureRoiIntensity(
				sequence, s_minimal, z, t, c, summary_type.getValue());
		double mean_edge_intensity = IntensityReader.measureRoiIntensity(
				sequence, edge_union, z, t, c, summary_type.getValue());

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
	 * relativeInt = (edgeInt - 1stOrderNBgInt)/(1stOrderNEdgeInt - 1stOrderNCellBgInt)
	 * 
	 * @param e edge to be normalized
	 * @param frame frame from which to measure intensities for the normalization
	 * @return
	 */
	private double computeRelativeEdgeIntensity(Edge e, FrameGraph frame){
		
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
		
		int frameNo = frame_i.getFrameNo();
		
		if(frameNo > 0 && !analyzeAllFrames)
			return;
		
		super.setGradientMaximum(max[frameNo]);
		super.setGradientMinimum(min[frameNo]);
		
		g.setColor(Color.blue);
		
		//paint all the edges of the graph
		for(Edge edge: frame_i.edgeSet()){

			assert(buffer_shape.containsKey(edge));

			Shape egde_shape = buffer_shape.get(edge);
			
			double intensity_measure = relativeEdgeIntensity.get(edge);
			
			if(intensity_measure < 0.0)
				continue;
	
			Color hsbColor = super.getScaledColor(intensity_measure);
			g.setColor(hsbColor);

			if(fillEdgeCheckbox.getValue())
				g.fill(egde_shape);
			else
				g.draw(egde_shape);

		}

	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		if(frame.getFrameNo() != 0 && !analyzeAllFrames)
			return;
		
		int c = 0;
		int r = 0;
		
		XLSUtil.setCellString(sheet, c++, r, "Edge id");
		XLSUtil.setCellString(sheet, c++, r, "Edge x");
		XLSUtil.setCellString(sheet, c++, r, "Edge y");
		XLSUtil.setCellString(sheet, c++, r, "Cell 1");
		XLSUtil.setCellString(sheet, c++, r, "Cell 2");
		XLSUtil.setCellString(sheet, c++, r, String.format("%s Edge Intensity",summary_type.getValue().getDescription()));
		XLSUtil.setCellString(sheet, c++, r, "Relative Edge intensity");
		XLSUtil.setCellString(sheet, c++, r, "Normalized Edge intensity");

		for(Edge e: frame.edgeSet()){
			
			//reset column and increment row
			c=0;
			r++;
			
			Point centroid = e.getGeometry().getCentroid();
			long edge_id = e.getPairCode(frame);
			
			int target = frame.getEdgeTarget(e).getTrackID();
			int source = frame.getEdgeSource(e).getTrackID();
			
			double relative_value = relativeEdgeIntensity.get(e);
			double normalized_value = normalizedEdgeIntensity.get(e);

			XLSUtil.setCellNumber(sheet, c++, r, edge_id);
			XLSUtil.setCellNumber(sheet, c++, r, centroid.getX());
			XLSUtil.setCellNumber(sheet, c++, r, centroid.getY());
			XLSUtil.setCellNumber(sheet, c++, r, target);
			XLSUtil.setCellNumber(sheet, c++, r, source);
			XLSUtil.setCellNumber(sheet, c++, r, e.getValue());
			XLSUtil.setCellNumber(sheet, c++, r, relative_value);
			XLSUtil.setCellNumber(sheet, c++, r, normalized_value);

		}

	}

	@Override
	public void specifyLegend(Graphics2D g, java.awt.geom.Line2D.Double line) {
		
		int binNo = 50;
		
		String min_value = String.format("%.1f",super.getGradientMinimum());
		String max_value = String.format("%.1f",super.getGradientMaximum());
		
		OverlayUtils.gradientColorLegend_ZeroOne(g, line, min_value, max_value, binNo,
				super.getGradientScale(), super.getGradientShift());
		
	}
	
	
}
