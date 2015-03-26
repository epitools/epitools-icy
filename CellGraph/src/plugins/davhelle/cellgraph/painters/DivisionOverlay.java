/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.util.XLSUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
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
 * is used (see below)
 * 
 * dividing cell - green
 * eliminated cell - red
 * both events - yellow
 * 
 * @author Davide Heller
 *
 */
public class DivisionOverlay extends StGraphOverlay {

	private boolean PLOT_DIVISIONS;
	private boolean PLOT_ELIMINATIONS;
	private boolean FILL_CELLS;
	
	public static final String DESCRIPTION = 
			"Highlights the cells that underwent division or elimination during the time lapse.\n\n" +
			"Color code:\n" +
			"* [blue] dividing cell\n" +
			"* [cyan] daughter cell\n" +
			"* [red] eliminated cell\n" +
			"* [yellow] dividing cell of which at least one daughter cell is eliminated";
	
	public DivisionOverlay(
			SpatioTemporalGraph stGraph,
			boolean PLOT_DIVSIONS,
			boolean PLOT_ELIMINATIONS,
			boolean FILL_CELLS){
		super("Divisions (green) and Eliminations (red)",stGraph);
		this.PLOT_DIVISIONS = PLOT_DIVSIONS;
		this.PLOT_ELIMINATIONS = PLOT_ELIMINATIONS;
		this.FILL_CELLS = FILL_CELLS;
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
							g.setColor(Color.blue);
						else
							g.setColor(Color.cyan);
						
						if(FILL_CELLS)
							g.fill(cell.toShape());
						else
							g.draw(cell.toShape());	
					}
				}
				
				if(PLOT_ELIMINATIONS){
					
					if(PLOT_DIVISIONS && cell.hasObservedDivision()){
						if(cell.getDivision().isMother(cell))
							if( cell.getDivision().getChild1().hasObservedElimination() ||
								cell.getDivision().getChild2().hasObservedElimination()){
								g.setColor(Color.yellow);
								
								if(FILL_CELLS)
									g.fill(cell.toShape());
								else
									g.draw(cell.toShape());
							}
					}
					
					if(cell.hasObservedElimination()){
						if(PLOT_DIVISIONS && cell.hasObservedDivision())
							g.setColor(Color.yellow);
						else
							g.setColor(Color.red);
						
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
}
