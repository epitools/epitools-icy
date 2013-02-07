package plugins.davhelle.cellgraph;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import be.humphreys.simplevoronoi.GraphEdge;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

/**
 * PolygonPainter depicts the java.awt.Polygons supplied to
 * it or builds the polygons from a Custom GraphEdge List
 * as created by the SimpleVoronoi package.
 * 
 * @author Davide Heller
 *
 */
public class PolygonPainter extends AbstractPainter{
	
	private int time_point;
	private ArrayList<Polygon> cell_polygon_list;
	private ArrayList<Color> cell_color_list;
	
	public PolygonPainter(ArrayList<Polygon> cell_polygon_list, int time_point){
		this.cell_polygon_list = cell_polygon_list;
		this.time_point = time_point;
	}
	
	public PolygonPainter(ArrayList<Point> cell_centers,
			List<GraphEdge> voronoi_edges, int time_point,
			ArrayList<Color> cell_color_list){
		
		this.cell_color_list = cell_color_list;
		
		int cell_no = cell_centers.size();
		HashMap<Integer,List<GraphEdge>> cell_edges = new HashMap<Integer,List<GraphEdge>>();
		
		//Assign every voronoi GraphEdge to it's belonging cells
		Iterator<GraphEdge> edge_it = voronoi_edges.iterator();
		
		while(edge_it.hasNext()){
			GraphEdge next = edge_it.next();
		
			if(cell_edges.containsKey(next.site1))
				cell_edges.get(next.site1).add(next);
			else{
				List<GraphEdge> new_list = new ArrayList<GraphEdge>();
				new_list.add(next);
				cell_edges.put(next.site1,new_list);
			}
			
			if(cell_edges.containsKey(next.site2))
				cell_edges.get(next.site2).add(next);
			else{
				List<GraphEdge> new_list = new ArrayList<GraphEdge>();
				new_list.add(next);
				cell_edges.put(next.site2,new_list);
			}			
		}
		
		//Build the polygons from the assigned Edges				
		this.cell_polygon_list = new ArrayList<Polygon>(cell_no);
		
		for(int i=0; i<cell_no; i++){
			
			//Given the list of edges assigned to cell_i
			List<GraphEdge> cell_i_edges = cell_edges.get(i);
			
			//Extract first edge to be inserted into a new polygon
			//and remove it from the set
			GraphEdge current_edge = cell_i_edges.remove(0);
			
			//Create new polygon
			Polygon voronoi_poly = new Polygon();
			
			//insert first vertex
			Point source = current_edge.getSource();
			voronoi_poly.addPoint(source.x, source.y);
			
			//Using the target vertex search for the
			//connecting edge to close the polygon
			//iteratively
			
			Point target = current_edge.getTarget();
			boolean POLYGON_STILL_OPEN = true;
			int j = 0;
			
			while(POLYGON_STILL_OPEN){
				GraphEdge next = cell_i_edges.get(j);
				
//				System.out.println(j+":"+target.toString()+
//						" == "+next.getSource().toString()+"?");
				
				//check for source correspondence
				if(target.equals(next.getSource())){
					source = next.getSource();
					target = next.getTarget();
					
					voronoi_poly.addPoint(source.x, source.y);

					cell_i_edges.remove(j);
					if(cell_i_edges.isEmpty())
						POLYGON_STILL_OPEN = false;
					else
						j = 0;
				}
				//check for target correspondence (reverted order)
				else if(target.equals(next.getTarget())){
				
					source = next.getTarget();
					target = next.getSource();
					
					voronoi_poly.addPoint(source.x, source.y);
					
					cell_i_edges.remove(j);
					if(cell_i_edges.isEmpty())
						POLYGON_STILL_OPEN = false;
					else
						j = 0;
				}
				//else check for correspondence in next edge
				else{
					j++;
					if(j == cell_i_edges.size()){
						//TODO:double approximation to be corrected
						// possibly by floor or ceiling
						System.out.println("Error: No connection found to target:"+target.toString());
						break;
					}
				}	
			}
			//if(!POLYGON_STILL_OPEN){ Commented to maintain proper cell-center correspondence
			this.cell_polygon_list.add(voronoi_poly);
			//
		}
		
		this.time_point = time_point;
		
	}
	
	public ArrayList<Polygon> getPolygons(){
		return cell_polygon_list;
	}

	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		//only display when on selected frame
		if(Icy.getMainInterface().getFirstViewer(sequence).getT() == time_point){
			//Initialize painter
			g.setStroke(new BasicStroke(1));
//			g.setColor(Color.BLUE);

			Iterator<Polygon> cell_polygon  = cell_polygon_list.iterator();
			Iterator<Color> cell_color = cell_color_list.iterator();
			while(cell_polygon.hasNext()){
				g.setColor(cell_color.next());
				g.drawPolygon(cell_polygon.next());
			}
			
		}
    }
}
