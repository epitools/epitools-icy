package plugins.davhelle.cellgraph.jts_poc;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
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
	private double color_amplification_coeff;
	private double alpha_level;
	private int time_point;
	
	public JtsAreaDifferenceMap(
			HashMap<Polygon,Point> cc_poly_map,
			HashMap<Point, Polygon> cc_voro_map,
			HashMap<Polygon,Boolean> border_polygon_map,
			double color_amplification_coeff,
			int time_point){
		
		this.time_point = time_point;
		this.non_border_poly = new HashMap<Polygon,Color>();
		this.color_amplification_coeff = color_amplification_coeff;
		this.alpha_level = 0.35;
		
		Iterator<Polygon> cell_it = border_polygon_map.keySet().iterator();
		
		//Compose AreaDifferenceColorMap ignoring border cells
		
		while(cell_it.hasNext()){
			Polygon cell = cell_it.next();
			Point cell_center = cc_poly_map.get(cell);
			Polygon	voronoi = cc_voro_map.get(cell_center);
			
			if(border_polygon_map.get(cell).booleanValue())
				non_border_poly.put(cell, Color.BLACK);
			else{
				//Compute area difference
				double cell_area = cell.getArea();
				double voronoi_area = voronoi.getArea();
				double area_difference = cell_area - voronoi_area;
				
				int intensity = 255 - (int)Math.min(
						color_amplification_coeff*Math.abs(area_difference), 255);
				
				Color difference_color;
				if(area_difference > 0)
					difference_color = new Color(intensity,255,255);
				else
					difference_color = new Color(255,intensity,255);
				
				non_border_poly.put(cell, difference_color);
			}
				
		}
		
		
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
//		//Set layer to 0.3 opacity
//		Layer current_layer = canvas.getLayer(this);
//		current_layer.setAlpha((float)alpha_level);
		
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
				g.fill(writer.toShape(p));
			}
			
			
		}
	}
	
}
