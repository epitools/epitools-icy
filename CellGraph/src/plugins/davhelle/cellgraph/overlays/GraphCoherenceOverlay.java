/**
 * 
 */
package plugins.davhelle.cellgraph.overlays;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Iterator;

import com.vividsolutions.jts.geom.Coordinate;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;
import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;

/**
 * Class evidences the level of coherence between Graphs at successive time points.
 * Cells are highlighted according to how many neighbors they preserved with respect
 * to the previous frame.
 * 
 * @author Davide Heller
 *
 */
public class GraphCoherenceOverlay extends Overlay {

	private SpatioTemporalGraph stGraph;
	private HashMap<Node, Double> coherenceMap ;
	
	public GraphCoherenceOverlay(SpatioTemporalGraph stGraph) {
		
		super("Graph coherence map");
		this.stGraph = stGraph;
		this.coherenceMap = new HashMap<Node, Double>();
		
		if(stGraph.hasTracking()){
			
			for(int t=1;t<stGraph.size();t++){
				Iterator<Node> cell_it = stGraph.getFrame(t).iterator();

				while(cell_it.hasNext()){
					
					Node cell = cell_it.next();

					if(cell.hasPrevious()){
						
						int coherence_counter = 0;
						int tracked_neighbor_no = 0;
						
						Node ancestor = cell.getPrevious();
						//TODO issue with cells skipping a frame but not so the neighbors.. 
						//TODO issue with dividing cells, correction for "non" tracked cells?
						// e.g. do not count the neighbors which do not have a tracking?
						
						for(Node n: cell.getNeighbors())
							if(n.hasPrevious()){		
								if(ancestor.getNeighbors().
										contains(n.getPrevious()))
									coherence_counter++;
								
								tracked_neighbor_no++;
							}
										
						
//						double coherence_ratio = coherence_counter / (double)(cell.getNeighbors().size());
						double coherence_ratio = coherence_counter / (double)(tracked_neighbor_no);	
						//insert the ratio in the map 
						coherenceMap.put(cell, coherence_ratio);
					}
					
				}
			}
			
			
		}
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.white);
			
			g.setFont(new Font("TimesRoman", Font.PLAIN, 8));
			
			for(Node cell: frame_i.vertexSet()){

				//skip boundary cells
				if(cell.onBoundary())
				continue;
				
				//if a coherence value has been computed go on
				if(coherenceMap.containsKey(cell)){

					//extract coordinates to write the value
					Coordinate centroid = 
							cell.getCentroid().getCoordinate();

					//transform the value to a single integer
					long coherence_val = Math.round(coherenceMap.get(cell) * 10);

					//write the integer inside the cell
					g.drawString(Long.toString(coherence_val), 
							(float)centroid.x - 2  , 
							(float)centroid.y + 2);

				}
			}
		}
	}
}
