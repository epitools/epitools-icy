/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
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
public class DivisionPainter extends Overlay {

	SpatioTemporalGraph stGraph;
	private boolean PLOT_DIVISIONS;
	private boolean PLOT_ELIMINATIONS;
	private boolean FILL_CELLS;
	
	public DivisionPainter(
			SpatioTemporalGraph stGraph,
			boolean PLOT_DIVSIONS,
			boolean PLOT_ELIMINATIONS,
			boolean FILL_CELLS){
		super("Divisions (green) and Eliminations (red)");
		this.stGraph = stGraph;
		this.PLOT_DIVISIONS = PLOT_DIVSIONS;
		this.PLOT_ELIMINATIONS = PLOT_ELIMINATIONS;
		this.FILL_CELLS = FILL_CELLS;
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas){
		
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size())
			paintFrame(g, time_point);
		
	}

	/**
	 * @param g
	 * @param time_point
	 */
	public void paintFrame(Graphics2D g, int time_point) {
		if(!FILL_CELLS)
			g.setStroke(new BasicStroke(3));
		
		FrameGraph frame_i = stGraph.getFrame(time_point);
		
		for(Node cell: frame_i.vertexSet()){
			if(cell.getFirst() != null){
				
				if(PLOT_DIVISIONS)
					if(cell.getFirst().hasObservedDivision()){
						if(cell.getDivision().isMother(cell))
							g.setColor(Color.blue);
						else
							g.setColor(Color.cyan);
						
						if(FILL_CELLS)
							g.fill(cell.toShape());
						else
							g.draw(cell.toShape());	
					}					
				if(PLOT_ELIMINATIONS)
					if(cell.getFirst().hasObservedElimination()){
						if(PLOT_DIVISIONS && cell.getFirst().hasObservedDivision())
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
		
		if(!FILL_CELLS)
			g.setStroke(new BasicStroke(1));
	}
}
