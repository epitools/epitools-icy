package plugins.davhelle.cellgraph.export;

import plugins.davhelle.cellgraph.io.CsvTrackWriter;
import plugins.davhelle.cellgraph.io.PdfPrinter;
import plugins.davhelle.cellgraph.io.SkeletonWriter;
import plugins.davhelle.cellgraph.io.WktPolygonExporter;

public enum ExportEnum {
	
	SPREADSHEET(BigXlsExporter.DESCRIPTION),
	GRAPHML(GraphExporter.DESCRIPTION),
	PDF_SCREENSHOT(PdfPrinter.DESCRIPTION),
	TIFF_SKELETONS(SkeletonWriter.DESCRIPTION),
	WKT_SKELETONS(WktPolygonExporter.DESCRIPTION),
	CSV_TRACKING(CsvTrackWriter.DESCRIPTION);
	
	private String description;
	private ExportEnum(String description){this.description = description;}
	public String getDescription(){return description;}
}
