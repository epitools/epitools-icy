package plugins.davhelle.cellgraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;


public class JtsMultiPainter extends AbstractPainter{

//	//TODO Might be useful to get back here if the graph is updated
	//TODO Maybe implement an instant switch to turn off/on the representation
	//		of polygons and/or cell centers.
	
	private Collection geomCol;
	private LinearRing borderRing;
	
	private ArrayList<Coordinate> border_list;

	private int time_point;
	
	public JtsMultiPainter(MultiPolygon all_jts_poly,int time_point){
		border_list = new ArrayList<Coordinate>();
		//cell_center_list.add(all_jts_poly.getCentroid());
		this.time_point = time_point;
		
		com.vividsolutions.jts.geom.Geometry boundary =
				all_jts_poly.getBoundary();
		
		System.out.println(boundary.toText());
		//readout suggests MultiLineString
		
		Coordinate[] boundary_coor = boundary.getCoordinates();
		
		for(Coordinate coor_i: boundary_coor)
			border_list.add(coor_i);	
	}
	
	public JtsMultiPainter(com.vividsolutions.jts.geom.Geometry union, int time_point){
		this.time_point=time_point;
		
		border_list = new ArrayList<Coordinate>();

		
		com.vividsolutions.jts.geom.Geometry boundary =
				union.getBoundary();
		
		borderRing = (LinearRing) boundary;
		
		System.out.println(boundary.toText());
		//readout suggests MultiLineString
		
		Coordinate[] boundary_coor = boundary.getCoordinates();
		
		for(Coordinate coor_i: boundary_coor)
			border_list.add(coor_i);
		
	}
	
	public LinearRing getBorderRing(){
		return borderRing;
	}
	
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		//only display when on selected frame
		if(Icy.getMainInterface().getFirstViewer(sequence).getT() == time_point){
			//Initialize painter
			g.setStroke(new BasicStroke(1));

			Iterator<Coordinate> border_point_it = border_list.iterator();

			while(border_point_it.hasNext()){

				Coordinate border_point = border_point_it.next();
				
				//Set polygon color
				g.setColor(Color.BLUE);

				g.drawOval(
						(int)border_point.x, 
						(int)border_point.y,
						1, 1);

			}
		}
	}

}

