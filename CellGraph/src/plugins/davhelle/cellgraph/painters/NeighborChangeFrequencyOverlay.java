package plugins.davhelle.cellgraph.painters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;
import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;

public class NeighborChangeFrequencyOverlay extends Overlay {

	HashMap<Node,Float> change_frequencies;
	SpatioTemporalGraph stGraph;
	
	
	public NeighborChangeFrequencyOverlay(SpatioTemporalGraph stGraph) {
		super("Neighbor Change Frequency");
		
		this.change_frequencies = new HashMap<Node,Float>();
		this.stGraph = stGraph;
		
		for(Node cell: stGraph.getFrame(0).vertexSet()){
			
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
	}
	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			FrameGraph frame_i = stGraph.getFrame(time_point);
			for(Node cell: frame_i.vertexSet()){
				if(change_frequencies.containsKey(cell)){
					float fixed_hue_factor = 0.3f;
					float final_cell_color = change_frequencies.get(cell) * fixed_hue_factor;
					g.setColor(Color.getHSBColor( final_cell_color , 1f, 1f));
					g.fill(cell.toShape());
				}
			}
		}
    }

}
