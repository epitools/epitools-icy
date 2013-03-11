package plugins.davhelle.cellgraph.jts_poc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

public class JtsVoronoiPainter extends AbstractPainter{
	
	private Geometry voronoi_poly;
	private HashMap<Point, Polygon> cc_voro_map;
	private int time_point;
	
	public JtsVoronoiPainter(ArrayList<Point> cell_centers,int time_point){
		this.time_point = time_point;
		
		VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
		vdb.setClipEnvelope(new Envelope(0, 512, 0, 512));
		//does not work, needs coordinates vdb.setSites(cell_centers);
		
		//Conversion to coordinates
		Collection<Coordinate> coords = new ArrayList<Coordinate>();
		Iterator<Point> cell_it = cell_centers.iterator();
		while(cell_it.hasNext())
			coords.add(cell_it.next().getCoordinate());
		//set vornoi sites with coords
		vdb.setSites(coords);
		
		this.voronoi_poly = vdb.getDiagram(new GeometryFactory());		
		//find out nature of vornoi_poly
		//System.out.println(voronoi_poly.toText());
		//-> GeometryCollection of Polygons
		
		//find index connection between voronoi_polygons and cell_centers
		//and store them in the cc_voro_map
		ArrayList<Point> cc_copy = new ArrayList<Point>(cell_centers);
		this.cc_voro_map = new HashMap<Point,Polygon>();
		
		while(!cc_copy.isEmpty()){
			for(int i=0; i<voronoi_poly.getNumGeometries(); i++){
				Polygon p = (Polygon)voronoi_poly.getGeometryN(i);
				Iterator<Point> cc_it = cc_copy.iterator();
				while(cc_it.hasNext()){
					Point cc = cc_it.next();
					if(p.contains(cc)){
						cc_it.remove();
						cc_voro_map.put(cc, p);
					}
				}
			}
		}
	}
	
	public Geometry getVoronoiDiagram(){
		return voronoi_poly;
	}
	
	public HashMap<Point, Polygon> getCellCenterVoronoiPolygonMap(){
		return cc_voro_map;
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		//only display when on selected frame
		if(Icy.getMainInterface().getFirstViewer(sequence).getT() == time_point){
			//Initialize painter
			g.setStroke(new BasicStroke(1));
			g.setColor(Color.GREEN);
			
			//Use JTS awt wrapper to print voronoi diagram
			ShapeWriter writer = new ShapeWriter();
			//Complete diagram
			g.draw(writer.toShape(voronoi_poly));
			
			//POC Single voronoi cell with center/original cell center
//			Iterator<Point> cc_it = cc_voro_map.keySet().iterator();
//			while(cc_it.hasNext()){
//				Point cc = cc_it.next();
//				g.draw(writer.toShape(cc));
//				g.draw(writer.toShape(cc_voro_map.get(cc)));
//				break;
//			}
			
			
		}
	}
}
