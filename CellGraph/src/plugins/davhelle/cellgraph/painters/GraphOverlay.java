/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import java.awt.Color;
import java.awt.Graphics2D;

import jxl.write.WritableSheet;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;
import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.util.XLSUtil;
import ij.process.EllipseFitter;

/**
 * Class to visualize all the edges of a 
 * spatial-temporal graph as overlay in icy.
 * 
 * @author Davide Heller
 *
 */
public class GraphOverlay extends StGraphOverlay{

	public static final String DESCRIPTION = "Shows the connectivity (neighbors) of each cell; " +
			"The XLS export containt the number " +
			"of neighbors for every inner cell " +
			"(i.e. not on the border of the segmentation)";
	
	private GeometryFactory factory;
	private ShapeWriter writer;
	
	public GraphOverlay(SpatioTemporalGraph stGraph) {
		super("Graph edges",stGraph);
		this.factory = new GeometryFactory();
		this.writer = new ShapeWriter();
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i){

		g.setColor(Color.orange);

		//paint all the edges of the graph
		for(Edge edge: frame_i.edgeSet()){

			//extract points
			Point a = frame_i.getEdgeSource(edge).getCentroid();
			Point b = frame_i.getEdgeTarget(edge).getCentroid();

			//transform to JTS coordinates
			Coordinate[] edge_vertices = {a.getCoordinate(),b.getCoordinate()};

			//define line
			LineString edge_line = factory.createLineString(edge_vertices);

			//draw line
			g.draw(writer.toShape(edge_line));
			g.draw(writer.toShape(a));

		}
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 2, 0, "Centroid y");
		XLSUtil.setCellString(sheet, 3, 0, "Number of Neighbors");

		int row_no = 1;

		for(Node n: frame.vertexSet()){
			if(!n.onBoundary()){
				
				int numberOfNeighbors = frame.degreeOf(n);
				
				XLSUtil.setCellNumber(sheet, 0, row_no, n.getTrackID());
				XLSUtil.setCellNumber(sheet, 1, row_no, n.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 2, row_no, n.getCentroid().getY());
				XLSUtil.setCellNumber(sheet, 3, row_no, numberOfNeighbors);
				
				row_no++;

			}
		}
		
	}
	
	
}
