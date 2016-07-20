package plugins.davhelle.cellgraph.io;

import icy.roi.BooleanMask2D;
import icy.roi.ROI;
import icy.roi.ROIUtil;
import icy.sequence.Sequence;
import icy.sequence.SequenceDataIterator;
import icy.type.collection.array.Array1DUtil;
import icy.type.point.Point5D;
import plugins.kernel.roi.roi2d.ROI2DArea;

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
	@SuppressWarnings("deprecation")
	public static double measureRoiIntensity(
			Sequence sequence,
			ROI roi,
			int z,
			int t,
			int c,
			IntensitySummaryType summaryType){
		
		double intensity_readout = -1.0;
		
		try{
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
		}
		catch(java.lang.UnsupportedOperationException exp){
			Point5D position5d = roi.getPosition5D();
			System.out.printf(
					"Could not compute intensity for roi: [%.2f,%.2f,%d,%d]\n",
					position5d.getX(),position5d.getY(),z,t);
		}
		
		return intensity_readout;
	}
	
	/**
	 * Method adds two more parameters with which to prune the
	 * pixels taken into account for the measurement.
	 * 
	 * @param sequence
	 * @param roi
	 * @param z
	 * @param t
	 * @param c
	 * @param summaryType
	 * @param topPercent Percentage that should be retained for measurement (e.g. 0.2 = 20 top-most percent)
	 * @param addROItoSequence Flag whether to add or not the pruned ROI to the sequence
	 * @return
	 */
	public static double measureRoiIntensity(
			Sequence sequence,
			ROI roi,
			int z,
			int t,
			int c,
			IntensitySummaryType summaryType,
			double topPercent, boolean addROItoSequence){
		
		// Initialize an array to contain the top-percentage highest values 
		
		double numPixels = roi.getNumberOfPoints();
        int top_size = (int) Math.round(numPixels * topPercent);
        //Safety fall back in case of a small ROI
        if(top_size < 1)
        	top_size = 1;
        double[] topIntensities = new double[top_size];

        // Iterate through the pixels of the rois and only keep the top most
        SequenceDataIterator it = new 
        		SequenceDataIterator(sequence, roi, false, z, t, c);
        
        while (!it.done())
        {
            final double value = it.get();

            if(value > topIntensities[0]){
	        
            		//index to substitute
            		int subIdx=0;
            	
            		//find highest compatible value
            		for(int i=1; i < top_size; i++)
	            		if(value > topIntensities[i])
	            			subIdx++;
	            		else
	            			break;
            		
            		//shift values below (first is always eliminated)
	            	for(int i=1; i < subIdx; i++)
	            		topIntensities[i-1] = topIntensities[i];
	            	
	            	//insert value into array
	            	topIntensities[subIdx] = value;
	            	
            }

            it.next();
        }

        double threshold = topIntensities[0];
        
		double[] doubleArray = Array1DUtil.arrayToDoubleArray(
			     sequence.getDataXY(0, 0, 0), sequence.isSignedDataType());
		boolean[] mask = new boolean[doubleArray.length];
        
		for (int i = 0; i < doubleArray.length; i++)
		     mask[i] = !(doubleArray[i] > threshold);
		BooleanMask2D mask2d = new BooleanMask2D(sequence.getBounds2D(), mask); 
		
		ROI topROI = ROIUtil.subtract(roi, new ROI2DArea(mask2d));
		if(addROItoSequence)
			sequence.addROI(topROI);
		
        return measureRoiIntensity(sequence, topROI, z, t, c, summaryType);
	}
	
	
}
