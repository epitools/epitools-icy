package plugins.davhelle.cellgraph.overlays;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/**
 * Helper methods to generate legends for the StGraphOverlays
 * 
 * @author Davide Heller
 *
 */
public class OverlayUtils {
	
	/**
	 * Color Gradient legend that scales from 0 to 1 and adds the min and max strings in the legend
	 * 
	 * @param g graphics handle
	 * @param line segment defining the location of the legend
	 * @param min_value minimum value string to display in the legend
	 * @param max_value maximum value string to display in the legend
	 * @param bin_no number of bins to form
	 * @param scaling_factor multiplying factor for the HSB color value (e.g. narrow to broad color range)
	 * @param shift_factor shifting factor for the HSB color value (e.g. from green to blue tones)
	 */
	public static void gradientColorLegend_ZeroOne(
			Graphics2D g,
			java.awt.geom.Line2D line,
			String min_value,
			String max_value,
			int bin_no,
			double scaling_factor,
			double shift_factor
			){
		
		double binWidth = (line.getX2() - line.getX1())/bin_no;
		int binHeight = 20;
		double step = 1.0/bin_no;
		
		for(int i=0; i<bin_no; i++){

			double h = (step*i);
			
			//adapt for certain color range
			//by multiplying with factor
			
			h = h * scaling_factor + shift_factor;
			
			Color hsbColor = Color.getHSBColor(
					(float)h,
					1f,
					1f);

			g.setColor(hsbColor);

			int x = (int)(binWidth*i + line.getX1());
			int y = (int)line.getY1();
			
			g.fillRect(x,y,(int)binWidth,binHeight);
			

		}
		
		g.setFont(new Font("TimesRoman", Font.PLAIN, 15));
		
		g.setColor(Color.WHITE);
		g.drawString(min_value, 
				(float)line.getX1(), 
				(float)line.getY1() + 15);
		
		FontMetrics fm = g.getFontMetrics();
		
		g.setColor(Color.WHITE);
		String s = max_value;
		g.drawString(s, 
				(float)line.getX2() - fm.stringWidth(s), 
				(float)line.getY1() + 15);
	}
	
	/**
	 * Color legend displaying a colored string 
	 * 
	 * @param g graphics handle
	 * @param line segment defining the location of the legend
	 * @param s String to display
	 * @param c Color to print the string s in 
	 * @param offset vertical distance in case of multiple statements, for default put 0
	 */
	public static void stringColorLegend(
			Graphics2D g,
			java.awt.geom.Line2D line,
			String s,
			Color c,
			int offset){
		
		g.setFont(new Font("TimesRoman", Font.PLAIN, 15));
		
		FontMetrics fm = g.getFontMetrics();
		g.setColor(c);
		g.fillRect((int)line.getX1() - 2, (int)line.getY1() - 2 + offset, fm.stringWidth(s) + 5, 15 + 2);
		
		g.setColor(Color.BLACK);
		g.drawRect((int)line.getX1() - 2, (int)line.getY1() - 2 + offset, fm.stringWidth(s) + 5, 15 + 2);
		
		g.drawString(s, 
				(float)line.getX1(), 
				(float)line.getY1() + 12 + offset);
		
	}
	
}
