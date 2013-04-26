package plugins.davhelle.cellgraph.io;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;

import java.util.ArrayList;
import java.util.Collection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;

public class SkeletonReader {

	GeometryFactory lineFactory;
	
	int skeleton_width;
	int skeleton_height;
	
	boolean[][] skeleton;
	
	public SkeletonReader(String file_name, Sequence sequence){

		lineFactory = new GeometryFactory();
		
		IcyBufferedImage img = sequence.getImage(0, 0);
		
		Object imageData = img.getDataXY( 0 );
		
		// Get a copy of the data in double.
		double[] dataBuffer = Array1DUtil.arrayToDoubleArray( imageData , img.isSignedDataType() );
			
		skeleton_width = img.getWidth();
		skeleton_height = img.getHeight();
		
		skeleton = new boolean[skeleton_width][skeleton_height];
		
		int idx = 0;
		for(int i=1; i<skeleton_height - 1; i++){
			idx = i * img.getWidth();
			for(int j=1; j < skeleton_width -1; j++){
				if(dataBuffer[idx + j] == 255.0){
					skeleton[j][i] = true;
				}
			}
		}

	}
	
	public ArrayList<Polygon> extractPolygons(){
		
		Collection line_collection = new ArrayList();
		for(int i=1; i<skeleton_height - 1; i++)
			for(int j=1; j < skeleton_width -1; j++)
				if(skeleton[j][i]){
					//if adjacent not free don't connect
					if(skeleton[j+1][i  ]) // rx
						//if(!skeleton[j+1][i-1] && !skeleton[j+1][i+1])
							line_collection.add(buildLine(j,i,j+1,i));  
					if(skeleton[j-1][i+1]) //llx
						if(!skeleton[j-1][i  ] && !skeleton[j  ][i+1])
							line_collection.add(buildLine(j,i,j-1,i+1));  
					if(skeleton[j  ][i+1]) //lcx
						//if(!skeleton[j-1][i+1] && !skeleton[j+1][i+1])
							line_collection.add(buildLine(j,i,j,i+1));  
					if(skeleton[j+1][i+1]) //lrw
						if(!skeleton[j  ][i+1] && !skeleton[j+1][i  ])
							line_collection.add(buildLine(j,i,j+1,i+1));  
				}
					
		Polygonizer polygonizer = new Polygonizer();
		
		 polygonizer.add(line_collection);
	        
	        //Polygonize line work
	        Collection raw_polygons = polygonizer.getPolygons();
	        
	        //Save as ArrayLIst
	        ArrayList<Polygon> jts_polygons = new ArrayList<Polygon>();
			for(Object p: raw_polygons)
				jts_polygons.add((Polygon)p);

	        return jts_polygons;
	}
	
	private LineString buildLine(int x1, int y1, int x2, int y2){
		Coordinate[] line_coordinates = new Coordinate[]{
				new Coordinate((double)x1, (double)y1),
				new Coordinate((double)x2, (double)y2)
		};
		
		return lineFactory.createLineString(line_coordinates);
	}
	
}
