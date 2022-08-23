package Loader;

import DataBase.DatasourceConfig;
import Lines.DirectLine;
import Lines.LineConverter;
import Lines.ReversedLine;
import OpenLRImpl.LineImpl;
import OpenLRImpl.MapDatabaseImpl;
import OpenLRImpl.NodeImpl;
import org.apache.commons.collections.ListUtils;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.geom.Point;


import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

import static org.jooq.sources.tables.Kanten.KANTEN;
import static org.jooq.sources.tables.Knoten.KNOTEN;
import static org.jooq.sources.tables.Metadata.METADATA;

/**
 *
 * Loader for routable OSM data in a PostgreSQL database with PostGIS extension.
 * Database queries are constructed with the help of JOOQ.
 *
 * Tables kanten, knoten, metadata are read here
 * Tables incidents, affected are written here
 *
 * @author Emily Kast
 *
 */

public class RoutableOSMMapLoader implements MapLoader {

    private final DSLContext ctx;
    private final Connection con;
    private final List<NodeImpl> allNodesList;
    private final List<LineImpl> allLinesList;
    private MapDatabaseImpl mdb;
    private final ArrayList<Double> boundingBoxInformation;
    private final Point midPoint;
    private final double distance;

    /**
     * @param midPoint mid-point of the loading-area
     * @param distance distance from the mid-point to the edge of the bounding box
     * @throws SQLException
     */
    public RoutableOSMMapLoader(Point midPoint, double distance) throws SQLException {
        con = DatasourceConfig.getConnection();
        ctx = DSL.using(con, SQLDialect.POSTGRES);
        this.midPoint = midPoint;
        this.distance = distance;
        allNodesList = getAllNodes();
        allLinesList = getAllLines();
        boundingBoxInformation = getBoundingBox();
    }

    @Override
    public List<NodeImpl> getAllNodesList() {
        return allNodesList;
    }

    @Override
    public List<LineImpl> getAllLinesList() {
        return allLinesList;
    }

    @Override
    public ArrayList<Double> getBoundingBoxInformation() {
        return boundingBoxInformation;
    }

    @Override
    public void setMdb(MapDatabaseImpl mdb) {
        this.mdb = mdb;
        allNodesList.forEach(node -> node.setMdb(this.mdb));
        allLinesList.forEach(line -> line.setMdb(this.mdb));
    }

    @Override
    public int numberOfNodes() {
        return allNodesList.size();
    }

    @Override
    public int numberOfLines() {
        return allLinesList.size();
    }

    @Override
    public List<NodeImpl> getAllNodes() {

        // Get node information from PostgreSQL database and write it into NodeImpl class using JOOQ
        List<NodeImpl> allNodes = ctx.select(KNOTEN.NODE_ID, KNOTEN.LAT, KNOTEN.LON)
                .from(KNOTEN).fetchInto(NodeImpl.class);

        // set node geometry for each node in list.
        setNodeGeometry(allNodes);
        // set list of connected lines for each node in list.
        setConnectedLinesList(allNodes);

        return allNodes;
    }

    /**
     * Sets list of connected line IDs for each node in the allNodesList by using SQL query.
     * @param allNodesList list containing all nodes in the road network
     */
    private void setConnectedLinesList(List<NodeImpl> allNodesList) {

        allNodesList.forEach(n -> {
            List<Long> connectedLinesIDs = ctx.select().from(KANTEN)
                    .where(KANTEN.START_NODE.eq(n.getID()))
                    .or(KANTEN.END_NODE.eq(n.getID()))
                    .fetch().getValues(KANTEN.LINE_ID);

            n.setConnectedLinesIDs(connectedLinesIDs);
        });

    }

    /** Creates node geometry for each node in the allNodesList. Longitude and latitude information (WGS84)
     * are used to create point geometry.
     *
     * @param allNodesList list containing all nodes in the road network
     */
    private void setNodeGeometry(List<NodeImpl> allNodesList) {

        // GeometryFactory needed to create point geometry
        GeometryFactory factory = new GeometryFactory();
        allNodesList.forEach(node -> {
            // Create point geometry for each node
            Point point = factory.createPoint(new Coordinate(node.getLon(), node.getLat()));
            // Set point geometry for the node
            node.setPointGeometry(point);
            // set MapDatabase for the node
            node.setMdb(mdb);
        });
    }


    public List<LineImpl> getAllLines() {

        List<LineImpl> linesDirect = new ArrayList<>();
        List<LineImpl> linesReversed = new ArrayList<>();

        // Get all lines from database
        List<DirectLine> directLines = ctx.select(KANTEN.LINE_ID, KANTEN.START_NODE, KANTEN.END_NODE, KANTEN.FRC, KANTEN.FOW,
                KANTEN.LENGTH_METER, KANTEN.NAME)
                .from(KANTEN)
                .fetchInto(DirectLine.class);

        directLines.forEach(directLine -> linesDirect.add(LineConverter.directLineToOpenLRLine(directLine)));

        // get all lines from database where oneway=false as reversed line > start and end node are switched and line
        // geometry is reversed.
        List<ReversedLine> reversedLines = ctx.select(KANTEN.LINE_ID, KANTEN.START_NODE, KANTEN.END_NODE, KANTEN.FRC, KANTEN.FOW,
                KANTEN.LENGTH_METER, KANTEN.NAME)
                .from(KANTEN)
                .where(KANTEN.ONEWAY.eq(false))
                .fetchInto(ReversedLine.class);

        reversedLines.forEach(reversedLine -> linesReversed.add(LineConverter.reversedLineToOpenLRLine(reversedLine)));

        List allLines = ListUtils.union(linesDirect, linesReversed);

        setLineNodes(allLines);
        setLineGeometry(allLines);

        return allLines;
    }

    /**
     * Sets start and end node for each line in the list.
     * @param linesList list containing all lines in the road network
     */
    private void setLineNodes(List<LineImpl> linesList) {
        linesList.forEach(l -> {

            long startNodeID = l.getStartNodeID();
            Optional<NodeImpl> startNode = allNodesList.stream().filter(n -> n.getID() == startNodeID).findFirst();
            startNode.ifPresent(l::setStartNode);

            long endNodeID = l.getEndNodeID();
            Optional<NodeImpl> endNode = allNodesList.stream().filter(n -> n.getID() == endNodeID).findFirst();
            endNode.ifPresent(l::setEndNode);
        });
    }

    /**
     * Method needed for JOOQ to get line geometry as WKT from Postgres DB using PostGIS function ST_AsText
     * @param isReversed boolean value if line geometry needs to be reversed.
     * @return JOOQ field
     */
    private static Field<?> st_asText(boolean isReversed) {
        if(!isReversed)
            return DSL.field("ST_AsText(geom)");
        else {
            return DSL.field("ST_AsText(ST_Reverse(geom))");
        }
    }

    /**
     * Sets line geometry for each line in the list using WKT representation.
     * @param linesList list containing all lines in the road network
     */
    private void setLineGeometry(List<LineImpl> linesList) {

        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        WKTReader reader = new WKTReader( geometryFactory );

        linesList.forEach(l -> {
            String wktString;
            wktString = ctx.select(st_asText(l.isReversed())).from(KANTEN).where(KANTEN.LINE_ID.eq(l.getID())).fetchOne().value1().toString();
            try {
                 l.setLineGeometry((LineString) reader.read(wktString));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        });
    }

    public ArrayList<Double> getBoundingBox() {

        double x = ctx.select(METADATA.LEFT_LAT).from(METADATA).fetchOne().value1();
        double y = ctx.select(METADATA.LEFT_LON).from(METADATA).fetchOne().value1();
        double width = ctx.select(METADATA.BBOX_WIDTH).from(METADATA).fetchOne().value1();
        double height = ctx.select(METADATA.BBOX_HEIGHT).from(METADATA).fetchOne().value1();

        ArrayList<Double> bboxInformation = new ArrayList<>();
        bboxInformation.add(x);
        bboxInformation.add(y);
        bboxInformation.add(width);
        bboxInformation.add(height);

        return bboxInformation;
    }

    /**
     * Closes database connection.
     * @throws Exception Exception
     */
    public void close() throws Exception {
        if (ctx != null) ctx.close();
        if (con != null) con.close();
    }
}
