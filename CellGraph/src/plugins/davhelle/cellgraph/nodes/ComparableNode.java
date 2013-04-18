package plugins.davhelle.cellgraph.nodes;

/**
 * ComparableNode is a utility class to access sort operations
 * and relatives using the java.util.Arrays methods.
 * 
 * Nodes are compared based on a numeric value.
 * 
 * @author Davide Heller
 *
 */
public class ComparableNode implements Comparable<ComparableNode> {
	
	Node key;
    Double value;
    
	public ComparableNode(Node key, Double value) {
		this.key = key;
		this.value = value;
	}

	public Node getNode(){
		return key;
	}
	
	@Override
	public int compareTo(ComparableNode o) {
		return value.compareTo(o.value);
	}
	
	public String toString(){
		return key.getTrackID()+":"+value.toString();
	}

}
