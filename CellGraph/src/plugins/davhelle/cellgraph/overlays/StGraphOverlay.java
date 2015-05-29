package plugins.davhelle.cellgraph.overlays;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.gui.dialog.SaveDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.system.IcyExceptionHandler;
import icy.util.XLSUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import plugins.adufour.vars.gui.swing.SwingVarEditor;
import plugins.adufour.vars.lang.Var;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.adufour.vars.lang.VarDouble;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;

/**
 * Base class for all overlays that interpret the spatio-temporal graph
 * 
 * @author Davide Heller
 *
 */
public abstract class StGraphOverlay extends Overlay implements ActionListener{

	/**
	 * Spatio-temporal graph interpreted  
	 */
	SpatioTemporalGraph stGraph;
	
	/**
	 * Boolean value to define whether to show the legend or not
	 * has a listener attached to detect user changes (i.e. when placed in the option panel)
	 */
	private final VarBoolean  showLegend = new VarBoolean("Show Legend", true)
	{
		public void setValue(Boolean newValue)
		{
			if (getValue().equals(newValue)) return;
			
			super.setValue(newValue);
			
			painterChanged();
			
		}
	};
	
	/**
	 * Minimum value to define the color gradient
	 * has a listener attached to detect user changes (i.e. when placed in the option panel)
	 */
	private final VarDouble minGradient = new VarDouble("min",0.0)
	{
		public void setValue(Double newValue)
		{
			if (getValue().equals(newValue)) return;
			
			super.setValue(newValue);
			
			painterChanged();
			
		}
	};
	
	/**
	 * Maximum value to define the color gradient
	 * has a listener attached to detect user changes (i.e. when placed in the option panel)
	 */
	private final VarDouble maxGradient = new VarDouble("max",1.0)
	{
		public void setValue(Double newValue)
		{
			if (getValue().equals(newValue)) return;
			
			super.setValue(newValue);
			
			painterChanged();
			
		}
	};
	
	
	
	/**
	 * Creates a new Overlay to interpret the 
	 * spatio-temporal graph (stGraph) in input
	 * 
	 * @param name Overlay name to display in the Layer menu
	 * @param stGraph spatio-temporal graph to analyze
	 */
	public StGraphOverlay(String name, SpatioTemporalGraph stGraph) {
		super(name);
		this.stGraph = stGraph;	
	}
	
	/**
	 * Method to display an overlay on the frame i of the input sequence
	 * 
	 * @param g graphics handle
	 * @param frame_i frameGraph to visualize
	 */
	public abstract void paintFrame(Graphics2D g, FrameGraph frame_i );
	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			FrameGraph frame_i = stGraph.getFrame(time_point);
			paintFrame(g, frame_i);
		}
		
		if(showLegend.getValue())
			paintLegend(g,sequence,canvas);
		
    }
	
	/**
	 * Paints a legend on the viewer
	 * 
	 * @param g graphics handle
	 * @param sequence sequence on which to paint the legend
	 * @param canvas
	 */
	protected void paintLegend(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
		if (g == null || !(canvas instanceof IcyCanvas2D)) return;
        
        IcyCanvas2D c2 = (IcyCanvas2D) canvas;
        Graphics2D g2 = (Graphics2D) g.create();
        
        g2.setColor(Color.GREEN);
        float thickness = 2;
        
        double length = 150;

        final Line2D.Double line = new Line2D.Double();
        
        //case VIEWER_TOP_LEFT:
        //supply the rectangle to draw within
        g2.transform(c2.getInverseTransform());
        line.x1 = canvas.getCanvasSizeX() * 0.05;
        line.x2 = line.x1 + length;// * c2.getScaleX();
        line.y1 = canvas.getCanvasSizeY() * 0.05;
        line.y2 = line.y1 + 20;
        
        specifyLegend(g2,line);
        
        g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        //g2.draw(line);
        
        g2.dispose();
		
	}

	/**
	 * Specify legend
	 * 
	 * @param g
	 * @param line
	 */
	public abstract void specifyLegend(Graphics2D g, Line2D.Double line);

	@Override
	public void actionPerformed(ActionEvent e) {
		try {
			String file_name = SaveDialog.chooseFile(
					"Please choose where to save the excel Sheet",
					"/Users/davide/",
					"test_file", XLSUtil.FILE_DOT_EXTENSION);
			
			if(file_name == null)
				return;
				
			WritableWorkbook wb = XLSUtil.createWorkbook(file_name);
			
			for(int i=0; i<stGraph.size(); i++){
				String sheetName = String.format("Frame %d",i);
				WritableSheet sheet = XLSUtil.createNewPage(wb, sheetName);
				writeFrameSheet(sheet,stGraph.getFrame(i));
			}
			
			XLSUtil.saveAndClose(wb);
			
			new AnnounceFrame("XLS file exported successfully to: "+file_name,10);
			
		} catch (WriteException writeException) {
			IcyExceptionHandler.showErrorMessage(writeException, true, true);
		} catch (IOException ioException) {
			IcyExceptionHandler.showErrorMessage(ioException, true, true);
		}
		
		
	}
	
	/**
	 * Writes the desired information extracted from the frameGraph provided 
	 * into the excel sheet.
	 * 
	 * @param sheet excel sheet to be written
	 * @param frame frame from which to extract the information to write
	 */
	abstract void writeFrameSheet(WritableSheet sheet, FrameGraph frame);
	
	@Override
	public JPanel getOptionsPanel() {
		
		JPanel optionPanel = new JPanel(new GridBagLayout());
		
		//Legend output
		addOptionPanelVariable(optionPanel,showLegend);
        
		//Gradient min
        addOptionPanelVariable(optionPanel,minGradient);
        
		//Gradient max
        addOptionPanelVariable(optionPanel,maxGradient);
        
		//Excel output button
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 10, 2, 5);
        gbc.fill = GridBagConstraints.BOTH;
        optionPanel.add(new JLabel("Export data to Excel: "), gbc);
        
        JButton OKButton = new JButton("Choose File");
        OKButton.addActionListener(this);
        optionPanel.add(OKButton,gbc);
        
        
		return optionPanel;
	}
	
	/**
	 * Adds the variable to the OptionPanel
	 * 
	 * @param optionPanel icy option panel
	 * @param variable Var variable to add
	 */
	@SuppressWarnings("rawtypes")
	private void addOptionPanelVariable(Container optionPanel, Var variable){
		GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.insets = new Insets(2, 10, 2, 5);
        gbc.fill = GridBagConstraints.BOTH;
        optionPanel.add(new JLabel(variable.getName()), gbc);
		
        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        SwingVarEditor<?> editor = (SwingVarEditor<?>) variable.createVarEditor(true);
        optionPanel.add(editor.getEditorComponent(), gbc);
	}
	
	/**
	 * @param state set true if legend should be shown, false if suppressed
	 */
	public void setLegendVisibility(boolean state){
		showLegend.setValue(state);
	}
	
	/**
	 * @param max maximum value for color gradient
	 */
	public void setMaximumGradient(double max){
		maxGradient.setValue(max);
	}
	
	/**
	 * @return
	 */
	public double getMaximumGradient(){
		return maxGradient.getValue();
	}
	
	/**
	 * @param min minimum value for the color gradient
	 */
	public void setMinimumGradient(double min){
		minGradient.setValue(min);
	}
	
	/**
	 * @return
	 */
	public double getMinimumGradient(){
		return minGradient.getValue();
	}
	
	/**
	 * @return true if the legend is visible
	 */
	public boolean isLegendVisible(){
		return showLegend.getValue();
	}

}
