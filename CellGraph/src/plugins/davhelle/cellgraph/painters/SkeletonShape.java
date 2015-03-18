package plugins.davhelle.cellgraph.painters;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.util.ArrayList;

public class SkeletonShape {

	protected Stroke stroke;
    protected Shape shape;
    protected Color shapeColor;
    protected int thickness;
    protected boolean fill;
    protected boolean isEditable;
    ArrayList<Point> list = new ArrayList<Point>();
    
    public void update(Point p)
    {
        list.add(p);
        GeneralPath path = new GeneralPath();
        for (int i = 0; i < list.size() - 1; ++i)
        {
            Point p1 = list.get(i);
            Point p2 = list.get(i + 1);
            path.append(new Line2D.Double(p1.x, p1.y, p2.x, p2.y), true);
        }
        shape = path;
    }

    public SkeletonShape(Point p, Color currentColor)
    {
    	
        shape = new GeneralPath();

        if (p != null)
            update(p);
    	
        shapeColor = currentColor;
        thickness = 2;
        stroke = new BasicStroke(getThickness(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        fill = false;
        isEditable = true;
    }

    public void drawShape(Graphics2D g)
    {
        final Graphics2D g2 = (Graphics2D) g.create();

        g2.setColor(getShapeColor());

        if (fill)
        {
            if (list.size() == 1)
            {
                Point p = list.get(0);
                g2.drawLine(p.x, p.y, p.x, p.y);
            }
            else
                g2.fill(shape);
        }
        else
        {
            if (list.size() == 1)
            {
                Point p = list.get(0);
                g2.drawLine(p.x, p.y, p.x, p.y);
            }
            else
                g2.draw(shape);
        }

        g2.dispose();
    }

    public boolean isFill()
    {
        return fill;
    }

    public void setFill(boolean fill)
    {
        this.fill = fill;
    }

    public boolean isEditable()
    {
        return isEditable;
    }

    public void setEditable(boolean isEditable)
    {
        this.isEditable = isEditable;
    }

    public Color getShapeColor()
    {
        return shapeColor;
    }

    public void setShapeColor(Color shapeColor)
    {
        this.shapeColor = shapeColor;
    }

    public int getThickness()
    {
        return thickness;
    }

    // public void setThickness(int thickness)
    // {
    // this.thickness = thickness;
    // }

    public void setStroke(Stroke stroke)
    {
        this.stroke = stroke;
    }

    public Stroke getStroke()
    {
        return stroke;
    }
}
