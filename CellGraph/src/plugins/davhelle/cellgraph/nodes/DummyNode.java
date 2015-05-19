package plugins.davhelle.cellgraph.nodes;

import java.awt.Color;
import java.awt.Shape;
import java.util.List;

import plugins.davhelle.cellgraph.graphs.FrameGraph;
import plugins.davhelle.cellgraph.tracking.HungarianTracking;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Dummy implementation of Node for use in {@link HungarianTracking}
 * 
 * @author Davide Heller
 *
 */
public class DummyNode implements Node {

	public DummyNode() {}

	@Override
	public Point getCentroid() {
		return null;
	}

	@Override
	public void setGeometry(Geometry node_geometry) {}

	@Override
	public Geometry getGeometry() {
		return null;
	}
	
	@Override
	public int getTrackID() {
		return -10;
	}

	@Override
	public void setTrackID(int tracking_id) {}

	@Override
	public void setNext(Node next_node) {}

	@Override
	public void setPrevious(Node next_node) {}

	@Override
	public boolean hasNext() {
		return false;
	}

	@Override
	public Node getNext() {
		return null;
	}

	@Override
	public Node getPrevious() {
		return null;
	}

	@Override
	public boolean hasPrevious() {
		return false;
	}

	@Override
	public boolean onBoundary() {
		return false;
	}

	@Override
	public void setBoundary(boolean onBoundary) {}

	@Override
	public boolean hasObservedDivision() {
		return false;
	}

	@Override
	public void setObservedDivision(boolean observedDivision) {}

	@Override
	public void setDivision(Division division) {}

	@Override
	public Division getDivision() {
		return null;
	}

	@Override
	public Shape toShape() {
		return null;
	}

	@Override
	public FrameGraph getBelongingFrame() {
		return null;
	}

	@Override
	public Node getFirst() {
		return null;
	}

	@Override
	public void setFirst(Node first) {}

	@Override
	public List<Node> getNeighbors() {
		return null;
	}

	@Override
	public void addParentCandidate(Node first) {}

	@Override
	public List<Node> getParentCandidates() {
		return null;
	}

	@Override
	public void setErrorTag(int errorTag) {}

	@Override
	public int getErrorTag() {
		return 0;
	}

	@Override
	public void setElimination(Elimination elimination) {}

	@Override
	public Elimination getElimination() {
		return null;
	}

	@Override
	public boolean hasObservedElimination() {
		return false;
	}

	@Override
	public Color getColorTag() {
		return null;
	}

	@Override
	public void setColorTag(Color color_tag) {}

	@Override
	public boolean hasColorTag() {
		return false;
	}

	@Override
	public int getFrameNo() {
		return 0;
	}

	@Override
	public boolean hasObservedOrigin() {
		return false;
	}

	@Override
	public Division getOrigin() {
		return null;
	}

	@Override
	public void setOrigin(Division origin) {}

}
