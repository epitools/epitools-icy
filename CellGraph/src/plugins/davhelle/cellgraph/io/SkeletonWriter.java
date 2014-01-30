package plugins.davhelle.cellgraph.io;

import icy.file.Saver;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;

import java.io.File;
import java.io.IOException;

import loci.formats.FormatException;
import plugins.davhelle.cellgraph.graphs.SpatioTemporalGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Class to save the read & processed skeletons
 * 
 * @author Davide Heller
 *
 */
public class SkeletonWriter {
	
	SpatioTemporalGraph stGraph;
	Sequence sequence;
	
	public SkeletonWriter(Sequence sequence, SpatioTemporalGraph stGraph){
		this.stGraph = stGraph;
		this.sequence = sequence;
		
	}
	
	public void write(){
		
		String file_name = "/Users/davide/tmp/reviewed_skeletons/neo0_000.tif";
		File skeleton_file = new File(file_name);
		FileNameGenerator skeleton_file_name_generator = 
				new FileNameGenerator(
						skeleton_file,
						InputType.SKELETON,
						true, 
						SegmentationProgram.SeedWater);
		
		for(int time_point = 0; time_point < stGraph.size(); time_point++){
			String current_file_name = skeleton_file_name_generator.getFileName(time_point);
			
			IcyBufferedImage skeleton_img = createSkeleton(time_point);
			
			saveSkeleton(current_file_name, skeleton_img);
		}
		
	}
	
	private IcyBufferedImage createSkeleton(int t) {
		
		int img_width = sequence.getSizeX();
		int img_height = sequence.getSizeY();
		
		IcyBufferedImage img = new IcyBufferedImage(img_width, img_height, 1, DataType.UBYTE);
		byte[] dataBuffer = img.getDataXYAsByte(0);

		for(Node cell: stGraph.getFrame(t).vertexSet()){
			Geometry polygon = cell.getGeometry();
			for(Coordinate vertex: polygon.getCoordinates()){
				int x = (int)Math.round(vertex.x);
				int y = (int)Math.round(vertex.y);
				
				dataBuffer[x + y * img_width] = (byte) 255; 
			}
		}
	
		return img;
	}
	
	private void saveSkeleton(String current_file_name, IcyBufferedImage img) {
		
		System.out.println("Saving skeleton:"+current_file_name);
		
		try {
			
			//open file
			File current_file = new File(current_file_name);

			//attempt saving
			Saver.saveImage(img, current_file, true);
			
		} catch (FormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
