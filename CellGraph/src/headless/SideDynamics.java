package headless;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.vividsolutions.jts.geom.Geometry;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.CsvWriter;
import plugins.davhelle.cellgraph.misc.PolygonalCellTileGenerator;
import plugins.davhelle.cellgraph.nodes.Division;
import plugins.davhelle.cellgraph.nodes.Edge;
import plugins.davhelle.cellgraph.nodes.Node;

public class SideDynamics {
	
	class SideChange{
		public SideChange(long id, int frameNo){
			this.id = id;
			this.frameNo = frameNo;
			duration = 1;
		}
		
		public long id;
		public int duration;
		public int frameNo;
	}
	
	Node n;
	ArrayList<SideChange> changes;
	HashSet<Long> initialEdgeIds;
	
	public SideDynamics(Node n){
		this.n = n;
		changes = new ArrayList<SideDynamics.SideChange>();
		initialEdgeIds = new HashSet<Long>();
		
		//Populate the initial edge ids
		FrameGraph frame = n.getBelongingFrame();
		for(Edge e: frame.edgesOf(n))
			initialEdgeIds.add(e.getPairCode(frame));
	}
	
	public boolean isInitialEdge(long id){
		return initialEdgeIds.contains(id);
	}
	
	public void detectSideGain(){
		Node cell = n;
		while(cell.hasNext()){
			cell = cell.getNext();
			
			FrameGraph frame = cell.getBelongingFrame();
			for(Edge e: frame.edgesOf(cell)){
				long id = e.getPairCode(frame);
				if(!isInitialEdge(id))
					addChange(id,frame.getFrameNo());
			}
		}
	}
	
	public void addChange(long id,int frameNo){
		for(SideChange change: changes){
			if(id == change.id){
				change.duration++;
				return;
			}
		}
		
		//add new side gain
		if(n.hasObservedDivision()) {
			int relativeFrameNo = n.getDivision().getTimePoint() - frameNo;
			changes.add(new SideChange(id,relativeFrameNo));
		} else
			changes.add(new SideChange(id,frameNo));
	}
	
	public String toString(){
		//if(changes.isEmpty())
			//return String.format("%d has no gain\n",n.getTrackID());
		
		StringBuilder out = new StringBuilder();
		
		for(SideChange change: changes){
			out.append(n.getTrackID());
			out.append(',');
			//out.append(change.id);
			//out.append(',');
			out.append(change.frameNo);
			out.append(',');
			out.append(change.duration);
			out.append('\n');
		}
			
		return out.toString();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		StringBuilder builder_main = new StringBuilder();
		builder_main.append(
				"sample,donor_id," +
				"acceptor_id,largest,longest,dividing\n");
		
//		int neo_no = 0;
		for(int neo_no=0;neo_no<3;neo_no++){
			//load neo
			SpatioTemporalGraph stGraph = LoadNeoWtkFiles.loadNeo(neo_no);

			//load edges
			//PolygonalCellTileGenerator.createPolygonalTiles(stGraph);
			//HashMap<Long, boolean[]> tracked_edges = EdgeTracking.trackEdges(stGraph);

			//go through divisions in first frame and register side gain
			FrameGraph frame = stGraph.getFrame(0);
			for(Node n: frame.vertexSet()){
				if(!n.onBoundary() && n.hasObservedDivision()){
					//SideDynamics sd = new SideDynamics(n);
					//sd.detectSideGain();
					//builder_main.append(sd.toString());

					Division d = n.getDivision();
					int divisionTime = n.getFrameNo();
					Node m = d.getMother();

					//Find the largest neighbors immediately before rounding (avg. over 5 tp)
					HashSet<Node> largestNeighbors = new HashSet<Node>();
					HashSet<Node> longestNeighbors = new HashSet<Node>();
					HashSet<Node> dividingNeighbors = new HashSet<Node>();
					
					//while(m.hasPrevious()){
						//m = m.getPrevious();
						
						//if(m.getFrameNo() == divisionTime - 6 ){

							largestNeighbors.add(findLargestAreaNeighbor(m));
							
							longestNeighbors.add(findLongestSideNeighbor(m));
							
							dividingNeighbors.addAll(findDividingNeighbors(m));
							
							//break;
						//}
						
						//if(m.getFrameNo() < divisionTime - 10)
							//break;
					//}

					//Find side gaining nodes
					HashSet<Node> sideGainingNeighbors = findAcceptors(d);
					
					if(sideGainingNeighbors.size() != 2)
						continue;
					
					//compute overlaps
					for(Node acceptor: sideGainingNeighbors){
						Node first = acceptor.getFirst();
						builder_main.append(neo_no);
						builder_main.append(',');
						builder_main.append(n.getTrackID());
						builder_main.append(',');
						builder_main.append(acceptor.getTrackID());
						builder_main.append(',');
						builder_main.append(String.valueOf(largestNeighbors.contains(first)).toUpperCase());
						builder_main.append(',');
						builder_main.append(String.valueOf(longestNeighbors.contains(first)).toUpperCase());
						builder_main.append(',');
						builder_main.append(String.valueOf(dividingNeighbors.contains(first)).toUpperCase());
						builder_main.append('\n');
					}
				}
			}
		}
		
		boolean write_out = true;
		if(write_out){
			//Write out to disc
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			String file_name = String.format("/Users/davide/tmp/sideDynamics_%s.csv",timeStamp);
			File output_file = new File(file_name);
			CsvWriter.writeOutBuilder(builder_main, output_file);
			System.out.println("Successfully wrote to "+file_name);
		}
	}

	private static HashSet<Node> findDividingNeighbors(Node m) {
		HashSet<Node> ddns = new HashSet<Node>();
		for(Node n: m.getNeighbors()){
			if(n.hasObservedDivision()){
				Division d = n.getDivision();
				if(d.getTimePoint() > m.getDivision().getTimePoint())
					ddns.add(n.getFirst());
			}
		}
		return ddns;
	}

	private static Node findLongestSideNeighbor(Node m) {
		double maxLength = -1;
		Node maxLengthNode = null;
		
		Geometry source_geo = m.getGeometry();
		
		for(Node n: m.getNeighbors()){

			Geometry neighbor_geo = n.getGeometry();
			Geometry intersection = source_geo.intersection(neighbor_geo);
			
			//updated weighted graph with edge length
			double edgeLength = intersection.getLength();
			
			if(edgeLength > maxLength){
				maxLength = edgeLength;
				maxLengthNode = n.getFirst();
			}
		}

		return maxLengthNode;
	}

	private static Node findLargestAreaNeighbor(Node m) {
		double maxArea = -1;
		Node maxAreaNode = null;
		
		for(Node n: m.getNeighbors()){
			double nArea = n.getGeometry().getArea();
			if(nArea > maxArea){
				maxArea = nArea;
				maxAreaNode = n.getFirst();
			}
		}

		return maxAreaNode;
	}

	/**
	 * @param d
	 * @return
	 */
	private static HashSet<Node> findAcceptors(Division d) {
		Node c1 = d.getChild1();
		Node c2 = d.getChild2();

		//find overlap
		HashSet<Node> c1N = new HashSet<Node>(c1.getNeighbors());
		HashSet<Node> c2N = new HashSet<Node>(c2.getNeighbors());

		c1N.retainAll(c2N);
		return c1N;
	}

}
