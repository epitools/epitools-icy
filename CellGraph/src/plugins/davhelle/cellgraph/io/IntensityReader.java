package plugins.davhelle.cellgraph.io;

import icy.roi.ROI;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;

/**
 * Utility class for image intensity methods
 * 
 * @author Davide Heller
 *
 */
public class IntensityReader {

	
	/**
	 * Measure the intensity of a ROI in an image sequence according to the
	 * selected summary statistic.
	 * 
	 * @param sequence image sequence to read the intensity from
	 * @param roi region of interest in the sequence to analyze
	 * @param z slice number to be analyzed
	 * @param t time point to be analyzed
	 * @param c channel number to be analyzed
	 * @param type
	 * @return intensity readout
	 */
	public static double measureRoiIntensity(
			Sequence sequence,
			ROI roi,
			int z,
			int t,
			int c,
			IntensitySummaryType summaryType){
		
		double intensity_readout = -1.0;
		
		switch (summaryType) {
		case Max:
			intensity_readout = ROIUtil.getMaxIntensity(sequence, roi, z, t, c);
			break;
		case Mean:
			intensity_readout = ROIUtil.getMeanIntensity(sequence, roi, z, t, c);
			break;
		case Min:
			intensity_readout = ROIUtil.getMinIntensity(sequence, roi, z, t, c);
			break;
		case StandardDeviation:
			intensity_readout = ROIUtil.getStandardDeviation(sequence, roi, z, t, c);
			break;
		case Sum:
			intensity_readout = ROIUtil.getSumIntensity(sequence, roi, z, t, c);
			break;
		default:
			System.out.println("Unknown Image Summary Method");
		}
		
		return intensity_readout;
	}
	
	
}
