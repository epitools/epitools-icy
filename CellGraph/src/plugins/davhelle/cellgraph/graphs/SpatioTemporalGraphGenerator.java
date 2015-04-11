package plugins.davhelle.cellgraph.graphs;

import java.io.File;

import plugins.davhelle.cellgraph.io.FileNameGenerator;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.SegmentationProgram;

/**
 * Generates a graph and populates it with Frames
 * 
 * @author Davide Heller
 *
 */
public class SpatioTemporalGraphGenerator {

	SpatioTemporalGraph stGraph;
	FileNameGenerator file_name_generator;
	FrameGenerator frame_generator;
	int frame_no;
	
	public SpatioTemporalGraphGenerator(GraphType type, File input_file, int time_points) {
		switch(type){
			case TISSUE_EVOLUTION:
				this.stGraph = new TissueEvolution();
			break;
		}
		
		boolean use_direct_input = true;
		
		this.file_name_generator = new FileNameGenerator(
				input_file,
				InputType.SKELETON, 
				use_direct_input,
				SegmentationProgram.SeedWater);
		
		this.frame_generator = new FrameGenerator(
				InputType.SKELETON);
		
		//check if files exist
		for(int i=0;i<time_points;i++)
			checkFileExistence(i);
		
		//populate spatiotemporal graph
		for(int i=0;i<time_points;i++)
			addFrame(i);
			
	}
	
	public SpatioTemporalGraphGenerator(GraphType type, File input_file, int time_points, InputType input_type) {
		switch(type){
			case TISSUE_EVOLUTION:
				this.stGraph = new TissueEvolution();
			break;
		}
		
		boolean use_direct_input = true;
		
		this.file_name_generator = new FileNameGenerator(
				input_file,
				InputType.SKELETON, 
				use_direct_input,
				SegmentationProgram.SeedWater);
		
		this.frame_generator = new FrameGenerator(
				input_type);
		
		//check if files exist
		for(int i=0;i<time_points;i++)
			checkFileExistence(i);
		
		//populate spatiotemporal graph
		for(int i=0;i<time_points;i++)
			addFrame(i);
			
	}

	private void checkFileExistence(int i) {
		String time_point_file_name = file_name_generator.getFileName(i);
		File time_point_file = new File(time_point_file_name);
		assert time_point_file.exists(): 
			String.format("Time point %s is missing",time_point_file_name);
	}
	
	private void addFrame(int frame_no){
		String frame_file_name = file_name_generator.getFileName(frame_no);
		
		long startTime = System.currentTimeMillis();
		FrameGraph frame = 
				frame_generator.generateFrame(frame_no, frame_file_name);
		long endTime = System.currentTimeMillis();
		
		stGraph.setFrame(frame, frame_no);
		
		System.out.println(String.format(
				"Frame %d: Found %d cells in %d milliseconds",
				frame_no,
				frame.size(),
				endTime - startTime));
	}
	
	public SpatioTemporalGraph getStGraph(){
		return this.stGraph;
	}

}
