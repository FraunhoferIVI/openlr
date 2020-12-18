package Lines;

/**
 * Needed information for direct line.
 * Direct in the sens of given as in the database.
 *
 * @author Emily Kast
 */

public class DirectLine {

    public long start_node;
    public long end_node;
    long line_id;
    int frc;
    int fow;
    int length;
    String name;
    boolean reversed;

    public DirectLine(long line_id, long start_node, long end_node, int frc, int fow, int length, String name) {
        this.line_id = line_id;
        this.start_node = start_node;
        this.end_node = end_node;
        this.frc = frc;
        this.fow = fow;
        this.length = length;
        this.name = name;
        this.reversed = false;
    }

}
