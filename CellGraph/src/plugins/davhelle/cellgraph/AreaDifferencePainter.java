package plugins.davhelle.cellgraph;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * AreaDifferencePainter computes and depicts the difference between 
 * cell areas and the correspondent Voronoi cell areas. 
 * 
 * Currently the cells with a positive difference are shaded in green 
 * (i.e. bigger than voronoi) and the ones with a negative difference
 * in red. Neutral (+/- THRESHOLD) are left white. 
 * 
 *  TODO Bug in the double int conversion, check area computation as well
 * 
 * @author Davide Heller
 *
 */
public class AreaDifferencePainter extends AbstractPainter{
	
	private final int DIFFERENCE_THRESHOLD = 10;
	
	private ArrayList<Polygon> cell_polygon_list;
	private ArrayList<Polygon> voronoi_polygon_list;
	private ArrayList<Color> area_difference_color_list;
	private ArrayList<Double> area_difference_val;

	private int color_scheme;
	private int time_point;
	
	public AreaDifferencePainter(
			ArrayList<Polygon> cell_polygon_list,
			ArrayList<Polygon> voronoi_polygon_list,
			int time_point){
		
		this.cell_polygon_list = cell_polygon_list;
		this.voronoi_polygon_list = voronoi_polygon_list;
		this.time_point = time_point;
		this.color_scheme = 2;
		
		this.area_difference_color_list = new ArrayList<Color>();
		this.area_difference_val = new ArrayList<Double>();
		
		computeAreaDifference();
	}
	
	private void computeAreaDifference(){
		
		Iterator<Polygon> cell_it = cell_polygon_list.iterator();
		Iterator<Polygon> voronoi_it = voronoi_polygon_list.iterator();
		
		while(cell_it.hasNext()){
			Polygon cell = cell_it.next();
			Polygon voronoi = voronoi_it.next();
			
			//TODO might need to change everything to double as Polygon saves int coordinates!
			double cell_area = PolygonUtils.PolygonArea(cell);
			double voronoi_area = PolygonUtils.PolygonArea(voronoi);
			double area_difference = cell_area - voronoi_area;
			area_difference_val.add(area_difference);
			
			//Color scheme generation
			
			if(color_scheme == 1){			
				//Color cells according to threshold into three categories
				double area_threshold = DIFFERENCE_THRESHOLD;
				if(area_difference > area_threshold)
					area_difference_color_list.add(Color.GREEN);
				else if(area_difference < area_threshold*(-1))
					area_difference_color_list.add(Color.RED);
				else
					area_difference_color_list.add(Color.WHITE);
			}
			else{		
				//color scheme which allows +/- 255 differences (255 being the MAX_DIFF)
				//magenta (negative diff.) to white (neutral) to light blue (positive)
				int intensity = 255 - (int)Math.min(Math.abs(area_difference), 255);
				if(area_difference > 0)
					area_difference_color_list.add(new Color(intensity,255,255));
				else
					area_difference_color_list.add(new Color(255,intensity,255));
			}
			
		}
	}
	
	public ArrayList<Double> getAreaDifference(){
		return area_difference_val;
	}
	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		//Set layer to 0.3 opacity
		Layer current_layer = canvas.getLayer(this);
		current_layer.setAlpha((float)0.3);		
		
		//only display when on selected frame
		if(Icy.getMainInterface().getFirstViewer(sequence).getT() == time_point){
			
			//Initialize painter
			g.setStroke(new BasicStroke(1));
//			g.setColor(Color.BLUE);
			
			//Shade every cell according to found difference
			Iterator<Polygon> cell_polygon  = cell_polygon_list.iterator();
			Iterator<Color> cell_color = area_difference_color_list.iterator();
			while(cell_polygon.hasNext()){
				g.setColor(cell_color.next());
				g.fillPolygon(cell_polygon.next());
			}	
			
		}
    }

}
