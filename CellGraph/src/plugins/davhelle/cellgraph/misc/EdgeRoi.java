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
public class EdgeRoi extends ROI2DShape {

	/**
	 * @param Takes a input shape and creates a ROI
	 */
	public EdgeRoi(Shape shape) {
		super(shape);
	}

}
