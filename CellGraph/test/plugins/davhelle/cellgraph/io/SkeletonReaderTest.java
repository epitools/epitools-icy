package plugins.davhelle.cellgraph.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class SkeletonReaderTest {
	
	@Test
	public void testSinglePolygon() {
		
		String file_name = "testData/square_example.tif";

		SkeletonReader reader = 
				new SkeletonReader(true, SegmentationProgram.MatlabLabelOutlines);
		ArrayList<Polygon> extracted_polygons = reader.extractPolygons(file_name);

		int true_no_of_cells = 1;
		assertSize(extracted_polygons, true_no_of_cells);
		
		Polygon square = extracted_polygons.get(0);
	
		//test polygon for containing specific point
		HashSet<Coordinate> coordinates = new HashSet<Coordinate>(Arrays.asList(square.getCoordinates()));			
		Assert.assertTrue(coordinates.contains(new Coordinate(288, 85)));
		
		//test polygon for holes
		Assert.assertEquals(square.getNumInteriorRing(), 0);
		
	}

	private void assertSize(Collection collection, int size) {
		Assert.assertEquals(collection.size(), size);
	}
	
	@Test //(invocationCount=100)
	public void testNestedImage() {
		
		String file_name = "testData/nested_example.tiff";

		SkeletonReader reader = 
				new SkeletonReader(true, SegmentationProgram.MatlabLabelOutlines);
		ArrayList<Polygon> extracted_polygons = reader.extractPolygons(file_name);

		int true_no_of_cells = 2;
		assertSize(extracted_polygons, true_no_of_cells);	
		
		Polygon bigger = extracted_polygons.get(0);
		//make sure we are handling the bigger polygon
		if(!bigger.getEnvelope().contains(extracted_polygons.get(1)))
			bigger = extracted_polygons.get(1);
		
		//check if bigger is indeed the larger polygon
		HashSet<Coordinate> coordinates = new HashSet<Coordinate>(Arrays.asList(bigger.getCoordinates()));			
		Assert.assertTrue(coordinates.contains(new Coordinate(288, 85)));
		
		//test if bigger polygon contains holes (nested polygon)
		Assert.assertEquals(bigger.getNumInteriorRing(), 1);
		
	}
	
	@Test
	public void testSampleCrop(){
		String file_name = "testData/cell_tissue_crop.tif";

		SkeletonReader reader = 
				new SkeletonReader(true, SegmentationProgram.SeedWater);
		ArrayList<Polygon> extracted_polygons = reader.extractPolygons(file_name);
		
		//verify correct no of cells
		int true_no_of_cells = 23;
		assertSize(extracted_polygons, true_no_of_cells);
		
		//verify no holes in polygons
		int no_of_expected_holes = 0;
		for(Polygon p: extracted_polygons)
			Assert.assertEquals(p.getNumInteriorRing(), no_of_expected_holes);
		
		//assess correctness given User defined ground truth
		Map<Integer, Point> multiPointRoi = getUserDefinedRoi();
		assertSize(multiPointRoi.entrySet(), 23);
		
		for(Polygon p: extracted_polygons){
			ArrayList<Point> contained = new ArrayList<Point>();
			for(Point roi: multiPointRoi.values())
				if(p.getEnvelope().contains(roi)) contained.add(roi);

			Assert.assertTrue(contained.size() == 1, p.getCentroid().toText()+" contains more than one point");
		}
	}

	private Map<Integer, Point> getUserDefinedRoi() {
		//TODO add file reader here (file already in TestData)
		String cellPoints = 
				"` 	Area	Mean	Min	Max	X	Y\n" + 
				"1	0	0	0	0	12.750	7.750\n" + 
				"2	0	0	0	0	23.250	7.625\n" + 
				"3	0	0	0	0	37.375	8.500\n" + 
				"4	0	0	0	0	47.250	5.750\n" + 
				"5	0	0	0	0	43.875	16.875\n" + 
				"6	0	0	0	0	34.250	17.875\n" + 
				"7	0	0	0	0	24.625	19.625\n" + 
				"8	0	0	0	0	16.500	23.625\n" + 
				"9	0	0	0	0	6.875	15.750\n" + 
				"10	0	0	0	0	6.875	23.625\n" + 
				"11	0	0	0	0	5	32.500\n" + 
				"12	0	0	0	0	15	31.875\n" + 
				"13	0	0	0	0	29.625	34.625\n" + 
				"14	0	0	0	0	40.625	31.750\n" + 
				"15	0	0	0	0	47.375	26.250\n" + 
				"16	0	0	0	0	49	43.750\n" + 
				"17	0	0	0	0	39.750	43.500\n" + 
				"18	0	0	0	0	30.625	43.250\n" + 
				"19	0	0	0	0	23	45.750\n" + 
				"20	0	0	0	0	21.750	38.250\n" + 
				"21	0	0	0	0	15.375	41.625\n" + 
				"22	0	0	0	0	8.125	37.375\n" + 
				"23	0	0	0	0	6.250	44.625\n";

		ImageJRoiReader roi_reader = new ImageJRoiReader();
		  
		  Map<Integer, Point> multiPointRoi = roi_reader.readMultiPointRoi(cellPoints);
		return multiPointRoi;
	}
	
	
}
