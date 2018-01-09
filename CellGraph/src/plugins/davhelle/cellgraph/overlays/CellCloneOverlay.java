package plugins.davhelle.cellgraph.overlays;

import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D.Double;
import java.util.ArrayList;
import java.util.HashMap;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.IntensityReader;
import plugins.davhelle.cellgraph.io.IntensitySummaryType;
import plugins.davhelle.cellgraph.misc.ShapeRoi;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;

public class CellCloneOverlay extends StGraphOverlay {
	
	public static final String DESCRIPTION = "Cell clone overlay";
	
	
	/**
	 * JTS class to convert JTS Geometries to AWT Shapes
	 */
	private ShapeWriter writer;
	
	int evidence_channel = 0;
	int detection_threshold = 100;
	
	ArrayList<Node> clones;
	HashMap<Node, Shape> shapes;
	
	public CellCloneOverlay(SpatioTemporalGraph stGraph, Sequence sequence) {
		super("Cell clones", stGraph);
		
		this.writer = new ShapeWriter();
		this.clones = new ArrayList<Node>();
		this.shapes = new HashMap<Node, Shape>();
		
		for(Node n: stGraph.getFrame(0).vertexSet()){
			
			
			Geometry cell_geo = n.getGeometry();
			Shape cell_shape = writer.toShape(cell_geo);
			
//			try{
				ShapeRoi cell_roi = new ShapeRoi(cell_shape);
				
				double mean_intensity = 
						IntensityReader.measureRoiIntensity(
								sequence, cell_roi, 0, 0, evidence_channel, IntensitySummaryType.Mean);
				
				if(mean_intensity > detection_threshold){
					this.clones.add(n);
					this.shapes.put(n, cell_shape);
				}
				
//			}catch(Exception ex){
//				Point centroid = cell_geo.getCentroid();
//				System.out.printf("Problems at %.2f %.2f",centroid.getX(),centroid.getY());
//			}
			
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

}
