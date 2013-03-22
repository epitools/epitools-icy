package plugins.davhelle.cellgraph.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.itk.simple.GeodesicActiveContourLevelSetImageFilter;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;

import au.com.bytecode.opencsv.CSVReader;
import icy.canvas.IcyCanvas;
import icy.gui.dialog.LoadDialog;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;
import plugins.davhelle.cellgraph.graphs.DevelopmentType;
import plugins.davhelle.cellgraph.graphs.TissueGraph;
import plugins.davhelle.cellgraph.nodes.NodeType;

/**
 * Csv File reader to add the division information to cells from
 * manually created tracking files [ROI based identification
 * done with FIJI]
 * 
 * 
 * @author Davide Heller
 *
 */
public class DivisionReader extends AbstractPainter{
	
	private DevelopmentType stGraph;
	HashMap<Integer,ArrayList<Point>> division_map;

	public DivisionReader(DevelopmentType spatioTemporalGraph) throws IOException {
		
		this.stGraph = spatioTemporalGraph;
		this.division_map = new HashMap<Integer,ArrayList<Point>>();
		
		String divisions_file = LoadDialog.chooseFile(
				"Please select division file to load...", 
				"/Users/davide/Dropbox/Mosaic/davide_mt/2012_05_16",
				"divisions", ".xls");
	
		
		CSVReader reader = new CSVReader(new FileReader(divisions_file), '\t');
		GeometryFactory factory = new GeometryFactory();
		String [] nextLine = reader.readNext();
		
		while ((nextLine = reader.readNext()) != null) {
			
			//System.out.println(nextLine[5]+","+nextLine[6]+","+nextLine[9]);
			double x = Double.parseDouble(nextLine[5]);
			double y = Double.parseDouble(nextLine[6]);
			Point division = factory.createPoint(new Coordinate(x,y));
			
			//assign to right time point
			int t = Integer.parseInt(nextLine[9]) - 1;
			if(!division_map.containsKey(t)){
				division_map.put(t, new ArrayList<Point>());
			}
			division_map.get(t).add(division);
			//TODO see whether a multipoint Geometry might be more convenient to be used
		}
		
		assignDivisions();

	}
	
//	private void readDivisions(){
//		
//	}
	
	private void assignDivisions(){
		
		//for all time points
		for(int i =0; i<stGraph.size(); i++){
			if(division_map.containsKey(i))
				for(Point division: division_map.get(i))
					for(NodeType cell: stGraph.getFrame(i).vertexSet())
						if(cell.getGeometry().contains(division)){
							//mark cell and all previous associated time points
							cell.setDivisionFlag(true);
							while(cell.getLast() != null){
								cell = cell.getLast();
								cell.setDivisionFlag(true);
							}
						}
		}

	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		if(time_point < stGraph.size()){
			
			TissueGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.red);
	
			for(NodeType cell: frame_i.vertexSet())
				if(cell.willDivide())
					g.fill(cell.toShape());

			
		}
		
	}
	

}
