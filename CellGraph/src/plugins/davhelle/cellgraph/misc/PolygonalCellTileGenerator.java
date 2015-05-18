package plugins.davhelle.cellgraph.misc;

import java.util.HashMap;

import plugins.adufour.ezplug.EzPlug;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Class to generate the PolygonalCellTiles for an entire graph at once
 * 
 * @author Davide Heller
 *
 */
public class PolygonalCellTileGenerator {
	
	/**
	 * Static method create polygonal tiles for all cells in the input stGraph. Gives progress back to
	 * EzPlug GUI using setProgressBarMessage
	 * 
	 * @param stGraph input graph
	 * @param plugin EzPlugin that launches the method
	 * @return Map of nodes to their polygon tile
	 */
	public static HashMap<Node, PolygonalCellTile> createPolygonalTiles(SpatioTemporalGraph stGraph, EzPlug plugin) {
		plugin.getUI().setProgressBarMessage("Identifying tiles...");

		HashMap<Node,PolygonalCellTile> cell_tiles = new HashMap<Node, PolygonalCellTile>();
		for(int i=0; i < stGraph.size(); i++){
			plugin.getUI().setProgressBarValue((double)i/stGraph.size());
			FrameGraph frame = stGraph.getFrame(i);
			for(Node n: frame.vertexSet()){
				PolygonalCellTile tile = new PolygonalCellTile(n);
				cell_tiles.put(n, tile);
			}
		}
		plugin.getUI().setProgressBarValue(1.0);
		System.out.println();
		return cell_tiles;
	}

	/**
	 * Same as above without progress to EzPlug but text based (for headless execution)
	 * 
	 * @param stGraph input graph
	 * @return Map of nodes to their polygon tile
	 */
	public static HashMap<Node, PolygonalCellTile> createPolygonalTiles(SpatioTemporalGraph stGraph) {
		System.out.println("Identifying the tiles..");
		HashMap<Node,PolygonalCellTile> cell_tiles = new HashMap<Node, PolygonalCellTile>();
		for(int i=0; i < stGraph.size(); i++){
			printProgressBar(i, stGraph.size());
			FrameGraph frame = stGraph.getFrame(i);
			for(Node n: frame.vertexSet()){
				PolygonalCellTile tile = new PolygonalCellTile(n);
				cell_tiles.put(n, tile);
			}
		}
		printProgressBar(stGraph.size(), stGraph.size());
		System.out.println();
		return cell_tiles;
	}
	
	
	/**
	 * Text based progress bar based on:
	 * http://nakkaya.com/2009/11/08/command-line-progress-bar/
	 * 
	 * @param current current iteration value
	 * @param max maximum iteration value
	 */
	public static void printProgressBar(int current, int max){
		int percent = (current * 100) / max;
		
	    StringBuilder bar = new StringBuilder("[");

	    for(int i = 0; i < 50; i++){
	        if( i < (percent/2)){
	            bar.append("=");
	        }else if( i == (percent/2)){
	            bar.append(">");
	        }else{
	            bar.append(" ");
	        }
	    }

	    bar.append("]   " + percent + "%     ");
	    System.out.print("\r" + bar.toString());
	}
	
}
