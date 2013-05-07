/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.nodes;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Helper class to sort polygons according to their 
 * geometric center coordinate.
 * 
 * First discerning over the x coordinate and then over
 * the y coordinate;
 * 
 * 
 * @author Davide Heller
 *
 */
public class ComparablePolygon implements Comparable<ComparablePolygon> {

	private Polygon poly;
	private Double x;
	private Double y;
	
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
