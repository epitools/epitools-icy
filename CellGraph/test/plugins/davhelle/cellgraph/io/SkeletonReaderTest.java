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

		int true_no_of_cells = 1;
		assertSize(extracted_polygons, true_no_of_cells);
		
		Polygon square = extracted_polygons.get(0);
	
		//test polygon for containing specific point
		HashSet<Coordinate> coordinates = new HashSet<Coordinate>(Arrays.asList(square.getCoordinates()));			
		Assert.assertTrue(coordinates.contains(new Coordinate(288, 85)));
		
		//test polygon for holes
		Assert.assertEquals(square.getNumInteriorRing(), 0);
		
	}

	private void assertSize(ArrayList<Polygon> collection, int size) {
		Assert.assertEquals(collection.size(), size);
	}
	
	@Test
	public void testNestedImage() {
		
		String file_name = "testData/nested_example.tiff";

		SkeletonReader reader = 
				new SkeletonReader(file_name, true, SegmentationProgram.MatlabLabelOutlines);
		ArrayList<Polygon> extracted_polygons = reader.extractPolygons();

		int true_no_of_cells = 2;
		assertSize(extracted_polygons, true_no_of_cells);	
		
		Polygon first = extracted_polygons.get(0);
		
		//test if first polygon contains holes (nested polygon)
		Assert.assertEquals(first.getNumInteriorRing(), 1);
		
	}
	
	@Test
	public void testComplexExample(){
		String file_name = "testData/cell_tissue_crop.tif";

		SkeletonReader reader = 
				new SkeletonReader(file_name, true, SegmentationProgram.SeedWater);
		ArrayList<Polygon> extracted_polygons = reader.extractPolygons();
		
		int true_no_of_cells = 1;
		assertSize(extracted_polygons, true_no_of_cells);

	}
	
	
}
