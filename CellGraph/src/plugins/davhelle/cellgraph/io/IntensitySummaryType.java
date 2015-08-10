package plugins.davhelle.cellgraph.io;

import plugins.davhelle.cellgraph.CellOverlay;

/**
 * Enumeration to help choosing the desired intensity 
 * readout method from {@link icy.roi.ROIUtil}
 * 
 * @author Davide Heller
 *
 */
public enum IntensitySummaryType {
	
	Mean("mean"),
	Max("max"),
	Min("min"),
	Sum("sum"),
	StandardDeviation("sd");
	
	/**
	 * Description String visualized by the {@link CellOverlay} plugin
	 */
	private String description;
	/**
	 * @param description the description of the overlay
	 */
	private IntensitySummaryType(String description){this.description = description;}
	/**
	 * @return the description of the overlay
	 */
	public String getDescription(){return description;}
	
}
