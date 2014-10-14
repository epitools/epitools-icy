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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.misc.T1Transition;
import plugins.davhelle.cellgraph.nodes.Edge;
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
	
	/**
	 * Save transitions in CSV format
	 * 
	 * @param file_name
	 */
	public void saveToCsv(String file_name){
		StringBuilder builder_main = new StringBuilder();
		StringBuilder builder_loser = new StringBuilder();
		
		for(T1Transition t1: transitions){
			builder_main.append(t1.getDetectionTime());
			builder_main.append(',');
			builder_main.append(t1.length());
			builder_main.append(',');
			
			Edge first_looser_edge = extractEdgeLength(builder_loser, t1);
			
			if(first_looser_edge != null)
				builder_main.append(String.format("%.2f,%.2f", 
						first_looser_edge.getGeometry().getCentroid().getX(),
						first_looser_edge.getGeometry().getCentroid().getY()));
			//Append length of the edges
			//looser before
			//winner after
			//check whether long looser/winner exist.
			builder_main.append('\n');
			builder_loser.append('\n');
		}
		
		File main_output_file = new File(file_name+"_main.csv");
		writeOutBuilder(builder_main, main_output_file);
		
		File loser_output_file = new File(file_name+"_loser.csv");
		writeOutBuilder(builder_loser, loser_output_file);
		
		System.out.printf("Successfully wrote to:\n\t%s\n\t%s\n",
				main_output_file.getName(),
				loser_output_file.getName());
		
	}

	/**
	 * @param builder_loser
	 * @param t1
	 * @return
	 */
	private Edge extractEdgeLength(StringBuilder builder_loser, T1Transition t1) {
		int[] loser_ids = t1.getLoserNodes();
		Edge first_looser_edge = null;
			
		for(int i=0; i<stGraph.size(); i++){
			FrameGraph frame_i = stGraph.getFrame(i);

			Node[] losers = new Node[loser_ids.length];
			
			for(int j=0; j<loser_ids.length; j++){
				int loser_id = loser_ids[j];
				if(frame_i.hasTrackID(loser_id))
					losers[j] = frame_i.getNode(loser_id);
			}
			
			if(frame_i.containsEdge(losers[0], losers[1])){
				Edge e = frame_i.getEdge(losers[0], losers[1]);
				builder_loser.append(String.format("%.2f", frame_i.getEdgeWeight(e)));
				builder_loser.append(',');
				
				if(frame_i.getFrameNo()==0)
					first_looser_edge = e;
			}
			else{
				builder_loser.append(0.0);
				builder_loser.append(',');
			}				
		}
		return first_looser_edge;
	}

	/**
	 * @param builder_main
	 * @param output_file
	 */
	public void writeOutBuilder(StringBuilder builder_main, File output_file) {
		FileWriter fstream;
		try {
			fstream = new FileWriter(output_file);
			BufferedWriter writer = new BufferedWriter(fstream);
			writer.append(builder_main);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
