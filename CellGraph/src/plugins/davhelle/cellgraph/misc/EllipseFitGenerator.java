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
package plugins.davhelle.cellgraph.misc;

import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.EllipseFitter;
import ij.process.ImageProcessor;

import java.awt.Shape;
import java.util.HashMap;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Generates EllipseFitter for SpatioTemporalGraph Object
 * 
 * Uses ImageJ's 'Fit ellipse' function
 * 
 * @author Davide Heller
 *
 */
public class EllipseFitGenerator {
	
	private HashMap<Node, EllipseFitter> fittedElipses;
	
	public EllipseFitGenerator(SpatioTemporalGraph stGraph){
		
		fittedElipses = new HashMap<Node, EllipseFitter>();
		ShapeWriter sw = new ShapeWriter();
		
		//initialize data structure for using imageJs roi functions
		ImagePlus imp = NewImage.createByteImage(
				"New image", 1392, 1040, 1, NewImage.FILL_BLACK);
		ImageProcessor ip = imp.getProcessor();
		
		//Compute fitted ellipses for all cells in stGraph
		for(int i=0; i<stGraph.size(); i++)
			for(Node n: stGraph.getFrame(i).vertexSet()){

				Geometry g = n.getGeometry();
				Shape shape = sw.toShape(g);

				ShapeRoi imageJ_roi = new ShapeRoi(shape);
				Roi[] rois = imageJ_roi.getRois();

				assert rois.length == 1: "More than one polygon found";
				assert rois[0] instanceof PolygonRoi: "Non polygonal roi found";
				PolygonRoi my_roi = (PolygonRoi)rois[0];

				ImageProcessor roi_mask = my_roi.getMask();
				assert roi_mask != null: "No mask defined";
				
				//set the current cell ROI to estimate the ellipse			
				ip.setRoi(my_roi);
				
				//Debugging functions
				//ij.gui.Overlay overlay = new ij.gui.Overlay(); 
				//overlay.add((Roi)my_roi); 
				//imp.setOverlay(overlay); 
				//imp.show(); 

				//compute ellipse
				EllipseFitter ef = new EllipseFitter(); 
				ef.fit(ip,null);
				
				//Visualize if needed
				//ef.drawEllipse(ip);
				//TODO transform this back to a shape somehow
				
				//store result
				fittedElipses.put(n, ef);	
			}
		
		imp.close();
	}
		
	public HashMap<Node, EllipseFitter> getFittedEllipses(){
		return fittedElipses;
	}

}
