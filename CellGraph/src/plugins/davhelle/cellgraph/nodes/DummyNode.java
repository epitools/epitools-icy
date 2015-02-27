package plugins.davhelle.cellgraph.nodes;

import java.awt.Color;
import java.awt.Shape;
import java.util.List;

import plugins.davhelle.cellgraph.graphs.FrameGraph;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class DummyNode implements Node {

	public DummyNode() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Point getCentroid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setGeometry(Geometry node_geometry) {
		// TODO Auto-generated method stub

	}

	@Override
	public Geometry getGeometry() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getProperty() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setProperty(Object property) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getTrackID() {
		// TODO Auto-generated method stub
		return -10;
	}

	@Override
	public void setTrackID(int tracking_id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setNext(Node next_node) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPrevious(Node next_node) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Node getNext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node getPrevious() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasPrevious() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onBoundary() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setBoundary(boolean onBoundary) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasObservedDivision() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setObservedDivision(boolean observedDivision) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDivision(Division division) {
		// TODO Auto-generated method stub

	}

	@Override
	public Division getDivision() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Shape toShape() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FrameGraph getBelongingFrame() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Node getFirst() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFirst(Node first) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Node> getNeighbors() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addParentCandidate(Node first) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Node> getParentCandidates() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setErrorTag(int errorTag) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getErrorTag() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setElimination(Elimination elimination) {
		// TODO Auto-generated method stub

	}

	@Override
	public Elimination getElimination() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasObservedElimination() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Color getColorTag() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setColorTag(Color color_tag) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasColorTag() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getFrameNo() {
		// TODO Auto-generated method stub
		return 0;
	}

}
