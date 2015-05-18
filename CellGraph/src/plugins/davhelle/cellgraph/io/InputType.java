package plugins.davhelle.cellgraph.io;

/**
 * Enumeration of possible input file types
 * 
 * @author Davide Heller
 *
 */
public enum InputType {
	/**
	 * VTK polydata mesh file
	 */
	VTK_MESH,
	
	/**
	 * Skeleton bitmap image 
	 */
	SKELETON,
	
	/**
	 * Well-known-text polygons 
	 */
	WKT,
}
