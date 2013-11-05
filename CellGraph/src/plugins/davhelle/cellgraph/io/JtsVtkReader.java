/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.io;

import icy.gui.frame.progress.AnnounceFrame;

import java.util.ArrayList;
import java.util.Collection;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;

import vtk.vtkCellArray;
import vtk.vtkIdTypeArray;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataReader;
import vtk.vtkPolyDataWriter;

/**
 * Class to read a VTK mesh file and elaborate it into a 
 * collection of JTS Geometry objects (method dependend)
 * 
 * @author Davide Heller
 *
 */
public class JtsVtkReader implements PolygonReader{
	
	vtkPolyDataReader reader;
	
	public JtsVtkReader(){	
		reader = new vtkPolyDataReader();	
	}
	
	private boolean is_not_polydata(){
		return reader.IsFilePolyData() != 1;
	}
	
	/**
	 * Method to extract the polygons from the linework in the
	 * vtk mesh. Uses the polygonizer function of JTS library.
	 * 
	 * @return Collection of jts polygons
	 */
	public ArrayList<Polygon> extractPolygons(String file_name){
		
		//define input
		reader.SetFileName(file_name);
		vtkPolyData polydata = new vtkPolyData();
        reader.SetOutput(polydata);
        reader.Update();
        
		//check for data correctness        
		if(is_not_polydata()){
			new AnnounceFrame("NO Poly data found in: "+file_name);
			return null;
		}
		
		//extract points from polydata      
        vtkPoints points = polydata.GetPoints();        
        int point_no = points.GetNumberOfPoints(); 
        ArrayList<Coordinate> corner_list = new ArrayList<Coordinate>();
        
		//insert points into corner_list and graph
        for(int i=0; i<point_no; i++){
        	double[] coor_i = points.GetPoint(i);
        	corner_list.add(new Coordinate(coor_i[0],coor_i[1],coor_i[2]));     	
        	//System.out.println(coor_i[2]);
        }
          
        //extract lines(edges) from polydata
        vtkCellArray lines = polydata.GetLines();
        vtkIdTypeArray linesId = lines.GetData();
        
        //Setup JTS variables
        Polygonizer polygonizer = new Polygonizer();
	    //use the default factory, which gives full double-precision
        GeometryFactory lineFactory = new GeometryFactory();
        Collection line_collection = new ArrayList();
        
        
        //try to extract data as following web site suggests:
        //http://forrestbao.blogspot.ch/2012/06/vtk-polygons-and-other-cells-as.html      
        
        //basic scheme: read line_point_no and do inner cycle to retrieve corresponding points
        for(int i = 0; i < linesId.GetNumberOfTuples(); i++){

        	//read first value of LineCell which tells the number of connected points
        	int line_elements = linesId.GetValue(i);

        	int[] corner_idx = new int[line_elements];    	
        	//parse points members of the line
        	for(int j=0; j < line_elements; j++)
        		corner_idx[j] = linesId.GetValue(i+j+1);
     	
        	//Register the lines, considering also the multiple_point line
        	if(line_elements > 1){ 		
        		//Add the edge to the the graph
        		for(int i1=0; i1 < line_elements-1; i1++){
        			
        			Coordinate a = corner_list.get(corner_idx[i1]);
        			Coordinate b = corner_list.get(corner_idx[i1+1]);
        			Coordinate[] line_coordinates = 
        					new Coordinate[]{a,b};
   
        			LineString line = lineFactory.createLineString(line_coordinates);
        			line_collection.add(line);    	
        			
        		}
        	}
        	i = i + line_elements;
        	
        }
        
        //TODO try adding directly without using the raw type collection
//        System.out.println("Lines in collection:"+line_collection.size());
        polygonizer.add(line_collection);
        
        //Polygonize line work
        Collection raw_polygons = polygonizer.getPolygons();
        
        //Save as ArrayLIst
        ArrayList<Polygon> jts_polygons = new ArrayList<Polygon>();
		for(Object p: raw_polygons)
			jts_polygons.add((Polygon)p);

        return jts_polygons;
	}
	
	//Add possible other readouts e.g. JTS linework for vertex graph
	
	
	
}
