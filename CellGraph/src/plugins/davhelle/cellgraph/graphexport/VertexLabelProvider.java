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
		case ALL:
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
			while(cell.hasNext()){
				cell = cell.getNext();
				cell_area = cell.getGeometry().getArea();
				csv_area_over_time += "," + Long.toString(Math.round(cell_area));
			}
			
			vertex_label = csv_area_over_time;
			break;
		default:
			break;
		}
		
		return vertex_label;
	}

}
