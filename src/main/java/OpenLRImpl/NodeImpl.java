package OpenLRImpl;

import openlr.map.*;
import org.locationtech.jts.geom.Point;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementation of the TomTom OpenLR Node interface.
 *
 * @author Emily Kast
 */

public class NodeImpl implements Node {

    long node_id;
    double lat;
    double lon;
    Point pointGeometry;
    List<Long> connectedLinesIDs;
    MapDatabaseImpl mdb;

    List<Line> connectedLines;

    public NodeImpl(long node_id, double lat, double lon) {
        this.node_id = node_id;
        this.lat = lat;
        this.lon = lon;

    }

    public void setPointGeometry(Point pointGeometry) {
        this.pointGeometry = pointGeometry;
    }

    public void setConnectedLinesIDs(List<Long> connectedLinesIDs) {
        this.connectedLinesIDs = connectedLinesIDs;
    }

    public void setMdb(MapDatabaseImpl mdb) {
        this.mdb = mdb;
    }

    public double getLat() {

        return lat;
    }

    public double getLon() {

        return lon;
    }

    public Point getPointGeometry() {

        return pointGeometry;
    }

    @Override
    public double getLatitudeDeg() {

        return lat;
    }

    @Override
    public double getLongitudeDeg() {

        return lon;
    }

    @Override
    public GeoCoordinates getGeoCoordinates() {
        GeoCoordinates coordinates = null;
        try {
            coordinates = new GeoCoordinatesImpl(lon, lat);
        } catch (InvalidMapDataException e) {
            e.printStackTrace();
        }
        return coordinates;
    }

    @Override
    public Iterator<Line> getConnectedLines() {

        if (connectedLines != null) {
            return connectedLines.iterator();
        }

        Iterator<Line> lineIterator = mdb.getAllLines();
        List<Line> getConnectedLines = new ArrayList<>();
        connectedLinesIDs.forEach(id -> {
            while(lineIterator.hasNext()) {
                Line l = lineIterator.next();
                if(id == l.getID())
                    getConnectedLines.add(l);
            }
        });
         connectedLines = getConnectedLines;
         return connectedLines.iterator();
    }

    @Override
    public int getNumberConnectedLines() {
        if (connectedLines != null){
            return connectedLines.size();
        }
        getConnectedLines();
        return  connectedLines.size();
    }

    @Override
    public Iterator<Line> getOutgoingLines() {
        if(connectedLines != null)
            getConnectedLines();

        assert connectedLines != null;
        List<Line> outgoingLines = connectedLines.stream()
                .filter(l -> l.getStartNode().getID() == node_id)
                .collect(Collectors.toList());
        return outgoingLines.iterator();
    }

    @Override
    public Iterator<Line> getIncomingLines() {
        if(connectedLines != null)
            getConnectedLines();

        assert connectedLines != null;
        List<Line> incomingLines = connectedLines.stream()
                .filter(l -> l.getEndNode().getID() == node_id)
                .collect(Collectors.toList());
        return incomingLines.iterator();
    }

    @Override
    public long getID() {
        return node_id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodeImpl node = (NodeImpl) o;
        return node_id == node.node_id &&
                Double.compare(node.lat, lat) == 0 &&
                Double.compare(node.lon, lon) == 0 &&
                pointGeometry.equals(node.pointGeometry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node_id, lat, lon, pointGeometry);
    }
}
