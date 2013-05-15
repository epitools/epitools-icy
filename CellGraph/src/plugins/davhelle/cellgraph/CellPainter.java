/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph;

import java.io.IOException;

import icy.gui.frame.progress.AnnounceFrame;
import icy.main.Icy;
import icy.painter.Painter;
import icy.sequence.Sequence;
import icy.swimmingPool.SwimmingObject;


import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarSequence;


import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.DivisionReader;
import plugins.davhelle.cellgraph.misc.VoronoiGenerator;
import plugins.davhelle.cellgraph.painters.BorderPainter;
import plugins.davhelle.cellgraph.painters.CentroidPainter;
import plugins.davhelle.cellgraph.painters.PolygonClassPainter;
import plugins.davhelle.cellgraph.painters.PolygonConverterPainter;
import plugins.davhelle.cellgraph.painters.PolygonPainter;
import plugins.davhelle.cellgraph.painters.VoronoiAreaDifferencePainter;
import plugins.davhelle.cellgraph.painters.VoronoiPainter;

/**
 * Plugin containing all the visualizations based
 * on a formerly created spatioTemporal graph
 * which has been placed into the ICY data sharing
 * architecture "Swimming Pool".
 * 
 * Alpha version!
 * 
 * @author Davide Heller
 *
 */
public class CellPainter extends EzPlug {
	
	//plotting modes
	private enum PlotEnum{
		CELLS,BORDER, VORONOI, POLYGON_CLASS,  READ_DIVISIONS,
	}
	
	EzVarBoolean				varRemovePainterFromSequence;
	
	EzVarEnum<PlotEnum> 		varPlotting;

	EzVarBoolean				varBooleanPolygon;
	EzVarBoolean				varBooleanDerivedPolygons;
	EzVarBoolean				varBooleanCCenter;

	EzVarBoolean				varBooleanAreaDifference;
	EzVarBoolean				varBooleanVoronoiDiagram;
	EzVarBoolean				varBooleanWriteCenters;
	EzVarBoolean				varBooleanWriteArea;

	//sequence to paint on 
	EzVarSequence				varSequence;
	Sequence sequence;

	@Override
	protected void initialize() {
		
		//Default options
		
		varSequence = new EzVarSequence("Input sequence");
		super.addEzComponent(varSequence);
		
		varRemovePainterFromSequence = new EzVarBoolean("Remove all painters", false);
		super.addEzComponent(varRemovePainterFromSequence);
		
		//Cells view
		varBooleanPolygon = new EzVarBoolean("Polygons", true);
		varBooleanCCenter = new EzVarBoolean("Centers", true);
		varBooleanWriteCenters = new EzVarBoolean("Write cell centers to disk",false);
		varBooleanDerivedPolygons = new EzVarBoolean("Derived Polygons", false);
		EzGroup groupCellMap = new EzGroup("CELLS elements",
				varBooleanPolygon,
				varBooleanDerivedPolygons,
				varBooleanCCenter,
				varBooleanWriteCenters);
		

		//Voronoi view
		varBooleanVoronoiDiagram = new EzVarBoolean("Voronoi Diagram", true);
		varBooleanAreaDifference = new EzVarBoolean("Area difference", false);
		
		EzGroup groupVoronoiMap = new EzGroup("VORONOI elements",
				varBooleanAreaDifference,
				varBooleanVoronoiDiagram);	
		
		//Which painter should be shown by default
		varPlotting = new EzVarEnum<PlotEnum>("Painter type",
				PlotEnum.values(),PlotEnum.CELLS);

		//Painter
		EzGroup groupPainters = new EzGroup("Painters",
				varPlotting,
				groupCellMap,
				groupVoronoiMap
		);
		
		varRemovePainterFromSequence.addVisibilityTriggerTo(groupPainters, false);
		varPlotting.addVisibilityTriggerTo(groupCellMap, PlotEnum.CELLS);
		varPlotting.addVisibilityTriggerTo(groupVoronoiMap, PlotEnum.VORONOI);
		//TODO varInput.addVisibilityTriggerTo(varBooleanDerivedPolygons, InputType.SKELETON);

		super.addEzComponent(groupPainters);
		
		
	}

	@Override
	protected void execute() {
		sequence = varSequence.getValue();
		
		// watch if objects are already in the swimming pool:
		//TODO time_stamp collection?
		
		if(Icy.getMainInterface().getSwimmingPool().hasObjects("stGraph", true))
		
			for ( SwimmingObject swimmingObject : 
				Icy.getMainInterface().getSwimmingPool().getObjects(
						"stGraph", true) ){

				if ( swimmingObject.getObject() instanceof SpatioTemporalGraph ){

					SpatioTemporalGraph wing_disc_movie = (SpatioTemporalGraph) swimmingObject.getObject();	

					System.out.println("CellVisualizer: loaded stGraph with "+wing_disc_movie.size()+" frames");
					System.out.println("CellVisualizer:	first frame has  "+wing_disc_movie.getFrame(0).size()+" cells");

					PlotEnum USER_CHOICE = varPlotting.getValue();

					switch (USER_CHOICE){
					case BORDER: sequence.addPainter(new BorderPainter(wing_disc_movie));
					break;
					case CELLS: cellMode(wing_disc_movie);
					break;
					case POLYGON_CLASS: sequence.addPainter(new PolygonClassPainter(wing_disc_movie));
					break;
					case READ_DIVISIONS: divisionMode(wing_disc_movie);
					break;
					case VORONOI: voronoiMode(wing_disc_movie);
					break;
					}

					//future statistical output statistics
					//			CsvWriter.trackedArea(wing_disc_movie);
					//			CsvWriter.frameAndArea(wing_disc_movie);
				}
			}
		else
			new AnnounceFrame("No spatio temporal graph found in ICYsp, please run CellGraph plugin first!");
	}

	@Override
	public void clean() {
		// TODO Auto-generated by Icy4Eclipse
	}

	private void voronoiMode(SpatioTemporalGraph wing_disc_movie){
		VoronoiGenerator voronoiDiagram = new VoronoiGenerator(wing_disc_movie);
	
		if(varBooleanVoronoiDiagram.getValue()){
			Painter voronoiCells = new VoronoiPainter(
					wing_disc_movie, 
					voronoiDiagram.getNodeVoroniMapping());
			sequence.addPainter(voronoiCells);
		}
	
		if(varBooleanAreaDifference.getValue()){
			Painter voronoiDifference = new VoronoiAreaDifferencePainter(
					wing_disc_movie, 
					voronoiDiagram.getAreaDifference());
			sequence.addPainter(voronoiDifference);	
		}
	}

	private void cellMode(SpatioTemporalGraph wing_disc_movie){
		
		if(varBooleanCCenter.getValue()){
			Painter centroids = new CentroidPainter(wing_disc_movie);
			sequence.addPainter(centroids);
		}
		
		if(varBooleanPolygon.getValue()){
			Painter polygons = new PolygonPainter(wing_disc_movie);
			sequence.addPainter(polygons);
		}
		
		if(varBooleanDerivedPolygons.getValue()){
			Painter derived_polygons = new PolygonConverterPainter(wing_disc_movie);
			sequence.addPainter(derived_polygons);
		}
		
		
	}

	private void divisionMode(SpatioTemporalGraph wing_disc_movie){
		//Divisions read in 
		try{
			DivisionReader division_reader = new DivisionReader(wing_disc_movie);
			division_reader.backtrackDivisions();
			sequence.addPainter(division_reader);
		}
		catch(IOException e){
			System.out.println("Something went wrong in division reading");
		}
	}
}
