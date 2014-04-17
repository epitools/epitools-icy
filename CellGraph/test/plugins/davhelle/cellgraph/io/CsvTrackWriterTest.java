package plugins.davhelle.cellgraph.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Test;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.TissueEvolution;
import plugins.davhelle.cellgraph.nodes.Cell;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class CsvTrackWriterTest {
  @Test
  public void testEmptyInput() { 	
	  //Input Data
	  TissueEvolution empty_stg = new TissueEvolution(1);
	  empty_stg.addFrame(new FrameGraph());
	  String output_folder = "/Users/davide/tmp/NewFolder/";

	  //Execute Program
	  CsvTrackWriter track_writer = new CsvTrackWriter(empty_stg,output_folder);
	  track_writer.writeTrackingIds();

	  //Verify file structure
	  assertFileExistence(output_folder,"tracking_t000.csv");
	  
	  File tracking_file = new File(output_folder + "tracking_t000.csv");
	  removeDummyFile(tracking_file);
  }
  
  @Test
  public void testOneCellInput(){
	  //Input Data
	  TissueEvolution single_frame_stg = new TissueEvolution(1);
	  FrameGraph first_frame = new FrameGraph();
	  single_frame_stg.addFrame(first_frame);
	  
	  //Populate the cell graph with a single cubic cell
	  GeometryFactory factory = new GeometryFactory();
	  Coordinate[] polygon_coordinate_array = {
			  new Coordinate(0.0, 0.0),
			  new Coordinate(0.0, 2.0),
			  new Coordinate(2.0, 2.0),
			  new Coordinate(2.0, 0.0),
			  new Coordinate(0.0, 0.0)};
	  
	  Polygon cell_polygon = factory.createPolygon(polygon_coordinate_array);
	  Cell first_cell = new Cell(cell_polygon,first_frame);
	  first_cell.setTrackID(1);
	  first_cell.setBoundary(true);
	  first_frame.addVertex(first_cell);
	  
	  //Assess that centroid is correct
	  Point centroid = first_cell.getCentroid();
	  boolean expected_on_boundary = true;
	  double expected_x = 1.0;
	  double expected_y = 1.0;
	  int expected_track_id = 1;
	  Assert.assertEquals(centroid.getX(), 1.0 , "X coordingate is not 1 but"+Double.toString(centroid.getX()));
	  Assert.assertEquals(centroid.getY(), 1.0 , "Y coordingate is not 1 but"+Double.toString(centroid.getY()));
	  Assert.assertEquals(first_cell.getTrackID(),1,String.format("Track ID is not 1 but %d",first_cell.getTrackID()));
	  Assert.assertEquals(first_cell.onBoundary(), expected_on_boundary);
	  	  
	  String output_folder = "/Users/davide/tmp/NewFolder/";
	  
	  //Execute Program
	  CsvTrackWriter track_writer = new CsvTrackWriter(single_frame_stg,output_folder);
	  track_writer.writeTrackingIds();

	  //Verify file existance
	  String expected_file_name = output_folder+"tracking_t000.csv";
	  File tracking_file = new File(expected_file_name);
	  Assert.assertTrue(tracking_file.exists(), tracking_file.getAbsolutePath() + " does not exist!");
	  
	  //Verify content
	  String expected_content = String.format("%d,%.2f,%.2f,%b",expected_track_id,expected_x,expected_y,expected_on_boundary);
	  System.out.println(expected_content);
	  
	  String file_content = read_file(tracking_file);
	  
	  Assert.assertEquals(file_content,expected_content,"File content is not as expected: "+file_content);
	  
	  removeDummyFile(tracking_file);
  
  }
  
  private void removeDummyFile(File dummy_file) {
	  try{
		  dummy_file.delete();
	  } catch (Exception e) {
		  e.printStackTrace();
	  }
  }

private void assertFileExistence(String output_folder, String file_name) {
	  File tracking_file = new File(output_folder + file_name);
	  Assert.assertTrue(tracking_file.exists(), tracking_file.getAbsolutePath() + " does not exist!");
  }
  
  private String read_file(File input_file) {
	  String first_line = "";

	  try {

		  FileReader fr = new FileReader(input_file);
		  BufferedReader br = new BufferedReader(fr);

		  first_line = br.readLine();

		  br.close();

	  } catch (IOException e) {
		  e.printStackTrace();
	  }

	  return first_line;
  }

}
