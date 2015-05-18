package plugins.davhelle.cellgraph.io;

import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Helper class to import Region of Interest (ROI) points
 * from ImageJ to CellGraph
 * 
 * @author Davide Heller
 *
 */
public class ImageJRoiReader {
	
	/**
	 * JTS factory for transforming the ImageJ ROIs in JTS point geometries
	 */
	private final GeometryFactory factory = new GeometryFactory();

	/**
	 * Reads all ROIs specified in the input string and converts them to 
	 * mapped JTS point to the original ROI number
	 * 
	 * @param imageJ_ROI_string input string containing the imageJ ROI file
	 * @return a map of ROIs accessible through the original ROI number
	 */
	public Map<Integer, Point> readMultiPointRoi(String imageJ_ROI_string) {
		
		Map<Integer, Point> map = new HashMap<Integer, Point>();
		String[] rows = imageJ_ROI_string.split("\n");
		
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
