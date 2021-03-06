package plugins.davhelle.cellgraph.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.WktPolygonImporter;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

/**
 * Class identifies the cells which constitute the border 
 * of the given geometry collection. The task is achieved by consid-
 * ering the union of all cells and asking which polygons
 * intersect with the outer ring.  
 * 
 * @author Davide Heller
 *
 */
public class BorderCells{

	/**
	 * The input graph
	 */
	private SpatioTemporalGraph stGraph;
	/**
	 * JTS geometry factory
	 */
	private PreparedGeometryFactory cached_factory;
	/**
	 * Array for boundary geometries
	 */
	private Geometry[] boundaries;
	
	/**
	 * @return the boundaries for all frames
	 */
	public Geometry[] getBoundaries() {
		return boundaries;
	}

	/**
	 * Initialize containers for size of stGraph
	 * 
	 * @param stGraph graph to compute boundaries for
	 */
	public BorderCells(SpatioTemporalGraph stGraph) {
		//Set data structures
		this.stGraph = stGraph;
		boundaries = new Geometry[stGraph.size()];
		cached_factory = new PreparedGeometryFactory();
	}
	
	
	/**
	 * Applies to all frames in the stGraph the following procedure:<br>
	 * 1. Border cell detection<br>
	 * 2. Border cell removal<br>
	 * 3. New Border cell detection<br>
	 */
	public void removeOneBoundaryLayerFromAllFrames(){
		
		 //TODO: split the method to allow for separate removal and labeling of boundary cells
		
		for(int time_point_i=0; time_point_i<stGraph.size();time_point_i++){
			
			long s = System.currentTimeMillis();
			FrameGraph frame_i = stGraph.getFrame(time_point_i);
			
			Geometry old_boundary = findBorderCells(frame_i);
			long e1 = System.currentTimeMillis() - s;
			
			removeBoundaryLayer(frame_i, old_boundary);
			long e2 = System.currentTimeMillis() - e1 - s;
			
			Geometry new_boundary = findBorderCells(frame_i);
			long e3 = System.currentTimeMillis() - e2 - e1 - s;
			
			markBorderCells(frame_i, new_boundary);
			long e4 = System.currentTimeMillis() - e3 - e2 - e1 - s;		

			boundaries[time_point_i] = new_boundary;
			
			System.out.printf("Boundary %d:\t%d\t%d\t%d\t%d\n",time_point_i,e1,e2,e3,e4);
			
		}
		
	}
	
	
	/**
	 * Detects and removes all border cells of the frame at the specified time point
	 * in the stGraph set through the constructor.
	 * 
	 * @param time_point_i time point to be processed
	 */
	public void removeOneBoundaryLayerFromFrame(int time_point_i){
		
		if(time_point_i < 0 || time_point_i > stGraph.size() - 1)
			time_point_i = 0;

		FrameGraph frame_i = stGraph.getFrame(time_point_i);
		
		Geometry boundary = findBorderCells(frame_i);
		removeBoundaryLayer(frame_i, boundary);
		
		//update boundary
		Geometry new_boundary = findBorderCells(frame_i);
		markBorderCells(frame_i, new_boundary);
		
		boundaries[time_point_i] = new_boundary;

		System.out.println("Removed one outer layer!");
		
	}

	/**
	 * Removes all cells that intersect the border from the input frame
	 * 
	 * @param frame_i input frame
	 * @param boundary boundary/border geometry
	 */
	public void removeBoundaryLayer(FrameGraph frame_i, Geometry boundary) {
		
		//Check via intersection if cell is border cell
		ArrayList<Node> borderCells = new ArrayList<Node>();
		PreparedGeometry cached_boundary = cached_factory.create(boundary);
		
		for(Node n: frame_i.vertexSet()){
			Geometry p = n.getGeometry();
			if(cached_boundary.intersects(p))
				borderCells.add(n);
		}

		//Can't remove vertices while iterating so doing it now
		for(Node n: borderCells)
			if(!frame_i.removeVertex(n))
				System.out.println("Elimination went wrong, please check!");
	}
	
	
	/**
	 * Marks all border cells in all frames in the stGraph. (i.e. cell.setBoundary(true))
	 */
	public void markOnly() {
		
		//Identify the boundary for every frame
		for(int time_point_i=0; time_point_i<stGraph.size();time_point_i++){
			FrameGraph frame_i = stGraph.getFrame(time_point_i);
			Geometry boundary = findBorderCells(frame_i);
			markBorderCells(frame_i,boundary);
			boundaries[time_point_i] = boundary;
		}
	}

	/**
	 * Find the border geometry by computing the union of all cells
	 * 
	 * @param frame_i frame to compute the border for
	 * @return LineString geometry representing the border
	 */
	private Geometry findBorderCells(FrameGraph frame_i) {

		//set up polygon container
		Geometry[] output = new Geometry[frame_i.size()];
		Iterator<Node> node_it = frame_i.iterator();
		for(int i=0; i<frame_i.size(); i++){
			output[i] = node_it.next().getGeometry();
		}		

		//create union
		Geometry union = CascadedPolygonUnion.union(Arrays.asList(output));

		//Compute boundary ring (linear ring)
		Geometry boundary = union.getBoundary();
		
		markBorderCells(frame_i, boundary);
		
		return boundary;
	}

	/**
	 * Mark all the nodes that intersect the boundary Geometry
	 * 
	 * @param frame_i frame in which to mark the boundary cells
	 * @param boundary boundary/border geometry
	 */
	public void markBorderCells(FrameGraph frame_i, Geometry boundary) {

		PreparedGeometry cached_boundary = cached_factory.create(boundary);
		
		for(Node n: frame_i.vertexSet()){
			Geometry p = n.getGeometry();
			if(cached_boundary.intersects(p))
				n.setBoundary(true);
		}
		
		frame_i.setBoundary(boundary);
	}
	
	/**
	 * Mark the frames in the set stGraph with a collection 
	 * of WKT border files in the folder supplied.<br><br>
	 * 
	 * Name convention: [export_folder]/border_000.wkt
	 * 
	 * @param export_folder location of the border files
	 */
	public void markBorderCellsWKT(String export_folder){
		WktPolygonImporter wkt_importer = new WktPolygonImporter();
		for(int i=0; i < stGraph.size(); i++){
			String expected_wkt_file = String.format("%s/border_%03d.wkt",export_folder,i);
			ArrayList<Geometry> boundaries = wkt_importer.extractGeometries(expected_wkt_file);

			FrameGraph frame = stGraph.getFrame(i);
			markBorderCells(frame, boundaries.get(0));
		}
	}

}
