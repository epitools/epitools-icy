package plugins.davhelle.cellgraph.misc;

import icy.sequence.Sequence;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.process.EllipseFitter;
import ij.process.ImageProcessor;

import java.awt.Shape;
import java.util.HashMap;
import java.util.Map;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Generates EllipseFitter for SpatioTemporalGraph Object
 * 
 * Uses ImageJ's 'Fit ellipse' function see source code here:
 * ij.process.EllipseFitter
 * 
 * Every cell geometry is transformed to a ImageJ ROI and
 * the ellipseFit is computed for the latter. A map of ellipse
 * fitter objects and nodes is created.
 * 
 * @author Davide Heller
 *
 */
public class EllipseFitGenerator {
	
	private Map<Node, EllipseFitter> fittedElipses;
	/**
	 * JTS writer to transform the JTS geometries into AWT objects 
	 */
	ShapeWriter sw;
	
	/**
	 * wrapper constructor that extracts the img height and width from the icy sequence
	 * 
	 * @param stGraph spatio temporal graph to be analyzed
	 * @param sequence sequence connected to the stGraph
	 */
	public EllipseFitGenerator(SpatioTemporalGraph stGraph,Sequence sequence){
		this(stGraph,sequence.getWidth(),sequence.getHeight());
	}
	
	/**
	 * Constructor that reads the graph to be analyzed and the original image dimensions to
	 * position the ellipses correctly in imageJ
	 * 
	 * @param stGraph input graph
	 * @param imgWidth graph input file's width 
	 * @param imgHeight graph input file's height
	 */
	public EllipseFitGenerator(SpatioTemporalGraph stGraph,int imgWidth,int imgHeight){
		
		fittedElipses = new HashMap<Node, EllipseFitter>();
		sw = new ShapeWriter();
		
		//initialize data structure for using imageJs roi functions
		//TODO missing flexibility for different image formats
		ImagePlus imp = NewImage.createByteImage(
				"New image", imgWidth, imgHeight, 1, NewImage.FILL_BLACK);
		ImageProcessor ip = imp.getProcessor();
		
		//Compute fitted ellipses for all cells in stGraph
		for(int i=0; i<stGraph.size(); i++)
			for(Node n: stGraph.getFrame(i).vertexSet()){
				EllipseFitter ef = computeEllipseFit(ip, n);
				fittedElipses.put(n, ef);	
			}
		
		imp.close();
	}
	
	/**
	 * Computes the ellipsFit for an individual node
	 * 
	 * @param ip imageJ processor on which the node geometry will be projected
	 * @param n input node
	 * @return Ellipse fitting
	 */
	public EllipseFitter computeEllipseFit(ImageProcessor ip, Node n) {
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
		
		assert ef.theta <= Math.PI: String.format(
				"Ouput is larger than PI: %.0f\n", Angle.toDegrees(ef.theta));
			
		//Visualize if needed
		//ef.drawEllipse(ip);
		//TODO transform this back to a shape somehow
		return ef;
	}
		
	/**
	 * @return Ellipse fitting map
	 */
	public Map<Node, EllipseFitter> getFittedEllipses(){
		return fittedElipses;
	}

}
