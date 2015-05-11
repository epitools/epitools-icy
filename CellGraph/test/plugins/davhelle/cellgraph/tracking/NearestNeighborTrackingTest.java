package plugins.davhelle.cellgraph.tracking;

import headless.LoadNeoWktFiles;

import org.testng.Assert;
import org.testng.annotations.Test;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;

public class NearestNeighborTrackingTest {
  @Test
  public void speedTest() {
	  
	  int time_points = 3;
	  
	  SpatioTemporalGraph st_graph = LoadNeoWktFiles.loadStGraph(0, time_points);
	  
	  Assert.assertEquals(st_graph.size(), time_points, "Input file not correctly loaded");
	  
	  TrackingAlgorithm tracker = new NearestNeighborTracking(st_graph, 5, 1, 1);
	  
	  tracker.track();
	  
	  Assert.assertTrue(st_graph.hasTracking(), "Tracking was not completed correctly");
	  
  }
}
