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
 * Overlay to represent the neighborhood degree with either
 * numbers or colors. Color scheme follows the convention established
 * in the paper of Ishihara et al., 2013
 * 
 * @author Davide Heller
 *
 */
public class PolygonClassOverlay extends StGraphOverlay{
	
	/**
	 * Description string for GUI
	 */
	public static final String DESCRIPTION = 
			"Displays the number of neighbors of each cell with<br/>" +
			"color code or number";
	
	/**
	 * Flag to display or not the polygon sidedness as number 
	 */
	private boolean use_numbers;
	
	/**
	 * Variable to display only a specific polygon class to highlight
	 * if == 0 all classes are displayed
	 */
	private int highlight_no;
	
	/**
	 * @param stGraph graph to analyze
	 * @param use_numbers flag to display the numbers instead of the colors
	 * @param hightlight_no a specific polygon class number to be highlighted, put 0 to display all
	 */
	public PolygonClassOverlay(SpatioTemporalGraph stGraph, boolean use_numbers, int hightlight_no) {
		super("Polygon class",stGraph);
		this.use_numbers = use_numbers;
		this.highlight_no = hightlight_no;
	}
	
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i ) {

		g.setFont(new Font("TimesRoman", Font.PLAIN, 15));

		for(Node cell: frame_i.vertexSet())
		{

			if(cell.onBoundary())
				continue;

			Coordinate centroid = 
					cell.getCentroid().getCoordinate();

			int cell_degree = frame_i.degreeOf(cell);

			if(highlight_no != 0)
				if(cell_degree != highlight_no)
					continue;

			if(use_numbers){
				g.setColor(Color.white);
				g.drawString(Integer.toString(cell_degree), 
						(float)centroid.x - 2  , 
						(float)centroid.y + 2);
			}
			else{
				switch(cell_degree){
				case 3:
					g.setColor(new Color(232, 233, 41)); //yellow
					g.fill(cell.toShape());
					break;
				case 4:
					g.setColor(new Color(223, 0, 8)); //red
					g.fill(cell.toShape());
					break;
				case 5:
					g.setColor(new Color(84, 176, 26)); //green
					g.fill(cell.toShape());
					break;
				case 6:
					g.setColor(new Color(190, 190, 190)); //grey
					g.fill(cell.toShape());
					break;
				case 7:
					g.setColor(new Color(18, 51, 143)); //blue
					g.fill(cell.toShape());
					break;
				case 8:
					g.setColor(new Color(158, 53, 145)); //violet
					g.fill(cell.toShape());
					break;
				case 9:
					g.setColor(new Color(128, 45, 20)); //brown
					g.fill(cell.toShape());
					break;
				default:
					continue;
				}
			}
		}
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {

		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 2, 0, "Centroid y");
		XLSUtil.setCellString(sheet, 3, 0, "Polygon no");

		int row_no = 1;

		for(Node n: frame.vertexSet()){
			if(!n.onBoundary()){
				int neighbor_no = frame.degreeOf(n);

				XLSUtil.setCellNumber(sheet, 0, row_no, n.getTrackID());
				XLSUtil.setCellNumber(sheet, 1, row_no, n.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 2, row_no, n.getCentroid().getY());
				XLSUtil.setCellNumber(sheet, 3, row_no, neighbor_no);

				row_no++;
			}
		}
	}

	@Override
	public void specifyLegend(Graphics2D g, Double line)  {
		
		int binWidth = (int)((line.x2 - line.x1)/7);
		
		for(int i=0; i<7; i++){

			switch(i + 3){ 
			case 3:
				g.setColor(new Color(232, 233, 41)); //yellow
				break;
			case 4:
				g.setColor(new Color(223, 0, 8)); //red
				break;
			case 5:
				g.setColor(new Color(84, 176, 26)); //green
				break;
			case 6:
				g.setColor(new Color(190, 190, 190)); //grey
				break;
			case 7:
				g.setColor(new Color(18, 51, 143)); //blue
				break;
			case 8:
				g.setColor(new Color(158, 53, 145)); //violet
				break;
			case 9:
				g.setColor(new Color(128, 45, 20)); //brown
				break;
			default:
				continue;
			}

			int x = binWidth*i + (int)line.x1;
			int y = (int)line.y1;
			
			g.fillRect(x,y,binWidth,binWidth);
			g.setColor(Color.white);
			g.drawString(Integer.toString(i+3), 
					(float)x + 5, 
					(float)y + 15);

		}
		
	}
	
}
