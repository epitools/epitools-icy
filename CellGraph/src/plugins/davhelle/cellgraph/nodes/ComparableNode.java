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
	
	/**
	 * Corresponding node
	 */
	Node key;
    /**
     * Comparable value associated with the node
     */
    Double value;
    
	public ComparableNode(Node key, Double value) {
		this.key = key;
		this.value = value;
	}

	public Node getNode(){
		return key;
	}
	
	public double getValue(){
		return value;
	}
	
	public void increaseValueByOne(){
		value = value + 1.0;
	}
	
	public void increaseValueBy(double toAdd){
		value = value + toAdd;
	}
	
	@Override
	public int compareTo(ComparableNode o) {
		return value.compareTo(o.value);
	}
	
	public String toString(){
		return key.getTrackID()+":"+String.format("%.2g", value.doubleValue());
	}

}
