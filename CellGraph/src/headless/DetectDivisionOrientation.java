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

		
		for(int i=5; i<stGraph.size(); i++){
			FrameGraph frame = stGraph.getFrame(i);
			Iterator<Division> divisions = frame.divisionIterator();
			while(divisions.hasNext()){
				Division d = divisions.next();
				Node mother = d.getMother();
				
				//recover mother cell 5 frames prior to division
				FrameGraph frame_prior_rounding = stGraph.getFrame(i - 5);
				assert frame_prior_rounding.hasTrackID(mother.getTrackID()): String.format("No mother %d at -5",mother.getTrackID());
				Node mother_before_rounding = frame_prior_rounding.getNode(mother.getTrackID());
				
				double longest_axis_angle_rad = fittedEllipses.get(mother_before_rounding).theta;
				
				//Get children axis
				Node child1 = d.getChild1();
				Node child2 = d.getChild2();
				
				//TODO: possibly substitute here with edge (saved geo)
				Geometry child_intersection = child1.getGeometry().intersection(child2.getGeometry());
				double new_junction_angle = findAngleOfLongestAxis(child_intersection);
				
				if(longest_axis_angle_rad != 0.0 || new_junction_angle != 0.0){
				double angle_difference = Angle.diff(new_junction_angle, longest_axis_angle_rad);
				System.out.printf(
						"%.2f\n",
						Angle.toDegrees(angle_difference));
				}
			}
		}

	}

	/**
	 * @param geometry_to_measure
	 * @return
	 */
	private static double findAngleOfLongestAxis(Geometry geometry_to_measure) {
		MinimumBoundingCircle mbc = new MinimumBoundingCircle(geometry_to_measure);
		
		Coordinate[] longest_axis = mbc.getExtremalPoints();
		if(longest_axis.length == 2){ 
		
		double longest_axis_angle_rad = Angle.angle(longest_axis[0], longest_axis[1]);
		double longest_axis_angle_deg = Angle.toDegrees(longest_axis_angle_rad);
		return longest_axis_angle_rad;
		}
		else{
			return 0.0;
		}
	}

}
