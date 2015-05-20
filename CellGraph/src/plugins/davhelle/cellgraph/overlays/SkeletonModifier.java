package plugins.davhelle.cellgraph.overlays;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.file.Saver;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.point.Point5D;
import icy.util.EventUtil;
import ij.ImagePlus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import loci.formats.FormatException;
import plugins.davhelle.cellgraph.io.FileNameGenerator;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.SegmentationProgram;

/**
 * A class to modify skeleton images with a simple painter method. <br>
 * 
 * Code is based in large extend on the painting plugin(
 * plugins.tprovoost.painting) by Thomas Provoost. <br>
 * 
 * Supports <br>
 * - Mode/Color switch with [SPACE] bar (White<>Black) <br>
 * - Undo/Redo (CTRL + Z) /+ SHIFT <br>
 * - Apply changes with [ENTER] key <br>
 * 
 * @author Davide Heller
 *
 */
public class SkeletonModifier extends Overlay {

	/**
	 * Painting Shapes added to the sequence
	 */
	private final LinkedList<SkeletonShape> shapes;
	/**
	 * Undone Shapes removed from the sequence
	 */
	private final LinkedList<SkeletonShape> undo;
	/**
	 * Starting Point of a new painting shape 
	 */
	private Point startPoint;
	/**
	 * Current Point in the new Painting shape
	 */
	private Point currentPoint;
	/**
	 * Current Mouse location
	 */
	private Point mouseMovePoint;
	/**
	 * Current Shape being drawn
	 */
	private SkeletonShape currentShape;
	/**
	 * Current Color
	 */
	private Color currentColor;
	
	/**
	 * Image to which the modifications should be applied
	 */
	private Sequence skeletonOutputSequence;
	/**
	 * Viewer attached to the output sequence
	 */
	private Viewer output_viewer;
	/**
	 * Flag whether to write the modifications to the disc
	 */
	private boolean saveSkeletonImage;
	/**
	 * Painter lock to suppress active drawing while saving
	 */
	private boolean painterLockFree;

	/**
	 * Initializes modification Overlay to support Painter operations
	 * 
	 * @param skeletonOutputSequence Sequence on which to apply modifications
	 * @param saveSkeletonImage set true if modifications should be written to file
	 */
	public SkeletonModifier(Sequence skeletonOutputSequence, boolean saveSkeletonImage){
		super("Skeleton Modifier");
		
		this.skeletonOutputSequence = skeletonOutputSequence;
		this.saveSkeletonImage = saveSkeletonImage;
		ArrayList<Viewer> output_viewers = Icy.getMainInterface().getViewers(skeletonOutputSequence);
		output_viewer = output_viewers.get(0);
		painterLockFree = true;
		
		shapes = new LinkedList<SkeletonShape>();
		undo = new LinkedList<SkeletonShape>();
		startPoint = null;
		currentPoint = null;
		mouseMovePoint = null;
		currentShape = null;
		currentColor = Color.WHITE;
	}

	@Override
	public void mousePressed(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas)
	{
		if (e.isConsumed())
			return;
		if (imagePoint == null)
			return;

		int x = (int) imagePoint.x;
		int y = (int) imagePoint.y;

		startPoint = new Point(x, y);

		shapes.add(new SkeletonShape(startPoint,currentColor));
		currentShape = shapes.getLast();
		painterChanged();

		e.consume();
	}

	@Override
	public void mouseMove(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas)
	{
		if (imagePoint == null)
			return;

		mouseMovePoint = new Point((int) imagePoint.x, (int) imagePoint.y);
		painterChanged();
	}

	@Override
	public void mouseDrag(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas)
	{
		if (e.isConsumed())
			return;
		if (imagePoint == null)
			return;

		if (startPoint == null)
		{
			mousePressed(e, imagePoint, canvas);
			return;
		}

		int x = (int) imagePoint.x;
		int y = (int) imagePoint.y;

		currentPoint = new Point(x, y);
		if (currentShape == null)
		{
			shapes.add(new SkeletonShape(startPoint,currentColor));
			currentShape = shapes.getLast();
		}
		else
		{
			currentShape.update(currentPoint);
		}

		painterChanged();
	}

	@Override
	public void mouseReleased(MouseEvent e, Point5D.Double imagePoint, IcyCanvas canvas)
	{

		if (currentShape != null)
		{
			currentShape.setEditable(false);
			currentShape = null;
			startPoint = null;
			currentPoint = null;

		}


		painterChanged();
	}

	@Override
	public void keyPressed(KeyEvent e, Point5D.Double imagePoint, IcyCanvas canvas)
	{
		
        if (EventUtil.isControlDown(e))
        {
            if (e.getKeyCode() == KeyEvent.VK_Z)
            {
                if (EventUtil.isShiftDown(e))
                {
                	SkeletonShape s = undo.pollFirst();
                    if (s != null)
                    {
                        shapes.add(s);
                        painterChanged();
                    }
                }
                else
                {
                    SkeletonShape s = shapes.pollLast();
                    if (s != null)
                    {
                        undo.addFirst(s);
                        painterChanged();
                    }
                }
            }
        }
		if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			startPoint = null;
			currentPoint = null;
			if (currentShape != null)
			{
				currentShape.setEditable(false);
				currentShape = null;
			}
		}
		
		if (e.getKeyCode() == KeyEvent.VK_SPACE){
			if (currentColor == Color.WHITE)
				currentColor = Color.BLACK;
			else
				currentColor = Color.WHITE;
			
			painterChanged();
		}
		
		if(e.getKeyCode() == KeyEvent.VK_ENTER){
			if(!shapes.isEmpty()){
				
				//suppress the paint method from drawing the tip
				painterLockFree = false;
				
				IcyBufferedImage img = applyModifications();
				if(saveSkeletonImage){
					String savedFileName = saveModifications(img);
					System.out.println("Updated skeleton file: "+savedFileName);
				}
				
				shapes.clear();
				undo.clear();
				
				painterChanged();
				
				painterLockFree = true;
			}
			else{
				new AnnounceFrame("No modifications detected. Please draw first!");
			}
		}
	}

	/**
	 * Permanently write the modified image to the original file location
	 * 
	 * @param img image containing the modified image
	 * @return the file path to which the modifications have been written
	 */
	private String saveModifications(IcyBufferedImage img) {
		
		String file_name = skeletonOutputSequence.getFilename();
		File skeleton_file = new File(file_name);
		FileNameGenerator skeleton_file_name_generator = 
				new FileNameGenerator(
						skeleton_file,
						InputType.SKELETON,
						true, 
						SegmentationProgram.SeedWater);
		
		int current_time_point = output_viewer.getPositionT();
		String current_file_name = skeleton_file_name_generator.getFileName(current_time_point);
		
		System.out.println("Saving changes to:"+current_file_name);
		try {
			
			//check existence!
			File current_file = new File(current_file_name);
			if(!current_file.exists())
				throw new IOException("File doesn't exists");
			
			//attempt saving
			Saver.saveImage(img, current_file, true);
			
		} catch (FormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return current_file_name;
		
	}

	/**
	 * Method to apply the painting overlay of the input sequence
	 * to the output sequence.
	 * 
	 * Based on the Painting plugin by
	 * Thomas Provoost, http://icy.bioimageanalysis.org/plugin/Painting
	 * 
	 * specifically the PaintingPainter.onMouseRelease() method
	 * 
	 * The implementing idea is to apply the paint method
	 * of the input overlay directly to the raw data of 
	 * the output data by extracting the needed data objects.
	 * I.e. Graphics device, Sequence and IcyCanvas 
	 * <-> paint(g, sequence, canvas)
	 * 
	 * @param modifications Painter/Overlay of the input sequence to be permanently applied
	 * @param output_viewer Viewer associated with the output sequence (by default the 1st viewer is chosen)
	 * @return the modified image
	 */private IcyBufferedImage applyModifications() {
		
		IcyCanvas output_canvas = output_viewer.getCanvas();
		IcyBufferedImage img = output_canvas.getCurrentImage();
		int skleton_image_type = BufferedImage.TYPE_BYTE_GRAY;
		
		BufferedImage imgBuff = IcyBufferedImageUtil.toBufferedImage(img, new BufferedImage(img.getWidth(),
				img.getHeight(), skleton_image_type));
		
		
		/* Future mechanism for image backup - reproducibility
		 * 
		 * 1. make a new buffered image with the same dimensions
		 * 2. fill it with a neutral (e.g. gray) color to discern the modifications
		 * 3. paint over it by using a custom function paintModifications(graphics) i.e. no canvas needed
		 * 4. save the image in the a sub-folder in the original skeleton location. 
		 * 		
		 * 5. add a time stamp to the file name such that the sequence of application is remembered
		 * 
		 * Think of a future update to re-apply these modifications
		 * alternatively store the shapes somehow.
		 * 
		 */
//		BufferedImage backupImgBuff = new BufferedImage(imgBuff.getWidth(),
//				imgBuff.getHeight(), skleton_image_type);
//		Saver.saveImage(img, current_file, true);
		
		//paint on top of sequence
		Graphics2D g2d = imgBuff.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		this.paint(g2d, skeletonOutputSequence, output_canvas);
		g2d.dispose();
		
		//reskeletonize image
		BufferedImage imgBuff_skeletonized = reskeletonize(imgBuff);

		//update sequence
		img = IcyBufferedImage.createFrom(imgBuff_skeletonized);
		output_canvas.getCurrentImage().setDataXY(0, img.getDataXY(0));

		output_canvas.getSequence().dataChanged();
		
		return img;
	}
	
	/**
	 * Re-skeletonization method to produce unambiguos skeletons
	 * 
	 * @param imgBuff original image
	 * @return skeletonized image
	 */
	private BufferedImage reskeletonize(BufferedImage imgBuff) {
		ImagePlus img = new ImagePlus("Corrected Image", imgBuff);
		ij.IJ.run(img, "Make Binary", "");
		ij.IJ.run(img, "Skeletonize", "");
		return img.getBufferedImage();
	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		if ((g == null) || !(canvas instanceof IcyCanvas2D))
			return;

		final Graphics2D g2 = (Graphics2D) g.create();

		for (SkeletonShape shape : shapes)
		{
            g2.setStroke(shape.getStroke());
            g2.setColor(shape.getShapeColor());
            shape.drawShape(g2);
		}

		//Mouse tip with drawing color
		if ((mouseMovePoint != null) 
				&& (currentShape == null) 
				&& painterLockFree)
		{
			g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND));
			g2.setColor(currentColor);
			g2.drawLine(mouseMovePoint.x, mouseMovePoint.y, mouseMovePoint.x, mouseMovePoint.y);
		}

		g2.dispose();
	}


}
