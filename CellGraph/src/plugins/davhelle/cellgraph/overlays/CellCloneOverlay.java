package plugins.davhelle.cellgraph.overlays;

import icy.sequence.Sequence;
import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jxl.write.WritableSheet;

import org.jgrapht.Graphs;
import org.jgrapht.alg.ConnectivityInspector;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.IntensityReader;
import plugins.davhelle.cellgraph.io.IntensitySummaryType;
import plugins.davhelle.cellgraph.misc.ShapeRoi;
import plugins.davhelle.cellgraph.misc.VirtualClone;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Overlay to detect groups of cell based on an intensity signal.
 * Groups of detected and connected cells are defined a clone and saved
 * as VirtualClone objects. Purpose of this Overlay is to detect and
 * quantify the VirtualClones. 
 * 
 * @author Davide Heller
 *
 */
public class CellCloneOverlay extends StGraphOverlay implements ChangeListener {
	
	public static final String DESCRIPTION = "Cell clone overlay";
	
	/**
	 * Slider to adjust the detection_treshold from the layerOptionPanel
	 */
	JSlider thresholdSlider;
	
	/**
	 * JTS class to convert JTS Geometries to AWT Shapes
	 */
	private ShapeWriter writer;
	
	/**
	 * Image channel to use for the detection
	 */
	int evidence_channel = 0;
	
	/**
	 * Detection threshold (intensity) to define the cells to be grouped into clones 
	 */
	int detection_threshold = 100;
	
	/**
	 * Set containing the virtual clones detected 
	 */
	Set<VirtualClone> clones;
	
	/**
	 * Cached mean intensities of all cells to avoid repeating ROI measurements 
	 */
	Map<Node,Double> mean_intensities;
	
	/**
	 * Sequence from which to read intensities 
	 */
	Sequence sequence;
	
	public CellCloneOverlay(SpatioTemporalGraph stGraph, Sequence sequence) {
		super("Cell clones", stGraph);
		
		this.sequence = sequence;
		this.writer = new ShapeWriter();
		this.mean_intensities = new HashMap<Node, Double>();
		
		detect_clones();
		
	}

	/**
	 * Method to detect clones based on the intensity signal
	 * 
	 * @param stGraph
	 * @param sequence
	 */
	private void detect_clones() {
		
		long start = System.currentTimeMillis();
		
		this.clones = new HashSet<VirtualClone>();
		
		//detect cells with mean intensity above threshold
		HashSet<Node> detected_cells = new HashSet<Node>();
		
		FrameGraph frame0 = super.stGraph.getFrame(0);
		for(Node n: frame0.vertexSet()){
			
			double mean_intensity = 0.0;
			if(mean_intensities.containsKey(n)){
				mean_intensity = mean_intensities.get(n);
			}
			else{
				Geometry cell_geo = n.getGeometry();
				Shape cell_shape = writer.toShape(cell_geo);

				ShapeRoi cell_roi = new ShapeRoi(cell_shape);

				mean_intensity = 
						IntensityReader.measureRoiIntensity(
								sequence, cell_roi, 0, 0, evidence_channel, IntensitySummaryType.Mean);
				
				mean_intensities.put(n, mean_intensity);
			}

			if(mean_intensity > detection_threshold)
				detected_cells.add(n);
				
		}
		
		// find clones by building a subgraph with the detected_cells
		// and finding the connected components of it
		
		// copy graph
		FrameGraph subgraph = new FrameGraph();
		Graphs.addGraph(subgraph, frame0);
		
		// remove nodes below threshold
		Set<Node> difference = new HashSet<Node>(frame0.vertexSet());
		difference.removeAll(detected_cells);
		
		if(subgraph.removeAllVertices(difference))
			System.out.println(String.format("Removed %d nodes", difference.size()));
		
		// find connected sets in remaining nodes
		ConnectivityInspector<Node, Edge> inspector = new ConnectivityInspector<Node, Edge>(subgraph);
		
		// create VirtualClones
		int clone_id = 0;
		for(Set<Node> clone: inspector.connectedSets()){
			this.clones.add(new VirtualClone(clone,clone_id++));
		}
		
		long end = System.currentTimeMillis();
		
		System.out.println(String.format("Found %d clones in %d ms",clones.size(),end-start));
		
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		for(VirtualClone clone: clones){
			g.setColor(Color.ORANGE);
			g.draw(clone.getShape());
			Point centroid = clone.getCentroid();
			g.drawString(String.valueOf(clone.getId()), (int)centroid.getX(), (int)centroid.getY());
		}

	}

	@Override
	public void specifyLegend(Graphics2D g, Line2D line) {
		// TODO Auto-generated method stub

	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		int c = 0;
		int r = 0;
		
		XLSUtil.setCellString(sheet, c++, r, "id");
		XLSUtil.setCellString(sheet, c++, r, "centroid_x");
		XLSUtil.setCellString(sheet, c++, r, "centroid_y");
		XLSUtil.setCellString(sheet, c++, r, "size_px");
		XLSUtil.setCellString(sheet, c++, r, "perimeter_px");
		XLSUtil.setCellString(sheet, c++, r, "cell_count");
		
		for(VirtualClone clone: clones){
			r++;
			c=0;
			XLSUtil.setCellNumber(sheet, c++, r, clone.getId());
			Point centroid = clone.getCentroid();
			XLSUtil.setCellNumber(sheet, c++, r, centroid.getX());
			XLSUtil.setCellNumber(sheet, c++, r, centroid.getY());
			XLSUtil.setCellNumber(sheet, c++, r, clone.getSize());
			XLSUtil.setCellNumber(sheet, c++, r, clone.getPerimeter());
			XLSUtil.setCellNumber(sheet, c++, r, clone.getCellCount());
		}

	}
	
	@Override
	public JPanel getOptionsPanel() {
		
		JPanel optionPanel = new JPanel(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 10, 2, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 0;
        
        thresholdSlider = new JSlider(1, 255, this.detection_threshold);
        thresholdSlider.addChangeListener(this);
    	optionPanel.add(new JLabel("Detection threshold"), gbc);
		optionPanel.add(thresholdSlider, gbc);
        
        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        
        //Excel output button
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 10, 2, 5);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 1;
        optionPanel.add(new JLabel("Export data to Excel: "), gbc);
        
        JButton OKButton = new JButton("Choose File");
        OKButton.addActionListener(this);
        optionPanel.add(OKButton,gbc);
        
        return optionPanel;
		
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		 this.detection_threshold = thresholdSlider.getValue();
		 detect_clones();
		 painterChanged();
	}

}
