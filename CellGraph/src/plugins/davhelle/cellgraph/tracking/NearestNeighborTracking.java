package plugins.davhelle.cellgraph.tracking;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

public class NearestNeighborTracking extends TrackingAlgorithm{

	public NearestNeighborTracking(SpatioTemporalGraph spatioTemporalGraph) {
		
		super(spatioTemporalGraph);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void track(){
		
		//for every frame extract all particles
		for(int time_point=0;time_point<stGraph.size() - 1; time_point++){
		
			//for every cell center
			for(Node current: stGraph.getFrame(time_point).vertexSet()){

				Point current_cell_center = current.getCentroid();
				
				//for every polygon in next time frame
				for(Node next: stGraph.getFrame(time_point + 1).vertexSet()){
					
					if(next.getGeometry().contains(current_cell_center)){

						if(next.getPrevious() == null)
							//if not yet associated
							updateCorrespondence(next, current);
						
						else{
							//else if closer substitute and report previous
							
							Point next_cell_center = next.getCentroid();
							Point previous_cell_center = next.getPrevious().getCentroid();
							
							if(DistanceOp.distance(next_cell_center, previous_cell_center)
									> DistanceOp.distance(next_cell_center, current_cell_center))
								updateCorrespondence(next, current);
						}
					}
				}
			}
		}
	}

}
