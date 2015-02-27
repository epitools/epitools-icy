/**
 * 
 */
package plugins.davhelle.cellgraph.misc;

import ij.process.EllipseFitter;

import java.util.HashMap;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.algorithm.MinimumBoundingCircle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * @author Davide Heller
 *
 */
public class DivisionOrientationFinder {

	SpatioTemporalGraph stGraph;
	HashMap<Node, EllipseFitter> fittedEllipses;
	int detection_distance;
	int detection_length;
	final boolean USE_INTERSECTION_METHOD = false;
	
	GeometryFactory factory;
	
	public DivisionOrientationFinder(
			SpatioTemporalGraph stGraph,
			HashMap<Node, EllipseFitter> fittedEllipses,
			int detection_distance,
			int detection_length){

		this.stGraph = stGraph;
		this.fittedEllipses = fittedEllipses;
		this.detection_distance = detection_distance;
		this.detection_length = detection_length;
		this.factory = new GeometryFactory();

	}
	
	public HashMap<Node,Double> run(){

		HashMap<Node, Double> division_orientations = new HashMap<Node, Double>();
		
		FrameGraph first_frame = stGraph.getFrame(0);
		
		for(Node n: first_frame.vertexSet()){
			
			if(n.hasObservedDivision()){
				
				int division_time = n.getDivision().getTimePoint();
				int detection_start = division_time - detection_distance;
				int detection_end = detection_start + detection_length;
				
				int no_angles_detected = 0;
				double division_angle_sum = 0;
				
				while(n.hasNext()){
					
					if(
							n.getFrameNo() >= detection_start && 
							n.getFrameNo() < detection_end)
					{
						//find the angle of maximal elongation of the current cell
						assert fittedEllipses.containsKey(n): "Fitted ellipse not found!";
						double longest_axis_angle = fittedEllipses.get(n).theta;
						longest_axis_angle = Math.abs(longest_axis_angle - Math.PI);
						
						//compute the division angle wrt to the latter
						division_angle_sum += computeDivisionOrientation(longest_axis_angle,n.getDivision());
						no_angles_detected++;
					}
					
					//update cell to next reference
					n = n.getNext();
				}
				
				double division_angle_avg = division_angle_sum / no_angles_detected;
				division_orientations.put(n.getFirst(), division_angle_avg);
			}
		}
		return division_orientations;
		
	}

	private double computeDivisionOrientation(double longest_axis_angle_rad,
			Division d) {

		//Get children axis
		Node child1 = d.getChild1();
		Node child2 = d.getChild2();

		Coordinate[] new_junction_ends = null;
		Geometry child_intersection = null;

		if(USE_INTERSECTION_METHOD){
			
			if(d.hasPlaneGeometry())
				child_intersection = d.getPlaneGeometry();
			else{
				child_intersection = child1.getGeometry().intersection(child2.getGeometry());
				d.setPlaneGeometry(child_intersection);
			}

			MinimumBoundingCircle mbc = new MinimumBoundingCircle(child_intersection);
			new_junction_ends = mbc.getExtremalPoints();
			
			assert new_junction_ends.length == 2: "Line has more than two endings!";
		}
		else{
			new_junction_ends = new Coordinate[2];
			new_junction_ends[0] = child1.getCentroid().getCoordinate();
			new_junction_ends[1] = child2.getCentroid().getCoordinate();
			child_intersection = factory.createLineString(new_junction_ends);
		}

		//Form the angle to measure
		Coordinate tail = new_junction_ends[1];
		Coordinate tip1 = new_junction_ends[0];
		Coordinate tip2 = new Coordinate(
				tail.x - Math.cos(longest_axis_angle_rad),
				tail.y - Math.sin(longest_axis_angle_rad));


		double angle_diff_in_rad = Angle.interiorAngle(tip1, tail, tip2);

		//find the smallest angle within [0,pi/2]
		if(angle_diff_in_rad > Math.PI)
			angle_diff_in_rad = Angle.PI_TIMES_2 - angle_diff_in_rad;
		if(angle_diff_in_rad > Angle.PI_OVER_2)
			angle_diff_in_rad = Math.PI - angle_diff_in_rad;

		if(!USE_INTERSECTION_METHOD)
			//because an alignment means that it divides perpendicularly
			angle_diff_in_rad = Angle.PI_OVER_2 - angle_diff_in_rad;
		
		return angle_diff_in_rad;
	}

	
}
