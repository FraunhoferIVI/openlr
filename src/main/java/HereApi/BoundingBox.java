package HereApi;

import openlr.map.GeoCoordinates;

/**
 *
 * This class describes the bounding box.
 *
 * @author  Emily Kast
 *
 */

public class BoundingBox extends TrafficItem {

    double height;
    double width;
    /**
     * The upper left corner latitude of the bounding box
     */
    private double upperLeftLat;
    /**
     * The upper left corner longitude of the bounding box
     */
    private double upperLeftLon;
    /**
     * The bottom right latitude corner of the bounding box
     */
    private double bottomRightLat;
    /** The bottom right latitude corner of the bounding box */
    private double bottomRightLon;

    public BoundingBox(double upperLeftLat, double upperLeftLon, double bottomRightLat, double bottomRightLon) {
        this.upperLeftLat = upperLeftLat;
        this.upperLeftLon = upperLeftLon;
        this.bottomRightLat = bottomRightLat;
        this.bottomRightLon = bottomRightLon;
        this.height = upperLeftLat - bottomRightLat;
        this.width = bottomRightLon - upperLeftLon;
    }

    public double getUpperLeftLat() {
        return upperLeftLat;
    }

    public double getUpperLeftLon() {
        return upperLeftLon;
    }

    public double getBottomRightLat() {
        return bottomRightLat;
    }

    public double getBottomRightLon() {
        return bottomRightLon;
    }

    public double getWidth() {

        return width;
    }

    public double getHeight() {

        return height;
    }

    /**
     * Builds String from bounding box information to use in Here Api request
     *
     * @return Bounding Box String to use in Here Api request
     */
    public String getBboxRequestString() {
        return "&bbox=" + upperLeftLat + "," + upperLeftLon + ";" + bottomRightLat + "," + bottomRightLon;
    }


}
