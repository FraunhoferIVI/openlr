package HereDecoder;

import openlr.xml.generated.PolygonLocationReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class FirstReferencePoint {
    @Getter @Setter
    public Geoposition coordinate;
    @Getter @Setter
    public LineProperties lineProperties;
    @Getter @Setter
    public PathProperties pathProperties;
    @Getter @Setter
    public boolean isValid;
    private int lat;
    private int lon;

    public int decode(int[] buff) {
        final int sizeOfAbsVal = 3;
        int totalBytesRead = sizeOfAbsVal;

        lon = GeoCoordinateLocationReference.decode_absolute(buff);
        lat = GeoCoordinateLocationReference.decode_absolute(Arrays.copyOfRange(buff, totalBytesRead, buff.length));
        totalBytesRead += sizeOfAbsVal;

        coordinate = OpenLocationReference.fromAbsoluteCoordinates(lat, lon);

        int[] selectorBytes = Arrays.copyOfRange(buff, totalBytesRead, buff.length);
        if (selectorBytes[0] != 0x00) {
            //Current do not support
            isValid = false;
            return totalBytesRead;
        }

        totalBytesRead++;

        OlrComponentHeader linePropertiesHeader = new OlrComponentHeader();
        totalBytesRead += linePropertiesHeader.decode(Arrays.copyOfRange(buff, totalBytesRead, buff.length));

        isValid = linePropertiesHeader.isValid();
        if (linePropertiesHeader.getGcId() != 0x09) {
            isValid = false;
        }

        if (isValid) {
            lineProperties = new LineProperties();
            totalBytesRead += lineProperties.decode(Arrays.copyOfRange(buff, totalBytesRead, buff.length));

            OlrComponentHeader pathPropertiesHeader = new OlrComponentHeader();
            totalBytesRead += pathPropertiesHeader.decode(Arrays.copyOfRange(buff, totalBytesRead, buff.length));

            isValid = pathPropertiesHeader.isValid();
            if (pathPropertiesHeader.getGcId() != 0x0a) {
                isValid = false;
            }

            pathProperties = new PathProperties();
            totalBytesRead += pathProperties.decode(Arrays.copyOfRange(buff, totalBytesRead, buff.length));
        }
        return totalBytesRead;
    }

    public byte[] encode() {

        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        int latitude = OpenLocationReference.absGeoEq1(coordinate.getLatitude());
        int longitude = OpenLocationReference.absGeoEq1(coordinate.getLongitude());
        byte[] selector = new byte[]{0x00}; // Selector: only 0x00 is supported
        byte[] linePropertiesBytes = lineProperties.encode();
        byte[] lineComponentHeaderBytes = OlrComponentHeader.encode(0x09, linePropertiesBytes.length + 1, linePropertiesBytes.length);
        byte[] pathPropertiesBytes = pathProperties.encode();
        byte[] pathPropertiesHeaderBytes = OlrComponentHeader.encode(0x0a, pathPropertiesBytes.length + 1, pathPropertiesBytes.length);

        try {
            buf.write(GeoCoordinateLocationReference.encode_absolute(longitude));
            buf.write(GeoCoordinateLocationReference.encode_absolute(latitude));
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
