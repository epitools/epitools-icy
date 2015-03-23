/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Map;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

/**
 * Overlay representing the voronoi tesselation of all inner cells
 * 
 * @author Davide Heller
 *
 */
public class VoronoiPainter extends StGraphOverlay {

	public static final String DESCRIPTION = 
			"Overlay displays the voronoi diagram computed from the cell centroids";
	private Map<Node, Geometry> nodeVoronoiMap;
	private ShapeWriter writer;
	
	
	public VoronoiPainter(SpatioTemporalGraph stGraph, Map<Node,Geometry> nodeVoronoiMap) {
		super("Voronoi Diagram",stGraph);
		this.nodeVoronoiMap = nodeVoronoiMap;
		this.writer = new ShapeWriter();

	}
	
	@Override
    public void paintFrame(Graphics2D g, FrameGraph frame_i)
    {
			
		g.setColor(Color.green);

		for(Node cell: frame_i.vertexSet())
			if(!cell.onBoundary())
				if(nodeVoronoiMap.containsKey(cell))
					g.draw((writer.toShape(nodeVoronoiMap.get(cell))));
			
    }

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		WKTWriter writer = new WKTWriter(2);
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Cell x");
		XLSUtil.setCellString(sheet, 2, 0, "Cell y");
		XLSUtil.setCellString(sheet, 3, 0, "Cell area");
		XLSUtil.setCellString(sheet, 4, 0, "Voronoi x");
		XLSUtil.setCellString(sheet, 5, 0, "Voronoi y");
		XLSUtil.setCellString(sheet, 6, 0, "Voronoi cell area");
		XLSUtil.setCellString(sheet, 7, 0, "WKT Voronoi Polygon");

		int row_no = 1;

		for(Node n: frame.vertexSet()){
			
			if(!n.onBoundary() && nodeVoronoiMap.containsKey(n)){
			
				Geometry voronoiGeo = nodeVoronoiMap.get(n);
				
				XLSUtil.setCellNumber(sheet, 0, row_no, n.getTrackID());
				XLSUtil.setCellNumber(sheet, 1, row_no, n.getGeometry().getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 2, row_no, n.getGeometry().getCentroid().getY());
				XLSUtil.setCellNumber(sheet, 3, row_no, n.getGeometry().getArea());
				XLSUtil.setCellNumber(sheet, 4, row_no, voronoiGeo.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 5, row_no, voronoiGeo.getCentroid().getY());
				XLSUtil.setCellNumber(sheet, 6, row_no, voronoiGeo.getArea());
				XLSUtil.setCellString(sheet, 7, row_no, writer.write(voronoiGeo));
				row_no++;
			}
		}
	}

}
