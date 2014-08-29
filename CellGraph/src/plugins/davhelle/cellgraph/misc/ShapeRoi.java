/**
 * 
 */
package plugins.davhelle.cellgraph.misc;

import java.awt.Shape;

import plugins.kernel.roi.roi2d.ROI2DShape;

/**
 * @author Davide Heller
 *
 */
public class ShapeRoi extends ROI2DShape {

	/**
	 * @param Takes a input shape and creates a ROI
	 */
	public ShapeRoi(Shape shape) {
		super(shape);
	}

}
