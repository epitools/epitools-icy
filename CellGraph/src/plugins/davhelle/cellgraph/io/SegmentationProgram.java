/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/

package plugins.davhelle.cellgraph.io;

/**
 * This enumeration lists the currently supported segmentation
 * programs from which one can directly import the output
 * files. 
 * 
 * @author Davide Heller
 *
 */
public enum SegmentationProgram {
		/**
		 * Segmentation program by Aigouy et al. Cell (2010) 142(5):773-86.
		 * <br>
		 * Custom support includes retrieval of skeleton bitmap from 
		 * folder structure, i.e. [raw_image_name]/handCorrection.png
		 */
		PackingAnalyzer, 
		/**
		 * Segmentation program by Mashburn et al. Cytometry (2012) (May): 409Ð18.
		 * <br>
		 * No support needed anymore. Deprecation soon 
		 */
		SeedWater, 
		/**
		 * Custom import for EpiTools.<br>
		 * No support needed anymore. Deprecation soon
		 */
		MatlabLabelOutlines,
}
