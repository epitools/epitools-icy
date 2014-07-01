/*=========================================================================
 *
 *  (C) Copyright (2012-2014) Basler Group, IMLS, UZH
 *  
 *  All rights reserved.
 *	
 *  author:	Davide Heller
 *  email:	davide.heller@imls.uzh.ch
 *  
 *=========================================================================*/

package headless;

import java.io.File;
import java.util.HashMap;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.GraphType;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraphGenerator;
import plugins.davhelle.cellgraph.misc.BorderCells;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.misc.SmallCellRemover;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.tracking.HungarianTracking;

/**
 * Utilities to assist a spatio temporal graph creation
 * 
 * @author Davide Heller
 *
 */
public class StGraphUtils {
	
	public static SpatioTemporalGraph createDefaultGraph(
			File test_file, int no_of_test_files) {
		System.out.println("Creating graph..");
		SpatioTemporalGraph stGraph = 
				new SpatioTemporalGraphGenerator(
						GraphType.TISSUE_EVOLUTION,
						test_file, 
						no_of_test_files).getStGraph();

		assert stGraph.size() == no_of_test_files: "wrong frame no";

		System.out.println("Identifying the border..");
		new BorderCells(stGraph).markOnly();
		
		System.out.println("Removing small cells..");
		new SmallCellRemover(stGraph).removeCellsBelow(10.0);

		System.out.println("Tracking cells..");
		new HungarianTracking(stGraph, 5, 5.0,1.0).track();
		return stGraph;
	}
	
	public static HashMap<Node, PolygonalCellTile> createPolygonalTiles(SpatioTemporalGraph stGraph) {
		System.out.println("Identifying the tiles..");
		HashMap<Node,PolygonalCellTile> cell_tiles = new HashMap<Node, PolygonalCellTile>();
		for(int i=0; i < stGraph.size(); i++){
			FrameGraph frame = stGraph.getFrame(i);
			for(Node n: frame.vertexSet()){
				PolygonalCellTile tile = new PolygonalCellTile(n);
				cell_tiles.put(n, tile);
			}
		}
		return cell_tiles;
	}
	
	
}
