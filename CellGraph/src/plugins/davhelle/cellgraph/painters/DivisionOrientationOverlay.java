/**
 * 
 */
package plugins.davhelle.cellgraph.painters;

import headless.DetectDivisionOrientation;
import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import ij.process.EllipseFitter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.HashMap;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Overaly to visualize the orientation of a dividing cell
 * and the new junction of the daughter cells
 * 
 * @author Davide Heller
 *
 */
public class DivisionOrientationOverlay extends Overlay {

	private SpatioTemporalGraph stGraph;
	private HashMap<Node, EllipseFitter> fittedEllipses;
	private HashMap<Node, Double> division_orientation;
	
	public DivisionOrientationOverlay(SpatioTemporalGraph spatioTemporalGraph) {
		super("Ellipse Fitter");
		stGraph = spatioTemporalGraph;
		fittedEllipses = new EllipseFitGenerator(stGraph).getFittedEllipses();
		
		division_orientation = DetectDivisionOrientation.computeDivisionOrientation(
				stGraph, fittedEllipses);
		
	}

	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		double[] heat_map = {0.0,0.25,0.5,0.75,1.0};
		
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			//TODO include 3D information (in case of VTK)!
			Color old = g.getColor();
			
			g.setColor(Color.green);
			int fontSize = 3;
			g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
			
			for(Node n: stGraph.getFrame(time_point).vertexSet()){
				if(fittedEllipses.containsKey(n)){
					if(division_orientation.containsKey(n.getFirst())){

						double cX = n.getGeometry().getCentroid().getX();
						double cY = n.getGeometry().getCentroid().getY();
						double angle = division_orientation.get(n.getFirst());



						double normalized_angle = angle/90;
						normalized_angle = normalized_angle * 0.3;

						Color hsbColor = Color.getHSBColor(
								(float)(normalized_angle),
								1f,
								1f);

						g.setColor(hsbColor);
						g.fill((n.toShape()));

						g.setColor(Color.black);
						g.drawString(String.format("%.0f", angle), 
								(float)cX - 5  , 
								(float)cY + 5);
					}
				}
				
				//add color scale bar [0-90]
				for(int i=0; i<heat_map.length; i++){
					
					Color hsbColor = Color.getHSBColor(
							(float)(heat_map[i] * 0.3),
							1f,
							1f);
					
					g.setColor(hsbColor);
					
					g.fillRect(20*i + 30,30,20,20);
				
				}
				
				
			}
			
			
			g.setColor(old);
		}

	
    }
	
}
