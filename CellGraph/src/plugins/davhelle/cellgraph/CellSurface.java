package plugins.davhelle.cellgraph;

import icy.sequence.Sequence;

import java.io.File;

import javax.vecmath.Point3d;

import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzLabel;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarDouble;
import plugins.adufour.ezplug.EzVarFile;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.roi.mesh.polygon.ROI3DPolygonalMesh;
import vtk.vtkCellArray;
import vtk.vtkDecimatePro;
import vtk.vtkIdTypeArray;
import vtk.vtkIntArray;
import vtk.vtkPolyData;
import vtk.vtkSimplePointsReader;
import vtk.vtkTransform;
import vtk.vtkTransformFilter;

/**
 * CellSurface reads the surface estimation saved by the selective plane
 * projection (EpiTools Matlab application) and renders it as 3D Mesh ROI 
 * 
 * @author Davide Heller
 *
 */
public class CellSurface extends EzPlug {

	EzVarSequence varSequence = new EzVarSequence("Input Sequence");
	EzVarFile varFile = new EzVarFile("Input Vtk", "");
	EzVarBoolean varDoDecimation = new EzVarBoolean("Decimate?",false);
	EzVarDouble varDecimationFactor = new EzVarDouble("Decimation factor",0.9,0.01,1.0,0.01);
	EzVarBoolean varReadTimeSeriese = new EzVarBoolean("Read Time Series", false);
	private long timer = 0;
	
	@Override
	protected void initialize() {
		
		EzLabel description = new EzLabel(
				"<ol><li> Verify [Pixel Size] in the Sequence Properties (Icy Side Bar)<br/>" +
				"    to scale the surface correctly" +
				" <li> Select [3DVTK mode] in the sequence viewer (2nd left icon)" +
				" <li> Consider [Decimate] for better performance and less <br/>memory use.<br/>" +
				"* By default two triangles for each pixel are generated <br/>" +
				"(e.g. 1024*1024 = 2.1 million t.)<br/>" +
				"* Decimate=0.9 reduces the t. by 90%</ol>" +
				" [WARNING] - If the sequence is a z-stack we recommend <br/>" +
				"removing the 3D mesh roi before leaving the 3DVTK mode.<br/>" +
				" If left, the change to 2D mode starts computing an expensive<br/>" +
				" 2D area roi representation.");
		
		EzVarBoolean varShowDescription = new EzVarBoolean("Show usage description",true);
		EzLabel descriptionHeader = new EzLabel("Usage:"); 
		
		super.addEzComponent(descriptionHeader);
		super.addEzComponent(description);
		super.addEzComponent(varShowDescription);
		
		varShowDescription.addVisibilityTriggerTo(descriptionHeader, varShowDescription.getValue());
		varShowDescription.addVisibilityTriggerTo(description, varShowDescription.getValue());

		varSequence.setToolTipText("Display sequence and from which scaling information will be read");
		
		varFile.setToolTipText("Input first [.vtk] file from your epitools analysis");
		
		varDoDecimation.setToolTipText("Simplifies the geometry by merging triangles");
		
		varDecimationFactor.setToolTipText("Degree of simplification, e.g. 0.9 = 90% less triangles");
		varDoDecimation.addVisibilityTriggerTo(varDecimationFactor, true);

		varReadTimeSeriese.setToolTipText("Loads files for all frames based on [###.vtk] pattern");
		
		EzGroup paramGroup = new EzGroup("1. PARAMETERS",
				varSequence,
				varFile,
				varDoDecimation,
				varDecimationFactor,
				varReadTimeSeriese);
		super.addEzComponent(paramGroup);
				
	}

	@Override
	public void clean() {
		// Could potentially clean the ROIs automatically to avoid persistence/2D area computation
	}
	
	/**
	 * Utility method for measuring generation time of 3D mesh ROI
	 * 
	 * @param message
	 */
	public void time(String message){
		long current_time = System.currentTimeMillis();
		
		if(timer != 0)
			message = String.format("%s completed:\t%d",
					message,current_time - timer);
			
		System.out.println(message);
		timer = System.currentTimeMillis();
	}

	@Override
	protected void execute() {
		Sequence sequence = varSequence.getValue();
		String file_name = varFile.getValue().getAbsolutePath();
		
		if(varReadTimeSeriese.getValue()){
			//assumption: files finish in 001.vtk
			String base_path = file_name.substring(0, file_name.length()-7);
			for(int i=1; i <= sequence.getSizeT(); i++){
				time(String.format("Generating Surface %d",i));
				File file_path = new File(String.format("%s%03d.vtk", base_path,i));
				if(file_path.exists()){
					ROI3DPolygonalMesh mesh = generateMesh(
							file_path.getAbsolutePath(), sequence);
					mesh.setT(i-1);
					mesh.setName(String.format(
							"Mesh frame %d: %d cells",
							i-1,
							mesh.getNumberOfCells()));
					
					sequence.addROI(mesh);
				}
				else
					System.out.printf("Error, mesh file doesn't exist: %s\n",
							file_path.getAbsolutePath());
			}
		}
		else{
			//single time point
			time("Starting to generate CellSurface..");
			ROI3DPolygonalMesh mesh = generateMesh(file_name, sequence);
			sequence.addROI(mesh);
			//Name overlay
			if(varDoDecimation.getValue())
				mesh.setName("Frame 0 decimated: " + mesh.getNumberOfCells());
			else
				mesh.setName("Frame 0: " + mesh.getNumberOfCells());
		}
		
	}

	/**
	 * Generates a mesh from the input file (generated by EpiTools for Matlab)
	 * and returns it s 3D Mesh ROI through use of the homonymous plugin by
	 * Alexandre Dufour (available at: http://icy.bioimageanalysis.org/plugin/3D_Mesh_ROI)
	 * 
	 * @param file_name Name of the mesh file containing the surface estimation by EpiTools Matlab
	 * @param sequence Sequence for which to generate the ROI (important for dimensions)
	 * @return
	 */
	private ROI3DPolygonalMesh generateMesh(String file_name, Sequence sequence) {
		
		vtkPolyData polydata = readSurface(file_name);
		time("reading");
		
		polydata = scaleSurface(sequence, polydata);
		time("scaling");
		
		triangulateSurface(sequence, polydata);
		time("triangulation");

        if(varDoDecimation.getValue()){
	        	polydata = decimateSurface(polydata);
	        	time("decimation");
        }
        
        //Transform vtk data structure to dufour's polygonal mesh ROI
        Point3d dim = new Point3d(
        		sequence.getPixelSizeX(), 
			sequence.getPixelSizeY(),
			sequence.getPixelSizeZ()); 
		ROI3DPolygonalMesh mesh = new ROI3DPolygonalMesh(dim, polydata);
		time("mesh generation");
		
		return mesh;
	}

	/**
	 * Scale the surface dimension according to the sequence unit measures
	 * 
	 * @param sequence sequence on which to project the surface
	 * @param polydata structure containing the surface as vtk polydata
	 * @return
	 */
	private vtkPolyData scaleSurface(Sequence sequence, vtkPolyData polydata) {
		vtkTransform transform = new vtkTransform();
		transform.Scale(
				sequence.getPixelSizeX(), 
				sequence.getPixelSizeY(),
				sequence.getPixelSizeZ());
		
		vtkTransformFilter filter = new vtkTransformFilter();
		filter.SetTransform(transform);
		filter.SetInputData(polydata);
		filter.Update();
		
		polydata = filter.GetPolyDataOutput();
		return polydata;
	}

	/**
	 * Reduces the amount of triangles in the polydata structure 
	 * to improve visualization performance and memory load.
	 * 
	 * see http://www.paraview.org/Wiki/VTK/Examples/Cxx/Meshes/Decimation
	 * for more information about decimation.
	 * 
	 * @param polydata
	 * @return decimated polyData structure.
	 */
	private vtkPolyData decimateSurface(vtkPolyData polydata) {
     	
      vtkDecimatePro decimate = new vtkDecimatePro();        
      decimate.SetInputData(polydata);
      decimate.SetTargetReduction(varDecimationFactor.getValue());

      vtkPolyData decimated_polydata = new vtkPolyData();
      decimate.SetOutput(decimated_polydata);
      decimate.Update();
      
      polydata = decimated_polydata;
		return polydata;
	}

	/**
	 * Reads the surface file generated by EpiTools for Matlab
	 * which contains a text based list of all coordinates of
	 * the mesh file
	 * 
	 * @param file_name
	 * @return
	 */
	private vtkPolyData readSurface(String file_name) {
		vtkPolyData polydata = new vtkPolyData();
		
		//Read individual xyz coords
		vtkSimplePointsReader reader  = new vtkSimplePointsReader();
		reader.SetFileName(file_name);
		reader.SetOutput(polydata);
		reader.Update();
		return polydata;
	}

	/**
	 * Populates the vtkPolyData structure with triangles
	 * between the grid points of input height map.
	 * 
	 * @param sequence sequence from which dimensions will be read
	 * @param polydata polydata structure to populate with faces
	 */
	private void triangulateSurface(Sequence sequence, vtkPolyData polydata) {
		// Triangle filter test=(206 199)
		int xSize = sequence.getSizeX();
		int ySize = sequence.getSizeY();
				
		int cellNumber = (xSize - 1)*(ySize - 1) * 2;
		int[] cells = new int[cellNumber * 4];
		int idx = 0;
		
		for(int col=0; col < xSize - 1; col++){
			for(int i = col*ySize; i < (col+1)*ySize - 1; i++){
			
				int j = i + 1;

				// the main triangle (counter-clockwise)
				
				cells[idx++] = 3; // three id point to follow:
				cells[idx++] = i; // 0. the current point
				cells[idx++] = j; // 1. the point below
				cells[idx++] = i + ySize; // 2. next column up

				// the opposite triangle (counter-clockwise)
				
				cells[idx++] = 3; // three id point to follow:
				cells[idx++] = j; // 0. the lower point
				cells[idx++] = j + ySize; // 1. next column lower
				cells[idx++] = i + ySize; // 2. next column up
			}
		}
		
		// Experp from vtkCellArray createCells(int cellNumber, int[] cells) in
		// https://github.com/jeromerobert/jCAE/blob/master/vtk-util/src/org/jcae/vtk/Utils.java
		vtkCellArray vtkCells = new vtkCellArray();
		vtkIdTypeArray array = new vtkIdTypeArray();
		vtkIntArray intArray = new vtkIntArray();
		intArray.SetJavaArray(cells);
		array.DeepCopy(intArray);
		vtkCells.SetCells(cellNumber, array);
        
        polydata.SetPolys(vtkCells);
	}
	
}
