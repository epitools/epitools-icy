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
	
	private SegmentationProgram tool;
	private String input_file_name;
	
	private int enumeration_start;
	private int start_file_no;
	private int file_no_idx;
	private int point_idx;

	private boolean use_tool_option;
	private File input_file;
	
	public FileNameGenerator(
			File ez_file, 
			InputType fileType, 
			boolean directInput, 
			SegmentationProgram tool){
		
		this.tool = tool;
		
		//Default file to use
		if(ez_file == null){
			String default_file = "Neo0_skeleton_001.png";
			String default_dir = "/Users/davide/Documents/segmentation/Epitools/Neo0/Skeleton/";
			//previous default: /Users/davide/Documents/segmentation/seedwater_analysis/2013_05_17/ManualPmCrop5h/8bit/Outlines/Outline_0_000.tif
			input_file = new File(default_dir+default_file);
		}
		else
			input_file = ez_file;
		
		//default values
		enumeration_start = 0;
		start_file_no = 0;
		input_file_name = input_file.getAbsolutePath();
		point_idx = input_file_name.indexOf('.');
		
		//assumption of 3 digit number
		//TODO: proper number read in (no. represented in 2 digit format...)
		//TODO: do it like fiji-loci-fileImporter : parse Option
		file_no_idx = point_idx - 3;
		if(point_idx > 3){		
			String file_str_no = input_file_name.substring(file_no_idx, point_idx);
			int file_no = Integer.valueOf(file_str_no);
			start_file_no = file_no;
		}
		
		//Customize file name generation in case skeletons come from
		//direct tool input
		use_tool_option = fileType == InputType.SKELETON && directInput;
		
		if(use_tool_option)
			switch(tool){
			case MatlabLabelOutlines:
				enumeration_start = 1;
				break;
			case PackingAnalyzer:
				enumeration_start = 0;
				break;
			case SeedWater:
				enumeration_start = 0;
				break;
		}	
	}
	
	public int getFrameNo(int i){
		//difference between file no and frame no, 
		//as the first might start at 1 while frames always start at 0
		//in icy-numeration
		return start_file_no + i - enumeration_start;
	}
	
	
	public String getFileName(int i){
		
		int file_no = start_file_no + i;
		String file_str_no = Integer.toString(file_no);
		
		//TODO add support for more than 2 digit no
		if(file_no < 10)
			file_str_no = "00" + file_str_no;  
		else if(file_no < 100)
			file_str_no = "0" + file_str_no;  
		
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
	
	public String getShortName(){
		return input_file.getName();
	}
}
