package plugins.davhelle.cellgraph.nodes;

import plugins.davhelle.cellgraph.graphs.FrameGenerator;

import com.vividsolutions.jts.geom.Polygon;

/**
 * Helper class to sort polygons according to their 
 * geometric center coordinate.
 * 
 * First discerning over the x coordinate and then over
 * the y coordinate;
 * 
 * Mainly used in {@link FrameGenerator} to order nodes.
 * 
 * @author Davide Heller
 *
 */
public class ComparablePolygon implements Comparable<ComparablePolygon> {

	/**
	 * Associated polygon to comparable object
	 */
	private Polygon poly;
	/**
	 * First comparable value (Centroid x coordinate)
	 */
	private Double x;
	/**
	 * Second comparable value (Centroid y coordinate)
	 */
	private Double y;
	
	/**
	 * Sets the polygon to be compared and computes the values
	 * on which basis the polygon will be compared, i.e.
	 * the two polygon centroid coordinates (x counts over y)
	 * 
	 * @param poly
	 */
	public ComparablePolygon(Polygon poly){
		this.poly = poly;
		this.x = poly.getCentroid().getX();
		this.y = poly.getCentroid().getY();
	}
	
	public Polygon getPolygon(){
		return poly;
	}
	
	@Override
	public int compareTo(ComparablePolygon other) {
		if(x.equals(other.x))
			return y.compareTo(other.y);
		else
			return x.compareTo(other.x);
	}

}
