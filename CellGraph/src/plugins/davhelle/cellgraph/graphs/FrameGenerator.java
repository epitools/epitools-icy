package plugins.davhelle.cellgraph.graphs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Polygon;

import plugins.davhelle.cellgraph.io.FileNameGenerator;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.JtsVtkReader;
import plugins.davhelle.cellgraph.io.PolygonReader;
import plugins.davhelle.cellgraph.io.SegmentationProgram;
import plugins.davhelle.cellgraph.io.SkeletonReader;
import plugins.davhelle.cellgraph.nodes.Cell;
import plugins.davhelle.cellgraph.nodes.ComparablePolygon;
import plugins.davhelle.cellgraph.nodes.Node;

public class FrameGenerator {
	
	PolygonReader polygonReader;
	
	public FrameGenerator(
			InputType input_type,
			boolean is_direct_input,
			SegmentationProgram tool,
			FileNameGenerator file_name_generator){
		
		switch(input_type){
		case SKELETON:
			
			boolean REDO_SKELETON = false;
			
			if(is_direct_input)
				switch(tool){
				case MatlabLabelOutlines:
					REDO_SKELETON = true;
					break;
				case PackingAnalyzer:
					REDO_SKELETON = false;
					break;
				case SeedWater:
					REDO_SKELETON = true;
					break;
				}
			
			this.polygonReader = new SkeletonReader(REDO_SKELETON, tool);
				
			break;
		case VTK_MESH:
			
			this.polygonReader = new JtsVtkReader();
			break;		
		}
		
	}
	
	public FrameGraph generateFrame(int frame_no, String file_name){
		
		FrameGraph frame = new FrameGraph(frame_no);
		
		ArrayList<Polygon> polygonMesh = polygonReader.extractPolygons(file_name);
		
		//insert all polygons into graph as CellPolygons
		ArrayList<Cell> cell_list = new ArrayList<Cell>();
		
		//order polygons according to cell center position
		ComparablePolygon[] poly_array = new ComparablePolygon[polygonMesh.size()];
		for(int k=0; k < poly_array.length; k++)
			poly_array[k] = new ComparablePolygon(polygonMesh.get(k));
		
		//order polygons according to comparator class
		Arrays.sort(poly_array);
		
		//obtain the polygons back and create cells
		for(ComparablePolygon polygon: poly_array){
			Cell c = new Cell(polygon.getPolygon(),frame);
			cell_list.add(c);
			frame.addVertex(c);
		}
		
		/* Algorithm to find neighborhood relationships between polygons
		 * 
		 * for every polygon
		 * 	for every non assigned face
		 * 	 find neighboring polygon
		 * 	  add edge to graph if neighbor found
		 * 	  if all faces assigned 
		 * 	   exclude from list
		 *    
		 */

		//TODO more efficient search
		Iterator<Node> cell_it = frame.iterator();
		
		while(cell_it.hasNext()){
			Cell a = (Cell)cell_it.next();
			Iterator<Cell> neighbor_it = cell_list.iterator();
			while(neighbor_it.hasNext()){
				Cell b = neighbor_it.next();
				//avoid creating the connection twice
				if(!frame.containsEdge(a, b))
					if(a.getGeometry().touches(b.getGeometry()))
						frame.addEdge(a, b);
			}
		}

		return frame;
	}
			

}
