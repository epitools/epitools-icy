package plugins.davhelle.cellgraph.io;

/**
 * Enumeration to help choosing the desired intensity 
 * readout method from {@link icy.roi.ROIUtil}
 * 
 * @author Davide Heller
 *
 */
public enum IntensitySummaryType {
	
	Mean,
	Max,
	Min,
	Sum,
	StandardDeviation
	
}
