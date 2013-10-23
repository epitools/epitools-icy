package plugins.davhelle.cellgraph.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class SkeletonReaderTest {
	
	@Test
	public void testSinglePolygon() {
		
		String file_name = "testData/square_example.tif";

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
	
	@Test
	public void testNestedImage() {
		
		String file_name = "testData/nested_example.tiff";

		SkeletonReader reader = 
				new SkeletonReader(file_name, true, SegmentationProgram.MatlabLabelOutlines);
		ArrayList<Polygon> extracted_polygons = reader.extractPolygons();

		Assert.assertEquals(extracted_polygons.size(), 2);
		
		Polygon first = extracted_polygons.get(0);
		
		//test if first polygon contains holes (nested polygon)
		Assert.assertEquals(first.getNumInteriorRing(), 1);
		
	}
	
	
	
}
