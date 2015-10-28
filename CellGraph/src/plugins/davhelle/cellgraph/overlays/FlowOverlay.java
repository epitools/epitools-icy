package plugins.davhelle.cellgraph.overlays;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D.Double;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.CatmullRom;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

public class FlowOverlay extends StGraphOverlay {
	
	HashMap<Node,LineString> flow = new HashMap<Node,LineString>();
	HashMap<Node,Geometry> simpleFlow = new HashMap<Node, Geometry>();
	HashMap<Node,LineString> smoothFlow = new HashMap<Node, LineString>();
	
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
			
            Geometry simple = DouglasPeuckerSimplifier.simplify(cell_path, 3.0);
            if (simple.getCoordinates().length > 2)
            		simpleFlow.put(n, simple);
            
            List<Coordinate> raw = new ArrayList<Coordinate>();
            
            for(int j=0; j < list.size()-1; j+=15){
            		raw.add(list.get(j));
            }
            
            raw.add(list.get(list.size() - 1));
            //raw.addAll(Arrays.asList(simple.getCoordinates()));
            
            List<Coordinate> spline = new ArrayList<Coordinate>(); 
            
            try {
				spline = CatmullRom.interpolate(raw, 20);
			} catch (Exception e) {
				e.printStackTrace();
			}
            
            if(spline.size() > 2){
            		LineString smooth_path = factory.createLineString(
            				spline.toArray(new Coordinate[spline.size()]));
            		
            		smoothFlow.put(n, smooth_path);
            }
            
            	
		}
		
	
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		for(Node n: frame_i.vertexSet()){
			
			n=n.getFirst();
			if(flow.containsKey(n)){
				LineString s = flow.get(n);
				Shape flow = writer.toShape(s);
				g.setColor(Color.cyan);
				//g.draw(flow);
			}
			
			if(smoothFlow.containsKey(n)){
				LineString s = smoothFlow.get(n);
				Shape flow = writer.toShape(s);
				g.setColor(Color.blue);
				g.draw(flow);
			}
			
			if(simpleFlow.containsKey(n)){
				Geometry s = simpleFlow.get(n);
				Shape flow = writer.toShape(s);
				g.setColor(Color.green);
				//g.draw(flow);
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
