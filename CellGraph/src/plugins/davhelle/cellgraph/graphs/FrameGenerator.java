/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.graphs;

import java.util.ArrayList;
import java.util.Arrays;
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
import plugins.davhelle.cellgraph.nodes.Node;

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
	public FrameGenerator(
			InputType input_type,
			boolean is_direct_input,
			SegmentationProgram tool){
		
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
		
		//Create Index
		STRtree index = new STRtree();
		for(Polygon p: polygonMesh)
			index.insert(p.getEnvelopeInternal(), p);
		
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
			ArrayList<Polygon> intersections = (ArrayList<Polygon>) index.query(a.getGeometry().getEnvelopeInternal());
			PreparedGeometry cached_a = cached_factory.create(a.getGeometry());
			Iterator<Cell> neighbor_it = cell_list.iterator();
			while(neighbor_it.hasNext()){
				Cell b = neighbor_it.next();
				//avoid creating the connection twice
				if(!frame.containsEdge(a, b))
					if(cached_a.touches(b.getGeometry())){
						assert intersections.contains(b.getGeometry()): "Polygon not contained!";
						frame.addEdge(a, b);
					}
			}
		}
	}
			

}
