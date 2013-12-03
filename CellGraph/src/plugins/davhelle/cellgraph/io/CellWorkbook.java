/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.io;

import icy.gui.frame.progress.AnnounceFrame;

import java.awt.Color;
import java.io.FileOutputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Excel spreadsheet output for tagged cells
 * based on skeleton code send by Alex in mail (3/12/2012)
 * 
 * @author Davide Heller
 *
 */
public class CellWorkbook {
	
	SpatioTemporalGraph stGraph;
	Workbook wb;
	
	public CellWorkbook(SpatioTemporalGraph stGraph){
		this.stGraph = stGraph;
		
		//create Workbook (2007 compatible)
		this.wb = new HSSFWorkbook();
		
		createSheet("Cell Areas");
		saveWorkbook();
	}

	private void createSheet(String sheet_name){
		//Create Sheet
	    Sheet sheet = wb.createSheet(sheet_name);

	    // Create a row and put some cells in it. Rows are 0 based.
	    Row row_green = sheet.createRow((short)0);
	    Row row_blue = sheet.createRow((short)1);
		
	    int green_cell_index = 0;
	    int blue_cell_index = 0;
		for(Node node: stGraph.getFrame(0).vertexSet()){
			double cell_area = node.getGeometry().getArea();
			if(node.getColorTag() == Color.green)
				row_green.createCell(green_cell_index++).setCellValue(cell_area);
			if(node.getColorTag() == Color.blue)
				row_blue.createCell(blue_cell_index++).setCellValue(cell_area);		
		}
	   
	}

	private void saveWorkbook() {
	    // Write the output to a file
	    FileOutputStream fileOut;
	    String file_name = "/Users/davide/tmp/workbook.xls";
		try {
			fileOut = new FileOutputStream(file_name);
			wb.write(fileOut);
			fileOut.close();
		} catch(Exception e){
			new AnnounceFrame("No valid output file:" + file_name);
			return;
		}
		
		System.out.println("Successfully wrote tagged data to "+file_name);
	}
}
