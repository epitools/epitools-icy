/**
 * 
 */
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import ij.process.EllipseFitter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.HashMap;

import com.vividsolutions.jts.algorithm.Angle;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.EllipseFitGenerator;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * @author Davide Heller
 *
 */
public class ElongationRatioOverlay extends Overlay{

	private HashMap<Node, EllipseFitter> fittedEllipses;
	private SpatioTemporalGraph stGraph;

	
	public ElongationRatioOverlay(SpatioTemporalGraph spatioTemporalGraph) {
		super("EllipseRatio Coloring");

		stGraph = spatioTemporalGraph;
		fittedEllipses = new EllipseFitGenerator(stGraph).getFittedEllipses();
	
	}

	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		double[] heat_map = {0.0,0.25,0.5,0.75,1.0};
		
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();
		
		int fontSize = 3;
		boolean show_only_divsion=false;
		
		g.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
		if(time_point < stGraph.size()){

			for(Node n: stGraph.getFrame(time_point).vertexSet()){

				if(show_only_divsion)
					if(!n.hasObservedDivision())
						continue;

				EllipseFitter ef = fittedEllipses.get(n);
				double elongation_ratio = ef.major/ef.minor;

				double normalized_ratio = (elongation_ratio - 1.0)/3.0;

				Color hsbColor = Color.getHSBColor(
						(float)(normalized_ratio*0.9 + 0.4		),
						1f,
						1f);

				g.setColor(hsbColor);
				
				if(show_only_divsion)
					g.draw((n.toShape()));
				else
					g.fill((n.toShape()));

				g.setColor(Color.black);
				g.drawString(String.format("%.1f",
						elongation_ratio), 
						(float)n.getCentroid().getX(), 
						(float)n.getCentroid().getY());
			}

		}
    }
}