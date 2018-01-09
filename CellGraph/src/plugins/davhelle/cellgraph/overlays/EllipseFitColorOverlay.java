package plugins.davhelle.cellgraph.overlays;

import icy.canvas.IcyCanvas;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.type.point.Point5D;
import icy.util.XLSUtil;
import ij.process.EllipseFitter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Line2D.Double;
import java.util.Map;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * Overlay to color cells according to the angle between the longest axis 
 * and a given roi center.
 * 
 * @author Davide Heller
 *
 */
public class EllipseFitColorOverlay extends StGraphOverlay{

	public static final String DESCRIPTION = 
			"To enable this plugin a Point ROI must be present on the<br/>" +
			" image. The overlay computes the angle with respect to (wrt)<br/>" +
			" the estimated ellipse and displays it as a color code. <br/><br/>" +
			" Red being the perpendicular case and Green the parallel case<br/>" +
			" (Longest axis vs Segment joining ROI and Ellipse Centroid)";
	
	/**
	 * Ellipse fit for each node
	 */
	private Map<Node, EllipseFitter> fittedEllipses;
	/**
	 * ROI coordinate specified by the user
	 */
	private Coordinate roi_coor;
	/**
	 * Sequence on which the ROI is placed
	 */
	private Sequence sequence;
	
	/**
	 * @param spatioTemporalGraph graph to be analyzed
	 * @param sequence Icy sequence on which the ROI is placed
	 */
	public EllipseFitColorOverlay(SpatioTemporalGraph spatioTemporalGraph, Sequence sequence) {
		super("Ellipse Orientation wrt PointROI",spatioTemporalGraph);

		fittedEllipses = new EllipseFitGenerator(stGraph,sequence).getFittedEllipses();
		this.sequence = sequence; 
		//initialize empty
		roi_coor = null;
		
		super.setGradientMaximum(90);
		super.setGradientMinimum(0);
		super.setGradientScale(-0.3);
		super.setGradientShift(0.3);
		super.setGradientControlsVisibility(true);
	
	}

	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		
		int time_point = canvas.getPositionT();

		if(time_point < stGraph.size()){
			
			if(sequence.hasROI()){
				if(sequence.getROIs().size() == 1 ){
					ROI point_roi = sequence.getROIs().get(0);

					Point5D position = point_roi.getPosition5D();

					double xp = position.getX();
					double yp = position.getY();
					
					//Update ROI coordinate
					roi_coor = new Coordinate(xp, yp);

					paintFrame(g, stGraph.getFrame(time_point));
					
				}
			}
			
			if(super.isLegendVisible())
				super.paintLegend(g,sequence,canvas);
		}
    }

	/**
	 * Compute angle between user specified ROI and longest axis of node n
	 * 
	 * @param roi_coor user specified ROI coordinate
	 * @param n node to be computed
	 * @return between user specified ROI and longest axis of node n 
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
		
		boolean show_guides = false;
		
		int fontSize = 2;
		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
				
		for(Node n: frame_i.vertexSet()){
			if(fittedEllipses.containsKey(n)){
				double angle_difference = computeAngleWrtLongestAxis(roi_coor,
						n);
				
				
				double normalized_angle = angle_difference/Angle.PI_OVER_2;

				Color hsbColor = Color.getHSBColor(
						(float)(normalized_angle * super.getGradientScale() + super.getGradientShift()),
						1f,
						1f);
				

				g.setColor(hsbColor);
				g.fill((n.toShape()));
				
				//DebugTools
				if(show_guides){
					g.setColor(Color.BLACK);
					g.draw(new Line2D.Double(roi_coor.x, roi_coor.y,n.getCentroid().getX(),	n.getCentroid().getY()));						
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

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		
		if(sequence.hasROI()){
			int binNo = 50;

			OverlayUtils.gradientColorLegend_ZeroOne(g, line,"0\u00b0","90\u00b0",
					binNo, super.getGradientScale(), super.getGradientShift());

		} else {

			String s = "No Point ROI detected - Please add one";
			Color c = Color.WHITE;
			int offset = 0;

			OverlayUtils.stringColorLegend(g, line, s, c, offset);

		}
		
	}
}
