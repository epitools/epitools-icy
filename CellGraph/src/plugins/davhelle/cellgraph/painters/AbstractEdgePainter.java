/**
 * 
 */
package plugins.davhelle.cellgraph.painters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;

import headless.T1Transitions;
import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Edge Painter
 * 
 * e.g. visualize the persistence of edges
 * 
 * @author Davide Heller
 *
 */
public class AbstractEdgePainter extends Overlay {
	
	private SpatioTemporalGraph stGraph;
	private ShapeWriter writer;
	private HashMap<Edge,Geometry> stable_set;
	private HashMap<Edge, Geometry> unstable_set;

	/**
	 * @param name
	 */
	public AbstractEdgePainter(SpatioTemporalGraph stGraph) {
		super("Abstract Edge Painter");
		
		this.stGraph = stGraph;
		this.stable_set = new HashMap<Edge, Geometry>();
		this.unstable_set = new HashMap<Edge, Geometry>();
		this.writer = new ShapeWriter();
		
		HashMap<Long, Integer> edge_stability = computeEdgeStability(stGraph);
		
		FrameGraph frame0 = stGraph.getFrame(0);
		int edge_survival_count = 0;
		for(long track_code:edge_stability.keySet()){
			int[] pair = Edge.getCodePair(track_code);

			Node a = frame0.getNode(pair[0]);
			Node b = frame0.getNode(pair[1]);

			Edge ab = frame0.getEdge(a, b);
			PolygonalCellTile a_tile = new PolygonalCellTile(a);

			int pair_survival_time = edge_stability.get(track_code);

			if(pair_survival_time == stGraph.size()){
				edge_survival_count++;
				stable_set.put(ab,a_tile.getTileEdge(b));
			}
			else
				unstable_set.put(ab,a_tile.getTileEdge(b));
		}
		
		double pct_edge_sourvival = edge_survival_count / (double)edge_stability.size() * 100;
		System.out.printf("Percentage of survived edges:%.2f\n",pct_edge_sourvival);
		
		//if edge not present in i+1: increase temporal edge otherwise skip or discard if s/t nodes absent
		//final divide edge according to length to measure stability
		
		System.out.println("Stable edge length:");
		for(Edge e: stable_set.keySet())
			System.out.printf("%.2f\n",frame0.getEdgeWeight(e));
		
		System.out.println("Unstable edge length:");
		for(Edge e: unstable_set.keySet())
			System.out.printf("%.2f\n",frame0.getEdgeWeight(e));

	}
	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			FrameGraph frame_i = stGraph.getFrame(time_point);
			for(Edge e: frame_i.edgeSet()){
				if(this.stable_set.containsKey(e)){
					g.setColor(Color.GREEN);
					g.draw(writer.toShape(stable_set.get(e)));
				}
				else{
					if(this.unstable_set.containsKey(e)){
						g.setColor(Color.red);
						g.draw(writer.toShape(unstable_set.get(e)));
					}
				}
			}
		}
    }

	private HashMap<Long, Integer> computeEdgeStability(
			SpatioTemporalGraph stGraph) {
		HashMap<Long,Integer> tracked_edges = new HashMap<Long,Integer>();
		
		for(int i=0; i<stGraph.size(); i++){
			
			FrameGraph frame_i = stGraph.getFrame(i);
			int no_of_tracked_edges = 0;
			for(Edge e: frame_i.edgeSet()){
				if(e.isTracked(frame_i)){
					
					long edge_track_code = e.getPairCode(frame_i);
				
					if(i==0)
						tracked_edges.put(edge_track_code,0);
					
					if(tracked_edges.containsKey(edge_track_code)){
						int old = tracked_edges.get(edge_track_code);
						tracked_edges.put(edge_track_code, old + 1);
						no_of_tracked_edges++;
					}
				}
			}
			System.out.printf("Sample contains %d/%d trackable edges in frame %d\n",
					no_of_tracked_edges,frame_i.edgeSet().size(),i);
			
			//introduce the difference between lost edge because of tracking and because of T1
			ArrayList<Long> to_eliminate = new ArrayList<Long>();
			for(long track_code:tracked_edges.keySet()){
				int[] pair = Edge.getCodePair(track_code);
				for(int track_id: pair){
					if(!frame_i.hasTrackID(track_id)){
						to_eliminate.add(track_code);
						break;
					}
				}
			}
			
			for(long track_code:to_eliminate)
				tracked_edges.remove(track_code);
		}
		return tracked_edges;
	}
}
