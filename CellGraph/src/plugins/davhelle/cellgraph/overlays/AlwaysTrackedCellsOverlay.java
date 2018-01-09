package plugins.davhelle.cellgraph.overlays;

import icy.util.XLSUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;
import java.util.ArrayList;
import java.util.Iterator;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Painter to visualize all cells that have contiguously tracked
 * for all tracked time points or at least until they divided.  
 * 
 * @author Davide Heller
 * 
 */
public class AlwaysTrackedCellsOverlay extends StGraphOverlay {
	
	/**
	 * Description string for GUI use
	 */
	public static final String DESCRIPTION = 
			"Contours in ORANGE only the cells that have been<br/>" +
			"continuously tracked throughout the time lapse";
	
	/**
	 * List of nodes are continuously tracked and will be highlighted
	 */
	private ArrayList<Node> nodesToBeHighlighted;

	/**
	 * Compute the nodes that are continuously tracked
	 * 
	 * @param stGraph graph to be analyzed
	 */
	public AlwaysTrackedCellsOverlay(SpatioTemporalGraph stGraph) {
		super("Always tracked cells", stGraph);
		nodesToBeHighlighted = new ArrayList<Node>();
		
		if(stGraph.hasTracking()){
			
			Iterator<Node> cell_it = stGraph.getFrame(0).iterator();

			while(cell_it.hasNext()){
				
				//retrieve cell from the first frame
				Node cell = cell_it.next();
				boolean fully_tracked = true;
				
				//check if the cell has a continuous tracking over all frames
				for(int t=1; t < stGraph.size(); t++){

					//check existence in next time point
					if(cell.hasNext()){
						
						//get successive time point
						cell = cell.getNext();
						
						//check if frame is contiguous in time
						if(cell.getBelongingFrame().getFrameNo() != t){
							System.out.println("cell "+cell.getTrackID()+" is not contiguous at frame "+t);
							fully_tracked = false;
							break;
						}
					}
					else{
						
						//test if tracking end is due to a division
						if(cell.hasObservedDivision()){
							if(cell.getDivision().getTimePoint() == t){
								
								//check if children were correctly tracked too
								Division d = cell.getDivision();
								if(checkContiguousTracking(d.getChild1(), t, stGraph.size()))
									nodesToBeHighlighted.add(d.getChild1());
								if(checkContiguousTracking(d.getChild2(), t, stGraph.size()))
									nodesToBeHighlighted.add(d.getChild2());

								//the mother node is considered fully tracked if 
								//he has been contiguously tracked till the division point
								break;
							}
						}

						else{		
							//if the cell is simply lost the tracking feature is false
							fully_tracked = false;
							break;
						}	
					}
				}
				
				//check if cell has been tracked till the last frame
				if(fully_tracked)
					nodesToBeHighlighted.add(cell.getFirst());

			}
			
			
		}
		
	}
	
	/**
	 * Helper method to check whether a cell is contiguously tracked in 
	 * time within a specified time range.
	 * 
	 * @param cell		cell whose tracking information should be checked
	 * @param tStart	starting time point
	 * @param tEnd		ending time point
	 * @return			true if the cell has been tracked for all time points between tStart and tEnd
	 */
	private boolean checkContiguousTracking(Node cell, int tStart, int tEnd){
		
		//set the time counter
		int t = tStart;

		while(cell.hasNext()){
			if(cell.getBelongingFrame().getFrameNo() != t)
				return false;
			else{
				t++;
				cell = cell.getNext();
			}
		}

		if(t + 1 == tEnd)
			return true;
		else
			return false;
			
	}
	
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame)
	{
		g.setColor(Color.orange);
		g.setStroke(new BasicStroke(3));
		
		for(Node cell: frame.vertexSet())
			if(nodesToBeHighlighted.contains(cell.getFirst()))
				g.draw(cell.toShape());
		
		g.setStroke(new BasicStroke(1));
		
	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Always tracked");

		int row_no = 1;
		for(Node node: frame.vertexSet()){
			if(nodesToBeHighlighted.contains(node.getFirst())){
				XLSUtil.setCellNumber(sheet, 0, row_no, node.getTrackID());
				if(nodesToBeHighlighted.contains(node))
					XLSUtil.setCellString(sheet, 1, row_no, "TRUE");
				else
					XLSUtil.setCellString(sheet, 1, row_no, "FALSE");
				row_no++;
			}
		}

	}

	@Override
	public void specifyLegend(Graphics2D g, Double line) {

		OverlayUtils.stringColorLegend(g, line, "Continuosly tracked cells", Color.orange, 0);
		
	}
	
}
