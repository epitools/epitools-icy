package plugins.davhelle.cellgraph;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Iterator;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


/**
 * Class to depict the difference between the voronoi polygons
 * and the segmentation polygons. Using the JTS boundary information
 * this class restricts the analysis to non-boundary cells only.
 * 
 * @author davide
 *
 */
public class JtsAreaDifferenceMap extends AbstractPainter{
	
	private HashMap<Polygon, Color> non_border_poly;
	
	private int time_point;
	
	public JtsAreaDifferenceMap(
			HashMap<Polygon,Point> cc_poly_map,
			HashMap<Point, Polygon> cc_voro_map,
			HashMap<Polygon,Boolean> border_polygon_map,
			int time_point){
		
		this.time_point = time_point;
		this.non_border_poly = new HashMap<Polygon,Color>();
		
		Iterator<Polygon> cell_it = border_polygon_map.keySet().iterator();
		
		//Paint voronoi cells according to the border condition
		//of the original cell they were generated from
		while(cell_it.hasNext()){
			Polygon cell = cell_it.next();
			Point cell_center = cc_poly_map.get(cell);
			Polygon	voronoi = cc_voro_map.get(cell_center);
			
			if(border_polygon_map.get(cell).booleanValue())
				non_border_poly.put(voronoi, Color.BLACK);
			else	
				non_border_poly.put(voronoi, Color.GREEN);
		}
		
		
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		//only display when on selected frame
		if(Icy.getMainInterface().getFirstViewer(sequence).getT() == time_point){
			//Initialize painter
			g.setStroke(new BasicStroke(1));
			
			//Use JTS awt wrapper to print voronoi diagram
			ShapeWriter writer = new ShapeWriter();

			//Color Polygons according to color in Map
			Iterator<Polygon> p_it = non_border_poly.keySet().iterator();
			while(p_it.hasNext()){
				Polygon p = p_it.next();
				g.setColor(non_border_poly.get(p));
				g.draw(writer.toShape(p));
			}
			
			
		}
	}
	
}
