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
import com.vividsolutions.jts.geom.GeometryFactory;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.io.CsvWriter;
import plugins.davhelle.cellgraph.misc.DivisionOrientationFinder;
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
		
		//int neo_no = 2;
		for(int neo_no=0;neo_no<3;neo_no++){

			SpatioTemporalGraph stGraph = LoadNeoWtkFiles.loadNeo(neo_no);
			HashMap<Node, EllipseFitter> fittedEllipses = 
					new EllipseFitGenerator(stGraph).getFittedEllipses();

			//output file
			StringBuilder elongationCellId = new StringBuilder();
			StringBuilder elongationAngle = new StringBuilder();
			StringBuilder elongationRatio = new StringBuilder();
			StringBuilder elongationArea = new StringBuilder();
			StringBuilder divisionAngle = new StringBuilder();
			
			DivisionOrientationFinder doFinder = new DivisionOrientationFinder(stGraph, fittedEllipses, 11, 5);

			frame0_loop:for(Node cell: stGraph.getFrame(0).vertexSet()){
				if(cell.hasObservedDivision()){

					Node n = cell;

						if(!fittedEllipses.containsKey(n) ||
								!n.hasNext() || n.onBoundary())
							continue;


					while(n.hasNext()){

						EllipseFitter ef = fittedEllipses.get(n);
						double longest_axis_angle = ef.theta;
						longest_axis_angle = Math.abs(longest_axis_angle - Math.PI);
						
						double division_angle = doFinder.computeDivisionOrientation(
								longest_axis_angle,cell.getDivision());
						if(Double.compare(division_angle,Double.MIN_VALUE) == 0)
							continue frame0_loop;

						//write out observation of
						//1. angle of elongation
						elongationAngle.append(String.format("%.2f,",ef.angle));
						//2. ratio of elongation
						elongationRatio.append(String.format("%.2f,",ef.major/ef.minor));
						//3. the area of the cell
						elongationArea.append(String.format("%.2f,",n.getGeometry().getArea()));

						//4. the angle between the elongation at i and the junction angle
						divisionAngle.append(String.format("%.2f,",Angle.toDegrees(division_angle)));

						//then, after the division happened
						//1. the angle of the new junction
						//2. elongation of the two daughter cells

						//update cell to next reference
						n = n.getNext();
					}


					trimCommaAndReturn(elongationAngle);
					trimCommaAndReturn(elongationArea);
					trimCommaAndReturn(elongationRatio);
					trimCommaAndReturn(divisionAngle);

					//save the details of the mother cell to maintain correspondence
					elongationCellId.append(
							String.format("%d,%.0f,%.0f\n",
									n.getTrackID(),
									n.getGeometry().getCentroid().getX(),
									n.getGeometry().getCentroid().getY()));
				}
			}

			String output_pattern = "/Users/davide/tmp/avgDivisionOrientation/%sNeo%d.csv";

			File output_file1 = new File(String.format(output_pattern,"elongationAngle",neo_no));
			CsvWriter.writeOutBuilder(elongationAngle, output_file1);
			File output_file2 = new File(String.format(output_pattern,"elongationRatio",neo_no));
			CsvWriter.writeOutBuilder(elongationRatio, output_file2);
			File output_file3 = new File(String.format(output_pattern,"elongationArea",neo_no));
			CsvWriter.writeOutBuilder(elongationArea, output_file3);
			File output_file4 = new File(String.format(output_pattern,"elongationCellId",neo_no));
			CsvWriter.writeOutBuilder(elongationCellId, output_file4);
			File output_file5 = new File(String.format(output_pattern,"divisionAngle",neo_no));
			CsvWriter.writeOutBuilder(divisionAngle, output_file5);

			System.out.printf("Successfully wrote:\n\t%s\n\t%s\n\t%s\n\t%s\n", 
					output_file1.getAbsolutePath(),
					output_file2.getAbsolutePath(),
					output_file3.getAbsolutePath(),
					output_file4.getAbsolutePath(),
					output_file5.getAbsolutePath());

			//		HashMap<Node, Double> division_orientation = computeDivisionOrientation(
			//				stGraph, fittedEllipses);
			//		
			//		for(Node cell: division_orientation.keySet())
			//			System.out.printf(
			//					"%.2f\n",
			//					division_orientation.get(cell));
		}
	}
	
	private static double computeDivisionAngle(Division d,
			EllipseFitter ef) {
		
		boolean USE_INTERSECTION_METHOD = false; //put false if the JOIN CENTROIDS METHOD SHOULD BE USED
		
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
			if(new_junction_ends.length != 2){ 
				System.out.printf("Children intersection has more than two endings: %d [%d,%.0f,%.0f]\n",
						new_junction_ends.length,
						d.getTimePoint(),
						child_intersection.getCentroid().getX(),
						child_intersection.getCentroid().getY());
				return Double.MIN_VALUE;
			}
		
		}
		else{
			new_junction_ends = new Coordinate[2];
			new_junction_ends[0] = child1.getCentroid().getCoordinate();
			new_junction_ends[1] = child2.getCentroid().getCoordinate();
		}
			
		//Compute the angle of the using the bounding circle

		//Form the angle to measure
		Coordinate tail = new_junction_ends[1];
		Coordinate tip1 = new_junction_ends[0];
		
		double longest_axis_angle = Math.abs(ef.theta - Math.PI);
		Coordinate tip2 = new Coordinate(
				tail.x - Math.cos(longest_axis_angle),
				tail.y - Math.sin(longest_axis_angle));

		double angle_diff_in_rad = Angle.interiorAngle(tip1, tail, tip2);

		//find the smallest angle within [0,pi/2]
		if(angle_diff_in_rad > Math.PI)
			angle_diff_in_rad = Angle.PI_TIMES_2 - angle_diff_in_rad;
		if(angle_diff_in_rad > Angle.PI_OVER_2)
			angle_diff_in_rad = Math.PI - angle_diff_in_rad;

		if(!USE_INTERSECTION_METHOD)
			//because an alignment means that it divides perpendicularly
			angle_diff_in_rad = Angle.PI_OVER_2 - angle_diff_in_rad;
		
		double angle_diff_in_degrees = Angle.toDegrees(angle_diff_in_rad);
		
		return angle_diff_in_degrees;
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
		
		boolean USE_INTERSECTION_METHOD = false; //put false if the JOIN CENTROIDS METHOD SHOULD BE USED
		
		GeometryFactory factory = new GeometryFactory();
		
		
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
					
				if(no_of_angles_in_avg == 0){
					continue;
				}
				
				//t: theta, angle of major axis, clockwise with respect to x axis. 
				//compute avg and convert from [0,2pi] to [0,pi]
				longest_axis_angle_rad = Math.atan2(y/no_of_angles_in_avg, x/no_of_angles_in_avg);

				longest_axis_angle_rad = Math.abs(longest_axis_angle_rad - Math.PI);
				d.setLongestMotherAxisOrientation(longest_axis_angle_rad);

				//Get children axis
				Node child1 = d.getChild1();
				Node child2 = d.getChild2();

				Coordinate[] new_junction_ends = null;
				Geometry child_intersection = null;
				
				if(USE_INTERSECTION_METHOD){
					//TODO: possibly substitute here with edge (saved geo)
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

				double angle_diff_in_degrees = Angle.toDegrees(angle_diff_in_rad);
				d.setDivisionOrientation(angle_diff_in_degrees);
				
				division_orientation.put(mother.getFirst(), Double.valueOf(angle_diff_in_degrees));

				//Inspection
				double new_junction_orientation_wrt_x = findAngleOfLongestAxis(child_intersection);
				double new_junction_angle_degrees = Angle.toDegrees(new_junction_orientation_wrt_x);
				
				if(USE_INTERSECTION_METHOD)
					d.setNewJunctionOrientation(new_junction_angle_degrees);
				else{
					//in case of centroid method take orthogonal
					new_junction_angle_degrees += 90;
					d.setNewJunctionOrientation(new_junction_angle_degrees);
				}
				
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
