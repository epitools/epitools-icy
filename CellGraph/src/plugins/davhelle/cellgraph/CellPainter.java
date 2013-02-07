package plugins.davhelle.cellgraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

/**
 * CellPainter depicts the identified Polygons in the VTK mesh 
 * From the main GUI polygon boarder and/or centers can be displayed.
 * 
 * @author Davide Heller
 *
 */
public class CellPainter extends AbstractPainter{
	
//	//TODO Might be useful to get back here if the graph is updated
	//TODO Maybe implement an instant switch to turn off/on the representation
	//		of polygons and/or cell centers.
	
	private boolean DRAW_POLYGONS;
	private boolean DRAW_CCENTERS;
	
	private Map<Integer, ArrayList<Integer>> cell_map;
	ArrayList<CellCorner> corner_list;
	
	private ArrayList<Polygon> cell_polygon_list;
	private ArrayList<Point> cell_center_list;
	private ArrayList<Color> cell_color_list;
	
	private int time_point;
	
	public CellPainter(Map<Integer, ArrayList<Integer>> cell_map,
			ArrayList<CellCorner> corner_list,
			boolean draw_polygons,boolean draw_cell_centers, int time_point){
		
		this.cell_map = cell_map;
		this.corner_list = corner_list;
		
		this.DRAW_POLYGONS = draw_polygons;
		this.DRAW_CCENTERS = draw_cell_centers;
		
		this.time_point = time_point;
		
		//Use updatehandler?
		this.updatePolygonList();

	}
	
	public void updatePolygonList(){
		//Random number for color generation
    	Random rand = new Random();
    	
    	//Get hash tags for all cells
    	Set<Integer> cell_keys = cell_map.keySet();
    	Iterator<Integer> cell_it = cell_keys.iterator();
    	
    	cell_polygon_list = new ArrayList<Polygon>();
    	cell_center_list = new ArrayList<Point>();
    	cell_color_list = new ArrayList<Color>();
    	
    	//Cycle through all cells to depict them as awt.Polygons
    	while(cell_it.hasNext()){
    		int cell_hash = cell_it.next();
    		ArrayList<Integer> poly_cell = cell_map.get(cell_hash);

    		//Build awt.Polygon for cell
    		Polygon cell_polygon = new Polygon();
    		
    		for(int i=0; i<poly_cell.size(); i++){
    			CellCorner corner_i = corner_list.get(poly_cell.get(i));
    			cell_polygon.addPoint(corner_i.getX(), corner_i.getY());
    		}
    		
    		Point cell_center = PolygonUtils.polygonCenterOfMass(cell_polygon);
    		
    		// Generate random color for cell
    		float r_idx = rand.nextFloat();
    		float g_idx = rand.nextFloat();
    		float b_idx = rand.nextFloat();      

    		Color cell_color = new Color(r_idx, g_idx, b_idx);
    		cell_color.brighter();
    		
    		cell_polygon_list.add(cell_polygon);
    		cell_center_list.add(cell_center);
    		cell_color_list.add(cell_color);
    	}
	}
	
	//Return all cell centers found
	public ArrayList<Point> getCellCenters(){
		return this.cell_center_list;
	}
	
	public ArrayList<Color> getCellColors(){
		return cell_color_list;
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

			Iterator<Polygon> cell_polygon  = cell_polygon_list.iterator();
			Iterator<Color> cell_color = cell_color_list.iterator();
			Iterator<Point> cell_center_it = cell_center_list.iterator();

			while(cell_color.hasNext()){

				//Set polygon color
				g.setColor(cell_color.next());

				//Draw cell polygon
				if(DRAW_POLYGONS)		
					g.drawPolygon(cell_polygon.next());

				//Draw cell center
				if(DRAW_CCENTERS){
					Point cell_center = cell_center_it.next();
					g.drawOval(cell_center.x, cell_center.y, 1, 1);
				}

			}
		}
    }

}
