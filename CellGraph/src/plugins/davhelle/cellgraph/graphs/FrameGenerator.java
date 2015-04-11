/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.graphs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.JtsVtkReader;
import plugins.davhelle.cellgraph.io.PolygonReader;
import plugins.davhelle.cellgraph.io.SegmentationProgram;
import plugins.davhelle.cellgraph.io.SkeletonReader;
import plugins.davhelle.cellgraph.io.WktPolygonImporter;
import plugins.davhelle.cellgraph.nodes.Cell;
import plugins.davhelle.cellgraph.nodes.ComparablePolygon;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.index.strtree.STRtree;

/**
 * FrameGenerator is a helper class to create FrameGraph objects
 * given a specific user input. Once initialized it can generate
 * FrameGraphs without the need to respecify all the generation
 * details, e.g. Input Type ecc...
 * 
 * @author Davide Heller
 *
 */
public class FrameGenerator {
	
	PolygonReader polygonReader;
	private PreparedGeometryFactory cached_factory;

	
	/**
	 * Initializes the parameters
	 * 
	 * @param input_type type of file from which the polygons will be extracted
	 * @param is_direct_input true if the files come from a known Segmentation Program
	 * @param tool Segmentation program used to create the skeletons
	 */
	public FrameGenerator(InputType input_type){
		
		switch(input_type){
		case SKELETON:
			this.polygonReader = new SkeletonReader();
				
			break;
		case VTK_MESH:
			
			this.polygonReader = new JtsVtkReader();
			break;
		
		case WKT:
			
			this.polygonReader = new WktPolygonImporter();
			break;
		default:
			break;		
		}
		
		cached_factory = new PreparedGeometryFactory();
		
	}
	
	/**
	 * generates a single FrameGraph for the specified time point 
	 * and absolute file name.
	 * 
	 * @param frame_no time point of the new frame
	 * @param file_name absolute path of the input file.
	 * @return the frameGraph object representing the input file
	 */
	public FrameGraph generateFrame(int frame_no, String file_name){
		
		FrameGraph frame = new FrameGraph(frame_no);
		
		frame.setFileSource(file_name);
		
		ArrayList<Polygon> polygonMesh = polygonReader.extractPolygons(file_name);
		
		populateFrame(frame, polygonMesh);

		return frame;
	}

	public void populateFrame(FrameGraph frame, ArrayList<Polygon> polygonMesh) {
		
		//insert all polygons into graph as CellPolygons
		ArrayList<Cell> cell_list = new ArrayList<Cell>();
		
		//order polygons according to cell center position
		//in order to insert them into the graph in geometric order (x,y) 
		ComparablePolygon[] poly_array = new ComparablePolygon[polygonMesh.size()];
		for(int k=0; k < poly_array.length; k++)
			poly_array[k] = new ComparablePolygon(polygonMesh.get(k));
		
		//order polygons according to comparator class
		Arrays.sort(poly_array);
		
		//obtain the polygons back and create cells & index
		STRtree index = new STRtree();
		HashMap<Polygon, Cell> index_to_cell = new HashMap<Polygon, Cell>();

		for(ComparablePolygon polygon: poly_array){
			Polygon cell_polygon = polygon.getPolygon();
			
			Cell c = new Cell(cell_polygon,frame);
			cell_list.add(c);
			frame.addVertex(c);

			//Populate tree and conversion map
			index.insert(cell_polygon.getEnvelopeInternal(), cell_polygon);
			index_to_cell.put(cell_polygon, c);
		}
		
		/* Algorithm to find neighborhood relationships between polygons
		 * 
		 * for every polygon
		 * 	obtain the STRtree neighbors
		 *	
		 *	if edge doesn't already exist
		 * 	  if str_tree_neighbor intersects
		 * 		 add edge to graph. 
		 *    
		 */

		Iterator<Node> cell_it = frame.iterator();

		while(cell_it.hasNext()){
			Cell a = (Cell)cell_it.next();
			Geometry geometry_a = a.getGeometry();
			PreparedGeometry cached_a = cached_factory.create(geometry_a);
			
			//Get candidates from STRtree
			ArrayList<Polygon> intersections = 
					(ArrayList<Polygon>) index.query(geometry_a.getEnvelopeInternal());
			
			for(Polygon intersection_neighbor: intersections){
				
				Geometry geometry_b = intersection_neighbor;
				if(geometry_a.hashCode() == geometry_b.hashCode())
					continue;
				
				Cell b = index_to_cell.get(intersection_neighbor);
				if(!frame.containsEdge(a, b)) {
					
					boolean cells_intersect = cached_a.intersects(b.getGeometry());
					
					if(cells_intersect){
						Edge newEdge = frame.addEdge(a, b);
						newEdge.setFrame(frame);
					}
				}
			}
		}		
	}
			

}
