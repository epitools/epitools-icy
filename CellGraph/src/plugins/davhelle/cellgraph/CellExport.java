package plugins.davhelle.cellgraph;

import icy.gui.dialog.SaveDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;

import java.io.File;

import javax.swing.JSeparator;

import com.vividsolutions.jts.geom.Geometry;

import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarFolder;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.davhelle.cellgraph.export.BigXlsExporter;
import plugins.davhelle.cellgraph.export.ExportEnum;
import plugins.davhelle.cellgraph.export.ExportFieldType;
import plugins.davhelle.cellgraph.export.GraphExporter;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.PdfPrinter;
import plugins.davhelle.cellgraph.io.WktPolygonExporter;

public class CellExport extends EzPlug {

//	private EzVarFile varSaveSkeleton;
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
		varTagExport = new EzVarBoolean("Only Tagged Cells", false);
		
		//Save skeletons using the well-known-text format (jts)
		
		EzGroup groupFormatChoice = new EzGroup("1. CHOOSE AN EXPORT FORMAT",
				varExport,
				varTagExport);
		addEzComponent(groupFormatChoice);
		
		varExport.addVisibilityTriggerTo(varTagExport, ExportEnum.SPREADSHEET_EXPORT);
//		varExport.addVisibilityTriggerTo(groupGraphML, ExportEnum.GRAPHML_EXPORT);
//		varExport.addVisibilityTriggerTo(groupSaveSkeleton, ExportEnum.SAVE_SKELETONS);
		
		
		varSequence = new EzVarSequence("Sequence");
		EzGroup groupSequenceDescription = new EzGroup("2. SELECT THE CONNECTED SEQUENCE",
				varSequence);
		addEzComponent(groupSequenceDescription);
		
		EzGroup groupPluginDescription = new EzGroup("3. RUN THE PLUGIN",
				new EzLabel("A save dialog will appear"));
		addEzComponent(groupPluginDescription);
		
		addComponent(new JSeparator(JSeparator.VERTICAL));
		
////		//Graph Export Mode
//		varExportType = new EzVarEnum<ExportFieldType>("Export", 
//				ExportFieldType.values(), ExportFieldType.STANDARD);
//		varFrameNo = new EzVarInteger("Frame no:",0,0,100,1);
//		
//		EzGroup groupGraphML = new EzGroup("GraphML export options",
//				varExportType,
//				varFrameNo);
//		
//		addEzComponent(groupGraphML);
		
//		//SAVE_SKELETON mode 
//		varSaveSkeleton = new EzVarFile("Output File", "");
//		EzGroup groupSaveSkeleton = new EzGroup(
//				"Skeleton export options",varSaveSkeleton);

		//save one complete excel file
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

					SpatioTemporalGraph wing_disc_movie = (SpatioTemporalGraph) swimmingObject.getObject();	

					ExportEnum USER_CHOICE = varExport.getValue();

					switch (USER_CHOICE){

//					case SAVE_SKELETONS:
//						new SkeletonWriter(sequence, wing_disc_movie).write(varSaveSkeleton.getValue(false).getAbsolutePath());
//						break;

					case PDF_SCREENSHOT:
						new PdfPrinter(wing_disc_movie,sequence);
						break;

					case GRAPHML_EXPORT:
						graphExportMode(wing_disc_movie);
						break;
					
					case SPREADSHEET_EXPORT:
						BigXlsExporter xlsExporter = new BigXlsExporter(wing_disc_movie,
								varTagExport.getValue(),sequence, this.getUI());
						xlsExporter.writeXLSFile();
						break;
					case WKT_SKELETONS:
						saveWktSkeletons(wing_disc_movie);
					default:
						break;	
					}
				}
			}
		}
		else
			new AnnounceFrame("No spatio temporal graph found in ICYsp, please load a CellGraph first!");
	}
	
	private void graphExportMode(SpatioTemporalGraph stGraph) {

		String file_name = SaveDialog.chooseFile(
				"Please enter a folder where to save the graphML xml files",
				"/Users/davide/",
				"frame000", ".xml");

		if(file_name == null)
			return;

		File output_file = new File(file_name);
		String base_dir = output_file.getParent();
		
		GraphExporter exporter = new GraphExporter(ExportFieldType.COMPLETE_CSV);

		for(int i=0; i<stGraph.size(); i++)
			exporter.exportFrame(
					stGraph.getFrame(i), 
					String.format("%s/frame%03d.xml",base_dir,i));

	}
	
	/**
	 * @param wing_disc_movie
	 */
	public void saveWktSkeletons(SpatioTemporalGraph wing_disc_movie) {
		
		String folder_name = SaveDialog.chooseFile(
				"Please select location and enter folder name",
				"/Users/davide/",
				"folderName");
		
		if(folder_name == null)
			return;
		
		File output_folder = new File(folder_name);
		if(output_folder.isDirectory()){
			new AnnounceFrame("Folder already exists, please select new name");
			return;
		}else{
			output_folder.mkdir();
		}
			
		WktPolygonExporter wkt_exporter = new WktPolygonExporter();
		String export_folder = output_folder.getAbsolutePath();
		
		for(int i=0; i < wing_disc_movie.size(); i++){
			FrameGraph frame_i = wing_disc_movie.getFrame(i);
			if(frame_i.hasBoundary())
				wkt_exporter.export(frame_i.getBoundary(), String.format("%s/border_%03d.wkt",export_folder,i));
			
			wkt_exporter.exportFrame(frame_i, String.format("%s/skeleton_%03d.wkt",export_folder,i));
		}
		
		System.out.println("Successfully saved Wkt Files to: "+export_folder);
	}
	
}
