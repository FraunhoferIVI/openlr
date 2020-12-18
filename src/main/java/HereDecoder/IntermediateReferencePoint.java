package HereDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class IntermediateReferencePoint {

    public Geoposition coordinate;
    public LineProperties lineProperties;
    public PathProperties pathProperties;
    public boolean isValid;
    private int lat;
    private int lon;

    public IntermediateReferencePoint() {
        isValid = true;
    }

    public Geoposition getCoordinate() {
        return coordinate;
    }

    public LineProperties getLineProperties() {
        return lineProperties;
    }

    public PathProperties getPathProperties() {
        return pathProperties;
    }

    public boolean isValid() {
        return isValid;
    }

    public int decode(int[] buff, Geoposition prev) {
        final int sizeOfRelVal = 2;
        int totalBytesRead = 0;

        lon = LinearLocationReference.decode_relative(buff);
        totalBytesRead += sizeOfRelVal;
        lat = LinearLocationReference.decode_relative(Arrays.copyOfRange(buff, totalBytesRead, buff.length));
        totalBytesRead += sizeOfRelVal;

        coordinate = OpenLocationReference.fronRelativeCoordinates(lat, lon, prev);

        FixedBitArray selector = new FixedBitArray();
        totalBytesRead += selector.decode(Arrays.copyOfRange(buff, totalBytesRead, buff.length));

        OlrComponentHeader linePropertiesHeader = new OlrComponentHeader();
        totalBytesRead += linePropertiesHeader.decode(Arrays.copyOfRange(buff, totalBytesRead, buff.length));

        isValid = linePropertiesHeader.isValid();

        if (linePropertiesHeader.getGcId() != 0x09)
            isValid = false;

        if (isValid) {
            lineProperties = new LineProperties();
            totalBytesRead += lineProperties.decode(Arrays.copyOfRange(buff, totalBytesRead, buff.length));

            OlrComponentHeader pathPropertiesHeader = new OlrComponentHeader();
            totalBytesRead += pathPropertiesHeader.decode(Arrays.copyOfRange(buff, totalBytesRead, buff.length));

            isValid = pathPropertiesHeader.isValid();

            if (pathPropertiesHeader.getGcId() != 0x0a)
                isValid = false;

            pathProperties = new PathProperties();
            totalBytesRead += pathProperties.decode(Arrays.copyOfRange(buff, totalBytesRead, buff.length));

        }

        return totalBytesRead;
    }

    public byte[] encode(Geoposition prev) {
        /*if(lat == null)
            lat = (int)(100000.0 * coordinate.getLatitude() -  100000.0 * prev.getLatitude());
        if(lon == null)
            lon = (int)(100000.0 * coordinate.getLatitude() -  100000.0 * prev.getLatitude());*/


        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        int latitude = OpenLocationReference.absGeoEq1(coordinate.getLatitude());
        int longitude = OpenLocationReference.absGeoEq1(coordinate.getLongitude());
        byte[] selector = new byte[]{0x00}; // Selector: only 0x00 is supported
        byte[] linePropertiesBytes = lineProperties.encode();
        byte[] lineComponentHeaderBytes = OlrComponentHeader.encode(0x09, linePropertiesBytes.length + 1, linePropertiesBytes.length);
        byte[] pathPropertiesBytes = pathProperties.encode();
        byte[] pathPropertiesHeaderBytes = OlrComponentHeader.encode(0x0a, pathPropertiesBytes.length + 1, pathPropertiesBytes.length);

        try {
            buf.write(LinearLocationReference.encode_relative(lon));
            buf.write(LinearLocationReference.encode_relative(lat));
            buf.write(selector);
            buf.write(lineComponentHeaderBytes);
            buf.write(lineProperties.encode());
            buf.write(pathPropertiesHeaderBytes);
            buf.write(pathProperties.encode());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf.toByteArray();
    }
}
