package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.DevelopmentType;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;

public class PolygonClassPainter extends AbstractPainter{
	
	private DevelopmentType stGraph;
	
	
	public PolygonClassPainter(DevelopmentType stGraph) {
		// TODO Auto-generated constructor stub
		
		this.stGraph = stGraph;
	}

	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.white);
			
			g.setFont(new Font("TimesRoman", Font.PLAIN, 8));
			
			for(Node cell: frame_i.vertexSet()){

//				if(cell.onBoundary())
//					g.setColor(Color.white);
//				else
					

				//Fill cell shape
				//if(!cell.onBoundary())
				g.draw(cell.toShape());
				
				Coordinate centroid = 
						cell.getCentroid().getCoordinate();
				
				g.drawString(Integer.toString(frame_i.degreeOf(cell)), 
						(float)centroid.x - 2  , 
						(float)centroid.y + 2);

			}
		}
	}
	
}
