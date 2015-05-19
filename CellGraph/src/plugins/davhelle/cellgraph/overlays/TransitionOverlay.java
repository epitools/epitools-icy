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
package plugins.davhelle.cellgraph.overlays;

import headless.DetectT1Transition;
import icy.gui.dialog.SaveDialog;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.geom.Line2D.Double;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.CellOverlay;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.CsvWriter;
import plugins.davhelle.cellgraph.misc.PolygonalCellTile;
import plugins.davhelle.cellgraph.misc.PolygonalCellTileGenerator;
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
public class TransitionOverlay extends StGraphOverlay{

	public static final String DESCRIPTION = 
			"Computes and displays the T1 transitions present in the time lapse [time consuming!]";
	
	ArrayList<T1Transition> transitions;
	final Color loser_color = Color.cyan;
	final Color winner_color = Color.magenta;
	
	public TransitionOverlay(SpatioTemporalGraph stGraph, CellOverlay plugin) {
		super(String.format("Transition Painter (min=%d)",plugin.varMinimalTransitionLength.getValue()),
				stGraph);
		
		//TODO move createPolygonalTiles to PolygonalCellTile class
		HashMap<Node, PolygonalCellTile> cell_tiles = PolygonalCellTileGenerator.createPolygonalTiles(stGraph,plugin);
		HashMap<Long, boolean[]> tracked_edges = EdgeTracking.trackEdges(stGraph, plugin);
		
		plugin.getUI().setProgressBarMessage("Analyzing Transitions..");
		this.transitions = DetectT1Transition.findTransitions(
				stGraph,
				cell_tiles,
				tracked_edges,
				plugin.varMinimalTransitionLength.getValue(),
				plugin.varMinimalOldSurvival.getValue());
		
		System.out.println("Transitions found: "+transitions.size());
	
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		saveToCsv();
	}
	
	/**
	 * Save transitions in CSV format
	 * 
	 * @param file_name
	 */
	public void saveToCsv(){
		
		String file_name = SaveDialog.chooseFile(
				"Please choose where to save the CSV transitions statistics", 
				"/Users/davide/analysis/",
				"t1_transitions",
				"");
		
		StringBuilder builder_main = new StringBuilder();
		StringBuilder builder_loser = new StringBuilder();
		StringBuilder builder_winner = new StringBuilder();
		
		for(T1Transition t1: transitions){
			if(!t1.hasWinners())
				continue;
			
			builder_main.append(t1.getDetectionTime());
			builder_main.append(',');
			builder_main.append(t1.length());
			builder_main.append(',');
			
			Edge first_looser_edge = extractEdgeLength(builder_loser, t1, true);
			Edge first_winner_edge = extractEdgeLength(builder_winner, t1, false);
			
			if(first_looser_edge != null){
				int[] cell_ids = t1.getLoserNodes();
				builder_main.append(String.format("%d,%d,", 
						cell_ids[0],
						cell_ids[1]));
			}
			
			if(first_winner_edge != null){
				int[] cell_ids = t1.getWinnerNodes();
				builder_main.append(String.format("%d,%d,", 
						cell_ids[0],
						cell_ids[1]));
			}
			
			//trim last comma position
			builder_main.setLength(builder_main.length() - 1);
			builder_loser.setLength(builder_loser.length() - 1);
			builder_winner.setLength(builder_winner.length() - 1);
			
			//next line
			builder_main.append('\n');
			builder_loser.append('\n');
			builder_winner.append('\n');
		}
		
		File main_output_file = new File(file_name+"_main.csv");
		CsvWriter.writeOutBuilder(builder_main, main_output_file);
		
		File loser_output_file = new File(file_name+"_loser.csv");
		CsvWriter.writeOutBuilder(builder_loser, loser_output_file);
		
		File winner_output_file = new File(file_name+"_winner.csv");
		CsvWriter.writeOutBuilder(builder_winner, winner_output_file);
		
		System.out.printf("Successfully wrote to:\n\t%s\n\t%s\n\t%s\n",
				main_output_file.getName(),
				loser_output_file.getName(),
				winner_output_file.getName());
		
	}

	/**
	 * For every time point the method extracts the associated
	 * 
	 * 
	 * @param builder_loser
	 * @param t1
	 * @return
	 */
	private Edge extractEdgeLength(StringBuilder builder, T1Transition t1, boolean extract_loser) {
		int[] cell_ids = null;
		if(extract_loser)
			cell_ids = t1.getLoserNodes();
		else
			cell_ids = t1.getWinnerNodes();
		
		Edge first_edge = extractJunctionLength(builder, cell_ids);
		return first_edge;
	}

	/**
	 * @param builder
	 * @param cell_ids
	 * @return
	 */
	public Edge extractJunctionLength(StringBuilder builder,
			int[] cell_ids) {
		Edge first_edge = null;
			
		for(int i=0; i<stGraph.size(); i++){
			FrameGraph frame_i = stGraph.getFrame(i);

			Node[] cell_nodes = new Node[cell_ids.length];
			
			for(int j=0; j<cell_ids.length; j++){
				int loser_id = cell_ids[j];
				if(frame_i.hasTrackID(loser_id))
					cell_nodes[j] = frame_i.getNode(loser_id);
			}
			
			if(frame_i.containsEdge(cell_nodes[0], cell_nodes[1])){
				Edge e = frame_i.getEdge(cell_nodes[0], cell_nodes[1]);
				builder.append(String.format("%.2f", frame_i.getEdgeWeight(e)));
				builder.append(',');
				
				if(first_edge == null)
					first_edge = e;
			}
			else{
				builder.append(0.0);
				builder.append(',');
			}				
		}
		return first_edge;
	}
	

	/**
	 * @param g
	 * @param frame_i
	 */
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
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

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		// TODO Auto-generated method stub
	}

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		
		String s = "T1 Transition Winners";
		Color c = Color.MAGENTA;
		int offset = 0;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);

		s = "T1 Transition Losers";
		c = Color.CYAN;
		offset = 20;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);
		
	}
	
}
