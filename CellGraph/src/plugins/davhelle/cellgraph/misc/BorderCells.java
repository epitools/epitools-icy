/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.misc;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.io.WktPolygonExporter;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

/**
 * Class identifies the cells which constitute the border 
 * of the given selection. The task is achieved by consid-
 * ering the union of all cells and asking which polygons
 * intersect with the outer ring. Currently this is an un-
 * optimized version and takes some time. SysOut given.  
 * 
 * @author Davide Heller
 *
 */
public class BorderCells extends Overlay{

	private SpatioTemporalGraph stGraph;
	private HashMap<FrameGraph,Geometry> frame_ring_map;
	
	public BorderCells(SpatioTemporalGraph stGraph) {
		super("Border cells");
		//Set data structures
		this.stGraph = stGraph;
		this.frame_ring_map = new HashMap<FrameGraph,Geometry>();
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
	public void applyBoundaryCondition(){
		
		//Identify the boundary for every frame
		for(int time_point_i=0; time_point_i<stGraph.size();time_point_i++){
			
			applyBoundaryConditionsToFrame(time_point_i);
		}
	}

	public void applyBoundaryConditionsToFrame(int time_point_i) {
		long startTime = System.currentTimeMillis();
		FrameGraph frame_i = stGraph.getFrame(time_point_i);

		//set up polygon container

		Geometry[] output = new Geometry[frame_i.size()];
		Iterator<Node> node_it = frame_i.iterator();
		for(int i=0; i<frame_i.size(); i++){
			output[i] = node_it.next().getGeometry();
		}		

		//Create union of all polygons
//			GeometryCollection polygonCollection = new GeometryCollection(output, new GeometryFactory());
//			Geometry union = polygonCollection.buffer(0);
		
		//On avg.faster and more robust method
		Geometry union = CascadedPolygonUnion.union(Arrays.asList(output));

		//Compute boundary ring
		Geometry boundary = union.getBoundary();
//			LinearRing borderRing = (LinearRing) boundary;

		
		//Get first boundary ring (not proper polygons)
		ArrayList<Node> wrong_cells = new ArrayList<Node>();
		
		//Check via intersection if cell is border cell
		node_it = frame_i.iterator();
		for(int i=0; i<frame_i.size(); i++){

			Node n = node_it.next();
			Geometry p = n.getGeometry();
			
			boolean is_border = p.intersects(boundary);

			
			//Cancel wrong boundary
			if(is_border)
				wrong_cells.add(n);

		}
		
		//Can't remove vertices while iterating so doining it now
		ArrayList<Geometry> wrong_boundary = new ArrayList<Geometry>();
		for(Node n: wrong_cells)
			if(frame_i.removeVertex(n))
				wrong_boundary.add(n.getGeometry());

		//Outer polygon boundary for which statistics won't computed
//			GeometryCollection polygonBoundary = 
//					new GeometryCollection(
//							wrong_boundary.toArray(
//									new Geometry[wrong_boundary.size()]),
//									new GeometryFactory());
//			
//			Geometry polygonRing = polygonBoundary.buffer(0);
		
		//faster method as above
		Geometry polygonRing = CascadedPolygonUnion.union(wrong_boundary);
				
		frame_ring_map.put(frame_i, polygonRing);
		
		node_it = frame_i.iterator();
		for(int i=0; i<frame_i.size(); i++){
			Node n = node_it.next();
			Geometry p = n.getGeometry();
			
			boolean is_border = p.intersects(polygonRing);
			
			//Remember good boundary
			n.setBoundary(is_border);
		}
		
		long endTime = System.currentTimeMillis();		
		System.out.println("Applied boundary conditions to frame "+time_point_i+" in " + (endTime - startTime) + " milliseconds");
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

		//set up polygon container
		Geometry[] output = new Geometry[frame_i.size()];
		Iterator<Node> node_it = frame_i.iterator();
		for(int i=0; i<frame_i.size(); i++){
			output[i] = node_it.next().getGeometry();
		}		

		//Create union of all polygons
//		GeometryCollection polygonCollection = new GeometryCollection(output, new GeometryFactory());
//		Geometry union = polygonCollection.buffer(0);
		
		//faster method as in main routine
		Geometry union = CascadedPolygonUnion.union(Arrays.asList(output));

		//Compute boundary ring
		Geometry boundary = union.getBoundary();
//		LinearRing borderRing = (LinearRing) boundary;
		
		//Check via intersection if cell is border cell
		node_it = frame_i.iterator();
		ArrayList<Node> borderCells = new ArrayList<Node>();
		for(int i=0; i<frame_i.size(); i++){

			Node n = node_it.next();
			Geometry p = n.getGeometry();
			
			boolean is_border = p.intersects(boundary);

			//Set border flag if intersect
			if(is_border)
				borderCells.add(n);
		}

		//Can't remove vertices while iterating so doining it now
		for(Node n: borderCells)
			if(!frame_i.removeVertex(n))
				System.out.println("Elimination went wrong, please check!");

		
		System.out.println("Removed one outer layer!");
		
	}
	
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.orange);
			ShapeWriter shapeWriter = new ShapeWriter();
			if(frame_ring_map.containsKey(frame_i)){
				Geometry ring = frame_ring_map.get(frame_i);
				g.draw(shapeWriter.toShape(ring));
			}
				
			
//			for(Node cell: frame_i.vertexSet()){
//
//				if(cell.onBoundary())
//					g.setColor(Color.white);
//				else
//					g.setColor(Color.green);
//
//				//Fill cell shape
//				//if(!cell.onBoundary())
//				g.fill(cell.toShape());
//
//			}
		}
		
	}

	/**
	 * Only mark border cells without eliminating them
	 * 
	 * 
	 */
	public void markOnly() {
		
		WktPolygonExporter bounary_exporter = new WktPolygonExporter();
		
		//Identify the boundary for every frame
		for(int time_point_i=0; time_point_i<stGraph.size();time_point_i++){
			
			Geometry boundary = markBoundaryCellsInFrame(time_point_i);
			
			bounary_exporter.export(boundary,time_point_i);
			
		}
	}

	private Geometry markBoundaryCellsInFrame(int time_point_i) {
		FrameGraph frame_i = stGraph.getFrame(time_point_i);

		//set up polygon container

		Geometry[] output = new Geometry[frame_i.size()];
		Iterator<Node> node_it = frame_i.iterator();
		for(int i=0; i<frame_i.size(); i++){
			output[i] = node_it.next().getGeometry();
		}		

		//Create union of all polygons
//			GeometryCollection polygonCollection = new GeometryCollection(output, new GeometryFactory());
//			Geometry union = polygonCollection.buffer(0);
		
		//More robust method
		Geometry union = CascadedPolygonUnion.union(Arrays.asList(output));

		//Compute boundary ring
		Geometry boundary = union.getBoundary();
//			LinearRing borderRing = (LinearRing) boundary;
		
		//Check via intersection if cell is border cell
		node_it = frame_i.iterator();
		for(int i=0; i<frame_i.size(); i++){

			Node n = node_it.next();
			Geometry p = n.getGeometry();
			
			boolean is_border = p.intersects(boundary);

			//Set border flag if intersect
			if(is_border)
				n.setBoundary(true);
		}
		
		return boundary;
	}

}
