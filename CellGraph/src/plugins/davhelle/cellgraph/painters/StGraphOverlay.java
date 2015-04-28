package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.gui.dialog.SaveDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.math.UnitUtil;
import icy.math.UnitUtil.UnitPrefix;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.system.IcyExceptionHandler;
import icy.util.XLSUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.vars.gui.swing.SwingVarEditor;
import plugins.adufour.vars.lang.VarBoolean;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Base class for all overlays that interpret the spatiotemporal graph
 * 
 * @author Davide Heller
 *
 */
public abstract class StGraphOverlay extends Overlay implements ActionListener{

	//fields are available to classes in the same package
	
	/* 
	 * Spatio-temporal graph interpreted  
	 */
	SpatioTemporalGraph stGraph;
	
	/* 
	 * Data to be returned in excel format 
	 */
	HashMap<Node,Double> data; 
	
	/**
	 * Boolean value to define whether to show the legend or not
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
	 * Creates a new Overlay to interpret the 
	 * to be set with spatio-temporal graph (stGraph) in input
	 * 
	 * @param name
	 * @param stGraph
	 */
	public StGraphOverlay(String name, SpatioTemporalGraph stGraph) {
		super(name);
		this.stGraph = stGraph;	
		this.data = new HashMap<Node, Double>();
	}
	
	/**
	 * Method to supply the EzVariables to the interface
	 * 
	 * @return
	 */
	//public abstract EzGroup getEzGroup();
	
	/**
	 * Method to display an overlay on the frame i of the input sequence
	 * 
	 * @param g
	 * @param frame_i
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
	
	private void paintLegend(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
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
	
	abstract void writeFrameSheet(WritableSheet sheet, FrameGraph frame);
	
	@Override
	public JPanel getOptionsPanel() {
		
		JPanel optionPanel = new JPanel(new GridBagLayout());
		
		//Legend output
		GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.insets = new Insets(2, 10, 2, 5);
        gbc.fill = GridBagConstraints.BOTH;
        optionPanel.add(new JLabel(showLegend.getName()), gbc);
		
        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        SwingVarEditor<?> editor = (SwingVarEditor<?>) showLegend.createVarEditor(true);
        optionPanel.add(editor.getEditorComponent(), gbc);
        
		//Excel output
		gbc = new GridBagConstraints();
        
        gbc.insets = new Insets(2, 10, 2, 5);
        gbc.fill = GridBagConstraints.BOTH;
        optionPanel.add(new JLabel("Export data to Excel: "), gbc);
        
        JButton OKButton = new JButton("Choose File");
        OKButton.addActionListener(this);
        optionPanel.add(OKButton,gbc);
        
        
		return optionPanel;
	}

}
