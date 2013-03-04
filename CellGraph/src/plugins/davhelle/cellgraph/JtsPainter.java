package plugins.davhelle.cellgraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;


public class JtsPainter extends AbstractPainter{

//	//TODO Might be useful to get back here if the graph is updated
	//TODO Maybe implement an instant switch to turn off/on the representation
	//		of polygons and/or cell centers.
	
	private Collection geomCol;
	
	private ArrayList<Point> cell_center_list;
	
	private int time_point;
	
	public JtsPainter(Collection collection,int time_point){
		
		geomCol = collection;
		
		this.time_point = time_point;
		
		//Use updatehandler?
		this.updatePolygonList();

	}
	
	public void updatePolygonList(){
		
		cell_center_list = new ArrayList<Point>();
		
		for(Object ob: geomCol){
			Polygon cell = (Polygon)ob;
			cell_center_list.add(cell.getCentroid());
		}
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		//only display when on selected frame
		if(Icy.getMainInterface().getFirstViewer(sequence).getT() == time_point){
			//Initialize painter
			g.setStroke(new BasicStroke(1));

			Iterator<Point> cell_center_it = cell_center_list.iterator();

			while(cell_center_it.hasNext()){

				//Set polygon color
				g.setColor(Color.RED);

				Point cell_center = cell_center_it.next();

				g.drawOval(
						(int)cell_center.getX(), 
						(int)cell_center.getY(),
						1, 1);

			}
		}
	}

}

