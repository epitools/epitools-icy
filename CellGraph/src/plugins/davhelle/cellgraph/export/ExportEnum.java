package plugins.davhelle.cellgraph.export;

import plugins.davhelle.cellgraph.io.PdfPrinter;
import plugins.davhelle.cellgraph.io.WktPolygonExporter;

public enum ExportEnum {
	
	SPREADSHEET_EXPORT(BigXlsExporter.DESCRIPTION),
	GRAPHML_EXPORT(GraphExporter.DESCRIPTION),
	//SAVE_SKELETONS("Saves the imported skeletons with modifications (e.g. small cell removal/border removal) as separate set"),
	PDF_SCREENSHOT(PdfPrinter.DESCRIPTION),
	WKT_SKELETONS(WktPolygonExporter.DESCRIPTION),
	CSV_TRACKING("");
	
	//SAVE_WKT_POLYGONS
	//SAVE_TRACKING
	//SAVE_CSV
	
	private String description;
	private ExportEnum(String description){this.description = description;}
	public String getDescription(){return description;}
}
