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
package plugins.davhelle.cellgraph.painters;

import headless.DetectT1Transition;
import headless.StGraphUtils;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.canvas.IcyCanvas;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashMap;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.misc.T1Transition;
import plugins.davhelle.cellgraph.nodes.Node;
import plugins.davhelle.cellgraph.tracking.EdgeTracking;

/**
 * Generate an overlay to display cell rearrangements
 * such as T1 transitions.
 * 
 * TODO generalize T1 to Transition interface
 * 
 * @author Davide Heller
 *
 */
public class TransitionOverlay extends Overlay{

	ArrayList<T1Transition> transitions;
	SpatioTemporalGraph stGraph;
	final Color loser_color = Color.cyan;
	final Color winner_color = Color.magenta;
	
	public TransitionOverlay(SpatioTemporalGraph stGraph) {
		super("Transition Painter");
		this.stGraph = stGraph;
		
		//TODO move createPolygonalTiles to PolygonalCellTile class
		HashMap<Node, PolygonalCellTile> cell_tiles = StGraphUtils.createPolygonalTiles(stGraph);
		HashMap<Long, boolean[]> tracked_edges = EdgeTracking.trackEdges(stGraph);
		
		this.transitions = DetectT1Transition.findTransitions(stGraph, cell_tiles, tracked_edges);
	
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			FrameGraph frame_i = stGraph.getFrame(time_point);
			
			//try to color the cells that loose the bond
			for(T1Transition t1: transitions){
				int[] losers = t1.getLoserNodes();
				
				for(int loser_id: losers){
					if(frame_i.hasTrackID(loser_id)){
						Node loser = frame_i.getNode(loser_id);
						g.setColor(loser_color);
						g.fill(loser.toShape());
					}
				}
			}
			
			for(T1Transition t1: transitions){
				if(t1.hasWinners()){
					int[] winner_ids = t1.getWinnerNodes();
					Node[] winners = new Node[winner_ids.length];
					g.setColor(winner_color);
					
					for(int i=0; i<winner_ids.length; i++){
						int winner_id = winner_ids[i];
						if(frame_i.hasTrackID(winner_id)){
							winners[i] = frame_i.getNode(winner_id);
							g.draw(winners[i].toShape());
						}
					}
					
					if(winners[0] != null && winners[1] != null)
						g.drawLine(
							(int)winners[0].getCentroid().getX(), 
							(int)winners[0].getCentroid().getY(),
							(int)winners[1].getCentroid().getX(), 
							(int)winners[1].getCentroid().getY());
				}
			}
		}		
		
	};
	
	
}
