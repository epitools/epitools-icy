package plugins.davhelle.cellgraph.io;

import java.util.ArrayList;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Interface for all classes that extract the 
 * JTS polygon structure from a given input 
 * file.
 * 
 * @author Davide Heller
 */
public interface PolygonReader {
	
	/**
	 * Extracts the polygons from the file
	 * indicated in the absolute path
	 * 
	 * @param file_name absolute path of the input file
	 * @return collection of JTS polygons representing the input
	 */
	public ArrayList<Polygon> extractPolygons(String file_name);

}
