package plugins.davhelle.cellgraph.tracking;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

public class NearestNeighborTracking extends TrackingAlgorithm{
	
	private int linkrange;

	public NearestNeighborTracking(SpatioTemporalGraph spatioTemporalGraph, int linkrange) {
		super(spatioTemporalGraph);
		this.linkrange = linkrange;
	}
	
	@Override
	public void track(){
		
		//for every frame extract all particles
		for(int time_point=0;time_point<stGraph.size() - 1; time_point++){
			System.out.println("Linking frame "+time_point);
		
			//for every cell center
			for(Node current: stGraph.getFrame(time_point).vertexSet()){

				Point current_cell_center = current.getCentroid();
				
				//for every polygon in next time frames
				for(int i=1; i <= linkrange && time_point + i < stGraph.size(); i++){
					
					for(Node next: stGraph.getFrame(time_point + i).vertexSet()){

						if(next.getGeometry().contains(current_cell_center)){

							if(next.getPrevious() == null)
								//if not yet associated
								updateCorrespondence(next, current);

							else{
								//else if closer substitute and report previous

								Point next_cell_center = next.getCentroid();
								Point previous_cell_center = next.getPrevious().getCentroid();

								double oldDist = DistanceOp.distance(next_cell_center, previous_cell_center);
								double newDist = DistanceOp.distance(next_cell_center, current_cell_center);
								
								if(oldDist > newDist ){
									
									Node old_previous = next.getPrevious();
									Node mostRecent = null;
									
									if(i == 1)
										updateCorrespondence(next, current);
									else{
										mostRecent = getMostRecentCorrespondence(next,current);
										updateCorrespondence(next, mostRecent);
									}
									//report if you kick out the node from the previous frame (need check if kicked_out == mostRecent..)
									if(old_previous.getBelongingFrame().getFrameNo() == next.getBelongingFrame().getFrameNo() - 1)
										if(old_previous != mostRecent)
											System.out.println(old_previous.getTrackID() + "lost connection");
									
								}
								
								//+ perhaps don't do any mostRecent stuff but rather a final update.
								//+ do a check that you have only a one to one correspondence
								//+ a child can only link the same node as the parent did (or do an optimization)
								//	+ optimization (what future node minimizes the distance to all previous nodes)
								//+ combine resolveUnassignedNodes into current algorithm
								//+	division object
							}
						}
					}
				}
			}
		}
	}
}
