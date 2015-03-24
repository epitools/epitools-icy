package plugins.davhelle.cellgraph.export;

public enum ExportEnum {
	
	SPREADSHEET_EXPORT("Export the currenlty loaded graph as one XLS file"),
	GRAPHML_EXPORT("Exports the currently loaded graph into a GraphML file"),
	SAVE_SKELETONS("Saves the imported skeletons with modifications (e.g. small cell removal/border removal) as separate set"),
	PDF_SCREENSHOT("Exports a pdf");
	
	//SAVE_WKT_POLYGONS
	//SAVE_TRACKING
	//SAVE_CSV
	
	private String description;
	private ExportEnum(String description){this.description = description;}
	public String getDescription(){return description;}
}
