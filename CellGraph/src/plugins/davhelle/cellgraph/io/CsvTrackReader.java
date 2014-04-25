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
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Elimination;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.tracking.TrackingAlgorithm;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Program for reading a saved tracking by CsvTrackWriter
 * 
 * @author Davide Heller
 *
 */
public class CsvTrackReader extends TrackingAlgorithm{

	private String output_directory;
	private int linkage_threshold;
	
	public static final String tracking_file_pattern = "tracking_t%03d.csv";
	public static final String division_file_pattern = "divisions.csv";
	public static final String elimination_file_pattern = "eliminations.csv";
	
	public CsvTrackReader(SpatioTemporalGraph stGraph,String output_directory) {
		 super(stGraph,false);
		 //Make sure directory ends with slash
		 //TODO: Substitute this whith new File(parent, child)
		 if(output_directory.charAt(output_directory.length() - 1) != '/')
			 output_directory += "/";
		 this.output_directory = output_directory;
		 //default linkage range (how many frame to go back to find a cell with the same id)
		 //might be adapted. TODO
		 this.linkage_threshold = 5;
	}
	
	@Override
	public void track(){
		//helper method to enable CSV reader as alternative Tracking Manager
		readTrackingIds();
		readDivisions();
		readEliminations();
		System.out.println("Successfully read tracking form: "+output_directory);
	}
	
	public void readTrackingIds(){
		for(int i=0; i < stGraph.size(); i++){
			FrameGraph frame = stGraph.getFrame(i);
			String file_name = output_directory + String.format(tracking_file_pattern,i);
			File input_file = new File(file_name);
			read(frame,input_file,ExportFieldType.TRACKING_POSITION);
		}
	}
	
	public void readDivisions(){
		if(stGraph.size() > 0){
			FrameGraph frame = stGraph.getFrame(0);
			String file_name = output_directory + String.format(division_file_pattern);
			File input_file = new File(file_name);
			read(frame,input_file,ExportFieldType.DIVISION);
		}
	}
	
	public void readEliminations(){
		if(stGraph.size() > 0){
			FrameGraph frame = stGraph.getFrame(0);
			String file_name = output_directory + String.format(elimination_file_pattern);
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

			    		Node matching_cell = null;
			    		
			    		Point point = gf.createPoint(
			    				new Coordinate( cell_x, cell_y ));
			    		for(Node cell: frame.vertexSet())
			    			if(cell.getGeometry().contains(point)){
			    				matching_cell = cell;
			    				break;
			    			}
			    		
			    		if(matching_cell == null){
			    			System.out.printf("Could not find matching cell for:%d [%.2f,%.2f]\n",
			    					cell_id,
			    					cell_x,
			    					cell_y);
			    			continue;
			    		}
			    		
			    		matching_cell.setTrackID(cell_id);
			    		matching_cell.setBoundary(cell_on_border);
			    		
			    		if(frame.getFrameNo() == 0)
			    			matching_cell.setFirst(matching_cell);
			    		else{
			    			Node previous_cell = null;
			    			boolean found_ancestor = false;
			    			int previous_frame_no = frame.getFrameNo() - 1;
			    			
			    			while(!found_ancestor && 
			    					previous_frame_no >= 0 &&
			    					frame.getFrameNo() - previous_frame_no < linkage_threshold){

			    				FrameGraph previous_frame = stGraph.getFrame(previous_frame_no);

			    				for(Node cell: previous_frame.vertexSet())
			    					if(cell.getTrackID() == cell_id){
			    						previous_cell = cell;
			    						found_ancestor = true;
			    						break;
			    					}
			    				previous_frame_no = previous_frame_no - 1;
			    			}
			    			
			    			if(found_ancestor)
			    				updateCorrespondence(matching_cell, previous_cell);
			    			else
			    				//problem of dividing cells
			    				//how to make the daughter cells fall in
			    				//in the first if clause
			    				//maybe a special clause if you are the *first*
			    				matching_cell.setFirst(matching_cell);
			    		}			    					    		
			    		
			    		
			    		break;
			    	case DIVISION:
			    		int division_time_point = Integer.parseInt(content[1]);
			    		
			    		//safety check, skip division if corresponding frame is missing
			    		if(division_time_point > stGraph.size())
			    			continue;
			    		
			    		int child1_id = Integer.parseInt(content[2]);
			    		int child2_id = Integer.parseInt(content[3]);
			    		
			    		Node mother = null;
			    		Node child1 = null;
			    		Node child2 = null;
			    		
			    		for(Node cell:frame.vertexSet())
			    			if(cell.getTrackID() == cell_id){
			    				mother = cell;
			    				break;
			    			}
			    		
			    		FrameGraph division_frame = stGraph.getFrame(division_time_point);
			    		
			    		for(Node cell:division_frame.vertexSet()){
			    			if(cell.getTrackID() == child1_id)
			    				child1 = cell;
			    			else if(cell.getTrackID() == child2_id)
			    				child2 = cell;
			    			else if(child1 != null && child2 != null)
			    				break;
			    		}
			    		
			    		Node future_mother = getMostRecentCorrespondence(division_time_point, mother);
			    		new Division(future_mother,child1,child2);
						break;
						
					case ELIMINATION:
						int elimination_frame_no = Integer.parseInt(content[1]);
						
						//safety check, skip elimination if corresponding frame is missing
			    		if(elimination_frame_no > stGraph.size())
			    			continue;
						
						FrameGraph elimination_frame = stGraph.getFrame(elimination_frame_no);
						Node eliminated_cell = null;
						
						for(Node cell:elimination_frame.vertexSet())
			    			if(cell.getTrackID() == cell_id){
			    				eliminated_cell = cell;
			    				break;
			    			}
						
						new Elimination(eliminated_cell);
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
