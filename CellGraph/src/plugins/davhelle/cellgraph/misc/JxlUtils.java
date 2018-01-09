/**
* Creation date: (19/12/2005) 
* @author: Svyatoslav Urbanovych surban@bigmir.net  svyatoslav.urbanovych@gmail.com 
*
* Adapted on 29.06.2016
* @author davide heller <davide.heller@imls.uzh.ch>
*/
 
/********************************************************************************
* 
* Copyright (C) 2005  Svyatoslav Urbanovych 
* 
* This program is free software; you can redistribute it and/or 
* modify it under the terms of the GNU General Public License 
* as published by the Free Software Foundation; either version 2 
* of the License, or (at your option) any later version. 
 
* This program is distributed in the hope that it will be useful, 
* but WITHOUT ANY WARRANTY; without even the implied warranty of 
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
* GNU General Public License for more details. 
 
* You should have received a copy of the GNU General Public License 
* along with this program; if not, write to the Free Software 
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
*********************************************************************************/ 

package plugins.davhelle.cellgraph.misc;

import java.awt.Color;
import java.util.HashMap;

import jxl.format.Colour;

/**
 * Utilities for Excel sheet modification with jxl
 * 
 * @author Davide Heller
 *
 */
public class JxlUtils {
	private static HashMap<Color,Colour> colorsCache;

	public static Colour getNearestColour(Color awtColor){ 
		 if(colorsCache==null) colorsCache=new HashMap<Color,Colour>(); 
		    Colour color = (Colour) colorsCache.get(awtColor); 
		     
		    if (color == null) 
		    { 
		        Colour[] colors = Colour.getAllColours(); 
		        if ((colors != null) && (colors.length > 0)) 
		        { 
		            Colour crtColor = null; 
		            int[] rgb = null; 
		            int diff = 0; 
		            int minDiff = 999; 
		 
		            for (int i = 0; i < colors.length; i++) 
		            { 
		                crtColor = colors[i]; 
		                rgb = new int[3]; 
		                rgb[0] = crtColor.getDefaultRGB().getRed(); 
		                rgb[1] = crtColor.getDefaultRGB().getGreen(); 
		                rgb[2] = crtColor.getDefaultRGB().getBlue(); 
		 
		                diff = Math.abs(rgb[0] - awtColor.getRed()) + Math.abs(rgb[1] - awtColor.getGreen()) + Math.abs(rgb[2] - awtColor.getBlue()); 
		 
		                if (diff < minDiff) 
		                { 
		                    minDiff = diff; 
		                    color = crtColor; 
		                } 
		            } 
		        } 
		         
		        colorsCache.put(awtColor, color); 
		    } 
		     
		    return color; 
		} 
}
