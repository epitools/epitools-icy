package plugins.davhelle.cellgraph.export;

/**
 * Export options for the GraphML file writer. See {@link VertexLabelProvider}
 * for their implementation
 * 
 * @author Davide Heller
 *
 */
public enum ExportFieldType {
	DIVISION, 
	AREA,
	TRACKING_ID,
	TRACKING_POSITION,
	COLOR_TAG,
	SEQ_AREA,
	COMPLETE_CSV,
	SEQ_X, 
	SEQ_Y,
	ELIMINATION,
	SEQ_NODE_DEGREE, STANDARD
}
