package plugins.davhelle.cellgraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
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
	private ArrayList<Polygon> jts_polygons;
	
	private int time_point;
	
	public JtsPainter(Collection collection,int time_point){
		
		geomCol = collection;
		Polygon[] jts_polys;
		
		this.time_point = time_point;
		
		//Use updatehandler?
		this.updatePolygonList();

	}
	
	public JtsPainter(MultiPolygon all_jts_poly,int time_point){
		cell_center_list = new ArrayList<Point>();
		cell_center_list.add(all_jts_poly.getCentroid());
		this.time_point = time_point;		
	}
	
	public void updatePolygonList(){
		
		cell_center_list = new ArrayList<Point>();
		jts_polygons = new ArrayList<Polygon>();

		for(Object ob: geomCol){
			Polygon cell = (Polygon)ob;
			jts_polygons.add(cell);
			cell_center_list.add(cell.getCentroid());
		}
	}
	
	public MultiPolygon getMultiPoly(){
		Polygon[] output = new Polygon[jts_polygons.size()];
		for(int i=0; i<jts_polygons.size(); i++)
			output[i] = jts_polygons.get(i);
		return new MultiPolygon(output, new GeometryFactory());
	}
	
	public com.vividsolutions.jts.geom.Geometry getPolyUnion(){
		Polygon[] output = new Polygon[jts_polygons.size()];
		for(int i=0; i<jts_polygons.size(); i++)
			output[i] = jts_polygons.get(i);
		GeometryCollection polygonCollection = new GeometryCollection(output, new GeometryFactory());
		com.vividsolutions.jts.geom.Geometry union = polygonCollection.buffer(0);
		return union;
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
				g.setColor(Color.BLUE);

				Point cell_center = cell_center_it.next();

				g.drawOval(
						(int)cell_center.getX(), 
						(int)cell_center.getY(),
						1, 1);

			}
		}
	}

}

