/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

/**
 * Class identifies the cells which constitute the border 
 * of the given selection. The task is achieved by consid-
 * ering the union of all cells and asking which polygons
 * intersect with the outer ring.  
 * 
 * @author Davide Heller
 *
 */
public class BorderCells{

	private SpatioTemporalGraph stGraph;
	private PreparedGeometryFactory cached_factory;
	private Geometry[] boundaries;
	
	/**
	 * @return the boundaries for all frames
	 */
	public Geometry[] getBoundaries() {
		return boundaries;
	}


	public BorderCells(SpatioTemporalGraph stGraph) {
		//Set data structures
		this.stGraph = stGraph;
		boundaries = new Geometry[stGraph.size()];
		cached_factory = new PreparedGeometryFactory();
	}
	
	
	/**
	 * The boundary cells of every frame are identified by merging all Node geometries.
	 * After removing the boundary cells from the graph the Nodes forming the new
	 * boundary are updated (setBoundary);
	 * 
	 * The removed layer is saved as polygon ring union.
	 * 
	 * TODO: split the method to allow for separate removal and labeling of boundary cells
	 */
	public void removeOneBoundaryLayerFromAllFrames(){
		
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
	 * The boundary cells of every frame are identified by merging all Node geometries.
	 * After removing the boundary cells from the graph the Nodes forming the new
	 * boundary are updated (setBoundary);
	 * 
	 * The removed layer is saved as polygon ring union.
	 * 
	 * TODO: split the method to allow for separate removal and labeling of boundary cells
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
	 * Removes all cells on the boundary from the graph
	 * 
	 * @param frame_i
	 * @param boundary
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
	 * Only mark border cells without eliminating them
	 * 
	 * 
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
	 * @param frame_i
	 * @return
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
	 * @param frame_i
	 * @param boundary
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

}
