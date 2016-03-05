package plugins.davhelle.cellgraph.overlays;

import icy.canvas.IcyCanvas;
import icy.roi.ROI;
import icy.sequence.Sequence;
import icy.util.XLSUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D.Double;
import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jxl.write.WritableSheet;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.davhelle.cellgraph.export.BigXlsExporter;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.CellColor;
import plugins.davhelle.cellgraph.nodes.Division;
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
public class CellColorTagOverlay extends StGraphOverlay {
	
	/**
	 * JTS Geometry factory convert mouse clicks in JTS Points
	 */
	private GeometryFactory factory;
	/**
	 * GUI handle of the color selection
	 */
	private EzVarEnum<CellColor> tag_color;
	/**
	 * JTS class to convert JTS Geometries to AWT Shapes
	 */
	private ShapeWriter writer;
	/**
	 * ICY sequence to use as display
	 */
	private Sequence sequence;
	/**
	 * Visualization Flag whether to fill or outline (draw) the color Tag
	 */
	private EzVarBoolean drawColorTag;
	/**
	 * Flag for presence of tags in the stGraph 
	 */
	private boolean tags_exist;
	
	/**
	 * Description string for GUI use
	 */
	public static final String DESCRIPTION = 
			"Overlay to interactively mark cells with a color of choice and export the selection.\n\n" +
			"1. Run [>] to activate the marker\n" +
			"2. Select the color to begin to mark with\n" + 
			"3. Click on any cell to mark it\n" +
			"4. Click again to remove or change color\n" +
			"5. The XLS export in the layer menu will build\n" +
			"   a spreadsheet with the marked cells.\n" +
			"6. Remove the overlay (Layer > [x]) to stop";
	
	/**
	 * Initialize the marker overlay
	 * 
	 * @param stGraph graph to tag
	 * @param varCellColor color handle
	 * @param drawColorTag visualization handle
	 * @param sequence icy sequence to overlay to
	 */
	public CellColorTagOverlay(SpatioTemporalGraph stGraph, 
			EzVarEnum<CellColor> varCellColor,
			EzVarBoolean drawColorTag, Sequence sequence) {
		super("Cell Color Tag",stGraph);
		this.factory = new GeometryFactory();
		this.tag_color = varCellColor;
		this.writer = new ShapeWriter();
		this.sequence = sequence;
		this.drawColorTag = drawColorTag;
		
		this.tags_exist = false;
		for(Node node: stGraph.getFrame(0).vertexSet()){
			if(node.hasColorTag()){
				this.tags_exist = true;
				break;
			}
		}

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
			 		tags_exist = true;
			 		
			 		Color new_tag = tag_color.getValue().getColor();

			 		if(cell.hasColorTag()){
			 			Color current_tag = cell.getColorTag();
			 			if(current_tag == new_tag)
			 				propagateTag(cell,null);
			 			else
			 				propagateTag(cell,new_tag);
			 		} else 
			 			propagateTag(cell,new_tag);
			 		
			 		painterChanged();
			 	}
			
		}

	}
	
	/**
	 * Propagates the Color Tag from the current frame to
	 * all linked frames for the input cell.
	 * 
	 * @param n cell being clicked
	 * @param tag color tag to propagate
	 */
	public void propagateTag(Node n, Color tag){
		
		if(n.getFirst() != null)
			n = n.getFirst();
		
		n.setColorTag(tag);
		
		while(n.hasNext()){
			n = n.getNext();
			n.setColorTag(tag);
		}
		
		if(n.hasObservedDivision()){
			Division d = n.getDivision();
			if(d.isMother(n)){
				propagateTag(d.getChild1(), tag);
				propagateTag(d.getChild2(), tag);
			}
		}
	}
	
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		for(Node cell: frame_i.vertexSet())
			if(cell.hasColorTag()){
				g.setColor(cell.getColorTag());
				if(drawColorTag.getValue()){
					g.setStroke(new BasicStroke(3));
					g.draw(writer.toShape(cell.getGeometry()));
					g.setStroke(new BasicStroke(1));
				} else {
					g.fill(writer.toShape(cell.getGeometry()));
				}
			}

	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		String cmd_string =e.getActionCommand(); 
		
		if(cmd_string == "Choose File"){
			BigXlsExporter xlsExport = new BigXlsExporter(stGraph, true, sequence);
			xlsExport.writeXLSFile();
		}else{
			if(sequence.hasROI()){
				if(sequence.getROIs().size() == 1 ){
					ROI roi = sequence.getROIs().get(0);
					
					for(Node n: super.stGraph.getFrame(0).vertexSet()){
						Point p = n.getCentroid();
						
				 		Color new_tag = tag_color.getValue().getColor();
						
				 		double x = p.getX();
				 		double y = p.getY(); 
				 		double z,t,c;
				 		z = t = c = 0.0;
				 		
						if(roi.contains(x,y,z,t,c))
							propagateTag(n,new_tag);
						
					}
					
					painterChanged();
				}
			}
			else
				System.out.println("No ROI found");
		}
	}
	
	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		//old write out method for simpler file. Keep as reference
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
	 * @param c input color
	 * @return String of Color
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

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		if(!tags_exist){
			String s = "Click on a cell to color-tag it";
			Color c = Color.WHITE;
			int offset = 0;

			OverlayUtils.stringColorLegend(g, line, s, c, offset);
		}
		
	}
	
	@Override
	public JPanel getOptionsPanel() {
		
		JPanel optionPanel = new JPanel(new GridBagLayout());
		
		//Excel output button
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 10, 2, 5);
        gbc.fill = GridBagConstraints.BOTH;
        optionPanel.add(new JLabel("Tag cells with ROI: "), gbc);
        
        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        
        JButton Roi_Button = new JButton("Use current ROI");
        Roi_Button.addActionListener(this);
        optionPanel.add(Roi_Button,gbc);
		
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 10, 2, 5);
        gbc.fill = GridBagConstraints.BOTH;
        optionPanel.add(new JLabel("Export data to Excel: "), gbc);
        
        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        
        JButton OKButton = new JButton("Choose File");
        OKButton.addActionListener(this);
        optionPanel.add(OKButton,gbc);
        
        return optionPanel;
		
	}
	
}
