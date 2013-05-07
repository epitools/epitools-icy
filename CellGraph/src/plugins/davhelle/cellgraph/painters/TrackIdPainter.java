/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.ArrayList;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;

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
public class TrackIdPainter extends AbstractPainter{
	
	private SpatioTemporalGraph stGraph;
	
	public TrackIdPainter(SpatioTemporalGraph spatioTemporalGraph){
		this.stGraph = spatioTemporalGraph;
		
	}
	
	
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		
    	int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		if(time_point < stGraph.size()){
			//print index int the center of the cell
			
			int fontSize = 3;
			g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
			g.setColor(Color.CYAN);
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
	
			for(Node cell: frame_i.vertexSet()){
			
				Coordinate centroid = cell.getCentroid().getCoordinate();

				g.drawString(Integer.toString(cell.getTrackID()), 
						(float)centroid.x - 2  , 
						(float)centroid.y + 2);
				
			}		
			
		}
    }
	

}
