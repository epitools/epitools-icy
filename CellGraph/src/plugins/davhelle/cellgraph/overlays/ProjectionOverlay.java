package plugins.davhelle.cellgraph.overlays;

import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;
import icy.util.XLSUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D.Double;
import java.io.File;
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
 * <b>ProjectionOverlay</b> visualizes as color gradient the magnitude of the z-component 
 * from the surface normal of every cell. <p> The overlay uses the estimated height map 
 * (e.g. from EpiTools for Matlab: x,y,z coordinates) to assign z-coordinates to all coordinates of the cell’s
 * polygon. To compute the apporximated 3D surface normal for every cells we use 
 * Newell’s method [1]. The z-component of the surface normal is useful to estimate 
 * the area projection bias through the proportional dependence between 3D and 
 * projected 2D area [2]. <p>All values can be exported in an excel sheet through the 
 * layer options menu in icy.
 * 
 * <p>[1] Sutherland, Evan E., Robert F. Sproull, and Robert a. Schumacker. 1974. “A Characterization of Ten Hidden-Surface Algorithms.” ACM Computing Surveys 6 (1): 1–55. doi:10.1145/356625.356626
 * <p>[2] John M. Snyder and Alan H. Barr. 1987. Ray tracing complex models containing surface tessellations. ACM Computer Graphics 21 (4): 119-128. doi:10.1145/37401.37417
 * 
 * @author Davide Heller
 *
 */
public class ProjectionOverlay extends StGraphOverlay {
	
	public static final String DESCRIPTION = 
			"Visualizes the z-component from the surface normal<br/>" +
			" of every cell by reading the surface estimation <br/>" +
			" saved by the projection module in EpiTools for Matlab.<br/><br/>" +
			"[Please verify the pixel size in the sequence properties<br/>" +
			" to ensure correct scaling!]";
	
	HashMap<Node,Coordinate> normal_map = new HashMap<Node,Coordinate>();

	private Coordinate pixel_size;
	private Sequence sequence;
	private boolean analyzeAllFrames;
		
	/**
	 * @param stGraph graph object for which to create the overlay
	 * @param sequence sequence on which the overlay will be added
	 */
	public ProjectionOverlay(
			SpatioTemporalGraph stGraph, 
			Sequence sequence, 
			String surface_filePath,
			boolean analyzeAllFrames) {
		super("Projection overlay", stGraph);

		//use data from sequence window to input pixel size
		this.pixel_size = new Coordinate(
				sequence.getPixelSizeX(), 
				sequence.getPixelSizeY(), 
				sequence.getPixelSizeZ());
		
		this.analyzeAllFrames = analyzeAllFrames;
		
		//initialize gradient parameters
		super.setGradientMaximum(-1);
		super.setGradientMinimum(2);
		super.setGradientScale(0.5);//default blue -> red color scheme
		super.setGradientShift(0.5);
		super.setGradientControlsVisibility(true);
		
		this.sequence = sequence;
		String vtk_path = surface_filePath;

		if(analyzeAllFrames){
			String base_path = vtk_path.substring(0, vtk_path.length()-7);
			for(int i=1; i <= sequence.getSizeT(); i++){
				File file_path = new File(String.format("%s%03d.vtk", base_path,i));
				if(file_path.exists()){
					computeSurfaceNormals(file_path.getAbsolutePath(), i-1);
					System.out.printf("Mesh file successfully read: %s\n",
							file_path.getAbsolutePath());
				}
				else{
					System.out.printf("Error, mesh file doesn't exist: %s\n",
							file_path.getAbsolutePath());
				}
			}
		}
		else{
			int frame_no = 0;
			computeSurfaceNormals(vtk_path, frame_no);
		}

	}

	/**
	 * Reads the height information from the file at surface_path and
	 * computes the surface normal for every cell in the FrameGraph at
	 * position i in the spatio Temporal graph associated with the overlay.
	 * 
	 * @param surface_path Path at which to find the surface file
	 * @param frame_no Number of the frame from which to use the cells
	 */
	private void computeSurfaceNormals(String surface_path, int frame_no) {
		
		IcyBufferedImage height_image = readHeightImage(surface_path);
		
		//Conversion to sequence for test visualization
		//Sequence height_sequence = new Sequence(height_image);
		//Icy.getMainInterface().addSequence(height_sequence);
		
		FrameGraph frame = super.stGraph.getFrame(frame_no);
		Iterator<Node> node_it = frame.iterator();

		while(node_it.hasNext()){
			Node cell = node_it.next();
			computeCellNormal(cell, height_image);
		}
		
		updateColorGradient();
		
	}

	/**
	 * Updates color gradient according to the current values in the z-map
	 */
	private void updateColorGradient() {
		double min = super.getGradientMinimum();
		double max = super.getGradientMaximum();
		
		for(Node n: normal_map.keySet()){
			double z = Math.abs(normal_map.get(n).z);
			
			if(z < min)
				min = z;
			
			if(z > max)
				max = z;
		}
		
		super.setGradientMaximum(max);
		super.setGradientMinimum(min);
	}

	/**
	 * Reads simple vtk points from text file (x,y,z) written by the projection 
	 * module by EpiTools from Matlab. 
	 * 
	 * @param surface_path Path of the surface text file
	 * @return Image where pixel intensities correspond to the height information (z)
	 */
	private IcyBufferedImage readHeightImage(String surface_path) {
		vtkPolyData polydata = new vtkPolyData();
		
		//Read individual xyz coords
		vtkSimplePointsReader reader  = new vtkSimplePointsReader();
		reader.SetFileName(surface_path);
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
				this.sequence.getSizeY(), this.sequence.getSizeX(), 
				this.sequence.getSizeC(), DataType.DOUBLE);
		
		Array1DUtil.doubleArrayToSafeArray(height, height_image.getDataXY(0), false);
		height_image.dataChanged();
		return height_image;
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
	 * Computes the surface normal for the cell's polygon.
	 * 
	 * @param cell cell for which the normal will be computed
	 * @param height_image image from which to extrapolate the height (z) information
	 */
	private void computeCellNormal(Node cell, IcyBufferedImage height_image) {
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

		normal_map.put(cell, n);
	}

	/* (non-Javadoc)
	 * @see plugins.davhelle.cellgraph.overlays.StGraphOverlay#paintFrame(java.awt.Graphics2D, plugins.davhelle.cellgraph.graphs.FrameGraph)
	 */
	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		for(Node cell: frame_i.vertexSet()){
			if(normal_map.containsKey(cell)){
				
				double ratio = Math.abs(normal_map.get(cell).z);
				
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
		
		if(frame.getFrameNo() != 0 && !analyzeAllFrames)
			return;
		
		XLSUtil.setCellString(sheet, 0, 0, "Cell id");
		XLSUtil.setCellString(sheet, 1, 0, "Projection area");
		XLSUtil.setCellString(sheet, 2, 0, "Unit n.x");
		XLSUtil.setCellString(sheet, 3, 0, "Unit n.y");
		XLSUtil.setCellString(sheet, 4, 0, "Unit n.z");
		XLSUtil.setCellString(sheet, 5, 0, "Position");
		XLSUtil.setCellString(sheet, 6, 0, "Centroid x");
		XLSUtil.setCellString(sheet, 7, 0, "Centroid y");
		
		int row_no = 1;
		for(Node node: frame.vertexSet()){
			String position = "inner";
			
			XLSUtil.setCellNumber(sheet, 0, row_no, node.getTrackID());
			
			double area2D = node.getGeometry().getArea() * pixel_size.x * pixel_size.y;
			XLSUtil.setCellNumber(sheet, 1, row_no, area2D);
			
			XLSUtil.setCellNumber(sheet, 2, row_no, normal_map.get(node).x);
			XLSUtil.setCellNumber(sheet, 3, row_no, normal_map.get(node).y);
			XLSUtil.setCellNumber(sheet, 4, row_no, normal_map.get(node).z);
			
			if(node.onBoundary())
				position = "border";
			
			XLSUtil.setCellString(sheet, 5, row_no, position);
			XLSUtil.setCellNumber(sheet, 6, row_no, node.getCentroid().getX());
			XLSUtil.setCellNumber(sheet, 7, row_no, node.getCentroid().getY());
			
			row_no++;
		}
	}

}
