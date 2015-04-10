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

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.HashSet;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;

/**
 * Edge Painter
 * 
 * e.g. visualize the persistence of edges
 * 
 * @author Davide Heller
 *
 */
public class EdgeStabilityOverlay extends StGraphOverlay {
	
	public static final String DESCRIPTION = 
			"Displays a color code for how stable edges are (green=stable, red=not stable)[time consuming!]";
	
	private ShapeWriter writer;
	private HashSet<Long> stable_set;
	private HashSet<Long> unstable_set;
	private HashSet<Long> novel_set;

	/**
	 * @param name
	 */
	public EdgeStabilityOverlay(SpatioTemporalGraph stGraph) {
		super("Edge Survival",stGraph);
		
		this.stable_set = new HashSet<Long>();
		this.unstable_set = new HashSet<Long>();
		this.novel_set = new HashSet<Long>();
		this.writer = new ShapeWriter();
		
		HashMap<Long, Integer> edge_stability = computeEdgeStability(stGraph);
		
		int edge_survival_count = 0;
		for(long track_code:edge_stability.keySet()){
			int pair_survival_time = edge_stability.get(track_code);

			if(pair_survival_time == stGraph.size()){
				edge_survival_count++;
				stable_set.add(track_code);
			}
			else
				unstable_set.add(track_code);
		}
		
		double pct_edge_sourvival = edge_survival_count / (double)edge_stability.size() * 100;
		System.out.printf("Percentage of survived edges:%.2f\n",pct_edge_sourvival);
		
		//if edge not present in i+1: increase temporal edge otherwise skip or discard if s/t nodes absent
		//final divide edge according to length to measure stability
		
	}
	
	public void drawEdge(Graphics2D g, Edge e, Color color){
		g.setColor(color);
		g.draw(writer.toShape(e.getGeometry()));
	}
	
	private HashMap<Long, Integer> computeEdgeStability(
			SpatioTemporalGraph stGraph) {
		HashMap<Long,Integer> tracked_edges = new HashMap<Long,Integer>();
		HashSet<Long> eliminated_edges = new HashSet<Long>();
		
		for(int i=0; i<stGraph.size(); i++){
			
			FrameGraph frame_i = stGraph.getFrame(i);
			int no_of_tracked_edges = 0;
			for(Edge e: frame_i.edgeSet()){
				if(e.canBeTracked(frame_i)){ //both source and target nodes are tracked in this frame
					
					if(!e.hasGeometry())
						e.computeGeometry(frame_i); //TODO: put into initial loading / i.e. graph creation
					
					long edge_track_code = e.getPairCode(frame_i);
				
					if(i==0)
						tracked_edges.put(edge_track_code,0);
					
					if(tracked_edges.containsKey(edge_track_code)){
						updateTrackEntry(tracked_edges, edge_track_code);
						no_of_tracked_edges++;
					}
					else if(!eliminated_edges.contains(edge_track_code)){
						
						Node source_node = frame_i.getEdgeSource(e);
						Node target_node = frame_i.getEdgeTarget(e);
						
						//options:
						//- new edge
						//		- no divisions involved 
						//- old edge but part of division event
						//		- division involved 
						//			- BUT non_dividing cell has not both children as neighbor	
						//- split edge (divided by division)
						//		- division involved 
						//			- AND non_dividing cell neighbors both children
						//- division plane
						//		- between children
						
						if(source_node.hasObservedDivision() || target_node.hasObservedDivision()){
							if(source_node.hasObservedDivision() && target_node.hasObservedDivision()){
								continue;
							}
							else{
								
								long division_code = -1;
								
								//Find out which node divides (source or target)
								//and compute the alternative id (i.e. using mother node's id)
								if(source_node.hasObservedDivision())
									division_code = computeAlternativeId(
											e, source_node, target_node);
								else
									division_code = computeAlternativeId(
											e, target_node, source_node);
								
								if(tracked_edges.containsKey(division_code)){
									updateTrackEntry(tracked_edges,division_code);
//									System.out.println("Successfully updated edge "+
//											Arrays.toString(Edge.getCodePair(division_code)));
								}
							
							}
						}
						else
							//check if the origin is the same
								//if not assign as normal cell boundary	
							novel_set.add(edge_track_code);
					}
				}
			}
			System.out.printf("Sample contains %d/%d trackable edges in frame %d\n",
					no_of_tracked_edges,frame_i.edgeSet().size(),i);
			
			//introduce the difference between lost edge because of tracking and because of T1
			//based on the fact whether the edge participants are both in the frame
			//i.e. if one cell is not tracked anymore the edge can't be tracked either.
			
			//conflicts with the tracking of dividing cell edges. 
			
//			ArrayList<Long> to_eliminate = new ArrayList<Long>();
//			for(long track_code:tracked_edges.keySet()){
//				int[] pair = Edge.getCodePair(track_code);
//				for(int track_id: pair){
//					if(!frame_i.hasTrackID(track_id)){
//						to_eliminate.add(track_code);
//						break;
//					}
//				}
//			}
//			
//			for(long track_code:to_eliminate){
//				tracked_edges.remove(track_code);
//				eliminated_edges.add(track_code);
//			}
		}
		return tracked_edges;
	}

	/**
	 * @param e
	 * @param dividing_node
	 * @param other_node
	 * @return
	 */
	private long computeAlternativeId(Edge e, Node dividing_node, Node other_node) {
		//TODO check whether input node is actually dividing
		Division division = dividing_node.getDivision();
		Node mother = division.getMother();
		long division_code = Edge.computePairCode(mother.getTrackID(), other_node.getTrackID());
		
		e.setDivision(true);
		e.setTrackId(division_code);
		return division_code;
	}

	/**
	 * @param tracked_edges
	 * @param edge_track_code
	 */
	private void updateTrackEntry(HashMap<Long, Integer> tracked_edges,
			long edge_track_code) {
		int old = tracked_edges.get(edge_track_code);
		tracked_edges.put(edge_track_code, old + 1);
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		for(Edge e: frame_i.edgeSet()){
			long track_code = e.getPairCode(frame_i);
			
			if(e.hasDivision())
				track_code = e.getTrackId();
			
			if(stable_set.contains(track_code))
				drawEdge(g,e,Color.green);
			else if(unstable_set.contains(track_code))
				drawEdge(g,e,Color.red);
			else if(novel_set.contains(track_code))
				drawEdge(g,e,Color.yellow);
		}
		
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		// TODO Auto-generated method stub
		
	}
}
