package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Point;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

public class CentroidPainter extends AbstractPainter{

	private SpatioTemporalGraph stGraph;
	private ShapeWriter writer;
	
	public CentroidPainter(SpatioTemporalGraph spatioTemporalGraph){
		this.stGraph = spatioTemporalGraph;
		this.writer = new ShapeWriter();
		
	}

	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		if(time_point < stGraph.size()){
			//TODO include 3D information!
			//TODO possible performance improvement if map<Node,Point> is created
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.blue);
	
			for(Node cell: frame_i.vertexSet()){
				Point centroid = cell.getCentroid();
				g.fillOval((int)centroid.getX(), (int)centroid.getY(), 2, 2);
			}
				

			
		}
    }
}
