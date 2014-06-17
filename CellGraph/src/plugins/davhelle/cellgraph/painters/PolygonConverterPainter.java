/*=========================================================================
 *
 *  Copyright Basler Group, Institute of Molecular Life Sciences, UZH
 *
 *=========================================================================*/
package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.painter.Overlay;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

/**
 * Converter Painter which retrieves the branching points of all cells, 
 * i.e. the junction between 3 or max. 4 cells
 * 
 * Given these points a minimal polygon can be constructed and depicted.
 * 
 * 
 * @author Davide Heller
 *
 */
public class PolygonConverterPainter extends Overlay{

	private SpatioTemporalGraph stGraph;
	private Map<Node, Polygon> polyMap;
	private ShapeWriter writer;

	/**
	 * @param spatioTemporalGraph
	 */
	public PolygonConverterPainter(SpatioTemporalGraph spatioTemporalGraph){
		super("Junction polygons (via intersections)");
		this.stGraph = spatioTemporalGraph;
		this.polyMap = new HashMap<Node, Polygon>();
		this.writer = new ShapeWriter();

		GeometryFactory factory = new GeometryFactory();

		//Cycle through all time points
		for(int time_point=0; time_point < stGraph.size(); time_point++){
			
			System.out.println("converting frame "+time_point);
			
			//count conflicting polygons
			int wrong_polygons = 0;
			
			//for every node in the frame compute the polygon reduction
			for(Node n: stGraph.getFrame(time_point).vertexSet()){

				//do not analyze boundary cells
				if(n.onBoundary())
					continue;
					
				//shortcut for node's geometry
				Geometry nGeo = n.getGeometry();

				//copy neighbors (n returns a static list)
				ArrayList<Node> neighbors = new ArrayList<Node>(n.getNeighbors());
				int nSize = neighbors.size();
				if(nSize > 2){

					ArrayList<Coordinate> ringCoords = new ArrayList<Coordinate>();
					//Coordinate[] ringCoords = new Coordinate[nSize + 1];

					Node a = neighbors.remove(0);
					Geometry aGeo = a.getGeometry();
					Geometry firstGeo = aGeo;
					int idx = 0;
					
					//find an adjacent neighbor and compute the midpoint intersection with n
					while(!neighbors.isEmpty()){
						
						//cycle through all neighbors of n to find a's neighbor b
						Node b = null;
						Geometry bGeo = null;	
						Geometry intersectionAB = null;
						
						for(Node neighbor: neighbors){
							bGeo = neighbor.getGeometry();	
							if(aGeo.intersects(bGeo)){
								//non-zero intersection exists
								intersectionAB = aGeo.intersection(bGeo);
								//security check with n 
								try{
									if(nGeo.intersects(aGeo.intersection(bGeo))){
										b = neighbor;
										break;
									}
								}
								catch(java.lang.IllegalArgumentException e){
									System.out.println("GeometryCollectionError @"+n.getCentroid().toText());
									System.out.println("Please verify if a nearby 1px - artifact cell is present!");
								}
							}
						}
						
						if(b == null){
							System.out.println("ERROR, no suitable b has been found for a:"+aGeo.toText());
							break;
						}
							
						//remove the corresponding neighbor from list
						neighbors.remove(b);
						
						//Check if it's a four-way vertex(4wv)
						boolean is_four_way = false;

						//Thus check within left nodes if there is another correspondence c
						//which might suggest the presence of a 4wv.
						Node c = null;
						Geometry cGeo = null;
						Geometry intersectionAC = null;
						
						for(Node neighbor: neighbors){
							cGeo = neighbor.getGeometry();

							//first condition for 4wv
							boolean aTouchesC = aGeo.intersects(cGeo);
							boolean bTouchesC = bGeo.intersects(cGeo);
							
							if(aTouchesC && bTouchesC){
								
								//compute intersection and do final check with n
								intersectionAC = aGeo.intersection(cGeo);
//								System.out.println(intersectionAC.toText());
								boolean nTouchesAC = false;
								
								try{
									nTouchesAC = nGeo.intersects(intersectionAC);
								}
								catch(java.lang.IllegalArgumentException e){
									System.out.println("GeometryCollectionError @"+n.getCentroid().toText());
									System.out.println("Please verify if a nearby 1px - artifact cell is present!");
								}
								
								
								boolean nTouchesBC = nGeo.intersects(bGeo.intersection(cGeo));
								
								if(nTouchesAC && nTouchesBC){
									//four way vertex found!
									is_four_way = true;
									c = neighbor;
									break;				
								}
							}
						}	
								
								
						//Determine new vertex and set next
						Geometry intersection = nGeo.intersection(intersectionAB);
						a = b;
						
						if(is_four_way){
							
							neighbors.remove(c);
							
//							System.out.println("X @ vertexNo."+ idx + " with " + neighbors.size() + 
//									" v. left @" + intersection.toText());
							
							/*
							 * If a four way vertex (4wv) occurs attention has to be
							 * made that the next node seeded is not in the
							 * middle of the 4wv. That is the node that shares a single
							 * point with n. Otherwise the same point is inserted
							 * twice into the polygon
							 * 
							 */

							if(intersectionAB.getNumPoints() != 1){
								a = c;
								intersection = nGeo.intersection(intersectionAC);
								
								if(idx==0 && intersectionAC.getNumPoints() != 1)
									firstGeo = bGeo; //could be c if a=b, just has not to stay a
									
							}
						}
						


						Coordinate intersectionVertex = intersection.getCoordinate();
						//Save coordinate
						if(intersectionVertex != null){
							ringCoords.add(intersectionVertex);
							idx++;
							
							//and update next node
							aGeo = a.getGeometry();
						}
						else{
							System.out.println("Failed coordinate creation @"+nGeo.getCentroid().toText());
							break;	
						}
					}

					
					//find the last intersection
					//assert idx == nSize - 1: "Connecting point missing, number less than expected!";
					if(!aGeo.intersects(firstGeo)){
						System.out.println();
						System.out.println(ringCoords.get(0).toString());
						System.out.println(aGeo.getCentroid().toText());
						System.out.println(firstGeo.getCentroid().toText());
						wrong_polygons++;
					}
					else{
						Geometry intersection = nGeo.intersection(aGeo.intersection(firstGeo));
						ringCoords.add(intersection.getCoordinate());
						idx++;
					}
					
					
					//complete the linearRing coordinate structure by putting the first coordinate
					//as last again. (see Polygon doc)			
					ringCoords.add(ringCoords.get(0));
					idx++;
				
					//visual output
					Coordinate[] polygon_coordinates = ringCoords.toArray(new Coordinate[idx]);
					
					if(!aGeo.intersects(firstGeo)){
						System.out.println(Arrays.toString(polygon_coordinates));
						System.out.println();
					}
					
					Polygon nPoly = null;
					
					//build polygon if safety condition is met
					if(ringCoords.size() >= 4)
						nPoly = factory.createPolygon(ringCoords.toArray(new Coordinate[idx]));
					
					//add the polygon to the map
					if(nPoly != null)
						polyMap.put(n, nPoly);
					else
						System.out.println("Failed polygon creation @ "+nGeo.getCentroid().toText());
				}
			}
		
			//System check
//			System.out.println();
//		System.out.println(wrong_polygons);
		}
	}


	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getPositionT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.orange);

			for(Node cell: frame_i.vertexSet())
				if(polyMap.containsKey(cell)){
					g.draw((writer.toShape(polyMap.get(cell))));
					
					//mark the derived cell center (separate color from normal cell center depiction)
					//g.fillOval((int)cell.getCentroid().getX(), (int)cell.getCentroid().getY(), 2, 2);
				}
		}
	}
	
	public int getTileNumber(){
		return polyMap.size();
	}
}

