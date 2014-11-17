/**
 * 
 */
package headless;

import java.util.Iterator;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.algorithm.MinimumBoundingCircle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
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
		
		for(int i=0; i<stGraph.size(); i++){
			FrameGraph frame = stGraph.getFrame(i);
			Iterator<Division> divisions = frame.divisionIterator();
			while(divisions.hasNext()){
				Division d = divisions.next();
				Node mother = d.getMother();
				
				Geometry mother_geomerty = mother.getGeometry();
				double longest_axis_angle_rad = findAngleOfLongestAxis(mother_geomerty);
				longest_axis_angle_rad = Math.abs(longest_axis_angle_rad);
				
				//Get children axis
				Node child1 = d.getChild1();
				Node child2 = d.getChild2();
				
				//TODO: possibly substitute here with edge (saved geo)
				Geometry child_intersection = child1.getGeometry().intersection(child2.getGeometry());
				double new_junction_angle = findAngleOfLongestAxis(child_intersection);
				
				double angle_difference = Angle.diff(new_junction_angle, longest_axis_angle_rad);
				System.out.printf(
						"Angle difference for dividing cell %d: %.2f\n",
						mother.getTrackID(),
						Angle.toDegrees(angle_difference));
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
		assert longest_axis.length == 2: "More than two coordinates!";
		
		double longest_axis_angle_rad = Angle.angle(longest_axis[0], longest_axis[1]);
		double longest_axis_angle_deg = Angle.toDegrees(longest_axis_angle_rad);
		return longest_axis_angle_rad;
	}

}
