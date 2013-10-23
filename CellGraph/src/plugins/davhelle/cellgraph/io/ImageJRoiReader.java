package plugins.davhelle.cellgraph.io;

import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class ImageJRoiReader {
	
	private final 	GeometryFactory factory = new GeometryFactory();

	public Map<Integer, Point> readMultiPointRoi(String testInput) {
		
		Map<Integer, Point> map = new HashMap<Integer, Point>();
		String[] rows = testInput.split("\n");
		
		//skip header row and parse location of each point roi
		for (int i = 1; i < rows.length; i++) {
			
			String[] fields = rows[i].split("\t");
			Integer roiNumber = Integer.valueOf(fields[0]);
			
			Double x = Double.valueOf(fields[5]);
			Double y = Double.valueOf(fields[6]);
			
			Point location = factory.createPoint(new Coordinate(x,y));
			map.put(roiNumber, location);
		}
		
		return map;	
	}

}
