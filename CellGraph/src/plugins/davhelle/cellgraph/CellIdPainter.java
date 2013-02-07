package plugins.davhelle.cellgraph;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

/**
 * CellIdPainter depicts the cell ID or String supplied at
 * the coordinates of cell centers supplied. 
 * 
 * @author Davide Heller
 *
 */
public class CellIdPainter extends AbstractPainter{
	
	private int time_point;
	private ArrayList<Point> my_cell_centers;
	private ArrayList<String> my_cell_text;
	
	public CellIdPainter(ArrayList<Point> my_cell_centers,int time_point){
		this.time_point = time_point;
		this.my_cell_centers = my_cell_centers;
		this.my_cell_text = new ArrayList<String>();
		
		for(int i=0;i<my_cell_centers.size();i++)
			my_cell_text.add(Integer.toString(i));
		
		
	}
	
	public CellIdPainter(
			ArrayList<Point> my_cell_centers,
			ArrayList<Double> my_cell_val,
			int time_point){
		
		this.time_point = time_point;
		this.my_cell_centers = my_cell_centers;
		this.my_cell_text = new ArrayList<String>();
		
		for(Double cell_val: my_cell_val){
			int rounded = cell_val.intValue();
			my_cell_text.add(Integer.toString(rounded));
		}
	
	}
	
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		//only display when on selected frame
		if(Icy.getMainInterface().getFirstViewer(sequence).getT() == time_point){
			
			//print index int the center of the cell
			
			int fontSize = 1;
			g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
			g.setColor(Color.CYAN);
			
			for(int i=0; i<my_cell_centers.size(); i++){
				int x_id = my_cell_centers.get(i).x;
				int y_id = my_cell_centers.get(i).y;
				String id = my_cell_text.get(i);
				
				g.drawString(id,x_id,y_id);
				
			}		
			
		}
    }
	

}
