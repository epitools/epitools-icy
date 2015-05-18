package plugins.davhelle.cellgraph.misc;

import java.awt.Shape;

import plugins.kernel.roi.roi2d.ROI2DShape;

/**
 * Wrapper class to extend ROI2Dshape
 * 
 * @author Davide Heller
 *
 */
public class ShapeRoi extends ROI2DShape {

	/**
	 * @param Takes a input shape and creates an icy ROI
	 */
	public ShapeRoi(Shape shape) {
		super(shape);
	}

}
