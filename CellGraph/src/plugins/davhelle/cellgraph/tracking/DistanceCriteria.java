package plugins.davhelle.cellgraph.tracking;


/**
 * Distance method names to evaluate the candidate list
 */

public enum DistanceCriteria{
	//MINIMAL_DISTANCE, 
	//AVERAGE_DISTANCE, 
	//AREA_OVERLAP,	
	//TODO Add weighted distance? decreasing in time, e.g. 1*(t-1) + 0.8*(t-2)....
	//AREA_DIFF_WITH_MIN_DISTANCE, 
	//WEIGHTED_MIN_DISTANCE, 
	OVERLAP_WITH_MIN_DISTANCE
}

//Previous implementations of distance criteria
//case AVERAGE_DISTANCE:
//	//compute the average distance out of all
//	//distances with same node (group)
//	int count = 1;
//	double sum = DistanceOp.distance(
//			voted_centroid,
//			current_cell_center);
//
//	while(candidate_it.hasNext()){
//		voted = candidate_it.next();
//		if( voted.getFirst() == first){
//			candidate_it.remove();
//			voted_centroid = voted.getCentroid();
//			sum +=  DistanceOp.distance(
//					voted_centroid,
//					current_cell_center);
//			count++;
//		}
//	}
//
//	double avg = sum / count;
//	group_value = avg;
//	break;
//	
//case MINIMAL_DISTANCE:
//	//compute the minimal distance out of all
//	//distances with same node (group)
//	double min = DistanceOp.distance(
//			voted_centroid,
//			current_cell_center);
//
//	while(candidate_it.hasNext()){
//		voted = candidate_it.next();
//		if( voted.getFirst() == first){
//			candidate_it.remove();
//			voted_centroid = voted.getCentroid();
//			double candidate_dist = DistanceOp.distance(
//					voted_centroid,
//					current_cell_center);
//			if(min > candidate_dist)
//				min = candidate_dist;
//
//		}
//	}
//
//	group_value = min;
//	break;
//	
//case AREA_OVERLAP:	
//	
//	//compute the intersection between the two cell geometries
//	Geometry intersection = current.getGeometry().intersection(voted.getGeometry());
//
//	//Use the ratio of the intersection area and current's area as metric	
//	double overlap_ratio = intersection.getArea() / current_area;
//	//or alternatively divided by the sum of the two cell areas
//	//double overlap_ratio = intersection.getArea() / (current_area + voted.getGeometry().getArea());
//	//adding the candidate's area should reduce the bias of a large cell to affect
//	//the ratio. 
//	
//	//init best_ratio
//	double best_ratio = overlap_ratio;
//	
//	while(candidate_it.hasNext()){
//		
//		//count only candidates with the same first cell
//		voted = candidate_it.next();
//		if( voted.getFirst() == first){
//			candidate_it.remove();
//			//compute the intersection between the two cell geometries
//			intersection = current.getGeometry().intersection(voted.getGeometry());
//
//			//Use the ratio of the intersection area and current's area as metric	
//			overlap_ratio = intersection.getArea() / current_area;
//			//or alternatively divided by the sum of the two cell areas
//			//double overlap_ratio = intersection.getArea() / (current_area + voted.getGeometry().getArea());
//			//adding the candidate's area should reduce the bias of a large cell to affect
//			//the ratio. 
//			
//			//choose the biggest overlap ration
//			if(overlap_ratio > best_ratio)
//				best_ratio = overlap_ratio;
//		
//		}
//	}
//	
//	//end by assigning the reverse value as group value, ordered by minimum
//	group_value = 1 - best_ratio;
//		
//	break;
//	
//case WEIGHTED_MIN_DISTANCE:
//	//compute the minimal distance out of all
//	//distances with same node (group)
//	
//	double candidate_dist = DistanceOp.distance(
//			voted_centroid,
//			current_cell_center);
//	
//	//compute the intersection between the two cell geometries
//	intersection = current.getGeometry().intersection(voted.getGeometry());
//	overlap_ratio = intersection.getArea() / current_area;
//
//	double weighted_candidateDistance = candidate_dist * (1 - overlap_ratio);
//	
//	min = weighted_candidateDistance;
//
//	while(candidate_it.hasNext()){
//		
//		voted = candidate_it.next();
//		if( voted.getFirst() == first){
//			candidate_it.remove();
//			voted_centroid = voted.getCentroid();
//			
//			candidate_dist = DistanceOp.distance(
//					voted_centroid,
//					current_cell_center);
//			
//			//compute the intersection between the two cell geometries
//			intersection = current.getGeometry().intersection(voted.getGeometry());
//			overlap_ratio = intersection.getArea() / current_area;
//		
//			weighted_candidateDistance = candidate_dist * (1 - overlap_ratio);
//		
//			
//		if(min > weighted_candidateDistance)
//				min = weighted_candidateDistance;
//
//		}
//	}
//
//	group_value = min;
//	
//	break;
//	
//case AREA_DIFF_WITH_MIN_DISTANCE:
//	
//	candidate_dist = DistanceOp.distance(
//			voted_centroid,
//			current_cell_center);
//	
//	//compute difference in area
//	double area_candidate = voted.getGeometry().getArea();
//	double area_current = current.getGeometry().getArea();
//	
//	double area_difference = Math.abs(area_candidate - area_current);
//	double normalized_area_diff = area_difference/area_candidate;
//	
//	//alternative
//	
//	//compute the intersection between the two cell geometries
//	intersection = current.getGeometry().intersection(voted.getGeometry());
//	double normalized_overlap = intersection.getArea() / area_candidate;
//
//	//final score
//	
//	
//	System.out.println(
//			voted.getTrackID()+
//			" dist:"+candidate_dist+
//			" to: ["+Math.round(current_cell_center.getX())+
//			","+Math.round(current_cell_center.getY())+"]");
//	
//	System.out.println(
//			voted.getTrackID()+" area:"+
//					"\n\t"+area_candidate+
//					"\n\t"+area_current+
//					"\n\t"+normalized_area_diff+
//					"\n\t"+1/normalized_overlap);
//	
//	weighted_candidateDistance = 
//			lambda1 * candidate_dist +
//			lambda2 * normalized_area_diff;
//	
//	
//	min = weighted_candidateDistance;
//
//	while(candidate_it.hasNext()){
//		
//		voted = candidate_it.next();
//		if( voted.getFirst() == first){
//			candidate_it.remove();
//			voted_centroid = voted.getCentroid();
//			
//			candidate_dist = DistanceOp.distance(
//					voted_centroid,
//					current_cell_center);
//			
//			//compute difference in area
//			area_candidate = voted.getGeometry().getArea();
//			area_current = voted.getGeometry().getArea();
//			
//			area_difference = Math.abs(area_candidate - area_current);
//			normalized_area_diff = area_difference/area_candidate;
//
//			//final score
//			
//			
////			System.out.println(voted.getTrackID()+" dist:"+candidate_dist);
////			System.out.println(voted.getTrackID()+" area:"+normalized_area_diff);
////			
//			weighted_candidateDistance = 
//					lambda1 * candidate_dist +
//					lambda2 * normalized_area_diff;
//					
//			if(min > weighted_candidateDistance)
//				min = weighted_candidateDistance;
//
//		}
//	}
//
//	group_value = min;
//	
//	//use the area overlap ratio to weight the distance from the intersection to current's cell center
//	break;