package plugins.davhelle.cellgraph.graphs;

import java.io.File;

import plugins.davhelle.cellgraph.io.FileNameGenerator;
import plugins.davhelle.cellgraph.io.InputType;
import plugins.davhelle.cellgraph.io.SegmentationProgram;

/**
 * Generates a {@link SpatioTemporalGraph} and populates it with {@link FrameGraph} objects
 * 
 * @author Davide Heller
 *
 */
public class SpatioTemporalGraphGenerator {

	/**
	 * The graph to be populated
	 */
	SpatioTemporalGraph stGraph;
	/**
	 * The naming generator for sequential skeleton files
	 */
	FileNameGenerator file_name_generator;
	/**
	 * The factory generating individual frameGraphs
	 */
	FrameGenerator frame_generator;
	/**
	 * Number of frames to be inserted
	 */
	int frame_no;
	
	/**
	 * Default generator using skeleton images as input
	 * 
	 * @param type which implementation of SpatioTemporalGraph to use
	 * @param input_file skeleton file of the first time point
	 * @param time_points number of time points to be read
	 */
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
	
	/**
	 * Generic generator with additional input file format type specification see {@link InputType}
	 * 
	 * @param type implementation of SpatioTemporalGraph to use
	 * @param input_file skeleton file of the first time point
	 * @param time_points number of time points in the graph
	 * @param input_type input file format to use
	 */
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
	
	/**
	 * Spatio temporal graph generator that initializes only the graph type
	 * and the input_type. Frames can be added with addFrame(i,path)
	 * 
	 * @param type type of graph to built
	 * @param input_type input format of the frames
	 */
	public SpatioTemporalGraphGenerator(GraphType type, InputType input_type){
		switch(type){
		case TISSUE_EVOLUTION:
			this.stGraph = new TissueEvolution();
		}
		
		this.frame_generator = new FrameGenerator(input_type);
		
		this.file_name_generator = null;
	}
	
	/**
	 * Add a frame at the specified to the spatiotemporal graph supplying the location
	 * 
	 * @param frame_no
	 * @param frame_file_name
	 */
	public void addFrame(int frame_no,String frame_file_name){
		
		if(new File(frame_file_name).exists()){
			FrameGraph frame = frame_generator.generateFrame(frame_no, frame_file_name);
			stGraph.setFrame(frame, frame_no);
		}
		else
			System.out.println("Input file does not exist: "+frame_file_name);
		
	}

	/**
	 * Checks if the file name generated for the specified time point exists
	 * 
	 * @param i time point to generate the file name for
	 */
	private void checkFileExistence(int i) {
		String time_point_file_name = file_name_generator.getFileName(i);
		File time_point_file = new File(time_point_file_name);
		assert time_point_file.exists(): 
			String.format("Time point %s is missing",time_point_file_name);
	}
	
	/**
	 * @param frame_no frame number to add
	 */
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
	
	/**
	 * @return the populated spatiotemporal graph
	 */
	public SpatioTemporalGraph getStGraph(){
		return this.stGraph;
	}

}
