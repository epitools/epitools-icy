package plugins.davhelle.cellgraph.io;

import gnu.jpdf.PDFJob;
import icy.gui.dialog.SaveDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.overlays.StGraphOverlay;

/**
 * PDF printer class that converts the AWT Overlays for StGraphs
 * into vector graphics using the gnu.jpdf package.
 * 
 * @author Davide Heller
 *
 */
public class PdfPrinter {
	
	/**
	 * Descriptor String for GUI
	 */
	public static final String DESCRIPTION = 
			"Exports a Vector graphic (PDF) file of the loaded graph structure.\n\n" +
			" Currently the only the selected frame is exported with all<br/>" +
			" overlays which are currently present on it. The underlying<br/>" +
			" bit-map image is not included in the pdf but sets the<br/>" +
			" dimensions of the file.";

	/**
	 * Specify the stGraph to be converted to PDF (sequence image excluded)
	 * 
	 * @param stGraph input graph to be converted
	 * @param sequence sequence on which the overlays are displayed
	 */
	public PdfPrinter(SpatioTemporalGraph stGraph, Sequence sequence){
		
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();
		if(time_point < 0 || time_point >= stGraph.size()){
			new AnnounceFrame("Time point is not available, please position selected sequence on valid time point",10);
			return;
		}
		
		String file_name = SaveDialog.chooseFile(
				"Please choose where to save the PDF transitions image", 
				"/Users/davide/",
				"test_pdf",
				"");
		
		if(file_name == null)
			return;
		
		//PDF generation	
		try {
			
			//open
			FileOutputStream fileOutputStream;
			String suffix = "";
			if(!file_name.endsWith(".pdf"))
				suffix = ".pdf";
			fileOutputStream = new FileOutputStream(new File(file_name + suffix));
			PDFJob job = new PDFJob(fileOutputStream);
			
			//apply custom format
			PageFormat format = new PageFormat();
			Paper paper=format.getPaper();
			paper.setSize(sequence.getWidth(),sequence.getHeight());
			format.setPaper(paper);
			Graphics2D pdfGraphics = (Graphics2D) job.getGraphics(format);
			
			FrameGraph frame0 = stGraph.getFrame(time_point);
			
			List<Overlay> overlays = sequence.getOverlays();
			for (Overlay overlay : overlays) {
				if(overlay instanceof StGraphOverlay){
					StGraphOverlay stgOverlay = (StGraphOverlay)overlay;
					stgOverlay.paintFrame(pdfGraphics, frame0);
				}
			}
			
			//test paint for specific overlays
			//new PolygonClassOverlay(stGraph,false,0).paintFrame(pdfGraphics,frame0);
			//new DivisionOrientationOverlay(stGraph).paintFrame(pdfGraphics, 0);
			//new PolygonOverlay(stGraph, Color.BLACK).paintFrame(pdfGraphics,frame0);
			
			//close
			pdfGraphics.dispose();
			job.end();
			fileOutputStream.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
