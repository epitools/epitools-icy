package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.Graphics2D;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;

public abstract class AbstractGraphPainter extends Overlay{

	private SpatioTemporalGraph stGraph;
	
	public AbstractGraphPainter(String name, SpatioTemporalGraph stGraph) {
		super(name);
		this.stGraph = stGraph;	
	}

	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			FrameGraph frame_i = stGraph.getFrame(time_point);
			paintFrame(g, frame_i);
		}
    }

	public abstract void paintFrame(Graphics2D g, FrameGraph frame_i);
}
