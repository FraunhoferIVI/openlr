package HereDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class PathProperties {

    //lowest functional road class next point
    public int lfrcnp;
    //distance to next point
    public int dnp;
    // against driving dir
    public boolean add;

    public String getLfrcnpStr() {
        return "FRC" + lfrcnp + 1;
    }

    public int decode(int[] buf) {
        int numberBytesRead = 0;

        //Byte 0
        int lowestFRCtoNextPoint = (buf[0]);
        numberBytesRead++;
        lfrcnp = lowestFRCtoNextPoint;

        //Byte 1 and 2
        IntUnLoMb distanceNextPoint = new IntUnLoMb();
        numberBytesRead += distanceNextPoint.decode(Arrays.copyOfRange(buf, numberBytesRead, buf.length));
        dnp = distanceNextPoint.getValue();

        //Byte 3
        FixedBitArray selector = new FixedBitArray();
        numberBytesRead += selector.decode(Arrays.copyOfRange(buf, numberBytesRead, buf.length));

        byte bitAgainstDrivingDirection = 0x01;
        if ((selector.getValue() & bitAgainstDrivingDirection) == bitAgainstDrivingDirection) {
            add = true;
        }
        return numberBytesRead;
    }

    public byte[] encode() {

        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        byte[] lfrcnpByte = new byte[]{(byte) lfrcnp};
        byte[] distanceBuf = IntUnLoMb.encode(dnp);
        byte[] selector = new byte[]{(byte) (add ? 0x01 : 0x00)};

        try {
            buf.write(lfrcnpByte);
            buf.write(distanceBuf);
            buf.write(selector);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf.toByteArray();
    }
}
