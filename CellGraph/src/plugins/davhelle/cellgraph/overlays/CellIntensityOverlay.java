package plugins.davhelle.cellgraph.overlays;

import icy.roi.ROI;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;
import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D.Double;
import java.util.HashMap;

import jxl.write.WritableSheet;
import plugins.adufour.ezplug.EzGUI;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.IntensityReader;
import plugins.davhelle.cellgraph.io.IntensitySummaryType;
import plugins.davhelle.cellgraph.misc.ShapeRoi;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.kernel.roi.roi2d.ROI2DArea;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Overlay to measure the intensity of individual cells.
 * Currently the measurement geometry is a difference
 * between the positive and negative buffer of the cell
 * geometry. The size of the buffer is set by the user.
 * The end geometry is a ring geometry.
 * 
 * @author Davide Heller
 *
 */
public class CellIntensityOverlay extends StGraphOverlay implements EzVarListener<Integer>{
	
	/**
	 * Description string for GUI
	 */
	public static final String DESCRIPTION = 
			"Measures cell border intensity";
	
	/**
	 * JTS class to convert JTS Geometries to AWT Shapes
	 */
	private ShapeWriter writer;
	
	/**
	 * ICY sequence to use as display
	 */
	private Sequence sequence;
	
	private EzGUI gui;
	
	/**
	 * Mean Edge Intensities for every cell 
	 */
	private EzVarInteger bufferWidth;
	private EzVarInteger channelNumber;
	private EzVarEnum<IntensitySummaryType> summary_type;
	private HashMap<Node,Shape> cell_rings;
	private ROI2DArea[] nanAreaRoi;

	private int totalCellNumber;
	
	public CellIntensityOverlay(SpatioTemporalGraph stGraph,
			EzVarInteger varBufferWidth,
			EzVarEnum<IntensitySummaryType> varIntensitySummaryType,
			EzVarInteger varIntensityChannel, Sequence sequence,
			EzGUI gui) {
		super("Cell Intensity", stGraph);
		
		this.writer = new ShapeWriter();
		this.sequence = sequence;
		this.gui = gui;
		
		this.cell_rings = new HashMap<Node, Shape>();
		this.bufferWidth = varBufferWidth;
		this.bufferWidth.addVarChangeListener(this);
		this.channelNumber = varIntensityChannel;
		this.summary_type = varIntensitySummaryType;
		this.nanAreaRoi = new ROI2DArea[stGraph.size()];
		
		totalCellNumber = 0;
		for(int i=0; i<stGraph.size(); i++)
			totalCellNumber += stGraph.getFrame(i).vertexSet().size();
		
		//Initialize cell ring geometries
		gui.setProgressBarMessage("Computing cell ring geometries");
		for(int i=0; i<stGraph.size(); i++){
			int cell_no = 0;
			for(Node n: stGraph.getFrame(i).vertexSet()){
				cell_no++;
				updateMeasurementGeometry(n);
				gui.setProgressBarValue(cell_no/(double)totalCellNumber);
			}
		}
		gui.setProgressBarValue(0);
		gui.setProgressBarMessage("");
		
	}
	
	private void updateMeasurementGeometry(Node n) {
		int buffer_width = this.bufferWidth.getValue();
		Geometry buffer_geo = n.getGeometry().buffer(buffer_width);
		Geometry reduced_geo = n.getGeometry().buffer(-buffer_width);
		Geometry final_geo = buffer_geo.difference(reduced_geo);
		Shape buffer_shape = writer.toShape(final_geo);
		this.cell_rings.put(n, buffer_shape);
	}
	
	private double getCellIntensity(Node node) {
		
		assert(cell_rings.containsKey(node));
		
		Shape cell_shape = cell_rings.get(node);

		int z=0;
		int t=node.getFrameNo();
		int c=channelNumber.getValue();
		
		ROI cell_roi_wo_nan = null;
		try{
			ShapeRoi cell_roi = new ShapeRoi(cell_shape);
			cell_roi_wo_nan = ROIUtil.subtract(cell_roi, nanAreaRoi[t]);
		}catch(Exception ex){
			Point centroid = node.getGeometry().getCentroid();
			System.out.printf("Problems at %.2f %.2f",centroid.getX(),centroid.getY());
			return -1.0;
		}
		
		//TODO possibly use getIntensityInfo here
		
		double mean_intensity = 
				IntensityReader.measureRoiIntensity(
						sequence, cell_roi_wo_nan, z, t, c, summary_type.getValue());
		
		return mean_intensity;
	}


	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#paintFrame(java.awt.Graphics2D, plugins.davhelle.cellgraph.graphs.FrameGraph)
	 */
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		for(Node cell: frame_i.vertexSet()){
			g.setColor(Color.BLUE);
			g.draw(cell_rings.get(cell));
		}
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#specifyLegend(java.awt.Graphics2D, java.awt.geom.Line2D.Double)
	 */
	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		String s = "Cell Intensity Geometry";
		Color c = Color.BLUE;
		int offset = 0;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);

	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#writeFrameSheet(jxl.write.WritableSheet, plugins.davhelle.cellgraph.graphs.FrameGraph)
	 */
	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		int i = frame.getFrameNo();
		
		gui.setProgressBarMessage("Computing Nan ROI for frame "+i);
		
		this.nanAreaRoi[i] = EdgeOrientationOverlay.computeNanAreaROI(
				sequence,i,0,channelNumber.getValue());
		
		gui.setProgressBarMessage("Exporting frame "+i);

		int row_no = 0;
		int col_no = 0;
		
		int col_x = col_no++;
		int col_y = col_no++;
		int col_area = col_no++;
		int col_intensity = col_no++;
		
		XLSUtil.setCellString(sheet, col_x, row_no, "X coor [px]");
		XLSUtil.setCellString(sheet, col_y, row_no, "Y coor [px]");
		XLSUtil.setCellString(sheet, col_area, row_no, "Area [px]");
		XLSUtil.setCellString(sheet, col_intensity, row_no, "Intensity [" + sequence.getDataType_().name() + "]");
		
		row_no++;
		
		for(Node node: frame.vertexSet()){

			gui.setProgressBarValue(row_no/(double)totalCellNumber);
			
			int xStart = (int)node.getGeometry().getCentroid().getX();
			int yStart = (int)node.getGeometry().getCentroid().getY();

			XLSUtil.setCellNumber(sheet, col_x, row_no, xStart);
			XLSUtil.setCellNumber(sheet, col_y, row_no, yStart);
			
			double area = node.getGeometry().getArea();
			double intensity = getCellIntensity(node);
			XLSUtil.setCellNumber(sheet, col_area, row_no, area);
			XLSUtil.setCellNumber(sheet, col_intensity, row_no, intensity);

			row_no++;
			
		}
		
		gui.setProgressBarMessage("");
		gui.setProgressBarValue(0);

	}

	@Override
	public void variableChanged(EzVar<Integer> source, Integer newValue) {
		
		gui.setProgressBarMessage("Updating Geometries..");
		
		int cell_no=0;
		for(Node n: cell_rings.keySet()){
			cell_no++;
			updateMeasurementGeometry(n);
			gui.setProgressBarValue(cell_no/(double)totalCellNumber);
		}
		
		gui.setProgressBarValue(0);
		gui.setProgressBarMessage("");
		
		painterChanged();
		
	}

}
