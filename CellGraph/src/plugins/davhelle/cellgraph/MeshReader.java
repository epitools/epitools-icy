package plugins.davhelle.cellgraph;

import java.util.ArrayList;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableUndirectedGraph;

import vtk.vtkCellArray;
import vtk.vtkIdTypeArray;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkPolyDataReader;

/**
 * MeshReader extracts the information from the VTK mesh file
 * supplied by the USER and inserts it into the JGraphT 
 * undirected graph.
 * 
 * @author Davide Heller
 *
 */
public class MeshReader {
	
	vtkPolyDataReader reader;
	vtkPolyData polydata;
	
	public MeshReader(String file_name){
		
		reader = new vtkPolyDataReader();
        reader.SetFileName(file_name);
        
        polydata = new vtkPolyData();
        reader.SetOutput(polydata);
              
        reader.Update();
		
	}
	
	public boolean is_not_polydata(){
		return reader.IsFilePolyData() != 1;
	}
	
	public int get_point_no(){
		return polydata.GetPoints().GetNumberOfPoints();
	}
	
	/**
	 * Method reads the loaded vtk polydata structure and fills the graph accordingly, each lineCell
	 * corresponds to an edge in the graph.
	 * 
	 * @param polydata vtk polydata mesh structure (LineCells)
	 * @param corner_list list of all the cell corners (points in the vtk structure) (void as for input)
	 * @param cell_corner_graph graph representation of the vtk mesh (void as for input)
	 */
	public void fill_graph(ArrayList<CellCorner> corner_list,ListenableUndirectedGraph<CellCorner, DefaultEdge> cell_corner_graph){
		
        //extract point from polydata      
        vtkPoints points = polydata.GetPoints();        
        int point_no = points.GetNumberOfPoints();
        
        //System.out.println("Going to extract:"+point_no+" points");  
		
        //TODO adapt to read 2D data as well without having to 
        // artificially ad a 0 to each line before!
        
        
		//insert points into corner_list and graph
        for(int i=0; i<point_no; i++){
        	//System.out.println(i + ": " + (int)points.GetPoint(i)[0] + " " + (int)points.GetPoint(i)[1] + " " + (int)points.GetPoint(i)[2]);
        	//System.out.println(points.GetPoint(i)[0]+":"+points.GetPoint(i)[1]);
        	corner_list.add(new CellCorner(i,points.GetPoint(i)));
        	cell_corner_graph.addVertex(corner_list.get(i));        	
        }
          
        //extract lines(edges) from polydata
        vtkCellArray lines = polydata.GetLines();
        vtkIdTypeArray linesId = lines.GetData();     
        		
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
        	if(line_elements > 1) 		
        		//Add the edge to the the graph
        		for(int i1=0; i1 < line_elements-1; i1++)   			
        			cell_corner_graph.addEdge(corner_list.get(corner_idx[i1]), corner_list.get(corner_idx[i1+1]));    	
        		
        	
        	i = i + line_elements;
        	
        }
	}

}
