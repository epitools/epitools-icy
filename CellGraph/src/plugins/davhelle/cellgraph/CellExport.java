package plugins.davhelle.cellgraph;

import icy.gui.dialog.SaveDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.swimmingPool.SwimmingObject;

import java.io.File;

import javax.swing.JSeparator;

import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarListener;
import plugins.davhelle.cellgraph.export.BigXlsExporter;
import plugins.davhelle.cellgraph.export.ExportEnum;
import plugins.davhelle.cellgraph.export.ExportFieldType;
import plugins.davhelle.cellgraph.export.GraphExporter;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.PdfPrinter;

public class CellExport extends EzPlug {

//	private EzVarFile varSaveSkeleton;
	private EzVarEnum<ExportEnum> varExport;
//	private EzVarEnum<ExportFieldType> varExportType;
//	private EzVarInteger varFrameNo;
	
	@Override
	protected void initialize()
	{
		
		this.getUI().setRunButtonText("Export");
		this.getUI().setParametersIOVisible(false);
		
		varExport = new EzVarEnum<ExportEnum>(
				"Export Format", ExportEnum.values());
		EzGroup groupFormatChoice = new EzGroup("1. CHOOSE AN EXPORT FORMAT",
				varExport);
		addEzComponent(groupFormatChoice);
		
		EzGroup groupPluginDescription = new EzGroup("2. RUN THE PLUGIN",
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
		
//		varExport.addVisibilityTriggerTo(groupGraphML, ExportEnum.GRAPHML_EXPORT);
//		varExport.addVisibilityTriggerTo(groupSaveSkeleton, ExportEnum.SAVE_SKELETONS);
		
	}    
	


	@Override
	public void clean() { }


	@Override
	protected void execute() { 
		//TODO Missing alert for missing stGraph
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
						new PdfPrinter(wing_disc_movie);
						break;

					case GRAPHML_EXPORT:
						graphExportMode(wing_disc_movie);
						break;
					
					case SPREADSHEET_EXPORT:
						BigXlsExporter xlsExporter = new BigXlsExporter(wing_disc_movie, this.getUI());
						xlsExporter.writeXLSFile();
						break;
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
}
