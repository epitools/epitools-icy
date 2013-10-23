package plugins.davhelle.cellgraph.io;

import java.util.ArrayList;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vividsolutions.jts.geom.Polygon;

import plugins.davhelle.cellgraph.io.SegmentationProgram;
import plugins.davhelle.cellgraph.io.SkeletonReader;

public class SkeletonReaderTest {
	
	String file_name = "/Users/davide/Documents/segmentation/square_example.tif";

	@Test
	public void testSinglePolygon() {
		SkeletonReader reader = 
				new SkeletonReader(file_name, true, SegmentationProgram.MatlabLabelOutlines);
		ArrayList<Polygon> extracted_polygons = reader.extractPolygons();

		Assert.assertEquals(extracted_polygons.size(), 1);
		
	}
}
