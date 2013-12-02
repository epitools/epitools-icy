package plugins.davhelle.cellgraph.misc;

import java.awt.Color;

public enum CellColor {
	RED(Color.red), GREEN(Color.green), BLUE(Color.blue);
	
	private Color color;
	
	private CellColor(Color color){
		this.color = color;
	}
	
	public Color getColor(){
		return color;
	}
}
