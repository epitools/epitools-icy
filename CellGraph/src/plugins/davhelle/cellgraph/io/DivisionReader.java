/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.io;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import au.com.bytecode.opencsv.CSVReader;
import icy.canvas.IcyCanvas;
import icy.gui.dialog.LoadDialog;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * CSV File reader to add the division information to cells from
 * manually created tracking files [ROI based identification
 * done with FIJI]
 * 
 * 
 * @author Davide Heller
 *
 */
public class DivisionReader extends AbstractPainter{
	
	private SpatioTemporalGraph stGraph;
	HashMap<Integer,ArrayList<Point>> division_map;

	public DivisionReader(SpatioTemporalGraph spatioTemporalGraph) throws IOException {
		
		this.stGraph = spatioTemporalGraph;
		this.division_map = new HashMap<Integer,ArrayList<Point>>();
		
		String divisions_file = LoadDialog.chooseFile(
				"Please select division file to load...", 
				"/Users/davide/Dropbox/Mosaic/davide_mt/2012_05_16",
				"divisions_2012_05_16", ".xls");
	
		//TODO OpenCSV superfluous?
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
		
		reader.close();

	}
	
	public void assignDivisions(){
		
		//for all time points
		for(int i =0; i<stGraph.size(); i++){
			if(division_map.containsKey(i))
				for(Point division: division_map.get(i))
					for(Node cell: stGraph.getFrame(i).vertexSet())
						if(cell.getGeometry().contains(division)){
							//mark cell 
							cell.setObservedDivision(true);
							
							
							//use tracking information if present to propagate
							//information (given we are only interested into
							//seeing 
//							if(cell.getFirst() != null)
//								cell.getFirst().setObservedDivision(true);
//							else
							
							while(cell.getPrevious() != null){
								cell = cell.getPrevious();
								cell.setObservedDivision(true);
							}
						}
		}

	}
	
	public void backtrackDivisions(){
		//bruteforce backtrack strategy: given the cell center of the dividing cell
		//find the corresponding cell in the previous frame and propagate the 
		//division information
		
		//for all time points
		for(int i =0; i<stGraph.size(); i++){
			System.out.println("Processing divisions at frame "+i);
			if(division_map.containsKey(i))
				for(Point division: division_map.get(i))
					for(Node cell: stGraph.getFrame(i).vertexSet())
						if(cell.getGeometry().contains(division)){
							//mark cell that has divided 
							cell.setObservedDivision(true);
							
							//Search all previous frames for correspondence
							Point centroid = cell.getCentroid();
							for(int j=i-1; j>=0; j--)
								for(Node candidate: stGraph.getFrame(j).vertexSet())
									if(candidate.getGeometry().contains(centroid)){
										candidate.setObservedDivision(true);
										centroid = candidate.getCentroid();
									}
								
						}
		}
		
	}
	
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.red);
	
			for(Node cell: frame_i.vertexSet())
				if(cell.hasObservedDivision())
//					if(!cell.onBoundary()) TODO: apply correspondence to first frame!
						g.fill(cell.toShape());
			

			
		}
		
	}
	

}
