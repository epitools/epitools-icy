/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.io;

import icy.image.IcyBufferedImage;
import icy.image.ImageUtil;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;

/**
 * SkeletonReader allows direct input of Skeleton images which 
 * are converted into JTK Polygon collection. 
 * 
 * @author Davide Heller
 *
 */
public class SkeletonReader {

	private GeometryFactory lineFactory;
	
	private int skeleton_width;
	private int skeleton_height;
	
	private boolean[][] skeleton;
	
	/**
	 * Constructor converts the input skeleton image
	 * into a boolean map which represents the presence
	 * of a white pixel (TODO REFINE) as true value.
	 * 
	 * @param file_name File name of the skeleton image
	 */
	public SkeletonReader(String file_name){

		lineFactory = new GeometryFactory();
		
		BufferedImage raw_img = ImageUtil.load(file_name);
		
		IcyBufferedImage img = IcyBufferedImage.createFrom(raw_img);
		
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
				//TODO what if the white value is not 255
				if(dataBuffer[idx + j] == 255.0){
					skeleton[j][i] = true;
				}
			}
		}

	}
	
	/**
	 * Given the boolean map, first a line collection is 
	 * build and then the latter is converted into a 
	 * Polygon collection by the Polygonizer function of JTS.
	 * 
	 * A line is added for every two adjacent white pixels.
	 * Exception are made for oblique connections which have
	 * to satisfy the criteria that neighboring pixels are
	 * empty.(Criteria can be violated by horizontal or vertical
	 * connections, e.g. cross situation)
	 * 
	 * 
	 * @return
	 */
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
					if(skeleton[j  ][i+1]) //lcx - vertical junction doesn't need check
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
	
	/**
	 * Helper method to create a line
	 * 
	 * @param x1 starting point x coordinate
	 * @param y1 starting point y coordinate
	 * @param x2 ending point x coordinate
	 * @param y2 ending point y coordinate
	 * @return
	 */
	private LineString buildLine(int x1, int y1, int x2, int y2){
		Coordinate[] line_coordinates = new Coordinate[]{
				new Coordinate((double)x1, (double)y1),
				new Coordinate((double)x2, (double)y2)
		};
		
		return lineFactory.createLineString(line_coordinates);
	}
	
}
