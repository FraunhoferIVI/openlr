package HereDecoder;

import java.util.Arrays;

/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class GeoCoordinateLocationReference extends BaseLocationReference {

    // Required
    public Geoposition coordinate;
    public OpenLocationReference.OLRType type;

    public static byte[] encode_absolute(int absoluteVal) {
        byte upperVal = (byte) ((absoluteVal >> 16) & 0xff);
        byte middleVal = (byte) ((absoluteVal >> 8) & 0xff);
        byte lowerVal = (byte) (absoluteVal & 0xff);

        return new byte[]{upperVal, middleVal, lowerVal};
    }

    public static int decode_absolute(int[] buf) {
        // Read first 3 bytes for longitude
        int upperVal = (buf[0] << 16);
        int middleVal = (buf[1] << 8);
        int lowerVal = buf[2];
        int absoluteVal = (upperVal | middleVal) | lowerVal;

        //if negative bit set
        if ((buf[0] & 0x80) == 0x80) {
            // do tow's complete
            byte byte0 = (byte) ~(buf[0]);
            byte byte1 = (byte) ~(buf[1]);
            byte byte2 = (byte) ~(buf[2]);

            upperVal = (Byte.toUnsignedInt(byte0) << 16);
            middleVal = (Byte.toUnsignedInt(byte1) << 8);
            lowerVal = (Byte.toUnsignedInt(byte2));
            absoluteVal = (upperVal | middleVal) | lowerVal;
            absoluteVal++;

            //return negative value
            absoluteVal = -absoluteVal;
        }
        return absoluteVal;
    }

    public Geoposition getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Geoposition coordinate) {
        this.coordinate = coordinate;
    }

    public OpenLocationReference.OLRType getType() {
        return OpenLocationReference.OLRType.GeoCoordinate;
    }

    public int decode(int[] buff) {
        final int sizeOfAbsVal = 3;
        int totalBytesRead = sizeOfAbsVal;

        int lon = decode_absolute(buff);
        int lat = decode_absolute(Arrays.copyOfRange(buff, totalBytesRead, buff.length));
        totalBytesRead += sizeOfAbsVal;

        coordinate = OpenLocationReference.fromAbsoluteCoordinates(lat, lon);

        //byte selectorByte = buff[totalBytesRead++];
        return totalBytesRead;
    }

    public byte[] encode() {
        byte[] buf = new byte[7];
        byte[] longitudeBuf = encode_absolute((int) coordinate.getLongitude());
        buf[0] = longitudeBuf[0];
        buf[1] = longitudeBuf[1];
        buf[2] = longitudeBuf[2];

        byte[] latitudeBuf = encode_absolute((int) coordinate.getLatitude());
        buf[3] = latitudeBuf[0];
        buf[4] = latitudeBuf[1];
        buf[5] = latitudeBuf[2];

        byte selectorByte = 0x00;
        buf[6] = selectorByte;

        return buf;
    }
}
