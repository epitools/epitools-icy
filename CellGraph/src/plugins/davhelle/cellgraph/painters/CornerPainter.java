package plugins.davhelle.cellgraph.painters;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;

import plugins.davhelle.cellgraph.nodes.CellCorner;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

/**
 * CornerPainter depicts the vertices of the original
 * VTK mesh supplied by the user
 * 
 * @author Davide Heller
 *
 */
public class CornerPainter extends AbstractPainter{
	
	ArrayList<CellCorner> cornerList;
	private int time_point;
	
	public CornerPainter(ArrayList<CellCorner> cornerList, int time_point){
		this.cornerList = cornerList;
		this.time_point = time_point;
	}
	
    @Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
    	
		//only display when on selected frame
		if(Icy.getMainInterface().getFirstViewer(sequence).getT() == time_point){

			Iterator<CellCorner> corner_it = cornerList.iterator();

			g.setStroke(new BasicStroke((float)0.3));
			g.setColor(Color.RED);

			while(corner_it.hasNext()){
				CellCorner corner_i = corner_it.next();

				//    		System.out.println(corner_i.getX() + ":" + corner_i.getY());

				int x = corner_i.getX();
				int y = corner_i.getY();

//				int corner_size = 1;
//				g.drawOval(x, y, corner_size, corner_size);

				//draw x on corner
				g.drawLine(x-1, y-1, x+1, y+1);
				g.drawLine(x-1, y+1, x+1, y-1);

			}
		}
    	
    }
}
