package plugins.davhelle.cellgraph.export;

import org.jgrapht.ext.VertexNameProvider;

import plugins.davhelle.cellgraph.nodes.Node;

/**
 * VertexIdProvider provides a unique ID for each Node
 * in the graph based on his Geometry HashCode;
 * 
 * 
 * @author Davide Heller
 *
 */
public class VertexIdProvider implements VertexNameProvider<Node> {

	@Override
	public String getVertexName(Node vertex) {
		// TODO Auto-generated method stub
		return Integer.toString(vertex.getGeometry().hashCode());
	}

}
