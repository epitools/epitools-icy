/**
 * 
 */
package plugins.davhelle.cellgraph.overlays;

import icy.sequence.Sequence;
import icy.util.XLSUtil;
import ij.process.EllipseFitter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.DivisionOrientationFinder;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.algorithm.Angle;

/**
 * Overaly to visualize the orientation of a dividing cell
 * and the new junction of the daughter cells
 * 
 * @author Davide Heller
 *
 */
public class DivisionOrientationOverlay extends StGraphOverlay {

	private Map<Node, EllipseFitter> fittedEllipses;
	private Map<Node, Double> division_orientation;
	private final boolean DEBUG_FLAG = false;

	public static final String DESCRIPTION = "Color codes the dividing cells according to their new junction orientation" +
			" (Longest axis of mother cell vs New junction). The more red the cells are the more the new junstion is " +
			"perpendicular to the longest axis of the mother cell, the more green the cell the more parallel the new" +
			"junction is.\n\n" +
			"[Detection start] = how many frames before the division should the longest axis be taken\n\n" +
			"[Detection length]= how many frames should be averaged for the longest axis and the new junction\n";
	
	public DivisionOrientationOverlay(SpatioTemporalGraph spatioTemporalGraph, Sequence sequence,
			int detection_distance, int detection_length) {
		super("Division Orientation",spatioTemporalGraph);
		fittedEllipses = new EllipseFitGenerator(stGraph,sequence).getFittedEllipses();
		
		division_orientation = 
				new DivisionOrientationFinder(stGraph, fittedEllipses, detection_distance, detection_length).run();
		
	}
	
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		Color old = g.getColor();
		double[] heat_map = {0.0,0.25,0.5,0.75,1.0};
		
		Color newJunctionAngleColor = Color.BLACK;
		Color longestAxisOrientationColor = Color.darkGray;

		int fontSize = 3;
		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
		
		int time_point = frame_i.getFrameNo();
		
		for(Node n: frame_i.vertexSet()){
			if(fittedEllipses.containsKey(n)){
				if(division_orientation.containsKey(n.getFirst())){

					double cX = n.getGeometry().getCentroid().getX();
					double cY = n.getGeometry().getCentroid().getY();
					double angle = Angle.toDegrees(division_orientation.get(n.getFirst()));

					//Set limit if desired
					//if(angle > 30)
					//	continue;
					
					double normalized_angle = Math.abs(1 - angle/90);
					normalized_angle = normalized_angle * 0.3;

					Color hsbColor = Color.getHSBColor(
							(float)(normalized_angle),
							1f,
							1f);

					g.setColor(hsbColor);
					g.fill((n.toShape()));

					
					//draw the future division axis
					
					double future_junction_angle_wrt_x = n.getDivision().getNewJunctionOrientation();
					
					double future_junction_angle_radians = Angle.toRadians(future_junction_angle_wrt_x);
					double x0 = cX + Math.cos(future_junction_angle_radians) * 5;
			        double y0 = cY + Math.sin(future_junction_angle_radians) * 5;
					double x1 = cX - Math.cos(future_junction_angle_radians) * 5;
			        double y1 = cY - Math.sin(future_junction_angle_radians) * 5;
					
			        double longestMotherAxisOrientation = n.getDivision().getLongestMotherAxisOrientation();
			        double mx0 = cX + Math.cos(longestMotherAxisOrientation) * 5;
			        double my0 = cY + Math.sin(longestMotherAxisOrientation) * 5;
					double mx1 = cX - Math.cos(longestMotherAxisOrientation) * 5;
			        double my1 = cY - Math.sin(longestMotherAxisOrientation) * 5;
			        
			        //Draw new Junction
			        g.setColor(newJunctionAngleColor);
					g.draw(new Line2D.Double(x0, y0,x1,y1));
					
					//Draw chosen Mother orientation
					if(DEBUG_FLAG){
						g.setColor(longestAxisOrientationColor);
						g.draw(new Line2D.Double(mx0, my0,mx1,my1));
					}
					
					//Give text information about angles
					if(DEBUG_FLAG)
						if(division_orientation.containsKey(n.getFirst())){

							double angle2 = Angle.toDegrees(
									division_orientation.get(n.getFirst()));

							EllipseFitter ef = fittedEllipses.get(n);
							double longest_axis_angle = Math.abs(ef.theta - Math.PI);
							double current_min_diff = Angle.diff(longest_axis_angle, future_junction_angle_radians);
							current_min_diff = Angle.toDegrees(current_min_diff);

							if(current_min_diff > 90)
								current_min_diff = Math.abs(current_min_diff - 180);

							int time_to_division = n.getDivision().getTimePoint() - time_point;

							g.setColor(newJunctionAngleColor);
							g.drawString(String.format(
									"-%d :%.0f,%.0f\n",
									time_to_division,angle,current_min_diff), 
									(float)cX - 5  , 
									(float)cY + 5);
						}
					//System.out.printf("%.2f\n",n.getDivision().getDivisionOrientation());
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

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		XLSUtil.setCellString(sheet, 0, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 1, 0, "Centroid y");
		XLSUtil.setCellString(sheet, 2, 0, "Division Orientation");

		int row_no = 1;
		
		Iterator<Division> divisions = frame.divisionIterator();
		while(divisions.hasNext()){
			Division d = divisions.next();
			Node mother = d.getMother();
			
			if(division_orientation.containsKey(mother.getFirst())){
				
				double angle = Angle.toDegrees(
						division_orientation.get(mother.getFirst()));
			
				XLSUtil.setCellNumber(sheet, 0, row_no, mother.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 1, row_no, mother.getCentroid().getY());
				XLSUtil.setCellNumber(sheet, 2, row_no, angle);

				row_no++;
			}
		}
	}

	@Override
	public void specifyLegend(Graphics2D g, java.awt.geom.Line2D.Double line) {
		
		int binNo = 50;
		double shift_factor = 0;

		OverlayUtils.gradientColorLegend_ZeroOne(g, line,"90\u00b0","0\u00b0",
				binNo, 0.3, shift_factor);
		
	}
	
}