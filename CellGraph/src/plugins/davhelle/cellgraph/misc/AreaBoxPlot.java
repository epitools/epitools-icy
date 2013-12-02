package plugins.davhelle.cellgraph.misc;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.main.Icy;
import icy.plugin.abstract_.PluginActionable;
import icy.swimmingPool.SwimmingObject;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.BoxAndWhiskerCalculator;
import org.jfree.data.statistics.BoxAndWhiskerXYDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerXYDataset;

import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

public class AreaBoxPlot extends PluginActionable{

	private SpatioTemporalGraph stGraph;
	JPanel mainPanel = GuiUtil.generatePanel("Graph");
    IcyFrame mainFrame = GuiUtil.generateTitleFrame("Chart demo", mainPanel, 
    		new Dimension(300, 100), true, true, true,true);
    
    
    public void run(){
    	if(Icy.getMainInterface().getSwimmingPool().hasObjects("stGraph", true))

			for ( SwimmingObject swimmingObject : 
				Icy.getMainInterface().getSwimmingPool().getObjects(
						"stGraph", true) ){

				if ( swimmingObject.getObject() instanceof SpatioTemporalGraph ){

					this.stGraph = (SpatioTemporalGraph) swimmingObject.getObject();	

    	
					//create Boxplot
					BoxAndWhiskerXYDataset dataset = createDataset();
					JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
							"Box and Whisker Chart", "Cell class", "Area", dataset, true);
					chart.setBackgroundPaint(Color.white);

					ChartPanel chartPanel = new ChartPanel(chart);
					chartPanel.setFillZoomRectangle(true);
					chartPanel.setMouseWheelEnabled(true);
					chartPanel.setPreferredSize(new Dimension(500, 270));


					//Send to icy
					mainPanel.add(chartPanel);
					mainFrame.pack();

					addIcyFrame(mainFrame);

					mainFrame.setVisible(true);
					mainFrame.center();
					mainFrame.requestFocus();
				}
			}
    	else
			new AnnounceFrame("No spatio temporal graph found in ICYsp, please run CellGraph plugin first!");
	}
    
    private BoxAndWhiskerXYDataset createDataset(){
    	DefaultBoxAndWhiskerXYDataset dataset = new 
    			DefaultBoxAndWhiskerXYDataset("Test");

    	int day = 1;
    	for(CellColor cell_color: CellColor.values()){
    		
    		ArrayList<Double> values = new ArrayList<Double>();
    		for(Node cell: stGraph.getFrame(0).vertexSet()){
				if(cell.getColorTag() == cell_color.getColor()){
					values.add(cell.getGeometry().getArea());
				}
    		}
    		
    		Date date = new Date(2013,12,day++);
    		dataset.add(date, 
    				 BoxAndWhiskerCalculator.calculateBoxAndWhiskerStatistics(values));
    	}
    	
    	return dataset;
    }

}
