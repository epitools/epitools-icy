package plugins.davhelle.cellgraph.overlays;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * CellIdPainter depicts the cell ID or String supplied at
 * the coordinates of the cell centers. 
 * 
 * @author Davide Heller
 *
 */
public class TrackIdOverlay extends StGraphOverlay{
	
	/**
	 * Description string for GUI
	 */
	public static final String DESCRIPTION = 
			"Overlay to paint the track id of each cell";
	
	/**
	 * Initialize the Tracking ID overlay for the input graph
	 * 
	 * @param spatioTemporalGraph the stgraph to be analyzed
	 */
	public TrackIdOverlay(SpatioTemporalGraph spatioTemporalGraph){
		super("Cell Tracking IDs",spatioTemporalGraph);
	}
	
	@Override
    public void paintFrame(Graphics2D g, FrameGraph frame_i)
    {
			
    	int fontSize = 3;
    	g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
    	g.setColor(Color.CYAN);

    	for(Node cell: frame_i.vertexSet()){

    		Coordinate centroid = cell.getCentroid().getCoordinate();

    		g.drawString(Integer.toString(cell.getTrackID()), 
    				(float)centroid.x - 2  , 
    				(float)centroid.y + 2);

    	}		
    }


	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Cell x");
		XLSUtil.setCellString(sheet, 2, 0, "Cell y");
		
		int row_no = 1;

		for(Node n: frame.vertexSet()){
			XLSUtil.setCellNumber(sheet, 0, row_no, n.getTrackID());
			XLSUtil.setCellNumber(sheet, 1, row_no, n.getGeometry().getCentroid().getX());
			XLSUtil.setCellNumber(sheet, 2, row_no, n.getGeometry().getCentroid().getY());
			row_no++;
		}
	}

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		//no legend for this layer as always plotted with TrackingOverlay
	}
	

}
