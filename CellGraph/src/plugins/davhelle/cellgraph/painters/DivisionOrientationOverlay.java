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

import com.vividsolutions.jts.algorithm.Angle;

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
		super("Division Orientation");
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

						double normalized_angle = Math.abs(1 - angle/90);
						normalized_angle = normalized_angle * 0.3;

						Color hsbColor = Color.getHSBColor(
								(float)(normalized_angle),
								1f,
								1f);

						g.setColor(hsbColor);
						g.fill((n.toShape()));

						g.setColor(Color.cyan);
						
						EllipseFitter ef = fittedEllipses.get(n);
						
						//draw the future division axis
						
						double future_junction_angle_wrt_x = n.getDivision().getNewJunctionOrientation();
						
						double x0 = cX + Math.cos(Angle.toRadians(future_junction_angle_wrt_x)) * 5;
				        double y0 = cY + Math.sin(Angle.toRadians(future_junction_angle_wrt_x)) * 5;
						double x1 = cX - Math.cos(Angle.toRadians(future_junction_angle_wrt_x)) * 5;
				        double y1 = cY - Math.sin(Angle.toRadians(future_junction_angle_wrt_x)) * 5;
						
						g.draw(new Line2D.Double(x0, y0,x1,y1));
						g.drawString(String.format(
								"%.0f,%.0f,%.0f\n",
								n.getDivision().getLongestMotherAxisOrientation(),
								n.getDivision().getNewJunctionOrientation(),
								n.getDivision().getDivisionOrientation()), 
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
					
					//g.fillRect(20*i + 30,30,20,20);
				
				}
				
				
			}
			
			
			g.setColor(old);
		}

	
    }
	
}
