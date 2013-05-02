package plugins.davhelle.cellgraph.painters;

import icy.canvas.IcyCanvas;
import icy.main.Icy;
import icy.painter.AbstractPainter;
import icy.sequence.Sequence;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

public class PolygonConverterPainter extends AbstractPainter{

	private SpatioTemporalGraph stGraph;
	private Map<Node, Polygon> polyMap;
	private ShapeWriter writer;

	/**
	 * @param spatioTemporalGraph
	 */
	public PolygonConverterPainter(SpatioTemporalGraph spatioTemporalGraph){
		this.stGraph = spatioTemporalGraph;
		this.polyMap = new HashMap<Node, Polygon>();
		this.writer = new ShapeWriter();

		GeometryFactory factory = new GeometryFactory();

		for(int time_point=0; time_point < stGraph.size(); time_point++){
			for(Node n: stGraph.getFrame(time_point).vertexSet()){

				if(n.onBoundary())
					continue;
					
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
					boolean last_neighbor = false;
					
					//find an adjacent neighbor and compute the midpoint intersection with n
					while(!neighbors.isEmpty()){
						
						Iterator<Node> node_it = neighbors.iterator();
						while(node_it.hasNext()){
							
							Node b = node_it.next();
							Geometry bGeo = b.getGeometry();
							
							if(aGeo.intersects(bGeo)){
								//found intersection
								
								Geometry intersectionAB = aGeo.intersection(bGeo);
								Geometry intercectionAC = null;
								
								if(!nGeo.intersects(intersectionAB))
									continue;
								
								node_it.remove();
								
								//Check if it's a four way vertex
								boolean is_four_way = false;
								Node c = null;
								Geometry cGeo = null;
								
								if(neighbors.size() > 1){
									
									Iterator<Node> left_it = neighbors.iterator();
									while(left_it.hasNext()){										
										c = left_it.next();
										cGeo = c.getGeometry();
										
										if(aGeo.intersects(cGeo) && bGeo.intersects(cGeo)){
//											System.out.println("!!!!!!!!!!!!!!!!------------------------hello");
											if(nGeo.intersects(aGeo.intersection(cGeo)) && 
													nGeo.intersects(bGeo.intersection(cGeo))){
												//four way vertex found!
												is_four_way = true;
//												System.out.println("a: "+aGeo.getCentroid().toText());
//												System.out.println("b: "+bGeo.getCentroid().toText());
//												System.out.println("c: "+cGeo.getCentroid().toText());
												left_it.remove();
												break;				
											}
										}
									}	
								}
								
								//Determine new vertex
								Geometry intersection = null;
								
								if(is_four_way){
									if(intersectionAB.getNumGeometries() == 1){
										intersection = nGeo.intersection(intersectionAB);
										//seed next node
										a = b;		
//										System.out.println("Chose B!");
									}
									else{
										Geometry intersectionAC = aGeo.intersection(cGeo);
										
										//Given the first cell was in the middle we have to preserve
										//one of the lateral cells
										if(intersectionAC.getNumGeometries() != 1){
//											System.out.println(idx);
											firstGeo = bGeo;
										}
											
										//seed next
										intersection = nGeo.intersection(intersectionAC);
										a = c;
										//System.out.println("Chose C!");
									}
								}
								else{
									intersection = nGeo.intersection(intersectionAB);
									//set next node
									a = b;
								}

								
								Coordinate intersectionVertex = intersection.getCoordinate();
								//Save coordinate
								if(intersectionVertex != null){
									ringCoords.add(intersectionVertex);
									idx++;
								}
								
								//update next node
								aGeo = a.getGeometry();
								break;
								
							}
//							else{
//								if(last_neighbor){
//									System.out.println(aGeo.toText());
//									System.out.println("and");
//									System.out.println(bGeo.toText());
//									System.out.println("don't touch");
//									neighbors.remove(b);
//								}
//							}
						}

//						if(idx == nSize - 2){
//							last_neighbor = true;
//						}
					}
					
					
					
					//find the last intersection
					//assert idx == nSize - 1: "Connecting point missing, number less than expected!";
					if(!aGeo.intersects(firstGeo)){
						System.out.println(aGeo.getCentroid().toText());
						System.out.println("and");
						System.out.println(firstGeo.getCentroid().toText());
						System.out.println("don't touch");
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
					//System.out.println(Arrays.toString(polygon_coordinates));
					
					
					//build polygon
					Polygon nPoly = factory.createPolygon(ringCoords.toArray(new Coordinate[idx]));

					
					//System.out.println("Created "+nPoly.toText());
					
					//add the polygon to the map
					if(nPoly != null)
						polyMap.put(n, nPoly);
					else
						System.out.println("BAD polygon:"+n.getCentroid().toText());
				}
			}
		}
	}


	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		int time_point = Icy.getMainInterface().getFirstViewer(sequence).getT();

		if(time_point < stGraph.size()){
			
			FrameGraph frame_i = stGraph.getFrame(time_point);
			g.setColor(Color.orange);

			for(Node cell: frame_i.vertexSet())
				if(polyMap.containsKey(cell))
					g.draw((writer.toShape(polyMap.get(cell))));
			
		}
	}
}

