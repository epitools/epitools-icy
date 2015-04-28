package plugins.davhelle.cellgraph.painters;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class OverlayUtils {

	public static void gradientColorLegend(
			Graphics2D g,
			java.awt.geom.Line2D.Double line,
			double min_value,
			double max_value,
			int bin_no,
			double scaling_factor,
			double shift_factor
			){
		
		int binWidth = (int)((line.x2 - line.x1)/bin_no);
		int binHeight = 20;
		double step = (max_value - min_value)/bin_no;
		
		for(int i=0; i<bin_no; i++){

			double h = (step*i)/max_value;
			
			//adapt for certain color range
			//by multiplying with factor
			
			h = h * scaling_factor + shift_factor;
			
			Color hsbColor = Color.getHSBColor(
					(float)h,
					1f,
					1f);

			g.setColor(hsbColor);

			int x = binWidth*i + (int)line.x1;
			int y = (int)line.y1;
			
			g.fillRect(x,y,binWidth,binHeight);
			

		}
		
		g.setFont(new Font("TimesRoman", Font.PLAIN, 15));
		
		g.setColor(Color.WHITE);
		g.drawString(String.format("[%.0f", min_value), 
				(float)line.x1, 
				(float)line.y1 + 15);
		
		FontMetrics fm = g.getFontMetrics();
		
		g.setColor(Color.WHITE);
		String s = String.format("%.0f]", max_value);
		g.drawString(s, 
				(float)line.x2 - fm.stringWidth(s), 
				(float)line.y1 + 15);
	}
	
	public static void gradientColorLegend_ZeroOne(
			Graphics2D g,
			java.awt.geom.Line2D.Double line,
			String min_value,
			String max_value,
			int bin_no,
			double scaling_factor,
			double shift_factor
			){
		
		int binWidth = (int)((line.x2 - line.x1)/bin_no);
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

			int x = binWidth*i + (int)line.x1;
			int y = (int)line.y1;
			
			g.fillRect(x,y,binWidth,binHeight);
			

		}
		
		g.setFont(new Font("TimesRoman", Font.PLAIN, 15));
		
		g.setColor(Color.WHITE);
		g.drawString(min_value, 
				(float)line.x1, 
				(float)line.y1 + 15);
		
		FontMetrics fm = g.getFontMetrics();
		
		g.setColor(Color.WHITE);
		String s = max_value;
		g.drawString(s, 
				(float)line.x2 - fm.stringWidth(s), 
				(float)line.y1 + 15);
	}
	
	public static void stringColorLegend(
			Graphics2D g,
			java.awt.geom.Line2D.Double line,
			String s,
			Color c,
			int offset){
		
		g.setFont(new Font("TimesRoman", Font.PLAIN, 15));
		
		FontMetrics fm = g.getFontMetrics();
		g.setColor(c);
		g.fillRect((int)line.x1 - 2, (int)line.y1 - 2 + offset, fm.stringWidth(s) + 5, 15 + 2);
		
		g.setColor(Color.BLACK);
		g.drawRect((int)line.x1 - 2, (int)line.y1 - 2 + offset, fm.stringWidth(s) + 5, 15 + 2);
		
		g.drawString(s, 
				(float)line.x1, 
				(float)line.y1 + 12 + offset);
		
	}
	
}
