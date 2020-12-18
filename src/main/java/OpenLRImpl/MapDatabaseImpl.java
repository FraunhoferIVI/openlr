package OpenLRImpl;

import GeometryFunctions.*;
import Loader.RoutableOSMMapLoader;
import openlr.map.Line;
import openlr.map.MapDatabase;
import openlr.map.Node;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.operation.distance.DistanceOp;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of the TomTom OpenLR MapDatabase interface.
 *
 * @author Emily Kast
 */

public class MapDatabaseImpl implements MapDatabase {

    RoutableOSMMapLoader osmLoader;

    GeometryFactory geometryFactory = new GeometryFactory();

    public MapDatabaseImpl(RoutableOSMMapLoader osmLoader) {
        this.osmLoader = osmLoader;
        osmLoader.setMdb(this);
    }

    @Override
    public boolean hasTurnRestrictions() {
        return false;
    }

    @Override
    public Line getLine(long id) {

         Optional<LineImpl> matchingLine = osmLoader.getAllLinesList().stream()
                 .filter(l -> l.getID() == id).findFirst();
         return matchingLine.get();

    }

    @Override
    public Node getNode(long id) {

        Optional<NodeImpl> matchingNode = osmLoader.getAllNodesList().stream()
            .filter(n -> n.getID() == id).findFirst();

        return matchingNode.get();
    }

    @Override
    public Iterator<Node> findNodesCloseByCoordinate(double longitude, double latitude, int distance) {

        double distanceDeg = GeometryFunctions.distToDeg(latitude, distance);
        Point p = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        List<Node> closeByNodes = new ArrayList(osmLoader.getAllNodesList().stream().filter(l ->
                DistanceOp.isWithinDistance(l.pointGeometry, p, distanceDeg)
        ).collect(Collectors.toList()));

        return closeByNodes.iterator();

    }

    @Override
    public Iterator<Line> findLinesCloseByCoordinate(double longitude, double latitude, int distance) {
        double distanceDeg = GeometryFunctions.distToDeg(latitude, distance);
        Point p = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        List closeByLines = new ArrayList(osmLoader.getAllLinesList().stream().filter(l ->
                DistanceOp.isWithinDistance(l.lineGeometry, p, distanceDeg)
        ).collect(Collectors.toList()));

        return closeByLines.iterator();
    }

    @Override
    public boolean hasTurnRestrictionOnPath(List<? extends Line> path) {
        return false;
    }

    @Override
    public Iterator<Node> getAllNodes() {

        List allNodes = osmLoader.getAllNodesList();
        return allNodes.iterator();
    }

    @Override
    public Iterator<Line> getAllLines() {

        List allLines = osmLoader.getAllLinesList();
        return allLines.iterator();
    }

    @Override
    public Rectangle2D.Double getMapBoundingBox() {

        ArrayList<Double> bbox = osmLoader.getBoundingBoxInformation();
        return new Rectangle2D.Double(bbox.get(0), bbox.get(1), bbox.get(2), bbox.get(3));
    }

    @Override
    public int getNumberOfNodes() {
       return osmLoader.numberOfNodes();
    }

    @Override
    public int getNumberOfLines() {
        return osmLoader.numberOfLines();
    }
}
