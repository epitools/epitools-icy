package plugins.davhelle.cellgraph.overlays;

import icy.util.XLSUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;
import java.util.Iterator;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Elimination;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Painter to visualize all cells that will divide or be eliminated
 * over the length of the movie. To solve the ambiguity of cells
 * undergoing both events (i.e. a cell divides and then loses
 * one of the siblings by elimination) a different color scheme
 * is used (see final static fields below)
 * 
 * @author Davide Heller
 *
 */
public class DivisionOverlay extends StGraphOverlay {

	//Visualization FLAGS
	/**
	 * Visualization flag whether to plot dividing cells
	 */
	private boolean PLOT_DIVISIONS;
	/**
	 * Visualization flag whether to plot eliminated cells
	 */
	private boolean PLOT_ELIMINATIONS;
	/**
	 * Visualization flag whether to fill or outline the cells
	 */
	private boolean FILL_CELLS;
	/**
	 * Visualization flag whether to connect daughter cells by a line
	 */
	private boolean CONNECT_DAUGHTER_CELLS;

	//Colorings
	/**
	 * Color for dividing cells
	 */
	private static final Color DIVIDING_CELL = Color.blue;
	/**
	 * Color for daughter/Children cells
	 */
	private static final Color DAUGHTER_CELL = Color.cyan;
	/**
	 * Color for eliminated cells
	 */
	private static final Color ELIMINATED_CELL = Color.red;
	/**
	 * Color for dividing and eliminated children cells
	 */
	private static final Color DIVIDING_AND_ELIMINATED_CELL = Color.magenta;
	
	/**
	 * Description String for GUI
	 */
	public static final String DESCRIPTION = 
			"Highlights the cells that underwent division or elimination<br/>" +
			" during the time lapse.<br/><br/>" +
			"Color code:<br/><ul>" +
			"<li>[blue] dividing cell" +
			"<li>[cyan] daughter cell" +
			"<li>[red] eliminated cell" +
			"<li>[magenta] elimination after division</ul>";
	
	/**
	 * @param stGraph graph to analyze
	 * @param PLOT_DIVSIONS flag to plot dividing cells
	 * @param PLOT_ELIMINATIONS flag to plot eliminated cells
	 * @param FILL_CELLS flag to fill or outline cells with color
	 * @param CONNECT_DAUGHTER_CELLS flag to connect daughter cells with a colored line segment
	 */
	public DivisionOverlay(
			SpatioTemporalGraph stGraph,
			boolean PLOT_DIVSIONS,
			boolean PLOT_ELIMINATIONS,
			boolean FILL_CELLS,
			boolean CONNECT_DAUGHTER_CELLS){
		super("Divisions and/or Eliminations",stGraph);
		this.PLOT_DIVISIONS = PLOT_DIVSIONS;
		this.PLOT_ELIMINATIONS = PLOT_ELIMINATIONS;
		this.FILL_CELLS = FILL_CELLS;
		this.CONNECT_DAUGHTER_CELLS = CONNECT_DAUGHTER_CELLS;
	}
	
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		if(!FILL_CELLS)
			g.setStroke(new BasicStroke(3));
		
		for(Node cell: frame_i.vertexSet()){
			if(cell.getFirst() != null){
				
				if(PLOT_DIVISIONS){
					if(cell.hasObservedDivision()){
						if(cell.getDivision().isMother(cell))
							g.setColor(DIVIDING_CELL);
						else
							g.setColor(DAUGHTER_CELL);
						
						
						if(FILL_CELLS)
							g.fill(cell.toShape());
						else
							g.draw(cell.toShape());
						
						//Connect daughter cells if possible
						if(CONNECT_DAUGHTER_CELLS){
							Division d = cell.getDivision();
							if(d.getTimePoint() <= frame_i.getFrameNo()){
								int id1 = d.getChild1().getTrackID();
								int id2 = d.getChild2().getTrackID();

								if(frame_i.hasTrackID(id1) && frame_i.hasTrackID(id2)){

									Node c1 = frame_i.getNode(id1);
									Node c2 = frame_i.getNode(id2);

									//draw a line connecting the daughter cells
									Color old = g.getColor();
									if(FILL_CELLS)
										g.setColor(Color.BLACK);

									g.setStroke(new BasicStroke(1));
									g.drawLine(
											(int)c1.getGeometry().getCentroid().getX(),
											(int)c1.getGeometry().getCentroid().getY(), 
											(int)c2.getGeometry().getCentroid().getX(), 
											(int)c2.getGeometry().getCentroid().getY());
									g.setStroke(new BasicStroke(3));

									if(FILL_CELLS)
										g.setColor(old);

								}
							}
						}
					}
				}
				
				if(PLOT_ELIMINATIONS){
					
					if(PLOT_DIVISIONS && cell.hasObservedDivision()){
						if(cell.getDivision().isMother(cell))
							if( cell.getDivision().getChild1().hasObservedElimination() ||
								cell.getDivision().getChild2().hasObservedElimination()){
								g.setColor(DIVIDING_AND_ELIMINATED_CELL);
								
								if(FILL_CELLS)
									g.fill(cell.toShape());
								else
									g.draw(cell.toShape());
							}
					}
					
					if(cell.hasObservedElimination()){
						if(PLOT_DIVISIONS && cell.hasObservedDivision())
							g.setColor(DIVIDING_AND_ELIMINATED_CELL);
						else
							g.setColor(ELIMINATED_CELL);
						
						if(FILL_CELLS)
							g.fill(cell.toShape());
						else
							g.draw(cell.toShape());
					}	
				}
			}
		}
		
		if(!FILL_CELLS)
			g.setStroke(new BasicStroke(1));
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		XLSUtil.setCellString(sheet, 0, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 1, 0, "Centroid y");
		XLSUtil.setCellString(sheet, 2, 0, "EVENT Type");

		int row_no = 1;
		
		if(PLOT_DIVISIONS){
			Iterator<Division> divisions = frame.divisionIterator();
			while(divisions.hasNext()){
				Division d = divisions.next();

				Node mother = d.getMother();
				XLSUtil.setCellNumber(sheet, 0, row_no, mother.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 1, row_no, mother.getCentroid().getY());
				XLSUtil.setCellString(sheet, 2, row_no, "DIVISION");

				row_no++;

			}
		}
		
		if(PLOT_ELIMINATIONS){
			Iterator<Elimination> eliminations = frame.eliminationIterator();
			while(eliminations.hasNext()){
				Elimination e = eliminations.next();

				Node eliminatedCell = e.getCell();
				XLSUtil.setCellNumber(sheet, 0, row_no, eliminatedCell.getCentroid().getX());
				XLSUtil.setCellNumber(sheet, 1, row_no, eliminatedCell.getCentroid().getY());
				XLSUtil.setCellString(sheet, 2, row_no, "ELIMINATION");

				row_no++;

			}
		}
	}

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		
		String s = "";
		Color c = null;
		int offset = 0;
		
		if(PLOT_DIVISIONS){

			s = "Dividing cell";
			c = Color.BLUE;
			offset = 0;

			OverlayUtils.stringColorLegend(g, line, s, c, offset);

			s = "Daughter cell";
			c = Color.CYAN;
			offset = 20;

			OverlayUtils.stringColorLegend(g, line, s, c, offset);
		}

		if(PLOT_ELIMINATIONS){
			s = "Eliminated cell";
			c = Color.RED;
			offset = 40;

			OverlayUtils.stringColorLegend(g, line, s, c, offset);

			if(PLOT_DIVISIONS){
				s = "Dividing & Eliminated cell";
				c = Color.MAGENTA;
				offset = 60;

				OverlayUtils.stringColorLegend(g, line, s, c, offset);
			}
		}

		
	}
}
