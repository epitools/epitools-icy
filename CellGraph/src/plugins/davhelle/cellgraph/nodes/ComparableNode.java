/**
 * 
 */
package plugins.davhelle.cellgraph.nodes;

/**
 * @author Davide Heller
 *
 */
public class ComparableNode implements Comparable<ComparableNode> {
	
	Node key;
    Double value;
	
	/**
	 * 
	 */
	public ComparableNode(Node key, Double value) {
		this.key = key;
		this.value = value;
		// TODO Auto-generated constructor stub
	}

	public Node getNode(){
		return key;
	}
	
	@Override
	public int compareTo(ComparableNode o) {
		// TODO Auto-generated method stub
		return value.compareTo(o.value);
	}
	
	public String toString(){
		return key.getTrackID()+":"+value.toString();
	}

}
