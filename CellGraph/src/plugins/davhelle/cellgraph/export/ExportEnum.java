package plugins.davhelle.cellgraph.export;

public enum ExportEnum {
	
	
	GRAPHML_EXPORT("Exports the currently loaded graph into a GraphML file"),
	SAVE_SKELETONS("Saves the imported skeletons with modifications (e.g. small cell removal/border removal) as separate set"),
	WRITE_OUT_DDN("Statistics output"),
	PDF_SCREENSHOT("Exports a pdf");
	
	//SAVE_SKELETONS
	//SAVE_WKT
	//SAVE_TRACKING
	//SAVE_CSV
	//
	
	
	private String description;
	private ExportEnum(String description){this.description = description;}
	public String getDescription(){return description;}
}
