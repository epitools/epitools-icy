/**
 * 
 */
package plugins.davhelle.cellgraph.painters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.HashMap;

import com.vividsolutions.jts.algorithm.ConvexHull;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;
import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import ij.gui.ShapeRoi;
import ij.process.EllipseFitter;
import ij.process.ImageProcessor;

/**
 * @author Davide Heller
 *
 */
public class ConvexHullOverlay extends Overlay {

	private SpatioTemporalGraph stGraph;
	private HashMap<Node, Shape> convexHulls;
	
	public ConvexHullOverlay(SpatioTemporalGraph spatioTemporalGraph) {
		super("Convex hull");
		stGraph = spatioTemporalGraph;
		convexHulls = new HashMap<Node, Shape>();
		ShapeWriter sw = new ShapeWriter();
		
		for(Node n: stGraph.getFrame(0).vertexSet()){
			
			Geometry g = n.getGeometry();
			Geometry convex_hull = new ConvexHull(g).getConvexHull();
			
			Shape shape = sw.toShape(convex_hull);
			
			ShapeRoi imageJ_roi = new ShapeRoi(shape);
			ImageProcessor ip = imageJ_roi.getMask();
			ip.setRoi(imageJ_roi);
			EllipseFitter ef = new EllipseFitter(); 
			ef.fit(ip,null);
			ef.makeRoi(ip);
			
			//transform this back to a shape somehow
		
			convexHulls.put(n, shape);	
		}
	}
	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			//TODO include 3D information (in case of VTK)!
			Color old = g.getColor();
			
			g.setColor(Color.green);
			
			for(Node n: stGraph.getFrame(time_point).vertexSet()){
				if(convexHulls.containsKey(n))
					g.draw(convexHulls.get(n));
				
				if(g.getColor() == Color.green)
					g.setColor(Color.blue);
				else
					g.setColor(Color.green);
				
			}
			
			g.setColor(old);
		}
    }

}
