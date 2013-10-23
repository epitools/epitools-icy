package plugins.davhelle.cellgraph.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vividsolutions.jts.awt.PointShapeFactory.Point;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class SkeletonReaderTest {
	
	String file_name = "testData/square_example.tif";

	@Test
	public void testSinglePolygon() {

		SkeletonReader reader = 
				new SkeletonReader(file_name, true, SegmentationProgram.MatlabLabelOutlines);
		ArrayList<Polygon> extracted_polygons = reader.extractPolygons();

		Assert.assertEquals(extracted_polygons.size(), 1);
		
		Polygon square = extracted_polygons.get(0);
	
		//test polygon for containing specific point
		HashSet<Coordinate> coordinates = new HashSet<Coordinate>(Arrays.asList(square.getCoordinates()));			
		Assert.assertTrue(coordinates.contains(new Coordinate(288, 85)));
		
		//test polygon for holes
		Assert.assertEquals(square.getNumInteriorRing(), 0);
		
	}
	
	
}
