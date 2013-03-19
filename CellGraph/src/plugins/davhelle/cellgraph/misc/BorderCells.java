package plugins.davhelle.cellgraph.misc;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import plugins.davhelle.cellgraph.graphs.DevelopmentType;
import plugins.davhelle.cellgraph.graphs.TissueGraph;
import plugins.davhelle.cellgraph.nodes.NodeType;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

public class BorderCells extends AbstractPainter{

	private DevelopmentType stGraph;
	private HashMap<NodeType,Boolean> border_cell_map; 
	private HashMap<TissueGraph,Geometry> frame_ring_map;
	
	public BorderCells(DevelopmentType stGraph) {
		
		//extract time point
		this.stGraph = stGraph;
		this.border_cell_map = new HashMap<NodeType,Boolean>();
		this.frame_ring_map = new HashMap<TissueGraph,Geometry>();
		
		for(int time_point_i=0; time_point_i<stGraph.size();time_point_i++){
			
			TissueGraph frame_i = stGraph.getFrame(time_point_i);

			//set up polygon container
		
			Geometry[] output = new Geometry[frame_i.size()];
			Iterator<NodeType> node_it = frame_i.iterator();
			for(int i=0; i<frame_i.size(); i++){
				output[i] = node_it.next().getGeometry();
			}		

			//Create union of all polygons
			GeometryCollection polygonCollection = new GeometryCollection(output, new GeometryFactory());
			Geometry union = polygonCollection.buffer(0);

			//Compute boundary ring
			Geometry boundary = union.getBoundary();
			LinearRing borderRing = (LinearRing) boundary;

			
			//Get first boundary ring (not proper polygons)
			ArrayList<NodeType> wrong_cells = new ArrayList<NodeType>();
			
			//Check via intersection if cell is border cell
			node_it = frame_i.iterator();
			for(int i=0; i<frame_i.size(); i++){

				NodeType n = node_it.next();
				Geometry p = n.getGeometry();
				
				boolean is_border = p.intersects(borderRing);

				
				//Cancel wrong boundary
				if(is_border)
					wrong_cells.add(n);

			}
			
			//Can't remove vertices while iterating so doining it now
			ArrayList<Geometry> wrong_boundary = new ArrayList<Geometry>();
			for(NodeType n: wrong_cells)
				if(frame_i.removeVertex(n))
					wrong_boundary.add(n.getGeometry());

			//Outer polygon boundary for which statistics won't computed
			GeometryCollection polygonBoundary = 
					new GeometryCollection(
							wrong_boundary.toArray(
									new Geometry[wrong_boundary.size()]),
									new GeometryFactory());
			
			Geometry polygonRing = polygonBoundary.buffer(0);
			frame_ring_map.put(frame_i, polygonRing);
			
			node_it = frame_i.iterator();
			for(int i=0; i<frame_i.size(); i++){
				NodeType n = node_it.next();
				Geometry p = n.getGeometry();
				
				boolean is_border = p.intersects(polygonRing);
				
				//Remember good boundary
				border_cell_map.put(n, Boolean.valueOf(is_border));
			}
		}
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		if(time_point < stGraph.size()){
			
			TissueGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.orange);
			ShapeWriter shapeWriter = new ShapeWriter();
			if(frame_ring_map.containsKey(frame_i)){
				Geometry ring = frame_ring_map.get(frame_i);
				g.draw(shapeWriter.toShape(ring));
			}
				
			
			for(NodeType cell: frame_i.vertexSet()){

				if(border_cell_map.containsKey(cell)){
					if(border_cell_map.get(cell))
						g.setColor(Color.white);
					else
						g.setColor(Color.green);
				}

				//Fill cell shape
				g.fill(cell.toShape());
			}
		}
		
	}

}
