/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.io;

import icy.util.XLSUtil;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
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
	WritableWorkbook wb;
	private String file_name;
	
	public CellWorkbook(SpatioTemporalGraph stGraph){
		this.stGraph = stGraph;
		
		file_name = "/Users/davide/tmp/workbook";
		file_name += XLSUtil.FILE_DOT_EXTENSION;
		
		//create Workbook (2007 compatible)
		openWorkbook();
		createSheet("Cell Areas");
		saveWorkbook();
	}

	private void createSheet(String sheet_name){
		//Create Sheet
		WritableSheet sheet = XLSUtil.createNewPage(wb, sheet_name);
		
		int color_no = 0;
		HashMap<Color, Integer> row_no = new HashMap<Color, Integer>();
		HashMap<Color, Integer> col_no = new HashMap<Color, Integer>();
		
		for(Node node: stGraph.getFrame(0).vertexSet()){
			
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
	
	private void openWorkbook(){
		try {
			this.wb = XLSUtil.createWorkbook( new File( file_name ) );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void saveWorkbook() {
	    // Write the output to a file
	    
		try {
			XLSUtil.saveAndClose(wb);
		} catch (WriteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Successfully wrote tagged data to "+file_name);
	}
}
