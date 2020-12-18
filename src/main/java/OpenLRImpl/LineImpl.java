package OpenLRImpl;

import GeometryFunctions.GeometryFunctions;
import com.spatial4j.core.distance.DistanceUtils;
import openlr.map.*;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.distance.DistanceOp;


import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Implementation of the TomTom OpenLR Line interface.
 *
 * @author Emily Kast
 */

public class LineImpl implements Line {

    long line_id;
    long startNode_id;
    long endNode_id;
    Node startNode;
    Node endNode;
    // functional road class
    int frc;
    //form of way
    int fow;
    int length_meter;
    String name;
    boolean isReversed;
    LineString lineGeometry;
    MapDatabaseImpl mdb;

    GeometryFactory geometryFactory = new GeometryFactory();

    public LineImpl(long line_id, long startNode_id, long endNode_id, int frc, int fow, int length_meter, String name, boolean isReversed) {
        this.line_id = line_id;
        this.startNode_id = startNode_id;
        this.endNode_id = endNode_id;
        this.frc = frc;
        this.fow = fow;
        this.length_meter = length_meter;
        this.name = name;
        this.isReversed = isReversed;
    }

    public void setStartNode(Node startNode) {
        this.startNode = startNode;
    }

    public void setEndNode(Node endNode) {
        this.endNode = endNode;
    }

    public void setLineGeometry(LineString lineGeometry) {
        this.lineGeometry = lineGeometry;
    }

    public void setMdb(MapDatabaseImpl mdb) {
        this.mdb = mdb; }

    public long getStartNodeID() { return startNode_id; }

    public long getEndNodeID() { return endNode_id; }

    public LineString getLineGeometry() { return lineGeometry; }

    public boolean isReversed() { return isReversed; }

    @Override
    public Node getStartNode() {

        return startNode;
    }

    @Override
    public Node getEndNode() {

        return endNode;
    }

    @Override
    public FormOfWay getFOW() {

        return FormOfWay.getFOWs().get(fow);
    }

    @Override
    public FunctionalRoadClass getFRC() {

        return FunctionalRoadClass.getFRCs().get(frc);
    }

    @Override
    public Point2D.Double getPointAlongLine(int distanceAlong) {

        if(distanceAlong < length_meter) {
            int segmentLengthTotal = 0;
            Coordinate[] lineCoordinates = lineGeometry.getCoordinates();

            for(int i = 0; i <= lineCoordinates.length-2; i++) {
                LineSegment segment = new LineSegment(lineCoordinates[i], lineCoordinates[i+1]);
                //Get segment length
                Coordinate[] segmentCoordinates = new Coordinate[]{segment.p0, segment.p1};
                int segmentLength = (int) Math.round(calculateOthodromicDist(segmentCoordinates));
                segmentLengthTotal += segmentLength;

                if(segmentLengthTotal >= distanceAlong) {

                    int newDistAlong = segmentLength-(segmentLengthTotal-distanceAlong);
                    Coordinate pointAlong = segment.pointAlong((double) newDistAlong/ (double) segmentLength);
                    return new Point2D.Double(pointAlong.x, pointAlong.y);
                }
            }
        }
        return new Point2D.Double(endNode.getLongitudeDeg(), endNode.getLatitudeDeg());
    }

    @Override
    public GeoCoordinates getGeoCoordinateAlongLine(int distanceAlong) {

        Point2D.Double pointAlongLine = getPointAlongLine(distanceAlong);
        GeoCoordinates coordinatesAlongLine = null;
        try {
            coordinatesAlongLine = new GeoCoordinatesImpl(pointAlongLine.getX(), pointAlongLine.getY());
        } catch (InvalidMapDataException e) {
            e.printStackTrace();
        }
        return coordinatesAlongLine;
    }

    @Override
    public int getLineLength() {

        return length_meter;
    }

    @Override
    public long getID() {

        return line_id;
    }

    @Override
    public Iterator<Line> getPrevLines() {

        List<Line> previousLines = new ArrayList<>();
        Iterator<Line> allLines = mdb.getAllLines();
        while(allLines.hasNext()) {
            Line line = allLines.next();
            if(line.getEndNode().getID() == startNode_id)
                previousLines.add(line);
        }
        return previousLines.iterator();
    }

    @Override
    public Iterator<Line> getNextLines() {

        List<Line> nextLines = new ArrayList<>();
        Iterator<Line> allLines = mdb.getAllLines();
        while(allLines.hasNext()) {
            Line line = allLines.next();
            if(line.getStartNode().getID() == endNode_id)
                nextLines.add(line);
        }
        return nextLines.iterator();
    }

    @Override
    public int distanceToPoint(double longitude, double latitude) {

        Point p = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        double distanceDeg  = DistanceOp.distance(lineGeometry, p);
        return GeometryFunctions.distToMeter(distanceDeg);
    }

    private double calculateOthodromicDist(Coordinate[] coordinates) {

        GeodeticCalculator gc = new GeodeticCalculator();
        gc.setStartingGeographicPoint(coordinates[0].x, coordinates[0].y);
        gc.setDestinationGeographicPoint(coordinates[1].x, coordinates[1].y);
        return gc.getOrthodromicDistance();
    }

    @Override
    public int measureAlongLine(double longitude, double latitude) {


        Point p = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        Point projectionPoint = geometryFactory.createPoint(DistanceOp.nearestPoints(lineGeometry, p)[0]);

        //Create Buffer around point geometry since jts is not able to check if line intersects point:
        double bufferDist = GeometryFunctions.distToDeg(latitude, 0.0001);
        Geometry pointBuffer = projectionPoint.buffer(bufferDist);

        Coordinate[] lineCoordinates = lineGeometry.getCoordinates();
        double distAlong = 0;

        //Create line segments from line coordinates
        for(int i = 1; i < lineCoordinates.length; i++){
            LineString lineSegment = geometryFactory.createLineString(new Coordinate[]{lineCoordinates[i-1], lineCoordinates[i]});

            if(lineSegment.intersects(pointBuffer)){

                // create new currentLine with coordinateOnTheLine as endpoint and calculate length
                LineString lineFraction = geometryFactory.createLineString(new Coordinate[]{lineCoordinates[i-1], projectionPoint.getCoordinate()});
                distAlong += calculateOthodromicDist(lineFraction.getCoordinates());
                return (int) Math.round(distAlong);
            }

            distAlong += calculateOthodromicDist(lineSegment.getCoordinates());
        }
        return length_meter;
    }

    @Override
    public Path2D.Double getShape() {

        //Optional method
        return null;
    }

    @Override
    public List<GeoCoordinates> getShapeCoordinates() {

        //Optional method
        return null;
    }

    @Override
    public Map<Locale, List<String>> getNames() {

        //Optional method
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineImpl line = (LineImpl) o;
        return line_id == line.line_id &&
                startNode_id == line.startNode_id &&
                endNode_id == line.endNode_id &&
                frc == line.frc &&
                fow == line.fow &&
                length_meter == line.length_meter &&
                isReversed == line.isReversed &&
                startNode.equals(line.startNode) &&
                endNode.equals(line.endNode) &&
                Objects.equals(name, line.name) &&
                lineGeometry.equals(line.lineGeometry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(line_id, startNode_id, endNode_id, startNode, endNode, frc, fow, length_meter, name, isReversed, lineGeometry);
    }
}
