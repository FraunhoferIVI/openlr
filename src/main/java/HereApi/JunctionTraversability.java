package HereApi;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * denotes, which junctions along a road are open
 * can be:
 * ALL_OPEN - all junctions along the roadway are open
 * ALL_CLOSED - all junctions are closed
 * INTERMEDIATE_CLOSED_EDGE_OPEN - junctions at beginning and end of the roadway are open, intermediate junctions are closed
 * START_OPEN_OTHERS_CLOSED - first edge junction is open, all others are closed
 * END_OPEN_OTHERS_CLOSED - last edge junction is open, all others are closed
 */
@Nullable
public enum JunctionTraversability

{
    ALL_OPEN, ALL_CLOSED, INTERMEDIATE_CLOSED_EDGE_OPEN, START_OPEN_OTHERS_CLOSED, END_OPEN_OTHERS_CLOSED;

    private static final Logger logger = LoggerFactory.getLogger(JunctionTraversability.class);

    @Override
    public String toString()
    {
        switch (this)
        {
            case ALL_OPEN: return "all_open";
            case ALL_CLOSED: return "all_closed";
            case END_OPEN_OTHERS_CLOSED: return "end_open_others_closed";
            case START_OPEN_OTHERS_CLOSED: return "start_open_others_closed";
            case INTERMEDIATE_CLOSED_EDGE_OPEN: return "intermediate_closed_edge_open";
            default: return null;
        }
    }

    @Nullable
    public static JunctionTraversability get(@NotNull String juncTrav)
    {
        switch (juncTrav)
        {
            case "":
                return JunctionTraversability.ALL_OPEN;
            case "intermediateClosedEdgeOpen":
                return JunctionTraversability.INTERMEDIATE_CLOSED_EDGE_OPEN;
            case "startOpenOthersClosed":
                return JunctionTraversability.START_OPEN_OTHERS_CLOSED;
            case "endOpenOthersClosed":
                return JunctionTraversability.END_OPEN_OTHERS_CLOSED;
            case "allClosed":
                return JunctionTraversability.ALL_CLOSED;
            default:
                logger.warn("Unknown junction traversability");
                return null;
        }
    }
}
