package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.util.HashMap;

import jxl.write.WritableSheet;

import plugins.adufour.ezplug.EzVarEnum;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.CellColor;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
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
public class CellMarker extends StGraphOverlay {
	
	private GeometryFactory factory;
	private EzVarEnum<CellColor> tag_color;
	private ShapeWriter writer;
	
	public static final String DESCRIPTION = 
			"Overlay to interactively mark cells with a color of choice and export the selection.\n\n" +
			"1. Run [>] to activate the marker\n" +
			"2. Select the color to begin to mark with\n" + 
			"3. Click on any cell to mark it\n" +
			"4. Click again to remove or change color\n" +
			"5. The XLS export in the layer menu will build\n" +
			"   a spreadsheet with the marked cells.\n" +
			"6. Remove the overlay (Layer > [x]) to stop";
	
	public CellMarker(SpatioTemporalGraph stGraph, EzVarEnum<CellColor> varCellColor) {
		super("Cell Color Tag",stGraph);
		this.factory = new GeometryFactory();
		this.tag_color = varCellColor;
		this.writer = new ShapeWriter();

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
			 		Color new_tag = tag_color.getValue().getColor();

			 		if(cell.hasColorTag()){
			 			Color current_tag = cell.getColorTag();
			 			if(current_tag == new_tag)
			 				cell.setColorTag(null);
			 			else
			 				cell.setColorTag(new_tag);
			 		} else 
			 			cell.setColorTag(new_tag);
			 	}
			
		}

	}
	
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		for(Node cell: frame_i.vertexSet())
			if(cell.hasColorTag()){
				g.setColor(cell.getColorTag());
				g.fill(writer.toShape(cell.getGeometry()));
			}

	}
	
	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		int color_no = 0;
		HashMap<Color, Integer> row_no = new HashMap<Color, Integer>();
		HashMap<Color, Integer> col_no = new HashMap<Color, Integer>();

		for(Node node: frame.vertexSet()){

			if(node.hasColorTag()){
				Color cell_color = node.getColorTag();
				if(!row_no.containsKey(cell_color)){
					row_no.put(cell_color, 1);
					col_no.put(cell_color, color_no);
					XLSUtil.setCellString(sheet, color_no++, 0, getColorName(cell_color));
				}

				int x = col_no.get(cell_color).intValue();
				int y = row_no.get(cell_color).intValue();

				XLSUtil.setCellNumber(sheet, x, y, node.getGeometry().getArea());

				row_no.put(cell_color, y+1);
			}
		}
		
	}
	
	/**
	 * source: http://stackoverflow.com/a/12828811
	 * 
	 * convert the color into a string if possible
	 * 
	 * @param c
	 * @return
	 */
	public static String getColorName(Color c) {
	    for (Field f : Color.class.getFields()) {
	        try {
	            if (f.getType() == Color.class && f.get(null).equals(c)) {
	                return f.getName();
	            }
	        } catch (java.lang.IllegalAccessException e) {
	            // it should never get to here
	        } 
	    }
	    return "unknown";
	}
	
	//simpler: interface launch marker with certain color
	//output: if==color take for certain column -> alex workbook

}
