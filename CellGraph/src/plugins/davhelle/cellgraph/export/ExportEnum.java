package plugins.davhelle.cellgraph.export;

import plugins.davhelle.cellgraph.io.CsvTrackWriter;
import plugins.davhelle.cellgraph.io.PdfPrinter;
import plugins.davhelle.cellgraph.io.SkeletonWriter;
import plugins.davhelle.cellgraph.io.WktPolygonExporter;

/**
 * Enumeration of available export options for the CellExport Plugin
 * 
 * @author Davide Heller
 */
public enum ExportEnum {
	
	/**
	 * Spreadsheet export using {@link BigXlsExporter} 
	 */
	SPREADSHEET(BigXlsExporter.DESCRIPTION),
	
	/**
	 * GraphML files exporter using {@link GraphExporter} 
	 */
	GRAPHML(GraphExporter.DESCRIPTION),
	
	/**
	 * PDF exporter using {@link PdfPrinter}
	 */
	PDF_SCREENSHOT(PdfPrinter.DESCRIPTION),
	
	/**
	 * TIFF Skeleton exporter using {@link SkeletonWriter} 
	 */
	TIFF_SKELETONS(SkeletonWriter.DESCRIPTION),
	
	/**
	 * Well Known Text (WKT) skeleton exporter using {@link WktPolygonExporter}
	 */
	WKT_SKELETONS(WktPolygonExporter.DESCRIPTION),
	
	/**
	 * CSV based Tracking file export using {@link CsvTrackWriter}
	 */
	CSV_TRACKING(CsvTrackWriter.DESCRIPTION);
	
	/**
	 * Export option description
	 */
	private String description;
	
	/**
	 * @param export option description
	 */
	private ExportEnum(String description){this.description = description;}
	
	/**
	 * @return description of the export option
	 */
	public String getDescription(){return description;}
}
