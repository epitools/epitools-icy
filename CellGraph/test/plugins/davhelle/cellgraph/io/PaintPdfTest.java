package plugins.davhelle.cellgraph.io;

import gnu.jpdf.PDFJob;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import plugins.davhelle.cellgraph.graphs.FrameGenerator;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;

public class PaintPdfTest {
  @Test
  public void testSimpleRectagleOutput() {
	  
		String tmp = "/Users/davide/tmp/test_awt.pdf";
		FileOutputStream fileOutputStream;
		try {
			//open
			fileOutputStream = new FileOutputStream(new File(tmp));
			PDFJob job = new PDFJob(fileOutputStream);
			Graphics pdfGraphics = job.getGraphics();
			
			//draw
			pdfGraphics.drawRect(60, 60, 200, 200);
			
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
  
  @Test
  public void testSingleGraphOutput(){
	  
	  //Graph Generation
	  String test_file_name = "/Users/davide/tmp/wkt_export/output.txt";
	  File test_file = new File(test_file_name);

		Assert.assertTrue(test_file.exists(),"Input File does not exist");

		FrameGenerator frame_generator = new FrameGenerator(
				InputType.WKT);
		
		FrameGraph frame = frame_generator.generateFrame(0, test_file_name);
		
		
		
		ShapeWriter writer = new ShapeWriter();
		
		//PDF generation	
		String tmp = "/Users/davide/tmp/test_awt_graph.pdf";
		FileOutputStream fileOutputStream;
		try {
			//open
			fileOutputStream = new FileOutputStream(new File(tmp));
			PDFJob job = new PDFJob(fileOutputStream);
			Graphics2D pdfGraphics = (Graphics2D) job.getGraphics();
			
			
			//draw all polygons in Graph
			for(Node n: frame.vertexSet())
				pdfGraphics.draw(writer.toShape(n.getGeometry()));
				
			
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
