package plugins.davhelle.cellgraph.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTFileReader;
import com.vividsolutions.jts.io.WKTReader;

/**
 * 
 * This class allows direct import of WKT polygons to construct a spatioTemporalGraph.
 * It offers increased speed compared to the Skeleton Bitmap image reader.
 * 
 * @author Davide Heller
 *
 */
public class WktPolygonImporter implements PolygonReader{

	/**
	 * JTS well known text reader
	 */
	private WKTReader reader;

	public WktPolygonImporter() {
		
		reader = new WKTReader();
		
	}
	
	/**
	 * Extracts individual JTS geometry from file
	 * 
	 * Used for border line extraction
	 * @param file_name path to WKT file
	 * @return List with single geometry
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Geometry> extractGeometries(String file_name){
		ArrayList<Geometry> stored_geometries = null;
		
		File file = new File(file_name);
		WKTFileReader file_reader = new WKTFileReader(file, reader);
		
		try {
			stored_geometries = (ArrayList<Geometry>) file_reader.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			System.out.println("Something went wrong with the WKT reading");
		}
		
		return stored_geometries;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<Polygon> extractPolygons(String file_name) {
		
		ArrayList<Polygon> stored_polygons = null;
		
		File file = new File(file_name);
		WKTFileReader file_reader = new WKTFileReader(file, reader);
		
		try {
			stored_polygons = (ArrayList<Polygon>) file_reader.read();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			System.out.println("Something went wrong with the WKT reading");
		}
		
		return stored_polygons;
	}

}
