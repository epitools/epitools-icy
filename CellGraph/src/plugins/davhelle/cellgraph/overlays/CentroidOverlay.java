/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.overlays;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Point;

/**
 * Overlay to display the centroid of each cell.
 * Exports the location of the centroid
 * 
 * 
 * @author Davide Heller
 *
 */
public class CentroidOverlay extends StGraphOverlay{
	
	public CentroidOverlay(SpatioTemporalGraph spatioTemporalGraph){
		super("Cell centroids", spatioTemporalGraph);
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		//TODO include 3D information!
		//TODO possible performance improvement if map<Node,Point> is created
		g.setColor(Color.red);
		
		for(Node cell: frame_i.vertexSet()){
			Point centroid = cell.getCentroid();
			g.fillOval((int)centroid.getX(), (int)centroid.getY(), 2, 2);
		}		
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		//what this module should print in every frame
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 2, 0, "Centroid y");

		int row_no = 1;
		for(Node node: frame.vertexSet()){
			XLSUtil.setCellNumber(sheet, 0, row_no, node.getTrackID());
			XLSUtil.setCellNumber(sheet, 1, row_no, node.getCentroid().getX());
			XLSUtil.setCellNumber(sheet, 2, row_no, node.getCentroid().getY());
			row_no++;
		}
	}

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		// TODO Auto-generated method stub
		
	}

}
