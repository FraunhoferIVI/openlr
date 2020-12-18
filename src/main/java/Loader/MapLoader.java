package Loader;

import OpenLRImpl.LineImpl;
import OpenLRImpl.MapDatabaseImpl;
import OpenLRImpl.NodeImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Loader interface provides methods needed to get road network and use it for the OpenLR decoding process.
 *
 * @author  Emily Kast
 */

public interface MapLoader {

    /**
     * Method to get all nodes from the database and write the into AllNodesList.
     * For each node the following information must be given:
     * node_id as long, latitude as double, longitude as double, point geometry as Point, list of connected
     * lineIDs and MapDatabaseImplementation.
     *
     * @return List of nodes
     */
    List<NodeImpl> getAllNodes();

    /**
     * Returns list of all nodes in the road network.
     * For each node the following information must be given:
     * node_id as long, latitude as double, longitude as double, point geometry as Point, list of connected
     * lineIDs and MapDatabaseImplementation.
     *
     * @return List of nodes
     */
    List<NodeImpl> getAllNodesList();

    /**
     * Methode to get all lines from the database and write into AllLinesList.
     * If the database only contains lines in one direct even though a line is passable in both directions,
     * the geometry of the these lines need to be reversed and added to the list. There needs to be a line for every
     * driving direction.
     * @return List of lines
     */
    List<LineImpl> getAllLines();

    /**
     * List of all lines in the road network.
     * For each line following information must be given:
     * line_id as long, startNode_id as long, endNode_id as long, startNode as Node, endNode as Node,
     * frc (functional road class) as int, fow (form of way) as int, length of the line in meter as int,
     * name of the line as String, information if road is reversed as boolean, line geometry as LineString
     * and MapDatabaseImplementation.
     *
     * @return List of lines
     */
    List<LineImpl> getAllLinesList();

    /**
     * ArrayList contains BoundingBox Information.
     * The ArrayList needs to contain the following information in the given order:
     * x coordinate (WGS84) of the upper left corner as double, y coordinate (WGS84) of the upper left corner as double,
     * width of the bounding box as double, height of the bounding box as double.
     *
     * @return ArrayList containing bounding box information
     */
    ArrayList<Double> getBoundingBoxInformation();

    /**
     * Set MapDataBase for every node and line. Is needed to request lists of nodes and lines.
     *
     * @param mdb MapDataBase
     */
    void setMdb(MapDatabaseImpl mdb);

    /**
     * Returns the number of nodes in the AllNodesList.
     *
     * @return number of nodes as integer
     */
    int numberOfNodes();

    /**
     * Returns number of lines in the AllLinesList
     * @return number of lines as integer
     */
    int numberOfLines();
}
