/**
 * 
 */
package headless;

import java.io.File;
import java.util.Iterator;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.CsvWriter;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Save the evolution of children's polyclass
 * 
 * @author Davide Heller
 *
 */
public class FollowChildrenPolyClass {

	public static void main(String[] args) {
		
		int neo_no = 0;

		SpatioTemporalGraph stGraph = LoadNeoWtkFiles.loadNeo(neo_no);
		
		StringBuilder builder = new StringBuilder();
		
		for(int i=0; i< stGraph.size(); i++){
			FrameGraph frame = stGraph.getFrame(i);
			
			Iterator<Division> divisions = frame.divisionIterator();
			while(divisions.hasNext()){
				Division d = divisions.next();
				addSequentialNodeDegree(d.getChild1(),builder);
				addSequentialNodeDegree(d.getChild2(),builder);
			}
		}

		//Save builder to CSV file
		String file_name = String.format("neo%d_children_polyclass.csv", neo_no);
		File output_file = new File("/Users/davide/tmp/"+file_name);
		CsvWriter.writeOutBuilder(builder, output_file);
		
		System.out.println("Successfully wrote: "+file_name);
	}
	
	private static void addSequentialNodeDegree(Node vertex, StringBuilder builder) {
		
		//Start line with cell id
		int t = 1;
		builder.append(vertex.getTrackID());
		
		for(;t < vertex.getBelongingFrame().getFrameNo();t++)
			builder.append(",NA");
		
		//Reached Division frame
		builder.append(',');
		builder.append(vertex.getNeighbors().size());

		//Register Next frames
		Node cell = vertex;
		while(cell.hasNext()){
			cell = cell.getNext();
			int t_new = cell.getBelongingFrame().getFrameNo();
			
			//in case of missing values add previous value
			for(;t < t_new - 1; t++)
				builder.append(",NA");
			
			builder.append(',');
			builder.append(cell.getNeighbors().size());
			t++;	
		}
		
		//Fill up end
		for(;t < 99; t++)
			builder.append(",NA");
			
		builder.append('\n');
		
	}

}
