package plugins.davhelle.cellgraph.io;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.TissueEvolution;
import plugins.davhelle.cellgraph.nodes.Cell;

public class CsvTrackReaderTest {
	@Test
	public void testSimpleReadOut() {
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

	}

	private SpatioTemporalGraph createSimpleGraph() {
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

		return single_frame_stg;
	}
}
