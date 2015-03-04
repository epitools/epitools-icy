package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import plugins.adufour.ezplug.EzGroup;
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
	public abstract void paintFrame(Graphics2D g, FrameGraph i );
	
	@Override
    public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
    {
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			FrameGraph frame_i = stGraph.getFrame(time_point);
			paintFrame(g, frame_i);
		}
    }
	
	@Override
	public void actionPerformed(ActionEvent e) {
		new AnnounceFrame("I will save your World",5);
	}
	
	@Override
	public JPanel getOptionsPanel() {
		JPanel optionPanel = new JPanel(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.insets = new Insets(2, 10, 2, 5);
        gbc.fill = GridBagConstraints.BOTH;
        optionPanel.add(new JLabel("Hello World!"), gbc);
        
        JButton OKButton = new JButton("Save Data Button");
        OKButton.addActionListener(this);
        optionPanel.add(OKButton,gbc);
        
        
		return optionPanel;
	}

}
