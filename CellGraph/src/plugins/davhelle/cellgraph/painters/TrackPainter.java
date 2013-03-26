package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Given the first frame as reference, a random color
 * is assigned to every cell and is maintained for all the
 * linked cells in the successive frames analyzed. 
 * 
 * @author Davide Heller
 *
 */
public class TrackPainter extends AbstractPainter{
	
	private SpatioTemporalGraph stGraph;
	private HashMap<Node,Color> correspondence_color;

	public TrackPainter(SpatioTemporalGraph stGraph) {
		
		//Color for each lineage
		this.correspondence_color = new HashMap<Node,Color>();
		this.stGraph = stGraph;
		
		//Assign color to cell lineages starting from first cell
		Iterator<Node> cell_it = stGraph.getFrame(0).iterator();
		Random rand = new Random();
		
		//Assign to every cell in the first
		//frame a random color and mark with the 
		//same color all successively linked cells.
		while(cell_it.hasNext()){
			
			Node cell = cell_it.next();
			
			// Generate random color for cell
			float r_idx = rand.nextFloat();
			float g_idx = rand.nextFloat();
			float b_idx = rand.nextFloat();      

			Color cell_color = new Color(r_idx, g_idx, b_idx);
			cell_color.brighter();
			
			while(cell != null){
				correspondence_color.put(cell, cell_color);
				cell = cell.getNext();
			}
		}
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();
		
		if(time_point < stGraph.size()){
			
			for(Node cell: stGraph.getFrame(time_point).vertexSet()){

				if(cell.getTrackID() != -1){
					if(correspondence_color.containsKey(cell)){
						//cell is part of registered lineage
						g.setColor(correspondence_color.get(cell));
						g.fill(cell.toShape());
					}
					else{
						//no tracking found
						g.setColor(Color.black);
						g.fill(cell.toShape());
					}
					//Fill cell shape
				}
			}
		}
	}

}
