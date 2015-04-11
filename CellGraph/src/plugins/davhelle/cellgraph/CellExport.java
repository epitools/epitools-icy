package plugins.davhelle.cellgraph;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JSeparator;

import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.export.BigXlsExporter;
import plugins.davhelle.cellgraph.export.ExportEnum;
import plugins.davhelle.cellgraph.export.ExportFieldType;
import plugins.davhelle.cellgraph.export.GraphExporter;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.CsvTrackWriter;
import plugins.davhelle.cellgraph.io.PdfPrinter;
import plugins.davhelle.cellgraph.io.SkeletonWriter;
import plugins.davhelle.cellgraph.io.WktPolygonExporter;

public class CellExport extends EzPlug {

	private EzVarEnum<ExportEnum> varExport;
	private EzVarSequence varSequence;
	private EzVarBoolean varTagExport;
//	private EzVarEnum<ExportFieldType> varExportType;
//	private EzVarInteger varFrameNo;
	
	@Override
	protected void initialize()
	{
		
		this.getUI().setRunButtonText("Export");
		this.getUI().setParametersIOVisible(false);
		
		varExport = new EzVarEnum<ExportEnum>(
				"Export Format", ExportEnum.values());

		//Spreadsheet option
		varTagExport = new EzVarBoolean("Only Tagged Cells", false);
		
//		//GraphML expert mode [currently not used!]
//		varExportType = new EzVarEnum<ExportFieldType>("Export", 
//				ExportFieldType.values(), ExportFieldType.STANDARD);
//		varFrameNo = new EzVarInteger("Frame no:",0,0,100,1);
//		
//		EzGroup groupGraphML = new EzGroup("GraphML export options",
//				varExportType,
//				varFrameNo);
//		addEzComponent(groupGraphML);
		
		EzGroup groupFormatChoice = new EzGroup("1. CHOOSE AN EXPORT FORMAT",
				varExport,
				varTagExport);
		addEzComponent(groupFormatChoice);
		
		varExport.addVisibilityTriggerTo(varTagExport, ExportEnum.SPREADSHEET);
//		varExport.addVisibilityTriggerTo(groupGraphML, ExportEnum.GRAPHML_EXPORT);
		
		//Sequence selection
		varSequence = new EzVarSequence("Sequence");
		EzGroup groupSequenceDescription = new EzGroup("2. SELECT THE CONNECTED SEQUENCE",
				varSequence);
		addEzComponent(groupSequenceDescription);
		
		EzGroup groupPluginDescription = new EzGroup("3. RUN THE PLUGIN",
				new EzLabel("A save dialog will appear"));
		addEzComponent(groupPluginDescription);
		
		
		//Export format description
		addComponent(new JSeparator(JSeparator.VERTICAL));
		
		//Use a list to allow multiple exports at ones?
//		getUI().setActionPanelVisible(true);
//		String[] data = {"one", "two", "three", "four","Five","Six","Seven","eight"};
//		JList myList = new JList(data);
//		addComponent(myList);
//		addEzComponent(new EzVarFloat("A number", 5.6f, 0, 10, 0.1f));
		
		final EzLabel varExportDescription = new EzLabel(varExport.getValue().getDescription());
		EzGroup groupDescription = new EzGroup("Format description",
				varExportDescription);
		addEzComponent(groupDescription);
		
		varExport.addVarChangeListener(new EzVarListener<ExportEnum>(){
			public void variableChanged(EzVar<ExportEnum> source, ExportEnum newValue) {
				varExportDescription.setText(newValue.getDescription());		
			}
		});
		
	}    
	


	@Override
	public void clean() { }


	@Override
	protected void execute() { 
		Sequence sequence = varSequence.getValue();
		
		if(sequence == null){
			new AnnounceFrame("Plugin requires active sequence! Please open an image on which to display results");
			return;
		}
		
		if(Icy.getMainInterface().getSwimmingPool().hasObjects("stGraph", true)){
			for ( SwimmingObject swimmingObject : 
				Icy.getMainInterface().getSwimmingPool().getObjects(
						"stGraph", true) ){

				if ( swimmingObject.getObject() instanceof SpatioTemporalGraph ){

					SpatioTemporalGraph stGraph = (SpatioTemporalGraph) swimmingObject.getObject();	

					ExportEnum USER_CHOICE = varExport.getValue();

					switch (USER_CHOICE){
					
					case PDF_SCREENSHOT:
						new PdfPrinter(stGraph,sequence);
						break;

					case GRAPHML:
						graphExportMode(stGraph);
						break;
					
					case SPREADSHEET:
						BigXlsExporter xlsExporter = new BigXlsExporter(stGraph,
								varTagExport.getValue(),sequence, this.getUI());
						xlsExporter.writeXLSFile();
						break;
						
					case TIFF_SKELETONS:
						saveTiffSkeletons(sequence, stGraph);
						break;
						
					case WKT_SKELETONS:
						saveWktSkeletons(stGraph);
						break;
						
					case CSV_TRACKING:
						saveCsvTracking(stGraph);
						break;
						
					}
				}
			}
		}
		else
			new AnnounceFrame("No spatio temporal graph found in ICYsp, please load a CellGraph first!");
	}
	
	/**
	 * Export 8bit tiff images from the loaded stGraph
	 * 
	 * @param sequence
	 * @param stGraph
	 */
	private void saveTiffSkeletons(Sequence sequence, SpatioTemporalGraph stGraph) {
		
		String export_folder = chooseFolder();
		if(export_folder == "")
			return;
		
		SkeletonWriter writer = new SkeletonWriter(sequence);
		
		for(int i=0; i < stGraph.size(); i++){
			FrameGraph frame_i = stGraph.getFrame(i);
			writer.write(frame_i,String.format("%s/skeleton_%03d.tiff",export_folder,i));
		}
	}



	/**
	 * Export the graph as collection of xml graph based files, i.e. GraphML
	 * 
	 * @param stGraph Spatiotemporal graph to export as GraphML files
	 */
	private void graphExportMode(SpatioTemporalGraph stGraph) {

		String export_folder = chooseFolder();
		if(export_folder == "")
			return;
		
		GraphExporter exporter = new GraphExporter(ExportFieldType.COMPLETE_CSV);

		for(int i=0; i<stGraph.size(); i++)
			exporter.exportFrame(
					stGraph.getFrame(i), 
					String.format("%s/frame%03d.xml",export_folder,i));

	}
	
	/**
	 * Export the graph geometries in Well-known text format
	 * 
	 * @param stGraph Spatiotemporal graph to export as WKT files
	 */
	private void saveWktSkeletons(SpatioTemporalGraph stGraph) {
		
		String export_folder = chooseFolder();
		if(export_folder == "")
			return;
			
		WktPolygonExporter wkt_exporter = new WktPolygonExporter();
		
		for(int i=0; i < stGraph.size(); i++){
			FrameGraph frame_i = stGraph.getFrame(i);
			if(frame_i.hasBoundary())
				wkt_exporter.export(frame_i.getBoundary(), String.format("%s/border_%03d.wkt",export_folder,i));
			
			wkt_exporter.exportFrame(frame_i, String.format("%s/skeleton_%03d.wkt",export_folder,i));
		}
		
		System.out.println("Successfully saved Wkt Files to: "+export_folder);
	}
	
	/**
	 * Export the tracking of the graph structure as CSV format
	 * 
	 * @param stGraph Spatiotemporal graph to export as CSV tracking files
	 */
	private void saveCsvTracking(SpatioTemporalGraph stGraph) {
		
		String export_folder = chooseFolder();
		if(export_folder == "")
			return;
		
		CsvTrackWriter track_writer = new CsvTrackWriter(stGraph,export_folder);
		track_writer.write();
		
		System.out.println("Successfully saved tracking to: "+export_folder);
		
	}
	
	/**
	 * Prompts a file chooser to specify a name for a new directory where to save the results 
	 * 
	 * @return Absolute path of chosen folder or empty string if invalid input
	 */
	private String chooseFolder(){
		
		//Choose location of test folder
		JFileChooser dialog = new JFileChooser();
		dialog.setDialogTitle("Please choose export folder location");
		dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		//Only proceed if the user puts in a valid directory
		if(dialog.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
			return "";
		
		final File f = dialog.getSelectedFile();
		
		return f.getAbsolutePath();
	}

	
}
