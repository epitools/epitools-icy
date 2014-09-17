package plugins.davhelle.cellgraph.painters;

import java.awt.Graphics2D;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;

public class ColorTagPainter extends AbstractGraphPainter {

	
	private ShapeWriter writer;
	
	public ColorTagPainter(SpatioTemporalGraph stGraph){
		super("Color tag", stGraph);
		this.writer = new ShapeWriter();
	}
	
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		for(Node cell: frame_i.vertexSet())
			if(cell.hasColorTag()){
				g.setColor(cell.getColorTag());
				g.fill(writer.toShape(cell.getGeometry()));
			}

	}

}
