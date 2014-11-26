package plugins.davhelle.cellgraph.io;

import gnu.jpdf.PDFJob;
import icy.gui.dialog.SaveDialog;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.vividsolutions.jts.geom.Coordinate;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.painters.EllipseFitColorOverlay;
import plugins.davhelle.cellgraph.painters.EllipseFitterOverlay;
import plugins.davhelle.cellgraph.painters.PolygonPainter;

public class PdfPrinter {
	public PdfPrinter(SpatioTemporalGraph stGraph){
		String file_name = SaveDialog.chooseFile(
				"Please choose where to save the PDF transitions image", 
				"/Users/davide/analysis/",
				"test_pdf",
				"");
		
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
			paper.setSize(1392,1040);
			format.setPaper(paper);
			Graphics2D pdfGraphics = (Graphics2D) job.getGraphics(format);
			
			//paint
			new PolygonPainter(stGraph, Color.BLACK).paintFrame(pdfGraphics, 0);
			Coordinate roi = new Coordinate(740, 694);
			new EllipseFitColorOverlay(stGraph).paintFrame(pdfGraphics, 0, roi.x, roi.y, false);
			pdfGraphics.setColor(Color.BLACK);
			pdfGraphics.fillOval((int)roi.x, (int)roi.y, 10, 10);
			new EllipseFitterOverlay(stGraph).paintFrame(pdfGraphics, 0, Color.BLACK);
			
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
