package plugins.davhelle.cellgraph;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import icy.canvas.IcyCanvas;
import icy.canvas.Layer;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.painter.Painter;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.tprovoost.painting.shapes.Cloud;
import plugins.tprovoost.painting.shapes.PaintingShape;

public class CellEditor extends EzPlug{
	
	EzVarSequence				varInputSeq;
	EzVarSequence				varOutputSeq;
	
	@Override
	protected void initialize() {
		varInputSeq = new EzVarSequence("Input sequence");
		varOutputSeq = new EzVarSequence("Output sequence");
		super.addEzComponent(varInputSeq);
		super.addEzComponent(varOutputSeq);
	}

	@Override
	protected void execute() {
		ArrayList<Viewer> viewer_list = Icy.getMainInterface().getViewers(varInputSeq.getValue());
		System.out.println("Found "+viewer_list.size()+" viewers attached to selected input sequecence");
		
		if(viewer_list.size() > 0){
			//Prepare input
			Viewer first = viewer_list.get(0);
			IcyCanvas first_canvas = first.getCanvas();
			IcyBufferedImage img = first_canvas.getCurrentImage();
			System.out.println(img.getSizeC());
			ArrayList<Layer> layer_list = first_canvas.getLayers();
			System.out.println("Found "+layer_list.size()+" layers attached to first viewer:");
			for(Layer l: layer_list){
				System.out.println("\t"+l.getName());
				if(l.getName().equals("Painting")){
					Painter extracted = l.getPainter();
					varOutputSeq.getValue().addPainter(extracted);
					//extracted.paint(g, sequence, canvas)
				}
			}
		}
		
	}
	
//	private void PaintOnOuput(){
//		
//		 IcyBufferedImage img;
//         int channelAffected = paintingTools.getChannelAffected();
//         if (channelAffected != -1)
//         {
//             img = IcyBufferedImageUtil.extractChannel(canvas.getCurrentImage(), channelAffected);
//         }
//         else
//         {
//             img = canvas.getCurrentImage();
//         }
//         BufferedImage imgBuff = IcyBufferedImageUtil.toBufferedImage(img, new BufferedImage(img.getWidth(),
//                 img.getHeight(), BufferedImage.TYPE_BYTE_GRAY));
//         Graphics2D g2d;
//         if (img.getSizeC() == 1)
//         {
//             g2d = imgBuff.createGraphics();
//         }
//         else
//             g2d = (Graphics2D) img.createGraphics();
//         PaintingShape shape = shapes.pollLast();
//         if (shape != null)
//         {
//             if (shape instanceof Cloud)
//                 g2d.setStroke(new BasicStroke(paintingTools.getThickness(), BasicStroke.CAP_ROUND,
//                         BasicStroke.JOIN_ROUND));
//             else
//                 g2d.setStroke(new BasicStroke(paintingTools.getThickness()));
//             shape.drawShape(g2d);
//         }
//         g2d.dispose();
//         if (channelAffected != -1)
//         {
//             img = IcyBufferedImage.createFrom(imgBuff);
//             canvas.getCurrentImage().setDataXY(channelAffected, img.getDataXY(0));
//         }
//         // Icy.addSequence(new Sequence(img));
//         canvas.getSequence().dataChanged();
//	}

	@Override
	public void clean() {
		// TODO Auto-generated method stub
		
	}

}
