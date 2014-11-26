/*=========================================================================
 *
 *  (C) Copyright (2012-2014) Basler Group, IMLS, UZH
 *  
 *  All rights reserved.
 *	
 *  author:	Davide Heller
 *  email:	davide.heller@imls.uzh.ch
 *  
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

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
 * Overlay to display the fitted ellipses for each polygon
 * 
 * @author Davide Heller
 * @date 21.11.2014
 *
 */
public class EllipseFitterOverlay extends Overlay {

	private SpatioTemporalGraph stGraph;
	private HashMap<Node, EllipseFitter> fittedEllipses;
	
	public EllipseFitterOverlay(SpatioTemporalGraph spatioTemporalGraph) {
		super("Ellipse Fitter");
		stGraph = spatioTemporalGraph;
		fittedEllipses = new EllipseFitGenerator(stGraph).getFittedEllipses();
	}
	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			//TODO include 3D information (in case of VTK)!
			Color old = g.getColor();
			Color ellipseColor = Color.green;
			paintFrame(g, time_point, ellipseColor);
			
			g.setColor(old);
		}
    }

	/**
	 * @param g
	 * @param time_point
	 */
	public void paintFrame(Graphics2D g, int time_point,Color color) {
		
		g.setColor(color);
		int fontSize = 3;
		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
		
		for(Node n: stGraph.getFrame(time_point).vertexSet()){
			if(fittedEllipses.containsKey(n)){
				EllipseFitter ef = fittedEllipses.get(n);
				/* 
				 * http://rsb.info.nih.gov/ij/developer/source/ij/process/EllipseFitter.java.html
				 * 
				 * ef.major: major axis 
				 * ef.minor: minor axis
				 * ef.theta:, angle of major axis, clockwise with respect to x axis
				 * 
				 * for complete ellipse drawing see
				 * Draws the ellipse on the specified image.
				 * public void drawEllipse(ImageProcessor ip) 
				 * 
				 * */
				
				double cX = n.getGeometry().getCentroid().getX();
				double cY = n.getGeometry().getCentroid().getY();
				double length = ef.major / 2.0;
				if(length > 10)
					length -= 5;
				
				double x0 = cX - Math.cos(ef.theta) * length;
		        double y0 = cY + Math.sin(ef.theta) * length;
		        double x1 = cX + Math.cos(ef.theta) * length;
		        double y1 = cY - Math.sin(ef.theta) * length;
				
				g.draw(new Line2D.Double(x0, y0, x1, y1));
			}
		}
	}

}
