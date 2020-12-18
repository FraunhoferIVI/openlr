package HereDecoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class OlrComponentHeader {

    public int gcId;
    public int lengthComp;
    public int lengthAttr;
    public boolean isValid;

    public static byte[] encode(int gcId, int lengthComp, int lengthAttr) {

        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        try {
            buf.write(new byte[]{(byte) gcId});
            buf.write(IntUnLoMb.encode(lengthComp));
            buf.write(IntUnLoMb.encode(lengthAttr));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buf.toByteArray();

    }

    public int getGcId() {
        return gcId;
    }

    public int getLengthComp() {
        return lengthComp;
    }

    public int getLengthAttr() {
        return lengthAttr;
    }

    public boolean isValid() {
        return isValid;
    }

    public int getTotalLength() {
        return lengthComp + lengthAttr - 1;
    }

    public int decode(int[] bytes) {

        IntUnLoMb lengthcomp = new IntUnLoMb();
        IntUnLoMb lengthattr = new IntUnLoMb();
        int totalBytesRead = 0;

        //IntUnTi one byte
        gcId = bytes[0];
        totalBytesRead++;

        //IntUnLoMB one byte
        totalBytesRead += lengthcomp.decode(Arrays.copyOfRange(bytes, totalBytesRead, bytes.length));
        totalBytesRead += lengthattr.decode(Arrays.copyOfRange(bytes, totalBytesRead, bytes.length));

        isValid = lengthcomp.isValid() && lengthattr.isValid();

        lengthComp = lengthcomp.getValue();
        lengthAttr = lengthattr.getValue();

        return totalBytesRead;
    }
}
