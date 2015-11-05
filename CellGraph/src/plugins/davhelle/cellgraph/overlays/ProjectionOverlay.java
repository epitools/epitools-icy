package plugins.davhelle.cellgraph.overlays;

import icy.gui.dialog.OpenDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;
import java.util.HashMap;
import java.util.Iterator;

import jxl.write.WritableSheet;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;
import vtk.vtkPoints;
import vtk.vtkPolyData;
import vtk.vtkSimplePointsReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.math.Vector3D;

/**
 * Test Overlay to try out new ideas
 * 
 * @author Davide Heller
 *
 */
public class ProjectionOverlay extends StGraphOverlay {
	
	public static final String DESCRIPTION = 
			"Test overlay to try out new ideas. " +
			"Modify cellgraph.overlays.TestOverlay.java" +
			"to work with this";
	
	HashMap<Node,java.lang.Double> normalZ_map = new HashMap<Node,java.lang.Double>();
	HashMap<Node,java.lang.Double> area_map = new HashMap<Node,java.lang.Double>();

	private Coordinate pixel_size;
		
	/**
	 * @param stGraph graph object for which to create the overlay
	 * @param sequence sequence on which the overlay will be added
	 */
	public ProjectionOverlay(SpatioTemporalGraph stGraph, Sequence sequence) {
		super("Test Overlay", stGraph);

		//use data from sequence window to input pixel size
		pixel_size = new Coordinate(
				sequence.getPixelSizeX(), 
				sequence.getPixelSizeY(), 
				sequence.getPixelSizeZ());

		//Default action. Just displays a welcome message
		new AnnounceFrame("Executed cellgraph.overlays.TestOverlay successfully!",5);
		
		String vtk_path = OpenDialog.chooseFile();
				//"/Users/davide/data/cellsurface/test/gridfit_frame_001.xyz";
		
		vtkPolyData polydata = new vtkPolyData();
		
		//Read individual xyz coords
		vtkSimplePointsReader reader  = new vtkSimplePointsReader();
		reader.SetFileName(vtk_path);
		reader.SetOutput(polydata);
		reader.Update();
		
		vtkPoints points = polydata.GetPoints();        
		int point_no = points.GetNumberOfPoints(); 
		
		double[] height = new double[point_no];

		//insert points into corner_list and graph
		for(int i=0; i<point_no; i++){
			double[] coor_i = points.GetPoint(i);
			height[i] = coor_i[2];
		}
		
		//from: http://icy.bioimageanalysis.org/index.php?display=javaCourse
		//MathUtil.divide(height, sequence.getSizeZ());
		
		//Add image to icy
		IcyBufferedImage height_image = new IcyBufferedImage(
				sequence.getSizeY(), sequence.getSizeX(), 
				sequence.getSizeC(), DataType.DOUBLE);
		
		Array1DUtil.doubleArrayToSafeArray(height, height_image.getDataXY(0), false);
		height_image.dataChanged();
		//Sequence height_sequence = new Sequence(height_image);
		//Icy.getMainInterface().addSequence(height_sequence);
		
		//extract 1 polygon
		FrameGraph frame = stGraph.getFrame(0);
		Iterator<Node> node_it = frame.iterator();

		while(node_it.hasNext()){
			Node cell = node_it.next();
			computeAreaRatio(cell, height_image);
		}
		
		double min = 2;
		double max = -1;
		
		for(Node n: normalZ_map.keySet()){
			double ratio = normalZ_map.get(n);
			
			if(ratio < min)
				min = ratio;
			
			if(ratio > max)
				max = ratio;
		}
		
		super.setGradientMaximum(max);
		super.setGradientMinimum(min);
		
		//default blue -> red color scheme
		super.setGradientScale(0.5);
		super.setGradientShift(0.5);
	
		super.setGradientControlsVisibility(true);

	}
	
	/**
	 * Utility to scale coordinate by a scaling factor
	 * 
	 * @param c coordinate to scale
	 * @param scale scaling values in coordinate form
	 */
	private void scaleCoordinate(Coordinate c, Coordinate scale){
		c.x *= scale.x;
		c.y *= scale.y;
		c.z *= scale.z;
	}

	/**
	 * 
	 * @param cell
	 * @param height_image
	 */
	private void computeAreaRatio(Node cell, IcyBufferedImage height_image) {
		Geometry geo = cell.getGeometry();

		//Compute normal
		Coordinate n = new Coordinate(0, 0, 0);
		
		Coordinate[] vertices = geo.getCoordinates().clone();
		
		for(int i=0; i<vertices.length - 1; i++){
			Coordinate current = new Coordinate(vertices[i]);
			Coordinate next = new Coordinate(vertices[i + 1]);

			current.z = height_image.getData((int)current.y,(int)current.x,0);
			scaleCoordinate(current, pixel_size);
			
			//current = Vector3D.normalize(current);
			
			next.z = height_image.getData((int)next.y,(int)next.x,0);
			scaleCoordinate(next, pixel_size);
			
			//next = Vector3D.normalize(next);
			
			n.x = n.x + ((current.y - next.y) * (current.z + next.z));
			n.y = n.y + ((current.z - next.z) * (current.x + next.x));
			n.z = n.z + ((current.x - next.x) * (current.y + next.y));
		}
		
		//normalize vector
		n = Vector3D.normalize(n);
		
		double conversionXY = pixel_size.x * pixel_size.y;
		
		double area2D = geo.getArea() * conversionXY;
		double area3D = area2D / Math.abs(n.z);
		double ratio = (area3D - area2D) / area2D;
		double percent = ratio * 100;
		
		//Compute area
		//System.out.printf("Projected area in 2Dz:\t%f\n", area2D );
		//System.out.printf("Area normalized in 3D:\t%f\n", area3D );
		//System.out.printf("Ratio: %.3f\n", );
//		String outputString = String.format("[%.0f,%.0f]\t%.2f\t%.2f\t%.2f",
//				cell.getCentroid().getX(),cell.getCentroid().getY(),
//				area2D,area3D,percent);
//		
//		System.out.println(outputString);
		//ratio_map.put(cell, ratio);
		normalZ_map.put(cell, Math.abs(n.z));
		area_map.put(cell, area2D);
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#paintFrame(java.awt.Graphics2D, plugins.davhelle.cellgraph.graphs.FrameGraph)
	 */
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		int fontSize = 1;
    		g.setFont(new Font("Arial", Font.PLAIN, fontSize));
		
		for(Node cell: frame_i.vertexSet()){
			if(normalZ_map.containsKey(cell)){
				
				double ratio = normalZ_map.get(cell);
				
				Color c = super.getScaledColor(ratio);
				
				g.setColor(c);
				
				g.fill((cell.toShape()));
			}
		}
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#specifyLegend(java.awt.Graphics2D, java.awt.geom.Line2D.Double)
	 */
	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		int bin_no = 50;
		double scaling_factor = super.getGradientScale();
		double shift_factor = super.getGradientShift();
		
		String min_value = String.format("%.1f",super.getGradientMinimum());
		String max_value = String.format("%.1f",super.getGradientMaximum());
		
		OverlayUtils.gradientColorLegend_ZeroOne(g, line,
				min_value, max_value, bin_no, scaling_factor, shift_factor);
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#writeFrameSheet(jxl.write.WritableSheet, plugins.davhelle.cellgraph.graphs.FrameGraph)
	 */
	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		
		if(frame.getFrameNo() != 0)
			return;
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Cell area");
		XLSUtil.setCellString(sheet, 2, 0, "Projection bias");
		XLSUtil.setCellString(sheet, 3, 0, "Position");

		int row_no = 1;
		for(Node node: frame.vertexSet()){
			String position = "inner";
			
			
			XLSUtil.setCellNumber(sheet, 0, row_no, node.getTrackID());
			XLSUtil.setCellNumber(sheet, 1, row_no, area_map.get(node));
			XLSUtil.setCellNumber(sheet, 2, row_no, normalZ_map.get(node));
			
			if(node.onBoundary())
				position = "border";
			
			XLSUtil.setCellString(sheet, 3, row_no, position);
			
			row_no++;
		}
	}

}
