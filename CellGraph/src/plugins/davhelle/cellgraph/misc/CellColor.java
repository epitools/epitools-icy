package plugins.davhelle.cellgraph.misc;

import java.awt.Color;

public enum CellColor {
	GREEN(Color.green), BLUE(Color.blue), WHITE(Color.white), RED(Color.red), BLACK(Color.black);
	
	private Color color;
	
	private CellColor(Color color){
		this.color = color;
	}
	
	public Color getColor(){
		return color;
	}
}
