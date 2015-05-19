package plugins.davhelle.cellgraph.overlays;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.HashMap;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;
import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;

/**
 * The generated overlay represents the amount of 
 * neighbor changes undergone by every cell over 
 * the available time points.
 * 
 * The current version simply sums the changes from one
 * frame to the next. E.g. given a cell that has the following
 * neighbor counts: 5,4,5,5,6 
 *  sum of changes: 0 1 1 0 1 = 3
 * ratio of change: 3/5 => in 60% percent of the 
 * 						   frames a neighbor change occurred 
 * 
 * The final ratio is used to color the cells
 * green  - low change ratio ( 0 - 15 % of the available frames show change in neighbor count)
 * yellow - intermediate change ratio ( 15 - 35%)
 * red	  -	increased change ratio (35 - 55 %)
 * purple -	very high change ratio (55%+)
 * 
 * Pitfalls: This version highlights only the instability of the cell count, not the stability of a change.
 *
 * @author Davide Heller
 *
 */
public class NeighborChangeFrequencyOverlay extends Overlay {

	HashMap<Node,Float> change_frequencies;
	SpatioTemporalGraph stGraph;
	
	
	public NeighborChangeFrequencyOverlay(SpatioTemporalGraph stGraph) {
		super("Neighbor Change Frequency");
		
		this.change_frequencies = new HashMap<Node,Float>();
		this.stGraph = stGraph;
		
		for(Node cell: stGraph.getFrame(0).vertexSet()){
			compute_neighbor_change_frequency(cell);
			
			//repeat analysis on progeny if present
			if(cell.hasObservedDivision()){
				Division division = cell.getDivision();
				compute_neighbor_change_frequency(division.getChild1());
				compute_neighbor_change_frequency(division.getChild2());
			}	
		}
	}

	private void compute_neighbor_change_frequency(Node cell) {
		int time_points = 0;
		int changes = 0;
		int previous_degree = cell.getNeighbors().size();
		
		Node next = cell;
		while(next.hasNext()){
			time_points++;
			next = next.getNext();
			int current_degree = next.getNeighbors().size();
			if(current_degree != previous_degree)
				changes++;
			previous_degree = current_degree;
		}

		if(time_points > 1){
			float change_frequency = changes / (float)(time_points);
			change_frequencies.put(cell,change_frequency);
		}
	}
	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		
		g.setFont(new Font("TimesRoman", Font.PLAIN, 5));
		
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			FrameGraph frame_i = stGraph.getFrame(time_point);
			for(Node cell: frame_i.vertexSet()){
				
				Node ancestor = cell.getFirst();
				if(change_frequencies.containsKey(ancestor)){
					float fixed_hue_factor = -0.7f;
					float fixed_hue_shift = 0.3f;
					float final_cell_color = change_frequencies.get(ancestor) * fixed_hue_factor + fixed_hue_shift;
					g.setColor(Color.getHSBColor( final_cell_color , 0.7f, 1f));
					g.fill(cell.toShape());
					g.setColor(Color.white);
					String label = String.format("%.2f",change_frequencies.get(ancestor) ); //final_cell_color
					g.drawString(label, 
							(float)cell.getCentroid().getX() - 2  , 
							(float)cell.getCentroid().getY() + 7);
				}
			}
		}
    }

}
