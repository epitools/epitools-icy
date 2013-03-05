package plugins.davhelle.cellgraph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

public class JtsVoronoiPainter extends AbstractPainter{
	
	private Geometry voronoi_poly;
	private int time_point;
	
	public JtsVoronoiPainter(ArrayList<Point> cell_centers,int time_point){
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
		this.time_point = time_point;
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		//only display when on selected frame
		if(Icy.getMainInterface().getFirstViewer(sequence).getT() == time_point){
			//Initialize painter
			g.setStroke(new BasicStroke(1));
			g.setColor(Color.GREEN);
			
			//Use JTS awt wrapper to print voroni diagram
			ShapeWriter writer = new ShapeWriter();
			g.draw(writer.toShape(voronoi_poly));
			
		}
	}
}
