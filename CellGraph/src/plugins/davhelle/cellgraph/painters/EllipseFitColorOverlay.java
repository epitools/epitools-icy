 	/**
 * 
 */
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.type.point.Point5D;
import icy.util.XLSUtil;
import ij.process.EllipseFitter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.HashMap;

import jxl.write.WritableSheet;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

/**
 * Overlay to color cells according to the longest axis 
 * and a given roi center.
 * @author Davide Heller
 *
 */
public class EllipseFitColorOverlay extends StGraphOverlay{

	public static final String DESCRIPTION = "To enable this plugin a Point ROI must be present on the image. "+
	"The overlay computes the angle with respect to (wrt) the estimated ellipse and displays it as a color code. " +
	"Red being the perpendicular case and Green the parallel case (Longest axis vs Segment joining ROI and Ellipse Centroid)";
	
	private HashMap<Node, EllipseFitter> fittedEllipses;
	private Coordinate roi_coor;
	
	public EllipseFitColorOverlay(SpatioTemporalGraph spatioTemporalGraph) {
		super("Ellipse Orientation wrt PointROI",spatioTemporalGraph);

		fittedEllipses = new EllipseFitGenerator(stGraph).getFittedEllipses();
		
		//initialize empty
		roi_coor = null;
	
	}

	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		double[] heat_map = {0.0,0.25,0.5,0.75,1.0};
		
		int time_point = canvas.getPositionT();

		if(time_point < stGraph.size()){
			
			if(sequence.hasROI()){
				if(sequence.getROIs().size() == 1 ){
					ROI point_roi = sequence.getROIs().get(0);

					Point5D position = point_roi.getPosition5D();

					double xp = position.getX();
					double yp = position.getY();
					boolean show_guides = false;

					paintFrame(g, time_point, xp, yp, show_guides);
				}
			}
		}
    }

	/**
	 * @param g
	 * @param time_point
	 * @param xp_roi
	 * @param yp_roi
	 * @param show_guides
	 */
	public void paintFrame(Graphics2D g, int time_point, double xp_roi,
			double yp_roi, boolean show_guides) {
		
		//Update ROI coor
		roi_coor = new Coordinate(xp_roi, yp_roi);
		
		int fontSize = 2;
		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
				
		for(Node n: stGraph.getFrame(time_point).vertexSet()){
			if(fittedEllipses.containsKey(n)){
				double angle_difference = computeAngleWrtLongestAxis(roi_coor,
						n);
				
				
				double normalized_angle = Math.abs(1 - angle_difference/Angle.PI_OVER_2);
				normalized_angle = normalized_angle * 0.3;

				Color hsbColor = Color.getHSBColor(
						(float)(normalized_angle),
						1f,
						1f);
				

				g.setColor(hsbColor);
				g.fill((n.toShape()));
				
				//DebugTools
				if(show_guides){
					g.setColor(Color.BLACK);
					g.draw(new Line2D.Double(xp_roi, yp_roi,n.getCentroid().getX(),	n.getCentroid().getY()));						
//					g.drawString(String.format("%.0f, %.0f, %.0f",
//							Angle.toDegrees(angle0),
//							Angle.toDegrees(angle1),
//							Angle.toDegrees(angle_difference)), 
//							(float)cell_center.x - 5  , 
//							(float)cell_center.y + 5);
				}
				
//				System.out.printf("%.2f\t%.2f\n",Angle.toDegrees(angle_difference),
//						new LineSegment(roi_coor,cell_center).getLength());
			}
		}
	}

	/**
	 * @param roi_coor
	 * @param n
	 * @return
	 */
	private double computeAngleWrtLongestAxis(Coordinate roi_coor, Node n) {
		EllipseFitter ef = fittedEllipses.get(n);
		
		double cX = n.getGeometry().getCentroid().getX();
		double cY = n.getGeometry().getCentroid().getY();
		
		//ellipse major axis coordinates
		double x0 = cX - Math.cos(ef.theta);
		double y0 = cY + Math.sin(ef.theta);
		double x1 = cX + Math.cos(ef.theta);
		double y1 = cY - Math.sin(ef.theta);
		
		Coordinate tip0 = new Coordinate(x0, y0);
		Coordinate tip1 = new Coordinate(x1, y1);
		
		Coordinate cell_center = n.getGeometry().getCentroid().getCoordinate();
		
		//This could be done with one angle since the two are complementary
		double angle0 = Angle.interiorAngle(roi_coor, cell_center, tip0);
		double angle1 = Angle.interiorAngle(roi_coor, cell_center, tip1);
		
		if(angle0 > Math.PI)
			angle0 = Angle.PI_TIMES_2 - angle0;
		if(angle1 > Math.PI)
			angle1 = Angle.PI_TIMES_2 - angle1;
		
		double angle_difference = Math.min(angle0, angle1);
		return angle_difference;
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		// TODO Auto-generated method stub
		
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		if(roi_coor == null)
			return;
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 2, 0, "Centroid y");
		XLSUtil.setCellString(sheet, 3, 0, 
				String.format("Longest axis oreintation wrt %.0f,%.0f",roi_coor.x,roi_coor.y));

		int row_no = 1;

		for(Node n: frame.vertexSet()){
			if(fittedEllipses.containsKey(n)){
				double angle_difference = computeAngleWrtLongestAxis(roi_coor,
						n);
			
				XLSUtil.setCellNumber(sheet, 0, row_no, n.getTrackID());
				XLSUtil.setCellNumber(sheet, 1, row_no, n.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 2, row_no, n.getCentroid().getY());
				XLSUtil.setCellNumber(sheet, 3, row_no, angle_difference);
				row_no++;

			}
		}
	}
}
