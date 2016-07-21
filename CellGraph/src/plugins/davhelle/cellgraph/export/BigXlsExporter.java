package plugins.davhelle.cellgraph.export;

import icy.gui.dialog.SaveDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.sequence.Sequence;
import icy.system.IcyExceptionHandler;
import icy.util.XLSUtil;
import ij.process.EllipseFitter;

import java.io.IOException;
import java.util.Map;

import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.adufour.ezplug.EzGUI;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.misc.VoronoiGenerator;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.overlays.CellColorTagOverlay;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Generate one big XLS sheet for a spatio-temporal graph
 * data structure. Containing most relevant information
 * 
 * 1 sheet per frame
 * containing:
 * 
 * cellID
 * position x
 * position y
 * area
 * neighbor number
 * voronoi area
 * ellipse major
 * ellipse minor
 * ellipse major angle
 * hasObservedDivision
 * timeOfDivision
 * hasObservedEliminiation
 * timeOfElimination
 * onSegmentationBoundary
 * 
 * @author Davide Heller
 *
 */
public class BigXlsExporter {
	
	/**
	 * Description read by the GUI-Plugins
	 */
	public static final String DESCRIPTION = 
			"Loaded graph is saved as one Excel Spreadsheet (.xls) with one<br/>" +
			"worksheet for every frame<br/>" +
			"Following fields are included:<br/><ul>" +
			"<li>Color Tag (if selected)"+
			"<li>Cell tracking ID" +
			"<li>Centroid position x" +
			"<li>Centroid position y" +
			"<li>Cell apical area" +
			"<li>Polygon/Neighbor number" +
			"<li>Voronoi cell area" +
			"<li>Best fit ellipse major axis length" +
			"<li>Best fit ellipse minor axis length" +
			"<li>Best fit ellipse major axis angle" +
			"<li>Divides during time lapse [T/F]" +
			"<li>Time of division" +
			"<li>Is eliminated during time lapse [T/F]" +
			"<li>Time of elimination" +
			"<li>Cell on segmentation border[T/F]</ul>";

	/**
	 * Spatio temporal graph to export
	 */
	SpatioTemporalGraph stGraph;
	
	/**
	 * Voronoi Tesselation container 
	 */
	Map<Node, Geometry> voronoiTesselation;
	
	
	Map<Node, EllipseFitter> fittedEllipses;

	private boolean exprotTaggedOnly;

	/**
	 * Creates object with progress bar feedback
	 * 
	 * @param stGraph
	 * @param exportTaggedOnly
	 * @param sequence 
	 * @param gui EzGUI handle from which setProgressBarMessage will be accessed
	 */
	public BigXlsExporter(SpatioTemporalGraph stGraph, boolean exportTaggedOnly, Sequence sequence, EzGUI gui){
		
		this.stGraph = stGraph;
		
		gui.setProgressBarMessage("Computing voronoi tesselation..");
		voronoiTesselation = new VoronoiGenerator(stGraph,sequence).getNodeVoroniMapping();
		
		gui.setProgressBarMessage("Computing ellipse fitting..");
		fittedEllipses = new EllipseFitGenerator(stGraph,sequence).getFittedEllipses();
		
		gui.setProgressBarMessage("Ready to write XLS file..");
		
		this.exprotTaggedOnly = exportTaggedOnly;
		
	}
	
	/**
	 * Creates the object to be written into a excel file
	 * 
	 * @param stGraph graph to be written out
	 * @param exportTaggedOnly flag indicating whether only tagged cells should be exported
	 * @param sequence icy sequence on with which the stgraph is coupled
	 */
	public BigXlsExporter(SpatioTemporalGraph stGraph, boolean exportTaggedOnly, Sequence sequence){
		
		this.stGraph = stGraph;
		
		new AnnounceFrame("XLS-Export: Computing voronoi tesselation..",5);
		voronoiTesselation = new VoronoiGenerator(stGraph,sequence).getNodeVoroniMapping();
		
		new AnnounceFrame("XLS-Export: Computing ellipse fitting..",5);
		fittedEllipses = new EllipseFitGenerator(stGraph,sequence).getFittedEllipses();
		
		this.exprotTaggedOnly = exportTaggedOnly;
		
	}
	
	/**
	 * Individual Excel sheet writer 
	 * 
	 * @param sheet Sheet to be written to
	 * @param frame Frame of stGraph to be written
	 */
	private void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		int row_no = 0;
		int col_no = 0;

		if(exprotTaggedOnly)
			XLSUtil.setCellString(sheet, col_no++, row_no, "colorTag");

		XLSUtil.setCellString(sheet, col_no++, row_no, "id");
		XLSUtil.setCellString(sheet, col_no++, row_no, "x");
		XLSUtil.setCellString(sheet, col_no++, row_no, "y");
		XLSUtil.setCellString(sheet, col_no++, row_no, "polygonNo");
		XLSUtil.setCellString(sheet, col_no++, row_no, "area");
		XLSUtil.setCellString(sheet, col_no++, row_no, "voronoiArea");
		XLSUtil.setCellString(sheet, col_no++, row_no, "ellipseMajorAxisLength");
		XLSUtil.setCellString(sheet, col_no++, row_no, "ellipseMinorAxisLength");
		XLSUtil.setCellString(sheet, col_no++, row_no, "ellipseMajorAxisAngle");
		XLSUtil.setCellString(sheet, col_no++, row_no, "hasObservedDivsion"); 		//redundant
		XLSUtil.setCellString(sheet, col_no++, row_no, "divisionTime"); 			//redundant
		XLSUtil.setCellString(sheet, col_no++, row_no, "hasObservedElimination"); 	//redundant
		XLSUtil.setCellString(sheet, col_no++, row_no, "eliminationTime"); 			//redundant
		XLSUtil.setCellString(sheet, col_no++, row_no, "onSegmentationBoundary");
		
		row_no++;
		for(Node node: frame.vertexSet()){
			//reset column
			col_no = 0;

			if(exprotTaggedOnly)
				if(node.hasColorTag()){
					String colorTag = CellColorTagOverlay.getColorName(node.getColorTag());
					XLSUtil.setCellString(sheet, col_no++, row_no, colorTag);
				}
				else
					continue;
			
			//position
			XLSUtil.setCellNumber(sheet, col_no++, row_no, node.getTrackID());
			
			
			XLSUtil.setCellNumber(sheet, col_no++, row_no, node.getCentroid().getX());
			XLSUtil.setCellNumber(sheet, col_no++, row_no, node.getCentroid().getY());
			
			//neighbor no
			XLSUtil.setCellNumber(sheet, col_no++, row_no, frame.degreeOf(node));
			
			//area
			XLSUtil.setCellNumber(sheet, col_no++, row_no, node.getGeometry().getArea());
			double voronoiArea = voronoiTesselation.get(node).getArea();
			if(node.onBoundary())
				voronoiArea = -1;
			XLSUtil.setCellNumber(sheet, col_no++, row_no, voronoiArea);

			//ellipse fit
			EllipseFitter ef = fittedEllipses.get(node);
			XLSUtil.setCellNumber(sheet, col_no++, row_no, ef.major);
			XLSUtil.setCellNumber(sheet, col_no++, row_no, ef.minor);
			XLSUtil.setCellNumber(sheet, col_no++, row_no, ef.angle);
			
			//division & elimination
			if(node.hasObservedDivision()){
				XLSUtil.setCellString(sheet, col_no++, row_no,"TRUE");
				XLSUtil.setCellNumber(sheet, col_no++, row_no,node.getDivision().getTimePoint());
			} else {
				XLSUtil.setCellString(sheet, col_no++, row_no,"FALSE");
				XLSUtil.setCellNumber(sheet, col_no++, row_no,-1);
			}
			
			if(node.hasObservedElimination()){
				XLSUtil.setCellString(sheet, col_no++, row_no,"TRUE");
				XLSUtil.setCellNumber(sheet, col_no++, row_no,node.getElimination().getTimePoint());
			} else {
				XLSUtil.setCellString(sheet, col_no++, row_no,"FALSE");
				XLSUtil.setCellNumber(sheet, col_no++, row_no,-1);
			}
			
			//border
			String booleanString = String.valueOf(node.onBoundary()).toUpperCase();
			XLSUtil.setCellString(sheet, col_no++, row_no, booleanString);
			
			//increase row
			row_no++;
		}
	}
	
	/**
	 * Writes the object to a user defined file (gui-popup)
	 */
	public void writeXLSFile(){
		try {
			String file_name = SaveDialog.chooseFile(
					"Please choose where to save the excel Sheet",
					"/Users/davide/",
					"test_file", XLSUtil.FILE_DOT_EXTENSION);
			
			if(file_name == null)
				return;
				
			WritableWorkbook wb = XLSUtil.createWorkbook(file_name);
			
			for(int i=0; i<stGraph.size(); i++){
				String sheetName = String.format("Frame %d",i);
				WritableSheet sheet = XLSUtil.createNewPage(wb, sheetName);
				writeFrameSheet(sheet,stGraph.getFrame(i));
			}
			
			XLSUtil.saveAndClose(wb);
			
			new AnnounceFrame("XLS file exported successfully to: "+file_name,10);
			
		} catch (WriteException writeException) {
			IcyExceptionHandler.showErrorMessage(writeException, true, true);
		} catch (IOException ioException) {
			IcyExceptionHandler.showErrorMessage(ioException, true, true);
		}
	}
	

}
