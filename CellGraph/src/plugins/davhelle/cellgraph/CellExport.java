package plugins.davhelle.cellgraph;

import java.io.FileWriter;
import java.io.IOException;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;
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
import plugins.davhelle.cellgraph.io.SaveFolderDialog;
import plugins.davhelle.cellgraph.io.SkeletonWriter;
import plugins.davhelle.cellgraph.io.WktPolygonExporter;
import plugins.davhelle.cellgraph.overlays.CellColorTagOverlay;

/**
 * Plugin to export the information contained in the 
 * spatio-temporal graph as various output formats.<br><br>
 * 
 * Currrently includes:<br>
 * - Spreadsheets<br>
 * - GraphML files<br>
 * - PDF vector graphics of the overlays<br>
 * - Skeleton files in WKT or TIFF format<br>
 * - CSV tracking files
 * 
 * @author Davide Heller
 *
 */
public class CellExport extends EzPlug {

	/**
	 * ezGUI Handle for Export format selection
	 */
	private EzVarEnum<ExportEnum> varExport;
	
	/**
	 * ezGUI Handle for Sequence selection associated with the stGraph to export 
	 */
	private EzVarSequence varSequence;
	
	/**
	 * ezGUI Handle for Boolean Flag whether to export only the tagged cells (see {@link CellColorTagOverlay})
	 */
	private EzVarBoolean varTagExport;
	
	@Override
	protected void initialize()
	{
		//Customize GUI for Export
		this.getUI().setRunButtonText("Export");
		this.getUI().setParametersIOVisible(false);
		
		varExport = new EzVarEnum<ExportEnum>(
				"Export Format", ExportEnum.values());

		//Spreadsheet option
		varTagExport = new EzVarBoolean("Only Tagged Cells", false);
		
		//Format Selection group
		EzGroup groupFormatChoice = new EzGroup("1. CHOOSE AN EXPORT FORMAT",
				varExport,
				varTagExport);
		addEzComponent(groupFormatChoice);
		
		varExport.addVisibilityTriggerTo(varTagExport, ExportEnum.SPREADSHEET);
		
		//Sequence Selection group
		varSequence = new EzVarSequence("Sequence");
		EzGroup groupSequenceDescription = new EzGroup("2. SELECT THE CONNECTED SEQUENCE",
				varSequence);
		addEzComponent(groupSequenceDescription);
		
		//Plugin Run group 
		EzGroup groupPluginRun = new EzGroup("3. RUN THE PLUGIN",
				new EzLabel("A save dialog will appear"));
		addEzComponent(groupPluginRun);
		
		//Divide GUI in two
		//addComponent(new JSeparator(JSeparator.VERTICAL));

		//Export format description
		EzLabel descriptionHeader = new EzLabel("Description:"); //<font color=\"#4d4d4d\"></font>
		final EzLabel varExportDescription = new EzLabel(varExport.getValue().getDescription());
		EzVarBoolean varShowDescription = new EzVarBoolean("Show export description",true);
		
		super.addEzComponent(descriptionHeader);
		super.addEzComponent(varExportDescription);
		super.addEzComponent(varShowDescription);
		varShowDescription.addVisibilityTriggerTo(descriptionHeader, varShowDescription.getValue());
		varShowDescription.addVisibilityTriggerTo(varExportDescription, varShowDescription.getValue());

		//Listener addition change according to selection
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
	 * @param sequence sequence on which the graph is projected
	 * @param stGraph graph to export
	 */
	private void saveTiffSkeletons(Sequence sequence, SpatioTemporalGraph stGraph) {
		
		String export_folder = SaveFolderDialog.chooseFolder("Tiff Skeleton images");
		if(export_folder == null)
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

		String export_folder = SaveFolderDialog.chooseFolder("GraphML files");
		if(export_folder == null)
			return;
		
		GraphExporter exporter = new GraphExporter(ExportFieldType.COMPLETE_CSV);
		
		//Write header file for complete_csv
		try {
			
			FileWriter fw = new FileWriter(String.format("%s/GraphML_attributeHeader.csv",export_folder));
			
			fw.write("id,x,y,area,on_border,has_division,has_elimination,division_time,elimination_time\n");
			fw.write("numeric,numeric,numeric,numeric,logical,logical,logical,numeric,numeric\n");
			
			fw.close();
 
		} catch (IOException e) {
			e.printStackTrace();
		}			
		
		
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
		
		String export_folder = SaveFolderDialog.chooseFolder("WKT skeleton files");
		if(export_folder == null)
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
		
		String export_folder = SaveFolderDialog.chooseFolder("CSV Tracking files");
		if(export_folder == null)
			return;
		
		CsvTrackWriter track_writer = new CsvTrackWriter(stGraph,export_folder);
		track_writer.write();
		
		System.out.println("Successfully saved tracking to: "+export_folder);
		
	}
	
}
