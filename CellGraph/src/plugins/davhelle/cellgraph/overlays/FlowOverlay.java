package plugins.davhelle.cellgraph.overlays;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D.Double;
import java.util.ArrayList;
import java.util.HashMap;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

public class FlowOverlay extends StGraphOverlay {
	
	HashMap<Node,LineString> flow = new HashMap<Node,LineString>();
	ShapeWriter writer = new ShapeWriter();

	public FlowOverlay(SpatioTemporalGraph stGraph) {
		super("Flow over time", stGraph);

		GeometryFactory factory = new GeometryFactory();
		
		FrameGraph frame = stGraph.getFrame(0);

		for(Node n: frame.vertexSet()){

			if(!n.hasNext())
				continue;

			ArrayList<Coordinate> list = new ArrayList<Coordinate>();

			list.add(n.getCentroid().getCoordinate());
			
			Node next = n;
			while(next.hasNext()){
				next = next.getNext();
				list.add(next.getCentroid().getCoordinate());
			}

			LineString cell_path = factory.createLineString(
					list.toArray(new Coordinate[list.size()]));

			flow.put(n,cell_path);

		}
		
	
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		for(Node n: frame_i.vertexSet()){
			if(flow.containsKey(n)){
				LineString s = flow.get(n);
				Shape flow = writer.toShape(s);
				g.setColor(Color.cyan);
				g.draw(flow);
			}
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
