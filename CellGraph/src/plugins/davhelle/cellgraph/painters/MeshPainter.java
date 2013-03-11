package plugins.davhelle.cellgraph.painters;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableUndirectedGraph;

import plugins.davhelle.cellgraph.nodes.CellCorner;

import icy.canvas.Canvas2D;
import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;


/**
 * MeshPainter generates a map depiction for the Graph extracted from
 * the raw vtk file. If z-height information is present the map
 * is colored accordingly.
 * 
 * TODO: graphics dimension should scale with zoom (previously automatic with ROI)
 * 
 * @author Davide Heller
 *
 */
public class MeshPainter extends AbstractPainter{
	
	//Fields describing the minimal and maximal z-height of read graph
	private int z_min;
	private int z_max;
	private int time_point;
	
	private ListenableUndirectedGraph<CellCorner, DefaultEdge> cell_corner_graph;
	
	public MeshPainter(ListenableUndirectedGraph<CellCorner, DefaultEdge> lug,
			int user_z_max,
			int time_point){
		
		cell_corner_graph = lug;
		this.time_point = time_point;
		
		//figure out what the min and maximal z values are
		Set<CellCorner> vertex_set = cell_corner_graph.vertexSet();
		Iterator<CellCorner> vertex_it = vertex_set.iterator();
		
		int v_z = (int)vertex_it.next().getZ();
		z_min = v_z;
		z_max = v_z;
		
		while(vertex_it.hasNext()){
			v_z = (int)vertex_it.next().getZ();
			if(v_z < z_min)
				z_min = v_z;
			else if(v_z > z_max)
				z_max = v_z;
		}
		
		if(user_z_max != 0)
			z_max = user_z_max;
		
	}
	
	private Color heatMapColor(int source_z, int target_z){

		//normalize by z_min
		int green_idx = (source_z - z_min) + (target_z - z_min);
		int red_idx = 0;
		
		//Maximum of 20 color heights (min to max)
		if(green_idx > 10){
			red_idx = green_idx - 10;
			green_idx = 10;
			if(red_idx > 10){
				red_idx = 10;
			}
		}
		
		return new Color(255 - red_idx * 25, green_idx * 25,0);
	}
	
    @Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
    	
    	//TODO Outsource the computation to 1-time method! as for CellPainter
    	
		//only display when on selected frame
		if(Icy.getMainInterface().getFirstViewer(sequence).getT() == time_point){

			// check if we are dealing with a canvas 2D
			if (canvas instanceof Canvas2D)
			{

				Set<DefaultEdge> edge_set = cell_corner_graph.edgeSet();
				Iterator<DefaultEdge> edge_it = edge_set.iterator();

				//Default parameters for depiction
				//    		g.setColor(Color.blue);
				g.setStroke(new BasicStroke(1));

				while(edge_it.hasNext()){

					DefaultEdge edge_i = edge_it.next();

					CellCorner v_source = cell_corner_graph.getEdgeSource(edge_i);
					CellCorner v_target = cell_corner_graph.getEdgeTarget(edge_i);

					//Decide color for Mesh
					int source_z = (int)v_source.getZ();
					int target_z = (int)v_target.getZ();

					//Only edges lower than user defined threshold are admitted
					if(source_z <= z_max || target_z <= z_max){ 

						Color edge_col = heatMapColor(source_z, target_z);

						g.setColor(edge_col);

						//    			//Check where points are drawn
						//    			System.out.println(
						//    					v_source.getX()+ " " + v_target.getX() + " " + v_source.getY() + " " +  v_target.getY());

						g.drawLine(v_source.getX(), v_source.getY(), v_target.getX(), v_target.getY());


					}
				}

			}
		}
    }

}
