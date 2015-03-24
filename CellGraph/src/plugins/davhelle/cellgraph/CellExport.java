package plugins.davhelle.cellgraph;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.swimmingPool.SwimmingObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.swing.JSeparator;

import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarFile;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.davhelle.cellgraph.export.BigXlsExporter;
import plugins.davhelle.cellgraph.export.ExportEnum;
import plugins.davhelle.cellgraph.export.ExportFieldType;
import plugins.davhelle.cellgraph.export.GraphExporter;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.PdfPrinter;
import plugins.davhelle.cellgraph.nodes.Node;

public class CellExport extends EzPlug {

	private EzVarFile varSaveSkeleton;
	private EzVarEnum<ExportEnum> varExport;
	private EzVarEnum<ExportFieldType> varExportType;
	private EzVarFile varOutputFile;
	private EzVarInteger varFrameNo;
	
	@Override
	protected void initialize()
	{
		
		varExport = new EzVarEnum<ExportEnum>(
				"Export", ExportEnum.values());
		
		addEzComponent(varExport);
		
//		//Graph Export Mode
		varExportType = new EzVarEnum<ExportFieldType>("Export field", 
				ExportFieldType.values(), ExportFieldType.STANDARD);
		varOutputFile = new EzVarFile("Output File", "/Users/davide/analysis");
		varFrameNo = new EzVarInteger("Frame no:",0,0,100,1);
		
		EzGroup groupGraphML = new EzGroup("GraphML export options",
				varExportType,
				varOutputFile,
				varFrameNo);
		
		addEzComponent(groupGraphML);
		
//		//SAVE_SKELETON mode 
		varSaveSkeleton = new EzVarFile("Output File", "");
		EzGroup groupSaveSkeleton = new EzGroup(
				"Skeleton export options",varSaveSkeleton);

		//save one complete excel file
//		getUI().setActionPanelVisible(true);
//		String[] data = {"one", "two", "three", "four","Five","Six","Seven","eight"};
//		JList myList = new JList(data);
//		addComponent(myList);
//		addEzComponent(new EzVarFloat("A number", 5.6f, 0, 10, 0.1f));
		addComponent(new JSeparator(JSeparator.VERTICAL));
		
		final EzLabel varExportDescription = new EzLabel(varExport.getValue().getDescription());
		EzGroup groupDescription = new EzGroup("Export description",
				varExportDescription);
		addEzComponent(groupDescription);
		
		varExport.addVarChangeListener(new EzVarListener<ExportEnum>(){
			public void variableChanged(EzVar<ExportEnum> source, ExportEnum newValue) {
				varExportDescription.setText(newValue.getDescription());		
			}
		});
		
		varExport.addVisibilityTriggerTo(groupGraphML, ExportEnum.GRAPHML_EXPORT);
		varExport.addVisibilityTriggerTo(groupSaveSkeleton, ExportEnum.SAVE_SKELETONS);
		
		//future statistical output statistics
		//			CsvWriter.trackedArea(wing_disc_movie);
		//			CsvWriter.frameAndArea(wing_disc_movie);
		//CsvWriter.custom_write_out(wing_disc_movie);

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

					case SAVE_SKELETONS:
						//new SkeletonWriter(sequence, wing_disc_movie).write(varSaveSkeleton.getValue(false).getAbsolutePath());
						break;

					case PDF_SCREENSHOT:
						new PdfPrinter(wing_disc_movie);
						break;

					case GRAPHML_EXPORT:
						graphExportMode(
								wing_disc_movie,
								varExportType.getValue(),
								varOutputFile.getValue(false),
								varFrameNo.getValue());
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
	}
	
	private void graphExportMode(
			SpatioTemporalGraph wing_disc_movie,
			ExportFieldType export_type,
			File output_file,
			Integer frame_no) {
		
		//safety checks
		if(frame_no >= wing_disc_movie.size())
			new AnnounceFrame("Requested frame no is not available! Please check");
		
		
		
		
		else if(varExportType.getValue() == ExportFieldType.STANDARD){
			
			String base_dir = output_file.getParent();
			
			//first frame TODO can add property to field like .getName() = '%s/frame%d.xml')
			GraphExporter exporter = new GraphExporter(ExportFieldType.COMPLETE_CSV);
			int i = 0;
			exporter.exportFrame(
					wing_disc_movie.getFrame(i), 
					String.format("%s/frame%d.xml",base_dir,i));
		
			//last frame
			exporter = new GraphExporter(ExportFieldType.COMPLETE_CSV);
			i = wing_disc_movie.size() - 1;
			exporter.exportFrame(
					wing_disc_movie.getFrame(i), 
					String.format("%s/frame%d.xml",base_dir,i));
			
			//seq_area
			exporter = new GraphExporter(ExportFieldType.SEQ_AREA);
			i = 0;
			exporter.exportFrame(
					wing_disc_movie.getFrame(i), 
					String.format("%s/seq_area.xml",base_dir,i));
			//seq_x
			exporter = new GraphExporter(ExportFieldType.SEQ_X);
			i = 0;
			exporter.exportFrame(
					wing_disc_movie.getFrame(i), 
					String.format("%s/seq_x.xml",base_dir,i));
			//seq_y
			exporter = new GraphExporter(ExportFieldType.SEQ_Y);
			i = 0;
			exporter.exportFrame(
					wing_disc_movie.getFrame(i), 
					String.format("%s/seq_y.xml",base_dir,i));
			
			
		}
		else
		{
			GraphExporter exporter = new GraphExporter(varExportType.getValue());
			FrameGraph frame_to_export = wing_disc_movie.getFrame(frame_no);
			exporter.exportFrame(frame_to_export, output_file.getAbsolutePath());
		}		
		
	}


	

}
