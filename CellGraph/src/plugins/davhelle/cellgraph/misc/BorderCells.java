package plugins.davhelle.cellgraph.misc;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Iterator;

import plugins.davhelle.cellgraph.graphs.DevelopmentType;
import plugins.davhelle.cellgraph.graphs.TissueGraph;
import plugins.davhelle.cellgraph.nodes.NodeType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;

public class BorderCells extends AbstractPainter{

	private DevelopmentType stGraph;
	HashMap<NodeType,Boolean> border_cell_map; 
	
	public BorderCells(DevelopmentType stGraph) {
		
		//extract time point
		this.stGraph = stGraph;
		TissueGraph frame_i = stGraph.getFrame(0);
		
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
		
		//Check which cells are on the border
		this.border_cell_map = new HashMap<NodeType,Boolean>();
		
		//Check via intersection if cell is border cell
		node_it = frame_i.iterator();
		for(int i=0; i<frame_i.size(); i++){
			
			NodeType n = node_it.next();
			Geometry p = n.getGeometry();
			boolean is_border = p.intersects(borderRing);

			border_cell_map.put(n, Boolean.valueOf(is_border));
			
		}
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		if(time_point < stGraph.size()){

			for(NodeType cell: stGraph.getFrame(time_point).vertexSet()){

				if(border_cell_map.get(cell))
					//cell is part of border
					g.setColor(Color.white);
				else
					g.setColor(Color.black);

				//Fill cell shape
				g.fill(cell.toShape());
			}
		}
		
	}

}
