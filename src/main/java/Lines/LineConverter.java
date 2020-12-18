package Lines;

import OpenLRImpl.LineImpl;

/**
 *
 *
 * @author Emily Kast
 */

public class LineConverter {

    /**
     * Creates OpenLR conform line from ReversedLine.
     * @param reversedLine reversed line
     * @return openLR line
     */
    public static LineImpl reversedLineToOpenLRLine(ReversedLine reversedLine) {
        return new LineImpl(reversedLine.line_id, reversedLine.start_node, reversedLine.end_node, reversedLine.frc, reversedLine.fow, reversedLine.length, reversedLine.name, reversedLine.reversed);
    }

    /**
     * Creates OpenLR conform line from DirectLine.
     * @param directLine direct line
     * @return openLR Line
     */
    public static LineImpl directLineToOpenLRLine(DirectLine directLine) {
        return new LineImpl(directLine.line_id, directLine.start_node, directLine.end_node, directLine.frc, directLine.fow, directLine.length, directLine.name, directLine.reversed);
    }
}
