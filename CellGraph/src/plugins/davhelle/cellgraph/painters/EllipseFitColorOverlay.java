/**
 * 
 */
package plugins.davhelle.cellgraph.painters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.nodes.Node;
import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.roi.ROI;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;
import icy.type.point.Point5D;
import ij.process.EllipseFitter;

/**
 * Overlay to color cells according to the longest axis 
 * and a given roi center.
 * @author Davide Heller
 *
 */
public class EllipseFitColorOverlay extends Overlay{

	private HashMap<Node, EllipseFitter> fittedEllipses;
	private SpatioTemporalGraph stGraph;

	
	public EllipseFitColorOverlay(SpatioTemporalGraph spatioTemporalGraph) {
		super("EllipseFit Coloring");

		stGraph = spatioTemporalGraph;
		fittedEllipses = new EllipseFitGenerator(stGraph).getFittedEllipses();
	
	}

	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		double[] heat_map = {0.0,0.25,0.5,0.75,1.0};
		
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			
			if(sequence.hasROI()){
				ROI point_roi = sequence.getSelectedROI();
				
				Point5D position = point_roi.getPosition5D();
				
				double xp = position.getX();
				double yp = position.getY();
				
				Coordinate roi_coor = new Coordinate(xp, yp);
						
				for(Node n: stGraph.getFrame(time_point).vertexSet()){
					if(fittedEllipses.containsKey(n)){
						EllipseFitter ef = fittedEllipses.get(n);
						double cell_orientation = ef.theta;

						Coordinate cell_center = n.getGeometry().getCentroid().getCoordinate();
						double roi_cell_angle = Angle.angle(roi_coor, cell_center);

						double angle_difference = Angle.diff(cell_orientation, roi_cell_angle);

						double normalized_angle = angle_difference/Math.PI;
						normalized_angle = normalized_angle * 0.3;

						Color hsbColor = Color.getHSBColor(
								(float)(normalized_angle),
								1f,
								1f);

						g.setColor(hsbColor);
						g.fill((n.toShape()));
					}
				}
			}
		}
    }
}
