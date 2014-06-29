package headless;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

public class DescribeT1Transition {

	public static void main(String[] args){
		
		//Input files
		File test_file = new File("/Users/davide/data/neo/1/T1_examples/T1_at_x62-y56/T1atx62-y56t0000.tif");
		int no_of_test_files = 10;

		SpatioTemporalGraph stGraph = StGraphUtils.createDefaultGraph(test_file,no_of_test_files);
		HashMap<Node, PolygonalCellTile> cell_tiles = StGraphUtils.createPolygonalTiles(stGraph);
		HashMap<Long,Integer> tracked_edges = new HashMap<Long,Integer>();
		
		//TODO: Edge initialization rather than i==0 condition 
		
		for(int i=0; i<stGraph.size(); i++)
		{
			FrameGraph frame_i = stGraph.getFrame(i);
			
			int no_of_tracked_edges = trackEdges(tracked_edges, frame_i);
			System.out.printf("Sample contains %d/%d trackable edges in frame %d\n",
					no_of_tracked_edges,frame_i.edgeSet().size(),i);
			
			removeUntrackedEdges(tracked_edges, frame_i);
		}
		
		//when change retrieve ending nodes
		//extract tile geometry
		//find neighboring cells
		//check if they are connected in LOST frame
		//and if they were unconnected in LAST frame before
		
		checkEdgeStability(tracked_edges);
	
	}

	private static void checkEdgeStability(HashMap<Long, Integer> tracked_edges) {
		int edge_survival_count = 0;
		for(long track_code:tracked_edges.keySet()){
			int[] pair = Edge.getCodePair(track_code);
			int pair_survival_time = tracked_edges.get(track_code);
			
			if(pair_survival_time == 10)
				edge_survival_count++;
			else
				System.out.printf("%s(%d)\n",Arrays.toString(pair),pair_survival_time);
		}
		
		double pct_edge_sourvival = edge_survival_count / (double)tracked_edges.size() * 100;
		System.out.printf("Percentage of survived edges:%.2f\n",pct_edge_sourvival);
	}

	private static int trackEdges(HashMap<Long, Integer> tracked_edges,
			FrameGraph frame_i) {
		int no_of_tracked_edges = 0;
		for(Edge e: frame_i.edgeSet()){
			if(e.isTracked(frame_i)){
				
				long edge_track_code = e.getPairCode(frame_i);
			
				if(frame_i.getFrameNo() == 0)
					tracked_edges.put(edge_track_code,0);
				
				if(tracked_edges.containsKey(edge_track_code)){
					int old = tracked_edges.get(edge_track_code);
					tracked_edges.put(edge_track_code, old + 1);
					no_of_tracked_edges++;
				}
			}
		}
		return no_of_tracked_edges;
	}

	private static void removeUntrackedEdges(
			HashMap<Long, Integer> tracked_edges, FrameGraph frame_i) {
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
	
}
