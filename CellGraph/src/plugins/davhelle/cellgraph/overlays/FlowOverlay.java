package plugins.davhelle.cellgraph.overlays;

import icy.util.XMLUtil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D.Double;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jxl.write.WritableSheet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.misc.CatmullRom;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

/**
 * Generate smooth view of the off cell center path.
 * 
 * Automatically exports path to XML format compatible with TrackManager.
 * 
 * [STILL IN TEST PHASE, NOT ACCESSIBLE THROUGH MAIN GUI!]
 * 
 * @author Davide Heller
 *
 */
public class FlowOverlay extends StGraphOverlay {
	
	public static final String DESCRIPTION =
			"Overlay representing the tracking positions of cell centers<br/>" +
			"as smooth line";
	
	HashMap<Node,LineString> flow = new HashMap<Node,LineString>();
	HashMap<Node,Geometry> simpleFlow = new HashMap<Node, Geometry>();
	HashMap<Node,LineString> smoothFlow = new HashMap<Node, LineString>();
	
	private int smooth_interval = 10;
	private int flow_paint_style = 0;
	
	ShapeWriter writer = new ShapeWriter();

	public FlowOverlay(SpatioTemporalGraph stGraph) {
		super("Flow over time", stGraph);

		GeometryFactory factory = new GeometryFactory();
		
		FrameGraph frame = stGraph.getFrame(0);

		for(Node n: frame.vertexSet()){

			if(!n.hasNext())
				continue;

			ArrayList<Coordinate> list = new ArrayList<Coordinate>();

			list.add(n.getCentroid().getCoordinate());
			
			Node next = n;
			while(next.hasNext()){
				next = next.getNext();
				list.add(next.getCentroid().getCoordinate());
			}

			LineString cell_path = factory.createLineString(
					list.toArray(new Coordinate[list.size()]));

			flow.put(n,cell_path);
			
            Geometry simple = DouglasPeuckerSimplifier.simplify(cell_path, 3.0);
            if (simple.getCoordinates().length > 2)
            		simpleFlow.put(n, simple);
            
            List<Coordinate> raw = new ArrayList<Coordinate>();
            
            for(int j=0; j < list.size()-1; j+=smooth_interval){
            		raw.add(list.get(j));
            }
            
            raw.add(list.get(list.size() - 1));
            //raw.addAll(Arrays.asList(simple.getCoordinates()));
            
            List<Coordinate> spline = new ArrayList<Coordinate>(); 
            
            try {
				spline = CatmullRom.interpolate(raw, smooth_interval + 1);
			} catch (Exception e) {
				e.printStackTrace();
			}
            
            if(spline.size() > 2){
            		LineString smooth_path = factory.createLineString(
            				spline.toArray(new Coordinate[spline.size()]));
            		
            		smoothFlow.put(n, smooth_path);
            }
            
		}
		
		saveXML(new File("/Users/davide/Desktop/flowTrack.xml"));
	
	}

	@Override
	public void paintFrame(Graphics2D g, FrameGraph frame_i) {
		
		for(Node n: frame_i.vertexSet()){

			n=n.getFirst();

			switch( flow_paint_style ){

			case 0:
				if(flow.containsKey(n)){
					LineString s = flow.get(n);
					Shape flow = writer.toShape(s);
					g.setColor(Color.cyan);
					g.draw(flow);
				}
				break;
			case 1:
				if(smoothFlow.containsKey(n)){
					LineString s = smoothFlow.get(n);
					Shape flow = writer.toShape(s);
					g.setColor(Color.blue);
					g.draw(flow);
				}
				break;
			case 3:
				if(simpleFlow.containsKey(n)){
					Geometry s = simpleFlow.get(n);
					Shape flow = writer.toShape(s);
					g.setColor(Color.green);
					g.draw(flow);
				}
				break;
			default:
				continue;
			}
		}
			

	}
	
	/**
	 * save the particleArrayList in XML.
	 * adapted from 
	 *  - plugins.fab.trackgenerator.BenchmarkSequence
	 *  - plugins.fab.trackmanager.TrackManager
	 * 
	 * @param XMLFile
	 */
	public void saveXML( File XMLFile )
	{

		boolean export_smooth = true;
		
		Document document = XMLUtil.createDocument( true );
		Element documentElement = document.getDocumentElement();
		
		Element versionElement = XMLUtil.addElement( 
				documentElement , "trackfile" ); 
		versionElement.setAttribute("version", "1");

		Element trackGroupElement = XMLUtil.addElement( 
				documentElement , "trackgroup" );
		
		for(Node n: stGraph.getFrame(0).vertexSet()){
			
			if(export_smooth){
				if(!smoothFlow.containsKey(n))
					continue;
				
				Element trackElement = XMLUtil.addElement( 
						trackGroupElement , "track" );				
				XMLUtil.setAttributeIntValue( 
						trackElement , "id" , n.getTrackID() );
				
				Coordinate[] smooth_track = smoothFlow.get(n).getCoordinates();
				int smooth_length = smooth_track.length;
				
				for(int i=0; i < smooth_length; i++){
						
					Element detection = document.createElement("detection");
					trackElement.appendChild( detection );

					Coordinate centroid = smooth_track[i];
					double x = roundDecimals2(centroid.x);
					double y = roundDecimals2(centroid.y);
					
					XMLUtil.setAttributeDoubleValue( detection , "x" , x );
					XMLUtil.setAttributeDoubleValue( detection , "y" , y );
					XMLUtil.setAttributeIntValue( detection , "t" , i );
					
				}
				
			}
			else{
				if(!n.hasNext())
					continue;
				
				Element trackElement = XMLUtil.addElement( 
						trackGroupElement , "track" );				
				XMLUtil.setAttributeIntValue( 
						trackElement , "id" , n.getTrackID() );
				
				//Add initial position
				addDetection(n, trackElement, document);
				
				while(n.hasNext()){
					n = n.getNext();
					addDetection(n, trackElement, document);
				}
			}
		}
		
		XMLUtil.saveDocument( document , XMLFile );
	}

	/**
	 * @param n
	 * @param track
	 * @param document
	 */
	private void addDetection(Node n, Element track, Document document) {
		Element detection = document.createElement("detection");
		track.appendChild( detection );

		Coordinate centroid = n.getCentroid().getCoordinate();
		double x = roundDecimals2(centroid.x);
		double y = roundDecimals2(centroid.y);
		int t = n.getBelongingFrame().getFrameNo();
		
		XMLUtil.setAttributeDoubleValue( detection , "x" , x );
		XMLUtil.setAttributeDoubleValue( detection , "y" , y );
		XMLUtil.setAttributeIntValue( detection , "t" , t );
	}
	
	/**
	 * from plugins.fab.trackgenerator.BenchmarkSequence
	 * @param value
	 * @return
	 */
	private double roundDecimals2(double value) {
		value = value * 1000d;
		value = Math.round(value);
		return value/1000d;		
	}

	@Override
	public void specifyLegend(Graphics2D g, Double line) {
		// TODO Auto-generated method stub

	}

	@Override
	void writeFrameSheet(WritableSheet sheet, FrameGraph frame) {
		// TODO Auto-generated method stub

	}

}
