package plugins.davhelle.cellgraph.export;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Geometry;

import icy.gui.dialog.SaveDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.sequence.Sequence;
import icy.system.IcyExceptionHandler;
import icy.util.XLSUtil;
import ij.process.EllipseFitter;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.adufour.ezplug.EzGUI;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.misc.VoronoiGenerator;
import plugins.davhelle.cellgraph.nodes.Node;

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
	
	public static final String DESCRIPTION = 
			"Loaded graph is saved as one Excel Spreadsheet (.xls) with one " +
			"worksheet for every frame." +
			"Following fields are included:\n\n" +
			"* Cell tracking ID\n" + 
			"* Centroid position x\n" +
			"* Centroid position y\n" +
			"* Cell apical area\n" +
			"* Polygon/Neighbor number\n" +
			"* Voronoi cell area\n" +
			"* Best fit ellipse major axis length\n" +
			"* Best fit ellipse minor axis length\n" +
			"* Best fit ellipse major axis angle\n" +
			"* Divides during time lapse [T/F]\n" +
			"* Time of division\n" +
			"* Is eliminated during time lapse [T/F]\n" +
			"* Time of elimination\n" +
			"* Cell on segmentation border[T/F]";

	SpatioTemporalGraph stGraph;
	
	Map<Node, Geometry> voronoiTesselation;
	HashMap<Node, EllipseFitter> fittedEllipses;

	public BigXlsExporter(SpatioTemporalGraph stGraph, Sequence sequence, EzGUI gui){
		
		this.stGraph = stGraph;
		
		gui.setProgressBarMessage("Computing voronoi tesselation..");
		voronoiTesselation = new VoronoiGenerator(stGraph,sequence).getNodeVoroniMapping();
		
		gui.setProgressBarMessage("Computing ellipse fitting..");
		fittedEllipses = new EllipseFitGenerator(stGraph,sequence).getFittedEllipses();
		
	}
	
	private void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		int row_no = 0;
		int col_no = 0;
		
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

		for(Node node: frame.vertexSet()){
			//increase row and reset column
			row_no++;
			col_no = 0;
			
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
			
		}
	}
	
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
