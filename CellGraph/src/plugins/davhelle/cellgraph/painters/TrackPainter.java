package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import com.vividsolutions.jts.geom.Point;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Given the first frame as reference, a random color
 * is assigned to every cell and is maintained for all the
 * linked cells in the successive frames analyzed. 
 * 
 * @author Davide Heller
 *
 */
public class TrackPainter extends AbstractPainter{
	
	private SpatioTemporalGraph stGraph;
	private HashMap<Node,Color> correspondence_color;
	private Map<Integer, Color> errorMap;
	private boolean highlightMistakes;

	public TrackPainter(SpatioTemporalGraph stGraph, Boolean highlightMistakes) {
		
		//Color for each lineage
		this.correspondence_color = new HashMap<Node,Color>();
		this.stGraph = stGraph;
		this.highlightMistakes = highlightMistakes.booleanValue();
		
		//Assign color to cell lineages starting from first cell
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
		
		//define the color map for error indication
		this.errorMap = new HashMap<Integer, Color>();
		errorMap.put(-2, Color.red);	//missing previous
		errorMap.put(-3, Color.yellow);	//missing next
		errorMap.put(-4, Color.green);	//missing both
		errorMap.put(-5, Color.blue);	//dividing in next frame
		errorMap.put(-6, Color.magenta);//brother cell missing
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
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();
		
		if(time_point < stGraph.size()){
			
			double percentage_tracked = 0;
			
			for(Node cell: stGraph.getFrame(time_point).vertexSet()){
				
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
			}
			
			percentage_tracked = (percentage_tracked/stGraph.getFrame(0).size())*100;
			
			//Text headline
//			g.setFont(new Font("TimesRoman", Font.PLAIN, 10));
//			
//			g.setColor(Color.white);
//			g.drawString("Tracked cells: "+(int)percentage_tracked+"%", 10 , 20);
//			
//			g.setColor(Color.red);
//			g.drawString("previous", 10 , 30);
//			
//			g.setColor(Color.yellow);
//			g.drawString("next", 60 , 30);
//			
//			g.setColor(Color.green);
//			g.drawString("none", 90 , 30);
		}
	}

}
