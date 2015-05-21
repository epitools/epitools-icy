package plugins.davhelle.cellgraph.io;

import java.io.File;

import org.testng.Assert;
import org.testng.annotations.Test;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.TissueEvolution;
import plugins.davhelle.cellgraph.nodes.Cell;
import plugins.davhelle.cellgraph.nodes.Division;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class CsvTrackReaderTest {
	@Test
	public void testSimpleReadOut() {
		//Input Data
		TissueEvolution single_frame_stg = new TissueEvolution(1);
		FrameGraph first_frame = new FrameGraph(0,single_frame_stg);

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

		String output_folder = "/Users/davide/tmp/NewFolder/";

		CsvTrackWriter track_writer = new CsvTrackWriter(
				single_frame_stg,
				output_folder);
		track_writer.writeTrackingIds();

		//change the tracking ID recorded
		first_cell.setTrackID(0);
		first_cell.setBoundary(false);

		CsvTrackReader track_reader = new CsvTrackReader(
				single_frame_stg,
				output_folder);
		track_reader.readTrackingIds();

		Assert.assertEquals(first_cell.getTrackID(), 1);
		Assert.assertEquals(first_cell.onBoundary(), true);

		cleanUp(output_folder);
	}
	
	/**
	 * Deletes all files in given directory
	 * source: http://stackoverflow.com/questions/13195797/delete-all-files-in-directory-but-not-directory-one-liner-solution
	 * 
	 * @param directory_name name of the directory to clean
	 */
	private void cleanUp(String directory_name){
		File dir = new File(directory_name);
		for(File file: dir.listFiles()) 
			file.delete();
	}
	
	@Test
	public void testSingleDivision(){
		//Input Data
		TissueEvolution one_division_stg = new TissueEvolution(2);
		FrameGraph first_frame = new FrameGraph(0,one_division_stg);
		FrameGraph second_frame = new FrameGraph(1,one_division_stg);
		
		Cell mother = buildDummyCell(first_frame,0.0,1);
		Cell child1 = buildDummyCell(second_frame,1.0,2);
		Cell child2 = buildDummyCell(second_frame,2.0,3);
		
		new Division(mother,child1,child2);
		
		String output_folder = "/Users/davide/tmp/NewFolder/";
		CsvTrackWriter track_writer = new CsvTrackWriter(one_division_stg, output_folder);
		track_writer.writeTrackingIds();
		track_writer.writeDivisions();
		
		//construct same but without Division graph
		TissueEvolution no_division_stg = new TissueEvolution(2);
		first_frame = new FrameGraph(0,no_division_stg);
		second_frame = new FrameGraph(1,no_division_stg);
		
		//same cells but no tracking ids
		mother = buildDummyCell(first_frame,0.0,-1);
		child1 = buildDummyCell(second_frame,1.0,-1);
		child2 = buildDummyCell(second_frame,2.0,-1);
		
		//read in tracking
		CsvTrackReader track_reader = new CsvTrackReader(no_division_stg, output_folder);
		track_reader.readTrackingIds();
		track_reader.readDivisions();
		
		Assert.assertEquals(mother.getTrackID(), 1,"mother has wrong id");
		Assert.assertEquals(child1.getTrackID(), 2,"daughter1 has wrong id");
		Assert.assertEquals(child2.getTrackID(), 3,"daughter2 has wrong id");
		
		Assert.assertEquals(mother.hasObservedDivision(),true);
		Assert.assertEquals(child1.hasObservedDivision(),true);
		Assert.assertEquals(child2.hasObservedDivision(),true);

		cleanUp(output_folder);
	}

	private Cell buildDummyCell(
			FrameGraph destination_frame,
			double x, int track_id)
	{
		  GeometryFactory factory = new GeometryFactory();
		  Coordinate[] polygon_coordinate_array = {
				  new Coordinate(x	, x),
				  new Coordinate(x	, x+1),
				  new Coordinate(x+1, x+1),
				  new Coordinate(x+1, x),
				  new Coordinate(x	, x)};
		  
		  Polygon cell_polygon = factory.createPolygon(polygon_coordinate_array);
		  Cell dummy_cell = new Cell(cell_polygon,destination_frame);
		  dummy_cell.setTrackID(track_id);
		  
		  destination_frame.addVertex(dummy_cell);
		  
		  
		  return(dummy_cell);
	  }
}
