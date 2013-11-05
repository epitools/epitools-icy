package plugins.davhelle.cellgraph.io;

import java.util.ArrayList;
import com.vividsolutions.jts.geom.Polygon;

public interface PolygonReader {
	
	public ArrayList<Polygon> extractPolygons(String file_name);

}
