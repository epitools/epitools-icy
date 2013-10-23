package plugins.davhelle.cellgraph.io;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;

public class MultiPointRoiReaderTest {
  @Test
  public void testSimpleInput() {
	  
	  String test_input = 
			  " 	Area	Mean	Min	Max	X	Y\n"+
					  "1	0	0	0	0	12.750	7.750\n"+
					  "2	0	0	0	0	23.250	7.625\n"+
					  "3	0	0	0	0	37.375	8.500\n";
	  
	  ImageJRoiReader roi_reader = new ImageJRoiReader();
	  
	  Map<Integer, Point> multiPointRoi = roi_reader.readMultiPointRoi(test_input);
	 
	  //check correct no of extracted points
	  int no_of_example_points = 3;
	  Assert.assertEquals(multiPointRoi.size(), no_of_example_points);
	  
	  //check location correctness
	  Coordinate second_point = multiPointRoi.get(2).getCoordinate();
	  Assert.assertEquals(second_point.x, 23.250);
	  Assert.assertEquals(second_point.y, 7.625);
	  
  }
}
