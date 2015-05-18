package plugins.davhelle.cellgraph.io;

import java.io.File;

/**
 * File name generator customized for input files
 * from specific tools {see SegmentationProgram enum} 
 * It is assumed that files are contiguous starting
 * from the input file number (not necessarily 0)
 * 
 * Current file name requirements are:
 * - Contiguous 3 digit enumeration
 * - Enumeration to start from 0 by default
 * 		(not necessarily the input)
 * 
 * @author Davide Heller
 *
 */
public class FileNameGenerator {
	
	/**
	 * Custom enumeration to support specific application input
	 */
	private SegmentationProgram tool;
	
	/**
	 * First input file path
	 */
	private String input_file_name;
	
	/**
	 * File sequence start (might not be 0)
	 */
	private int start_file_no;
	
	/**
	 * Frame index 
	 */
	private int file_no_idx;
	
	/**
	 * Position of the point 
	 */
	private int point_idx;

	/**
	 * Flag to use application specific input
	 */
	private boolean use_tool_option;
	
	/**
	 * Original file input
	 */
	private File input_file;
	
	/**
	 * File name generator for sequences
	 * 
	 * Assumption: [file_name][Zero tabbed - 3 digit number].[extension]
	 * 
	 * @param input_file First frame of a series
	 * @param fileType Type of input (i.e. skeletons, vkt ecc..)
	 * @param directInput Flag for Application Specific input
	 * @param tool Application Enumeration Tag
	 */
	public FileNameGenerator(
			File input_file, 
			InputType fileType, 
			boolean directInput, 
			SegmentationProgram tool){
		
		this.tool = tool;
		this.input_file = input_file;
		
		//default values
		start_file_no = 0;
		input_file_name = input_file.getAbsolutePath();
		point_idx = input_file_name.lastIndexOf('.');
		
		//Assumption: Zero tabbed - 3 digit number
		//TODO: do it like fiji-loci-fileImporter : parse Option
		file_no_idx = point_idx - 3;
		if(point_idx > 3){		
			String file_str_no = input_file_name.substring(file_no_idx, point_idx);
			int file_no = Integer.valueOf(file_str_no);
			start_file_no = file_no;
		}

	}
	
	/**
	 * @param i time point to generate the file name for
	 * @return absolute path of input file for time point i
	 */
	public String getFileName(int i){
		
		int file_no = start_file_no + i;
		String file_str_no = String.format("%03d", file_no);
		
		String abs_path = "";
	
		if(use_tool_option)
			switch(tool){
			case PackingAnalyzer:
				abs_path = input_file_name.substring(0, file_no_idx) + file_str_no + "/handCorrection.png";
				break;
			case SeedWater:
				abs_path = input_file_name.substring(0, file_no_idx) + file_str_no + input_file_name.substring(point_idx);
				break;
			case MatlabLabelOutlines:
				abs_path = input_file_name.substring(0, file_no_idx) + file_str_no + input_file_name.substring(point_idx);
				break;		
			}
		else	
			abs_path = input_file_name.substring(0, file_no_idx) + file_str_no + input_file_name.substring(point_idx);
		
		return abs_path;
	}
	
	/**
	 * @return Short first input file name for input GUI
	 */
	public String getShortName(){
		return input_file.getName();
	}
}
