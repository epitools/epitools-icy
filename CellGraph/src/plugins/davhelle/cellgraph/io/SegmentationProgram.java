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
		PackingAnalyzer, 
		SeedWater, 
		MatlabLabelOutlines,
}
