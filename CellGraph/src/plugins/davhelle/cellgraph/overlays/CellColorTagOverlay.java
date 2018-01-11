package plugins.davhelle.cellgraph.overlays;

import icy.canvas.IcyCanvas;
import icy.gui.dialog.ConfirmDialog;
import icy.gui.dialog.OpenDialog;
import icy.gui.dialog.SaveDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.roi.ROI;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;
import icy.system.IcyExceptionHandler;
import icy.util.XLSUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.format.Colour;
import jxl.read.biff.BiffException;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.davhelle.cellgraph.export.BigXlsExporter;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.IntensityReader;
import plugins.davhelle.cellgraph.io.IntensitySummaryType;
import plugins.davhelle.cellgraph.overlays.EdgeOrientationOverlay;
import plugins.davhelle.cellgraph.misc.CellColor;
import plugins.davhelle.cellgraph.misc.ShapeRoi;
import plugins.davhelle.cellgraph.misc.JxlUtils;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.kernel.roi.roi2d.ROI2DArea;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
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
public class CellColorTagOverlay extends StGraphOverlay implements EzVarListener<Integer>{
	
	private static final String EXPORT_FILE = "Export File";
	private static final String CONVERT_ROI = "Convert ROI";
	private static final String ERASE_TAGS = "Erase tags";
	private static final String IMPORT_FILE = "Import File";
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
	 * Mean Edge Intensities for every cell 
	 */
	private EzVarInteger bufferWidth;
	private EzVarInteger channelNumber;
	private EzVarBoolean showIntensity;
	private EzVarEnum<IntensitySummaryType> summary_type;
	private HashMap<Node,Shape> cell_rings;
	private ROI2DArea[] nanAreaRoi;
	
	/**
	 * Description string for GUI use
	 */
	public static final String DESCRIPTION = 
			"Overlay to interactively mark cells with a color of choice<br/>"+
			"and export the selection.<br /><ol>" +
			"<li>Run [>] to activate the marker" +
			"<li>Select the color to begin to mark with" + 
			"<li>Click on any cell to mark it" +
			"<li>Click again to remove or change color" +
			"<li>Export XLS through the layer menu" +
			"<li>Remove the overlay (Layer > [x]) to stop</ol>";
	
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
			EzVarBoolean drawColorTag, Sequence sequence,
			EzVarBoolean varShowIntensity,
			EzVarInteger varBufferWidth,
			EzVarEnum<IntensitySummaryType> varIntensitySummaryType,
			EzVarInteger varIntensityChannel) {
		super("Cell Color Tag",stGraph);
		this.factory = new GeometryFactory();
		this.tag_color = varCellColor;
		this.writer = new ShapeWriter();
		this.sequence = sequence;
		this.drawColorTag = drawColorTag;
		
		this.cell_rings = new HashMap<Node, Shape>();
		this.showIntensity = varShowIntensity; 
		this.bufferWidth = varBufferWidth;
		this.bufferWidth.addVarChangeListener(this);
		this.channelNumber = varIntensityChannel;
		this.summary_type = varIntensitySummaryType;
		
		this.tags_exist = false;
		for(Node node: stGraph.getFrame(0).vertexSet()){
			if(node.hasColorTag()){
				this.tags_exist = true;
				break;
			}
		}
		
		this.nanAreaRoi = new ROI2DArea[stGraph.size()];
		
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
		updateMeasurementGeometry(n);
		
		while(n.hasNext()){
			n = n.getNext();
			n.setColorTag(tag);
			updateMeasurementGeometry(n);
		}
		
		if(n.hasObservedDivision()){
			Division d = n.getDivision();
			if(d.isMother(n)){
				propagateTag(d.getChild1(), tag);
				propagateTag(d.getChild2(), tag);
			}
		}
	}
	
	private void updateMeasurementGeometry(Node n) {
		int buffer_width = this.bufferWidth.getValue();
		Geometry buffer_geo = n.getGeometry().buffer(buffer_width);
		Geometry reduced_geo = n.getGeometry().buffer(-buffer_width);
		Geometry final_geo = buffer_geo.difference(reduced_geo);
		Shape buffer_shape = writer.toShape(final_geo);
		this.cell_rings.put(n, buffer_shape);
	}
	
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		for(Node cell: frame_i.vertexSet())
			if(cell.hasColorTag()){
				g.setColor(cell.getColorTag());
				
				if(cell_rings.containsKey(cell) && showIntensity.getValue())
					g.draw(cell_rings.get(cell));
				else{
					if(drawColorTag.getValue()){
						g.setStroke(new BasicStroke(3));
						g.draw(writer.toShape(cell.getGeometry()));
						g.setStroke(new BasicStroke(1));
					} else {
						g.fill(writer.toShape(cell.getGeometry()));
					}
				}
			}

	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		String cmd_string =e.getActionCommand(); 
		
		if(cmd_string.equals(EXPORT_FILE)){
			if(showIntensity.getValue()){
				try {
					
					for(int i=0; i < stGraph.size(); i++)
						this.nanAreaRoi[i] = EdgeOrientationOverlay.computeNanAreaROI(
								sequence,i,0,channelNumber.getValue());
					
					String file_name = SaveDialog.chooseFile(
							"Choose save location","/Users/davide/",
							"test_file", XLSUtil.FILE_DOT_EXTENSION);
					if(file_name == null)
						return;
					
					//Open workbook
					WritableWorkbook wb = XLSUtil.createWorkbook(file_name);
					String sheetName = String.format("Area and Intensity",0);
					WritableSheet sheet = XLSUtil.createNewPage(wb, sheetName);
					
					//TODO Write single sheet workbook with area and intensity 
					writeFrameSheet(sheet,stGraph.getFrame(0));
					
					//Close workbook
					XLSUtil.saveAndClose(wb);
					new AnnounceFrame("XLS file exported successfully to: "+file_name,10);
				} catch (WriteException writeException) {
					IcyExceptionHandler.showErrorMessage(writeException, true, true);
				} catch (IOException ioException) {
					IcyExceptionHandler.showErrorMessage(ioException, true, true);
				}
			}else{
				BigXlsExporter xlsExport = new BigXlsExporter(stGraph, true, sequence);
				xlsExport.writeXLSFile();
			}
		}else if (cmd_string.equals(CONVERT_ROI)){
			convertROI();
		}else if (cmd_string.equals(ERASE_TAGS)){
			boolean answer = ConfirmDialog.confirm(
					"Please confirm", "Do you really want to remove all tags?");
			if(answer)
				eraseTags();
		}else if (cmd_string.equals(IMPORT_FILE)){
			importTags();
		}
	}
	
	private void importTags(){
		String file_name = OpenDialog.chooseFile();
		
		if(file_name == null)
			return;
		
		File import_file = new File(file_name);
		
		if(!import_file.exists())
			System.out.printf("File unknown: %s\n",file_name);
		try {
			System.out.printf("Importing file: %s\n",file_name);
			
			GeometryFactory gf = new GeometryFactory();
			
			long f1 = System.currentTimeMillis();
			Workbook wb = XLSUtil.loadWorkbookForRead(import_file);
			f1 = System.currentTimeMillis() - f1;
			System.out.printf("Importing workbook file:\t%d ms\n",f1);
			
			//Check that xls matches! i.e. stGraph.size == sheets_no
			if(wb.getNumberOfSheets() != stGraph.size()){
				new AnnounceFrame("Imported file has a different number of frames!");
				return;
			}
			
			long f4 = System.currentTimeMillis();
			Sheet[] sheets = wb.getSheets();
			f4 = System.currentTimeMillis() - f4;
			System.out.printf("Importing all frame sheets:\t%d ms\n",f4);
			
			for(int i=0; i<wb.getNumberOfSheets(); i++){
				Sheet s = sheets[i];
				
				FrameGraph frame = stGraph.getFrame(i);
				
				final int COLOR_COL = 0;
				final int X_COL = 2;
				final int Y_COL = 3;
				
				long search_sum = 0;
				
				long f2 = System.currentTimeMillis();
				Cell[] colors = s.getColumn(COLOR_COL);
				Cell[] x_cells = s.getColumn(X_COL);
				Cell[] y_cells = s.getColumn(Y_COL);
				f2 = System.currentTimeMillis() - f2;
				System.out.printf("Reading sheet %d:\t%d ms\n",i,f2);
				
				int row_no=1;
				for(; row_no < colors.length; row_no++){

					long f3 = System.currentTimeMillis();
					
					Cell color_cell  = colors[row_no];
					Cell x_cell = x_cells[row_no];
					Cell y_cell = y_cells[row_no];

					CellColor cell_color = getCellColor(color_cell.getContents());

					if(cell_color == null)
						break;

					double x = java.lang.Double.parseDouble(x_cell.getContents());
					double y = java.lang.Double.parseDouble(y_cell.getContents());

					Point point = gf.createPoint(new Coordinate( x, y ));
					for(Node cell: frame.vertexSet()){
						//if(cell.getGeometry().contains(point)){
						if(point.within(cell.getGeometry())){
							cell.setColorTag(cell_color.getColor());
							tags_exist = true;
							break;
						}
					}
					
					f3 = System.currentTimeMillis() - f3;
					search_sum += f3;
				}
				
				//double avg_search_time = (double)search_sum/row_no;
				System.out.printf("Tagging frame %d:\t%d ms\n\n",i,search_sum);
				
			}
			
			painterChanged();
			
		} catch (BiffException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * Erase all current tags
	 */
	private void eraseTags(){
		
		for(int i=0; i<stGraph.size(); i++)
			for(Node node: stGraph.getFrame(i).vertexSet())
				node.setColorTag(null);
		
		painterChanged();
		tags_exist = false;
	}

	/**
	 * Convert ROI to cell tags by checking if cell centroid is within ROI
	 */
	private void convertROI() {
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
			 		
					if(roi.contains(x,y,z,t,c)){
						propagateTag(n,new_tag);
						tags_exist = true;
					}
					
				}
				
				painterChanged();
			}
		}
		else
			System.out.println("No ROI found");
	}
	
	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		int col_no = 0;
		
		for(Node node: frame.vertexSet()){
			
			if(node.hasColorTag()){
				
				int row_no = 0;
				int col_area = col_no * 2;
				int col_intensity = col_area + 1;
				col_no++;
				
				String colorTag = CellColorTagOverlay.getColorName(node.getColorTag());
				
				String header = String.format("[%s - %d]",colorTag,node.getTrackID());
				XLSUtil.setCellString(sheet, col_area, row_no, header);
				XLSUtil.setCellString(sheet, col_intensity, row_no++, header);
				
				XLSUtil.setCellString(sheet, col_area, row_no, "[area - px]");
				XLSUtil.setCellString(sheet, col_intensity, row_no++, "[intensity]");
				
				//First cell
				row_no = node.getFrameNo() + 2;
				double area = node.getGeometry().getArea();
				double intensity = getCellIntensity(node);
				XLSUtil.setCellNumber(sheet, col_area, row_no, area);
				XLSUtil.setCellNumber(sheet, col_intensity, row_no, intensity);
				
				//Color first cell according to color TAG
				WritableCell c = sheet.getWritableCell(col_area,0);
				WritableCellFormat newFormat = new WritableCellFormat();
				Color c_awt = node.getColorTag();
				Colour c_jxl = JxlUtils.getNearestColour(c_awt);
				try {
					newFormat.setBackground(c_jxl);
				} catch (WriteException e) {
					e.printStackTrace();
				}
				c.setCellFormat(newFormat);
				
				//Later cells if tracked
				while(node.hasNext()){
					node = node.getNext();
				
					row_no = node.getFrameNo() + 2;
					
					area = node.getGeometry().getArea();
					intensity = getCellIntensity(node);
					XLSUtil.setCellNumber(sheet, col_area, row_no, area);
					XLSUtil.setCellNumber(sheet, col_intensity, row_no++, intensity);
				}
				
			}
		}
		
	}	
	
	private double getCellIntensity(Node node) {
		
		assert(cell_rings.containsKey(node));
		
		Shape cell_shape = cell_rings.get(node);

		int z=0;
		int t=node.getFrameNo();
		int c=channelNumber.getValue();
		
		ROI cell_roi_wo_nan = null;
		try{
			ShapeRoi cell_roi = new ShapeRoi(cell_shape);
			cell_roi_wo_nan = ROIUtil.subtract(cell_roi, nanAreaRoi[t]);
		}catch(Exception ex){
			Point centroid = node.getGeometry().getCentroid();
			System.out.printf("Problems at %.2f %.2f",centroid.getX(),centroid.getY());
			return -1.0;
		}
		
		//TODO possibly use getIntensityInfo here
		
		double mean_intensity = 
				IntensityReader.measureRoiIntensity(
						sequence, cell_roi_wo_nan, z, t, c, summary_type.getValue());
		
		return mean_intensity;
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
	
	/**
	 * Given a string name find the matching cellColor or return false
	 * 
	 * @param color_name string description of the color
	 * @return cell_color matching the string description
	 */
	public static CellColor getCellColor(String color_name) {
		
		if(color_name == "")
			return null;
		
		for(CellColor cell_color: CellColor.values()){
			
			Color c = cell_color.getColor();
			
			String cell_color_name = getColorName(c);
		
			if(cell_color_name.equals(color_name))
				return cell_color;
	    }
		
	    return null;
	}

	@Override
	public void specifyLegend(Graphics2D g, Line2D line) {
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
		
		addOptionButton(optionPanel, 
				IMPORT_FILE, "Import data from Excel: ");
		
		addOptionButton(optionPanel,
				ERASE_TAGS, "Remove current cell tags:");
		
		addOptionButton(optionPanel, 
				CONVERT_ROI, "Tag cells with ROI: ");
		
		addOptionButton(optionPanel, 
				EXPORT_FILE, "Export data to Excel: ");
        
        return optionPanel;
		
	}

	/**
	 * @param optionPanel
	 * @param button_text
	 * @param button_description
	 */
	private void addOptionButton(JPanel optionPanel, 
			String button_text,
			String button_description) {
		
		GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 10, 2, 5);
        gbc.fill = GridBagConstraints.BOTH;
		optionPanel.add(new JLabel(button_description), gbc);
        
        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        
		JButton Roi_Button = new JButton(button_text);
        Roi_Button.addActionListener(this);
        optionPanel.add(Roi_Button,gbc);
	}
	
	@Override
	public void variableChanged(EzVar<Integer> source, Integer newValue) {
		
		for(Node n: cell_rings.keySet())
			updateMeasurementGeometry(n);
		
		painterChanged();
		
	}
	
}
