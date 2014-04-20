/*=========================================================================
 *
 *  (C) Copyright (2012-2014) Basler Group, IMLS, UZH
 *  
 *  All rights reserved.
 *	
 *  author:	Davide Heller
 *  email:	davide.heller@imls.uzh.ch
 *  
 *=========================================================================*/
package plugins.davhelle.cellgraph.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import plugins.davhelle.cellgraph.graphexport.ExportFieldType;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Program for reading a saved tracking by CsvTrackWriter
 * 
 * @author Davide Heller
 *
 */
public class CsvTrackReader {

	private SpatioTemporalGraph wing_disc_movie;
	private String output_directory;
	
	public CsvTrackReader(SpatioTemporalGraph wing_disc_movie,String output_directory) {
		 this.wing_disc_movie = wing_disc_movie;
		 this.output_directory = output_directory;
	}
	
	public void readTrackingIds(){
		for(int i=0; i < wing_disc_movie.size(); i++){
			FrameGraph frame = wing_disc_movie.getFrame(i);
			String file_name = output_directory + String.format("tracking_t%03d.csv",i);
			File input_file = new File(file_name);
			read(frame,input_file,ExportFieldType.TRACKING_POSITION);
		}
	}
	
	public void readDivisions(){
		if(wing_disc_movie.size() > 0){
			FrameGraph frame = wing_disc_movie.getFrame(0);
			String file_name = output_directory + String.format("divisions.csv");
			File input_file = new File(file_name);
			read(frame,input_file,ExportFieldType.DIVISION);
		}
	}
	
	public void readEliminations(){
		if(wing_disc_movie.size() > 0){
			FrameGraph frame = wing_disc_movie.getFrame(0);
			String file_name = output_directory + String.format("eliminations.csv");
			File input_file = new File(file_name);
			read(frame,input_file,ExportFieldType.ELIMINATION);
		}
	}
	
	private void read(FrameGraph frame, File input_file,ExportFieldType export_field){

		try{
			FileInputStream fis = new FileInputStream(input_file);
			BufferedReader br = new BufferedReader(new InputStreamReader(fis,"UTF-8"));
			GeometryFactory gf = new GeometryFactory();
			String line;
			
			while ((line = br.readLine()) != null) {
				String[] content = line.split(",");
				int cell_id = Integer.parseInt(content[0]);
			    
			    switch(export_field){
			    	case TRACKING_POSITION:
			    		double cell_x = Double.parseDouble(content[1]);
			    		double cell_y = Double.parseDouble(content[2]);
			    		boolean cell_on_border = Boolean.parseBoolean(content[3]);

			    		Point point = gf.createPoint(
			    				new Coordinate( cell_x, cell_y ));
			    		for(Node cell: frame.vertexSet())
			    			if(cell.getGeometry().contains(point)){
			    				cell.setTrackID(cell_id);
			    				cell.setBoundary(cell_on_border);
			    				break;
			    			}
			    		
			    		break;
			    	case DIVISION:
			    		int division_frame = Integer.parseInt(content[1]);
			    		int child1_id = Integer.parseInt(content[2]);
			    		int child2_id = Integer.parseInt(content[3]);
			    		
			    		for(Node cell:frame.vertexSet())
			    			if(cell.getTrackID() == cell_id){
			    				//assign new Division here
			    				//assuming tissue has been tracked
			    				//what about the already assigned track_ids?
			    				break;
			    			}
						break;
					case ELIMINATION:
						int elimination_frame = Integer.parseInt(content[1]);
						for(Node cell:frame.vertexSet())
			    			if(cell.getTrackID() == cell_id){
			    				//assign new Elimination here
			    				//assuming tissue has been tracked
			    				break;
			    			}
						break;
					default:
						System.out.print("Export field is currenlty not available for read");
					}
			}
			br.close();

		}catch (IOException e){
			e.printStackTrace();
		}
		
	}


}
