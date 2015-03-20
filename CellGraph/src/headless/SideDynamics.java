package headless;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.io.CsvWriter;
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
				"sample,division_frame_no,donor_id," +
				"acceptor_id,division_distance\n");
		
//		int neo_no = 0;
		for(int neo_no=0;neo_no<3;neo_no++){
			//load neo
			SpatioTemporalGraph stGraph = LoadNeoWtkFiles.loadNeo(neo_no);

			//load edges
			//HashMap<Node, PolygonalCellTile> cell_tiles = PolygonalCellTileGenerator.createPolygonalTiles(stGraph);
			//HashMap<Long, boolean[]> tracked_edges = EdgeTracking.trackEdges(stGraph);

			//go through divisions in first frame and register side gain
			FrameGraph frame = stGraph.getFrame(0);
			for(Node n: frame.vertexSet()){
				if(!n.onBoundary() && n.hasObservedDivision()){
					//SideDynamics sd = new SideDynamics(n);
					//sd.detectSideGain();
					//builder_main.append(sd.toString());

					Division d = n.getDivision();
					Node m = d.getMother();
					HashSet<Node> mN = new HashSet<Node>(m.getNeighbors());

					Node c1 = d.getChild1();
					Node c2 = d.getChild2();

					//find overlap
					HashSet<Node> c1N = new HashSet<Node>(c1.getNeighbors());
					HashSet<Node> c2N = new HashSet<Node>(c2.getNeighbors());

					c1N.retainAll(c2N);
					
					if(c1N.size() != 2)
						continue;
					
					for(Node acceptor: c1N){
						
						if(acceptor.hasObservedDivision()){
							
							//sample
							builder_main.append(neo_no);
							builder_main.append(',');
							
							//division time point
							builder_main.append(d.getTimePoint());
							builder_main.append(',');
							
							//dividing cell id
							builder_main.append(n.getTrackID());
							builder_main.append(',');
							
							//accepting cell id
							builder_main.append(acceptor.getTrackID());
							builder_main.append(',');
							
							//compute time difference to acceptor's division
							//> negative = earlier division
							//> positive = later division
							Division aD = acceptor.getDivision();
							int divisionDistance =  aD.getTimePoint() - d.getTimePoint();
							builder_main.append(divisionDistance);
							builder_main.append('\n');
							
						}
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

}
