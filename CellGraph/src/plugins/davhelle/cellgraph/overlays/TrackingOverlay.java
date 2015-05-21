package plugins.davhelle.cellgraph.overlays;

import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Point;

/**
 * Given the first frame as reference, a random color
 * is assigned to every cell and is maintained for all the
 * linked cells in the successive frames analyzed. 
 * 
 * @author Davide Heller
 *
 */
public class TrackingOverlay extends StGraphOverlay{
	
	/**
	 * Description string for GUI
	 */
	public static final String DESCRIPTION = 
			"Overlay to review the tracking in case the default " +
			"Overlay has been eliminated or to highlight different aspects\n\n" +
			"The TrackColor that defines uniquely every cell can be used to" +
			" either outline or fill the cell polygon." +
			" The additional information is added complementarily.\n\n" +
			"Additional color codes:\n" +
			"* [red] cell missing in previous frame\n" +
			"* [yellow] cell missing in next frame\n" +
			"* [green] cell missing in previous&next\n" +
			"* [blue] cell dividing in next frame\n" +
			"* [magenta] brother cell missing\n" +
			"* [cyan] cell eliminated in next frame\n" +
			"* [gray] brother cell was eliminated\n";
	
	/**
	 * A Random bright color for each cell
	 */
	private HashMap<Node,Color> correspondence_color;
	/**
	 * A static color map for each error type
	 */
	private static final Map<Integer, Color> errorMap;
	static{
		Map<Integer, Color> aMap = new HashMap<Integer, Color>();
		aMap.put(-2, Color.red);		//cell missing in previous frame
		aMap.put(-3, Color.yellow);		//cell missing in next frame
		aMap.put(-4, Color.green);		//cell missing in previous&next
		aMap.put(-5, Color.blue);		//cell dividing in next frame
		aMap.put(-6, Color.magenta);	//brother cell missing
		aMap.put(-7, Color.cyan);   	//cell eliminated in next frame
		aMap.put(-8, Color.lightGray); 	//brother cell was eliminated
		errorMap = Collections.unmodifiableMap(aMap);
	}
	
	/**
	 * Flag whether to highlight mistakes or fill the cells
	 */
	private boolean highlightMistakes;
	/**
	 * Flag to show frame related tracking statistics
	 */
	private boolean SHOW_STATISTICS = false;

	/**
	 * Initializes the Tracking overlay
	 * 
	 * @param stGraph graph to be analyzed
	 * @param highlightMistakes set true for tracking events to be highlighted/ false to fill cells with their assigned color
	 */
	public TrackingOverlay(SpatioTemporalGraph stGraph, Boolean highlightMistakes) {
		super("Cell Tracking Color",stGraph);
		
		//Color for each cell line
		this.correspondence_color = new HashMap<Node,Color>();
		this.stGraph = stGraph;
		this.highlightMistakes = highlightMistakes.booleanValue();
		
		//Assign color to cell line starting from first cell
		Iterator<Node> cell_it = stGraph.getFrame(0).iterator();
		Random rand = new Random();
		
		//Assign to every first cell a random color, also 
		//attach the same color to ev. children
		while(cell_it.hasNext()){
			
			Node cell = cell_it.next();
			Color cell_color = newColor(rand);
			correspondence_color.put(cell, cell_color);
			
			if(cell.hasObservedDivision()){
				Division division = cell.getDivision();
				//same color for children or cell_color = newColor(rand);
				correspondence_color.put(division.getChild1(),cell_color);
				correspondence_color.put(division.getChild2(),cell_color);
			}

		}
	}
	
	/**
	 * Generate random color for cell
	 * 
	 * @param rand Random number generator
	 * @return random bright color
	 */
	private Color newColor(Random rand){
		float r_idx = rand.nextFloat();
		float g_idx = rand.nextFloat();
		float b_idx = rand.nextFloat();      

		Color cell_color = new Color(r_idx, g_idx, b_idx);
		cell_color.brighter();
		
		
		return cell_color;
	}
	
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame)
	{

		double percentage_tracked = 0;

		for(Node cell: frame.vertexSet()){

			if(cell.getTrackID() != -1){
				if(correspondence_color.containsKey(cell.getFirst())){
					percentage_tracked++;
					//cell is part of registered correspondence
					g.setColor(correspondence_color.get(cell.getFirst()));

					if(highlightMistakes)
						g.draw(cell.toShape());
					else
						g.fill(cell.toShape());


					Point lost = cell.getCentroid();

					if(errorMap.containsKey(cell.getErrorTag())){
						g.setColor(errorMap.get(cell.getErrorTag()));

						if(highlightMistakes)
							g.fill(cell.toShape());
						else
							g.draw(cell.toShape());

						g.drawOval((int)lost.getX(),(int)lost.getY(), 5, 5);
					}

				}
				else{
					//no tracking found
					g.setColor(Color.white);
					g.draw(cell.toShape());

					Point lost = cell.getCentroid();

					if(errorMap.containsKey(cell.getErrorTag())){

						g.setColor(errorMap.get(cell.getErrorTag()));

						if(highlightMistakes)
							g.fill(cell.toShape());
						else
							g.draw(cell.toShape());

						g.drawOval((int)lost.getX(),(int)lost.getY(), 5, 5);
					}
				}
			}
			else{
				//Mark cells in green which do have all neighbors tracked
				//and are not on the boundary
				if(!cell.onBoundary()){
					g.setColor(Color.green);

					boolean all_assigned = true;
					for(Node neighbor: cell.getNeighbors())
						if(neighbor.getTrackID() == -1)
							all_assigned = false;

					if(all_assigned)
						g.fill(cell.toShape());
				}
			}
		}

		percentage_tracked = (percentage_tracked/stGraph.getFrame(0).size())*100;

		
		//Statistics Text headline
		if(SHOW_STATISTICS){
			g.setFont(new Font("TimesRoman", Font.PLAIN, 10));

			g.setColor(Color.white);
			g.drawString("Tracked cells: "+(int)percentage_tracked+"%", 10 , 20);

			g.setColor(Color.red);
			g.drawString("previous", 10 , 30);

			g.setColor(Color.yellow);
			g.drawString("next", 60 , 30);

			g.setColor(Color.green);
			g.drawString("none", 90 , 30);
		}
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 2, 0, "Centroid y");
		XLSUtil.setCellString(sheet, 3, 0, "Cell area");

		int row_no = 1;
		for(Node node: frame.vertexSet()){
			if(node.hasNext() || node.hasPrevious()){
				XLSUtil.setCellNumber(sheet, 0, row_no, node.getTrackID());
				XLSUtil.setCellNumber(sheet, 1, row_no, node.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 2, row_no, node.getCentroid().getY());
				XLSUtil.setCellNumber(sheet, 3, row_no, node.getGeometry().getArea());
				row_no++;
			}
		}

	}

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		
		String s = "Missing in previous frame";
		Color c = Color.RED;
		int offset = 0;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);

		s = "Missing in next frame";
		c = Color.YELLOW;
		offset = 20;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);
		
		s = "Missing in previous&next";
		c = Color.GREEN;
		offset = 40;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);
		
		s = "Dividing in next frame";
		c = Color.BLUE;
		offset = 60;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);
		
		s = "Sibling missing";
		c = Color.MAGENTA;
		offset = 80;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);
		
		s = "Eliminated in next frame";
		c = Color.CYAN;
		offset = 100;

		OverlayUtils.stringColorLegend(g, line, s, c, offset);
		
	}

}
