package plugins.davhelle.cellgraph.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Iterator;

import plugins.davhelle.cellgraph.graphs.DevelopmentType;
import plugins.davhelle.cellgraph.graphs.TissueGraph;
import plugins.davhelle.cellgraph.nodes.NodeType;
import icy.gui.dialog.SaveDialog;

public class CsvWriter {
	
	private String fileName;
	private DevelopmentType stGraph;
	private BufferedWriter out;
	

	public CsvWriter(DevelopmentType stGraph) {

		this.fileName = SaveDialog.chooseFile();
		
		System.out.println(fileName);
	}
	
	//TODO make static
	public boolean trackedArea(){
		
		// Create file for mosaic particle tracking
		try{
			
			FileWriter fstream = new FileWriter(fileName);
			out = new BufferedWriter(fstream);
			
			if(stGraph.hasTracking()){
				TissueGraph frame_0 = stGraph.getFrame(0);
				Iterator<NodeType> cell_it = frame_0.iterator();

				while(cell_it.hasNext()){
					NodeType cell = cell_it.next();

					while(cell.getNext() != null){
						out.write(Double.toString(cell.getGeometry().getArea())+",");
						cell = cell.getNext();
					}

					out.write("\n");

				}

			}

			out.close();
		}
		catch (Exception e){
			System.err.println("Error: " + e.getMessage());
		}
		
		return stGraph.hasTracking();
	}
	
	

}
