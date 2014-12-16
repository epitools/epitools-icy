/**
 * 
 */
package headless;

import ij.process.EllipseFitter;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.algorithm.MinimumBoundingCircle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.io.CsvWriter;
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
		
		//output file
		StringBuilder elongationCellId = new StringBuilder();
		StringBuilder elongationAngle = new StringBuilder();
		StringBuilder elongationRatio = new StringBuilder();
		StringBuilder elongationArea = new StringBuilder();
		for(Node cell: stGraph.getFrame(0).vertexSet()){
			if(cell.hasObservedDivision()){
				
				Node n = cell;
				
				if(!fittedEllipses.containsKey(n) ||
						!n.hasNext() || n.onBoundary())
					continue;
				
				elongationCellId.append(
						String.format("%d,%.0f,%.0f\n",
								n.getTrackID(),
								n.getGeometry().getCentroid().getX(),
								n.getGeometry().getCentroid().getY()));

				while(n.hasNext()){

					EllipseFitter ef = fittedEllipses.get(n);

					//write out observation of
					//1. angle of elongation
					elongationAngle.append(String.format("%.2f,",ef.angle));
					//2. ratio of elongation
					elongationRatio.append(String.format("%.2f,",ef.major/ef.minor));
					//3. the area of the cell
					elongationArea.append(String.format("%.2f,",n.getGeometry().getArea()));

					//then, after the division happened
					//1. the angle of the new junction
					//2. elongation of the two daughter cells

					//update cell to next reference
					n = n.getNext();
				}
				
				trimCommaAndReturn(elongationAngle);
				trimCommaAndReturn(elongationArea);
				trimCommaAndReturn(elongationRatio);
			}
		}

		
		File output_file1 = new File("/Users/davide/tmp/elongationAngleNeo0.csv");
		CsvWriter.writeOutBuilder(elongationAngle, output_file1);
		File output_file2 = new File("/Users/davide/tmp/elongationRatioNeo0.csv");
		CsvWriter.writeOutBuilder(elongationRatio, output_file2);
		File output_file3 = new File("/Users/davide/tmp/elongationAreaNeo0.csv");
		CsvWriter.writeOutBuilder(elongationArea, output_file3);
		File output_file4 = new File("/Users/davide/tmp/elongationCellIdNeo0.csv");
		CsvWriter.writeOutBuilder(elongationCellId, output_file4);
		
		System.out.printf("Successfully wrote:\n\t%s\n\t%s\n\t%s\n\t%s\n", 
				output_file1.getAbsolutePath(),
				output_file2.getAbsolutePath(),
				output_file3.getAbsolutePath(),
				output_file4.getAbsolutePath());
		
//		HashMap<Node, Double> division_orientation = computeDivisionOrientation(
//				stGraph, fittedEllipses);
//		
//		for(Node cell: division_orientation.keySet())
//			System.out.printf(
//					"%.2f\n",
//					division_orientation.get(cell));
	}
	
	private static void trimCommaAndReturn(StringBuilder builder){
		builder.setLength(builder.length() - 1);
		builder.append('\n');
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
		int prior_frames = 7;
		int frame_no_to_avg = 5;
		
		for(int i=prior_frames; i<stGraph.size(); i++){
			FrameGraph frame = stGraph.getFrame(i);
			Iterator<Division> divisions = frame.divisionIterator();
			while(divisions.hasNext()){
				Division d = divisions.next();
				Node mother = d.getMother();
				double longest_axis_angle_rad = 0.0;
				
				//avg angles:http://stackoverflow.com/questions/491738/how-do-you-calculate-the-average-of-a-set-of-angles
				double x=0;
				double y=0;
				
				//get mother cell orientation by avg orientations prior to rounding
				int no_of_angles_in_avg = 0;
				
//				for(int j=0; j < i - 5; j++){
					int j = i - prior_frames;
					FrameGraph frame_prior_rounding = stGraph.getFrame(j);
					if(frame_prior_rounding.hasTrackID(mother.getTrackID())){
						Node mother_before_rounding = frame_prior_rounding.getNode(mother.getTrackID());
						longest_axis_angle_rad = fittedEllipses.get(mother_before_rounding).theta;
					
//						double degrees = Angle.toDegrees(longest_axis_angle_rad);
//						
//						if(longest_axis_angle_rad > Angle.PI_OVER_2)
//							longest_axis_angle_rad = - Math.abs(Math.PI - longest_axis_angle_rad);
//						
//						double degrees2 = Angle.toDegrees(longest_axis_angle_rad);
//						if(mother.getTrackID() == 902)
//							System.out.printf("[%d] %.0f %.0f\n",j,degrees,degrees2);
						
						x += Math.cos(longest_axis_angle_rad);
						y += Math.sin(longest_axis_angle_rad);
					
						no_of_angles_in_avg++;
					}
//					else
//						System.out.printf("No time point for mother %d at %d\n",mother.getTrackID(),prior_frames);
//				}
					
				if(no_of_angles_in_avg == 0)
					continue;
				
				//compute avg and convert from [0,2pi] to [0,pi]
				longest_axis_angle_rad = Math.atan2(y/no_of_angles_in_avg, x/no_of_angles_in_avg);

				longest_axis_angle_rad = Math.abs(longest_axis_angle_rad - Math.PI);
				d.setLongestMotherAxisOrientation(longest_axis_angle_rad);

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
				d.setDivisionOrientation(angle_diff_in_degrees);
				
				division_orientation.put(mother.getFirst(), Double.valueOf(angle_diff_in_degrees));

				//Inspection
				double new_junction_orientation_wrt_x = findAngleOfLongestAxis(child_intersection);
				d.setNewJunctionOrientation(Angle.toDegrees(new_junction_orientation_wrt_x));
				
//				System.out.printf("Mother %d at %d:\t%.0f\t%.0f\t= %.0f\n",
//						mother.getTrackID(),i - prior_frames,
//						Angle.toDegrees(longest_axis_angle_rad),
//						Angle.toDegrees(new_junction_orientation_wrt_x),
//						Angle.toDegrees(new_junction_angle));
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
