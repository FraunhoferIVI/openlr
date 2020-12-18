package HereDecoder;

/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class Geoposition {
    public static final int scale_factor = 10000000; // scale factor for converting to integers.

    private int latitude;
    private int longitude;

    public Geoposition() {
        this.latitude = 0;
        this.longitude = 0;
    }

    // Constructor with Latitude and Longitude in degrees
    public Geoposition(double latitude, double longitude) {
        this.latitude = toInt(latitude);
        this.longitude = toInt(longitude);
    }

    private static int toInt(double value) {
        return (int) (value * scale_factor);
    }

    private static double fromInt(int value) {
        return ((double) value) / scale_factor;
    }

    public double getLatitude() {
        return fromInt(latitude);
    }

    public void setLatitude(double latitude) {
        this.latitude = toInt(latitude);
    }

    public double getLongitude() {
        return fromInt(longitude);
    }

    public void setLongitude(double longitude) {
        this.longitude = toInt(longitude);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Geoposition) {
            Geoposition geoposition = (Geoposition) other;
            if (geoposition.latitude == this.latitude) {
                if (geoposition.longitude == this.longitude) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 17;

        hash = hash * 23 + Integer.valueOf(this.latitude).hashCode();
        hash = hash * 23 + Integer.valueOf(this.longitude).hashCode();
        return hash;

    }

    @Override
    public String toString() {
        return getLatitude() + ", " + getLongitude();
    }

}
