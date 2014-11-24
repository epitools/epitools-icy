/**
 * 
 */
package headless;

import ij.process.EllipseFitter;

import java.util.HashMap;
import java.util.Iterator;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.algorithm.MinimumBoundingCircle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * @author Davide Heller
 *
 */
public class DetectDivisionOrientation {

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		SpatioTemporalGraph stGraph = LoadNeoWtkFiles.loadNeo(0);
		HashMap<Node, EllipseFitter> fittedEllipses = 
				new EllipseFitGenerator(stGraph).getFittedEllipses();

		HashMap<Node, Double> division_orientation = computeDivisionOrientation(
				stGraph, fittedEllipses);
		
		for(Node cell: division_orientation.keySet())
			System.out.printf(
					"%.2f\n",
					division_orientation.get(cell));
	}

	/**
	 * Computes the difference between the orientation of the 
	 * longest axis of the mother cell prior to division [1]
	 * and the orientation of the new junction formed between 
	 * the two daughter cells immediately after division [2]
	 * 
	 * @param stGraph
	 * @param fittedEllipses
	 * @return
	 */
	public static HashMap<Node, Double> computeDivisionOrientation(
			SpatioTemporalGraph stGraph,
			HashMap<Node, EllipseFitter> fittedEllipses) {
		
		HashMap<Node, Double> division_orientation = new HashMap<Node, Double>();
		
		//how many frames before division should the mother cell orientation be detected
		int prior_frames = 10;
		
		for(int i=prior_frames; i<stGraph.size(); i++){
			FrameGraph frame = stGraph.getFrame(i);
			Iterator<Division> divisions = frame.divisionIterator();
			while(divisions.hasNext()){
				Division d = divisions.next();
				Node mother = d.getMother();

				//recover mother cell 5 frames prior to division
				FrameGraph frame_prior_rounding = stGraph.getFrame(i - prior_frames);
				if(!frame_prior_rounding.hasTrackID(mother.getTrackID())){
					System.out.printf("No time point for mother %d at %d\n",mother.getTrackID(),prior_frames);
					continue;
				}
				Node mother_before_rounding = frame_prior_rounding.getNode(mother.getTrackID());

				double longest_axis_angle_rad = fittedEllipses.get(mother_before_rounding).theta;
				
				longest_axis_angle_rad = Math.abs(longest_axis_angle_rad - Math.PI);

				//Get children axis
				Node child1 = d.getChild1();
				Node child2 = d.getChild2();

				//TODO: possibly substitute here with edge (saved geo)
				Geometry child_intersection = child1.getGeometry().intersection(child2.getGeometry());

				MinimumBoundingCircle mbc = new MinimumBoundingCircle(child_intersection);
				Coordinate[] new_junction_ends = mbc.getExtremalPoints();
				assert new_junction_ends.length == 2: "Line has more than two endings!";


				//Form the angle to measure
				Coordinate tail = new_junction_ends[1];
				Coordinate tip1 = new_junction_ends[0];
				Coordinate tip2 = new Coordinate(
						tail.x - Math.cos(longest_axis_angle_rad),
						tail.y - Math.sin(longest_axis_angle_rad));


				double new_junction_angle = Angle.interiorAngle(tip1, tail, tip2);

				//find the smallest angle within [0,pi/2]
				if(new_junction_angle > Math.PI)
					new_junction_angle = Angle.PI_TIMES_2 - new_junction_angle;
				if(new_junction_angle > Angle.PI_OVER_2)
					new_junction_angle = Math.PI - new_junction_angle;

				double angle_diff_in_degrees = Angle.toDegrees(new_junction_angle);

				division_orientation.put(mother.getFirst(), Double.valueOf(angle_diff_in_degrees));

				//Inspection
				System.out.printf("Mother %d at %d:\t%.0f\t%.0f\t= %.0f\n",
						mother.getTrackID(),i - prior_frames,
						Angle.toDegrees(longest_axis_angle_rad),
						Angle.toDegrees(findAngleOfLongestAxis(child_intersection)),
						Angle.toDegrees(new_junction_angle));
			}
		}
		return division_orientation;
	}

	/**
	 * @param geometry_to_measure
	 * @return
	 */
	private static double findAngleOfLongestAxis(Geometry geometry_to_measure) {
		MinimumBoundingCircle mbc = new MinimumBoundingCircle(geometry_to_measure);
		
		Coordinate[] longest_axis = mbc.getExtremalPoints();
		if(longest_axis.length == 2)
			return Angle.angle(longest_axis[0], longest_axis[1]);
		else
			return 0.0;
	}

}
