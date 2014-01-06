package plugins.davhelle.cellgraph.graphexport;

import java.awt.Color;

import org.jgrapht.ext.VertexNameProvider;

import plugins.davhelle.cellgraph.nodes.Node;

/**
 * VertexLabelProvider extracts a desired information from 
 * a Node given a user defined input in the constructor.
 * Further field options can be added by extending 
 * the ExportFieldType class; 
 * 
 * @author Davide Heller
 *
 */
public class VertexLabelProvider implements VertexNameProvider<Node> {
	
	ExportFieldType export_field;
	
	
	public VertexLabelProvider(ExportFieldType field){
		this.export_field = field;
	}
	
	@Override
	public String getVertexName(Node vertex) {
		
		String vertex_label = "";
		
		switch(export_field){
		case AREA:
			double vertex_area = vertex.getGeometry().getArea();
			vertex_label = Long.toString(Math.round(vertex_area));
			break;
		case DIVISION:
			boolean has_observed_division = vertex.hasObservedDivision();
			vertex_label = Boolean.toString(has_observed_division);
			break;
		case TRACKING_ID:
			vertex_label = Integer.toString(vertex.getTrackID());
			break;
		case TRACKING_POSITION:
			vertex_label = 	Integer.toString(vertex.getTrackID()) +  
							"," +
							Math.round(vertex.getCentroid().getX()) + 
							"," +
							Math.round(vertex.getCentroid().getY());
		case COLOR_TAG:
			vertex_label = Boolean.toString(vertex.getColorTag() == Color.red);
			break;
		case SEQ_AREA:			
			double cell_area = vertex.getGeometry().getArea();
			String csv_area_over_time = Integer.toString(vertex.getTrackID()) + "," + Long.toString(Math.round(cell_area));
		
			Node cell = vertex;
			int t = vertex.getBelongingFrame().getFrameNo();
			while(cell.hasNext()){
				
				cell = cell.getNext();
				int t_new = cell.getBelongingFrame().getFrameNo();
				
				//in case of missing values add previous value
				for(;t < t_new - 1; t++)
					csv_area_over_time += "," + Long.toString(Math.round(cell_area));
				
				//add new value at the end of the list
				cell_area = cell.getGeometry().getArea();
				csv_area_over_time += "," + Long.toString(Math.round(cell_area));
				t++;
					
			}
			
			vertex_label = csv_area_over_time;
			break;
		case COMPLETE_CSV:
			StringBuilder builder = new StringBuilder();
			
			builder.append(vertex.getTrackID());
			addComma(builder);
			builder.append(Math.round(vertex.getCentroid().getX()));
			addComma(builder);
			builder.append(Math.round(vertex.getCentroid().getY()));
			addComma(builder);
			builder.append(vertex.getGeometry().getArea());
			addComma(builder);
			builder.append(vertex.onBoundary());
			addComma(builder);
			builder.append(vertex.hasObservedDivision());
			addComma(builder);
			builder.append(vertex.hasObservedElimination());
			addComma(builder);
			if(vertex.hasObservedDivision())
				builder.append(vertex.getDivision().getTimePoint());
			else
				builder.append(-1);
			addComma(builder);
			if(vertex.hasObservedElimination())
				builder.append(vertex.getElimination().getTimePoint());
			else
				builder.append(-1);
			
			
			vertex_label = builder.toString();
			break;
		case SEQ_X:
			String csv_x_over_time = getSequentialXCoordinates(vertex);
			vertex_label = csv_x_over_time;
			break;
		case SEQ_Y:
			String csv_y_over_time = getSequentialYCoordinates(vertex);
			vertex_label = csv_y_over_time;
			break;
			
			
		default:
			break;
		}
		
		return vertex_label;
	}
	
	private String getSequentialYCoordinates(Node vertex) {
		
		double cell_y = vertex.getCentroid().getY();
		String csv_y_over_time = Integer.toString(vertex.getTrackID()) + "," + Long.toString(Math.round(cell_y));
	
		Node cell = vertex;
		int t = vertex.getBelongingFrame().getFrameNo();
		while(cell.hasNext()){
			
			cell = cell.getNext();
			int t_new = cell.getBelongingFrame().getFrameNo();
			
			//in case of missing values add previous value
			for(;t < t_new - 1; t++)
				csv_y_over_time += "," + Long.toString(Math.round(cell_y));
			
			//add new value at the end of the list
			cell_y = cell.getCentroid().getY();
			csv_y_over_time += "," + Long.toString(Math.round(cell_y));
			t++;
				
		}
		
		return csv_y_over_time;
	}

	private String getSequentialXCoordinates(Node vertex) {
		
		double cell_x = vertex.getCentroid().getX();
		String csv_x_over_time = Integer.toString(vertex.getTrackID()) + "," + Long.toString(Math.round(cell_x));
	
		Node cell = vertex;
		int t = vertex.getBelongingFrame().getFrameNo();
		while(cell.hasNext()){
			
			cell = cell.getNext();
			int t_new = cell.getBelongingFrame().getFrameNo();
			
			//in case of missing values add previous value
			for(;t < t_new - 1; t++)
				csv_x_over_time += "," + Long.toString(Math.round(cell_x));
			
			//add new value at the end of the list
			cell_x = cell.getCentroid().getX();
			csv_x_over_time += "," + Long.toString(Math.round(cell_x));
			t++;
				
		}
		
		return csv_x_over_time;
	}

	private void addComma(StringBuilder builder){
		builder.append(',');
	}

}
