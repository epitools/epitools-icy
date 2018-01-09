package plugins.davhelle.cellgraph.io;

import icy.file.Saver;
import icy.image.IcyBufferedImage;
import icy.sequence.Sequence;
import icy.type.DataType;

import java.io.File;
import java.io.IOException;

import loci.formats.FormatException;
import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.nodes.Node;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Class to save the cell graph skeletons as TIFF skeleton images
 * 
 * @author Davide Heller
 *
 */
public class SkeletonWriter {
	
	/**
	 * Description Sting for Export Plugin
	 */
	public static final String DESCRIPTION = 
			"Export each frame of the spatiotemporal graph loaded as<br/>" +
			" individual TIFF skeleton file. This skeleton reflext <br/>" +
			"also modification such as small cell or border removal.<br/><br/>" +
			"NOTE: Useful for further editing with CellEditor.";
	
	/**
	 * Sequence attached to stGraph
	 */
	Sequence sequence;
	
	public SkeletonWriter(Sequence sequence){
		this.sequence = sequence;
	}
	
	/**
	 * writes FrameGraph to skeleton TIFF image
	 * 
	 * @param frame_i FrameGraph to export
	 * @param file_name output path
	 */
	public void write(FrameGraph frame_i,String file_name){
		
		IcyBufferedImage skeleton_img = createSkeleton(frame_i);
		
		saveSkeleton(file_name, skeleton_img);
		
	}
	
	/**
	 * Transformation JTS Geometry to Buffered ICY image
	 * 
	 * @param frame_i FrameGraph to transform
	 * @return buffered icy image of input frame
	 */
	private IcyBufferedImage createSkeleton(FrameGraph frame_i) {
		
		int img_width = sequence.getSizeX();
		int img_height = sequence.getSizeY();
		
		IcyBufferedImage img = new IcyBufferedImage(img_width, img_height, 1, DataType.UBYTE);
		byte[] dataBuffer = img.getDataXYAsByte(0);

		for(Node cell: frame_i.vertexSet()){
			Geometry polygon = cell.getGeometry();
			for(Coordinate vertex: polygon.getCoordinates()){

				int x = (int)Math.round(vertex.x);
				int y = (int)Math.round(vertex.y);

				dataBuffer[x + y * img_width] = (byte) 255; 
			}
		}
	
		return img;
	}
	
	/**
	 * Saving Procedure of ICY buffered image
	 * 
	 * @param current_file_name output path
	 * @param img image to save
	 */
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
