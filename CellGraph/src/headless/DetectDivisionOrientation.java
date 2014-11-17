/**
 * 
 */
package headless;

import java.util.Iterator;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.algorithm.MinimumBoundingCircle;
import com.vividsolutions.jts.geom.Coordinate;

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
				
				MinimumBoundingCircle mbc = new MinimumBoundingCircle(mother.getGeometry());
				
				Coordinate[] longest_axis = mbc.getExtremalPoints();
				assert longest_axis.length == 2: "More than two coordinates!";
				
				double longest_axis_angle_rad = Angle.angle(longest_axis[0], longest_axis[1]);
				double longest_axis_angle_deg = Angle.toDegrees(longest_axis_angle_rad);
				longest_axis_angle_rad = Math.abs(longest_axis_angle_rad);
				
				
			}
		}

	}

}
