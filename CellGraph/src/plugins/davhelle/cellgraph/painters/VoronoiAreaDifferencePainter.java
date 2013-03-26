package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.PolygonUtils;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * AreaDifferencePainter computes and depicts the difference between 
 * cell areas and the correspondent Voronoi cell areas. 
 * 
 * Currently the cells with a positive difference are shaded in green 
 * (i.e. bigger than voronoi) and the ones with a negative difference
 * in red. Neutral (+/- THRESHOLD) are left white. 
 * 
 *  TODO Bug in the double int conversion, check area computation as well
 * 
 * @author Davide Heller
 *
 */
public class VoronoiAreaDifferencePainter extends AbstractPainter{
	
	private final int DIFFERENCE_THRESHOLD = 10;
	
	private Map<Node, Double> area_difference_map;
	private Map<Node, Color> color_map;

	private double color_amplification;
	private int color_scheme;
	private double alpha_level;
	
	private ShapeWriter writer;
	private SpatioTemporalGraph stGraph;
	
	
	public VoronoiAreaDifferencePainter(SpatioTemporalGraph stGraph, Map<Node,Double> area_difference_map) {
		this.stGraph = stGraph;
		this.writer = new ShapeWriter();
		this.area_difference_map = area_difference_map;

		this.color_scheme = 2;
		this.color_amplification = 5;
		this.alpha_level = 0.35;
		
		this.color_map = new HashMap<Node, Color>();
		defineColorMap();
		
	}

	//Color scheme generation
	private void defineColorMap(){
		
		//for every frame and node
		for(int i=0; i<stGraph.size(); i++){

			for(Node cell: stGraph.getFrame(i).vertexSet()){
				
				if(area_difference_map.containsKey(cell)){

					double area_difference = area_difference_map.get(cell);

					if(color_scheme == 1){			
						//Color cells according to threshold into three categories
						double area_threshold = DIFFERENCE_THRESHOLD;
						if(area_difference > area_threshold)
							color_map.put(cell,Color.GREEN);
						else if(area_difference < area_threshold*(-1))
							color_map.put(cell,Color.RED);
						else
							color_map.put(cell,Color.WHITE);
					}
					else{		
						//color scheme which allows +/- 255 differences (255 being the MAX_DIFF)
						//magenta (negative diff.) to white (neutral) to light blue (positive)
						int intensity = 255 - (int)Math.min(
								color_amplification*Math.abs(area_difference), 255);
						if(area_difference > 0)
							color_map.put(cell,new Color(intensity,255,255));
						else
							color_map.put(cell,new Color(255,intensity,255));
					}
				}
			}
		}
	}
	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		//Set layer to 0.3 opacity
		Layer current_layer = canvas.getLayer(this);
		current_layer.setAlpha((float)alpha_level);		
		
		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			
			for(Node cell: frame_i.vertexSet()){
				if(!cell.onBoundary()){
					if(color_map.containsKey(cell)){
						g.setColor(color_map.get(cell));
						g.fill(cell.toShape());
					}
				}
			}
		}
    }
}

