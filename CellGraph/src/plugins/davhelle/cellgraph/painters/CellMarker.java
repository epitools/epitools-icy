package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import plugins.adufour.ezplug.EzVarEnum;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.CellColor;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Interactive painter to mark cell with a certain
 * color tag set in the UI. Current behavior tags
 * a cell is the default color is present (black)
 * or if another color is present. If the user
 * tries to repaint a cell with the same color it 
 * already has, the cell is put back to default.
 * 
 * 
 * @author Davide Heller
 *
 */
public class CellMarker extends Overlay {
	
	private SpatioTemporalGraph stGraph;
	private GeometryFactory factory;
	private EzVarEnum<CellColor> tag_color;
	
	public CellMarker(SpatioTemporalGraph stGraph, EzVarEnum<CellColor> varCellColor) {
		super("Cell Marker");
		this.stGraph = stGraph;
		this.factory = new GeometryFactory();
		this.tag_color = varCellColor;
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
			 	if(cell.getGeometry().contains(point_geometry)){
			 		Color current_tag = cell.getColorTag();
			 		Color new_tag = tag_color.getValue().getColor();
			 		if(current_tag == Color.black)
			 			cell.setColorTag(new_tag);
			 		else if(current_tag == new_tag)
			 			cell.setColorTag(Color.black);
			 		else
			 			cell.setColorTag(new_tag);
			 	}
			
		}

	}
	
	//simpler: interface launch marker with certain color
	//output: if==color take for certain column -> alex workbook

}
