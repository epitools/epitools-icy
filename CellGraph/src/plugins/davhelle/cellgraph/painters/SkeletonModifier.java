package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import icy.type.point.Point5D;
import icy.util.EventUtil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.LinkedList;

/**
 * Skeleton Modifier. Heavily copied from 
 * package plugins.tprovoost.painting;
 * 
 * Supports
 * Color switch with SPACE bar (White<>Black)
 * Undo/Redo (CTRL + Z) /+ SHIFT
 * 
 * @author Davide Heller
 *
 */
public class SkeletonModifier extends Overlay {

	private final LinkedList<SkeletonShape> shapes;
	private final LinkedList<SkeletonShape> undo;
	private Point startPoint;
	private Point currentPoint;
	private Point mouseMovePoint;
	private SkeletonShape currentShape;
	private Color currentColor;


	public SkeletonModifier(){
		super("Skeleton Modifier");

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
		if ((mouseMovePoint != null) && (currentShape == null))
		{
			g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND,
					BasicStroke.JOIN_ROUND));
			g2.setColor(currentColor);
			g2.drawLine(mouseMovePoint.x, mouseMovePoint.y, mouseMovePoint.x, mouseMovePoint.y);
		}

		g2.dispose();
	}


}
