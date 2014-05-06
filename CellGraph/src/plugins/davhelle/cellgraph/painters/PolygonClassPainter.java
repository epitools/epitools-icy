/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;

public class PolygonClassPainter extends Overlay{
	
	private SpatioTemporalGraph stGraph;
	private boolean use_numbers;
	private int highlight_no;
	
	
	public PolygonClassPainter(SpatioTemporalGraph stGraph, boolean use_numbers, int hightlight_no) {
		super("Polygon class");
		this.stGraph = stGraph;
		this.use_numbers = use_numbers;
		this.highlight_no = hightlight_no;
	}

	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			
			g.setFont(new Font("TimesRoman", Font.PLAIN, 12));
			
			for(Node cell: frame_i.vertexSet()){
				
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
					switch(cell_degree){ //Colors following Ishihara, 2013
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
	}
	
}
