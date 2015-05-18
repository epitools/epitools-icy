package plugins.davhelle.cellgraph.misc;

import java.awt.Color;

/**
 * Enumeration for Cell Colors used by EdgeTag and CellTag overlays
 * 
 * @author Davide Heller
 *
 */
public enum CellColor {
	GREEN(Color.green), BLUE(Color.blue), WHITE(Color.white), RED(Color.red), BLACK(Color.black), CYAN(Color.cyan);
	
	private Color color;
	
	private CellColor(Color color){
		this.color = color;
	}
	
	public Color getColor(){
		return color;
	}
}
