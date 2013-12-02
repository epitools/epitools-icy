package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class CellMarker extends Overlay {
	
	private SpatioTemporalGraph stGraph;
	private GeometryFactory factory;
	private Color tag_color;
	
	public CellMarker(SpatioTemporalGraph stGraph, Color tag_color) {
		super("Cell Marker");
		this.stGraph = stGraph;
		this.factory = new GeometryFactory();
		this.tag_color = tag_color;
	}
	
	@Override
	public void mouseClick(MouseEvent e, Point2D imagePoint, IcyCanvas canvas){
		int time_point = canvas.getPositionT();
		
		if(time_point < stGraph.size()){
			
			//create point Geometry
			Coordinate point_coor = new Coordinate(imagePoint.getX(), imagePoint.getY());
			Point point_geometry = factory.createPoint(point_coor);			
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			for(Node cell: frame_i.vertexSet())
			 	if(cell.getGeometry().contains(point_geometry))
			 		cell.setColorTag(tag_color);
		}

	}
	
	//simpler: interface launch marker with certain color
	//output: if==color take for certain column -> alex workbook

}
