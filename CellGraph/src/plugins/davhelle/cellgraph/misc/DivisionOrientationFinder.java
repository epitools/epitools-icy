package plugins.davhelle.cellgraph.misc;

import ij.process.EllipseFitter;

import java.util.HashMap;
import java.util.Map;

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
 * Class to detect the division angles, i.e. the angle formed by the longest
 * axis of the mother cell and the connecting axis of the the daughter cells. 
 * 
 * @author Davide Heller
 *
 */
public class DivisionOrientationFinder {

	/**
	 * StGraph being analyzed
	 */
	SpatioTemporalGraph stGraph;
	/**
	 * EllipseFit map for every cell
	 */
	Map<Node, EllipseFitter> fittedEllipses;
	/**
	 * number of time points before division to start measuring the longest axis angle
	 */
	int detection_distance;
	/**
	 * number of time points to average for the longest axis and the children axis
	 */
	int detection_length;
	/**
	 * Flag to use the geometrical intersection instead of the centroid connection for the children axis
	 */
	final boolean USE_INTERSECTION_METHOD = false;
	
	GeometryFactory factory;
	
	/**
	 * @param stGraph graph to analyze
	 * @param fittedEllipses map of fitted ellipses for each cell in the stGraph
	 * @param detection_distance number of time points before division to start measuring the longest axis angle 
	 * @param detection_length number of time points to average for the longest axis and the children axis
	 */
	public DivisionOrientationFinder(
			SpatioTemporalGraph stGraph,
			Map<Node, EllipseFitter> fittedEllipses,
			int detection_distance,
			int detection_length){

		this.stGraph = stGraph;
		this.fittedEllipses = fittedEllipses;
		this.detection_distance = detection_distance;
		this.detection_length = detection_length;
		this.factory = new GeometryFactory();

	}
	
	/**
	 * Identify all division orientations and return a map with one angle per division
	 * 
	 * @return map with the division angle for every dividing node
	 */
	public Map<Node,Double> run(){

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
				
				if(no_angles_detected != 0){
					double division_angle_avg = division_angle_sum / no_angles_detected;
					division_orientations.put(n.getFirst(), division_angle_avg);
				}
			}
		}
		return division_orientations;
		
	}

	/**
	 * Computes the angle difference between the longest axis angle of input 
	 * and the children intersection at several time points from the moment 
	 * of division. (The number is defined by the detection_distance field)
	 * 
	 * @param longest_axis_angle_rad the orientation of the longest mother cell orientation
	 * @param d the division object being investigated
	 * @return the averaged division orientation
	 */
	public double computeDivisionOrientation(double longest_axis_angle_rad,
			Division d) {
		
		//Get children axis
		Node child1 = d.getChild1();
		Node child2 = d.getChild2();
		
		int no_segments_detected = 0;
		double angle_diff_sum = 0;
		
		for(int i=0; i<detection_length; i++){

			int detection_time = d.getTimePoint() + i;
			
			if(i > 0 && child1.hasNext())
				child1 = child1.getNext();
			
			if(i > 0 && child2.hasNext())
				child2 = child2.getNext();
			
			if(child1.getFrameNo() != detection_time || child2.getFrameNo() != detection_time)
				continue;

			Coordinate[] new_junction_ends = computeChildrenIntersection(child1,
				child2);
			
			if(d.getNewJunctionOrientation() == 0.0)
				d.setNewJunctionOrientation(Angle.angle(new_junction_ends[0], new_junction_ends[1]));

			angle_diff_sum += findSmallestInteriorAngle(
				longest_axis_angle_rad, new_junction_ends);
			
			no_segments_detected++;
		}
		
		return angle_diff_sum / no_segments_detected;
	}

	/**
	 * Given two children cells this function computes the connecting axis between the two children cell
	 * 
	 * @param child1 first child cell of the division
	 * @param child2 second child cell of the division
	 * @return coordinates of the connecting segment
	 */
	private Coordinate[] computeChildrenIntersection(Node child1, Node child2) {
		Coordinate[] new_junction_ends = null;
		Geometry child_intersection = null;

		if(USE_INTERSECTION_METHOD){

			child_intersection = child1.getGeometry().intersection(child2.getGeometry());

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
		return new_junction_ends;
	}

	/**
	 * Given the children intersection segment and the angle of the longest axis
	 * of the mother cell, this function computes the minimal angle between them.
	 * 
	 * @param longest_axis_angle_rad angle of the longest axis of the mother cell
	 * @param new_junction_ends segment connecting the two children cells
	 * @return the division orientation angle
	 */
	private double findSmallestInteriorAngle(double longest_axis_angle_rad,
			Coordinate[] new_junction_ends) {
		
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
