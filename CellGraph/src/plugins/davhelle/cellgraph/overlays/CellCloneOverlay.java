package plugins.davhelle.cellgraph.overlays;

import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.geom.Line2D.Double;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;

public class CellCloneOverlay extends StGraphOverlay implements ChangeListener {
	
	public static final String DESCRIPTION = "Cell clone overlay";
	
	JSlider segmentSlider;
	
	/**
	 * JTS class to convert JTS Geometries to AWT Shapes
	 */
	private ShapeWriter writer;
	
	int evidence_channel = 0;
	int detection_threshold = 100;
	
	Set<Node> clones;
	HashMap<Node, Shape> shapes;
	
	Sequence sequence;
	
	public CellCloneOverlay(SpatioTemporalGraph stGraph, Sequence sequence) {
		super("Cell clones", stGraph);
		
		this.sequence = sequence;
		
		this.writer = new ShapeWriter();
		
		this.clones = new HashSet<Node>();
		this.shapes = new HashMap<Node, Shape>();
		
		detect_clones();
		
	}

	/**
	 * @param stGraph
	 * @param sequence
	 */
	private void detect_clones() {
		
		// reset for now
		this.clones = new HashSet<Node>();
		this.shapes = new HashMap<Node, Shape>();
		
		FrameGraph frame0 = super.stGraph.getFrame(0);
		for(Node n: frame0.vertexSet()){
			
			// save readout that doesn't change by adjusting threshold
			Geometry cell_geo = n.getGeometry();
			Shape cell_shape = writer.toShape(cell_geo);
			

			ShapeRoi cell_roi = new ShapeRoi(cell_shape);

			double mean_intensity = 
					IntensityReader.measureRoiIntensity(
							sequence, cell_roi, 0, 0, evidence_channel, IntensitySummaryType.Mean);

			if(mean_intensity > detection_threshold){
				this.clones.add(n);
				this.shapes.put(n, cell_shape);
			}
				
		}
		
		// find groups by building a subgraph with the highlighted nodes
		// and finding the connected components of it
		
		// copy graph
		FrameGraph subgraph = new FrameGraph();
		Graphs.addGraph(subgraph, frame0);
		
		// remove nodes below threshold
		Set<Node> difference = new HashSet<Node>(frame0.vertexSet());
		difference.removeAll(this.clones);
		
		if(subgraph.removeAllVertices(difference))
			System.out.println(String.format("Removed %d nodes", difference.size()));
		
		// find connected sets in remaining nodes
		ConnectivityInspector<Node, Edge> inspector = new ConnectivityInspector<Node, Edge>(subgraph);
		
		System.out.println("Clones found: ");
		for(Set<Node> clone: inspector.connectedSets()){
			System.out.println("Clone with "+clone.size()+" cells");
		}
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		for(Node clone: clones){
			g.setColor(Color.ORANGE);
			g.fill(shapes.get(clone));
		}

	}

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		// TODO Auto-generated method stub

	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public JPanel getOptionsPanel() {
		
		JPanel optionPanel = new JPanel(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 10, 2, 5);
        gbc.fill = GridBagConstraints.BOTH;
        
        segmentSlider = new JSlider(1, 255, this.detection_threshold);
        segmentSlider.addChangeListener(this);
		optionPanel.add(segmentSlider, gbc);
        
        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        
        return optionPanel;
		
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		 this.detection_threshold = segmentSlider.getValue();
		 detect_clones();
		 painterChanged();
	}

}
