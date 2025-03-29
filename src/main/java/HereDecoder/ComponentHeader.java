package HereDecoder;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */
@Getter 
public class ComponentHeader {
    @Setter
    private int gcId;
    @Setter
    private int lengthCompCH;
    @Setter
    private int lengthAttrCH;
    @Setter
    private boolean isValid;
    private int totalLength;
    private int bytesToRead;

    public static byte[] encode(int gcId, int lengthCompValue, int lengthAttrValue) {
        return new byte[]{(byte) gcId, (byte) lengthCompValue, (byte) lengthAttrValue};
    }

    public int decode(int[] bytes) {
        IntUnLoMb lengthComp = new IntUnLoMb();
        IntUnLoMb lengthAttr = new IntUnLoMb();
        int totalBytesRead = 0;

        if (bytes.length < 2) {
            return 0;
        }

        //IntUnTi one byte
        gcId = bytes[0];
        totalBytesRead++;

        //IntUnLoMb one byte

        totalBytesRead += lengthComp.decode(Arrays.copyOfRange(bytes, totalBytesRead, bytes.length));
        totalBytesRead += lengthAttr.decode(Arrays.copyOfRange(bytes, totalBytesRead, bytes.length));

        isValid = lengthComp.isValid() && lengthAttr.isValid();

        lengthCompCH = lengthComp.getValue();
        lengthAttrCH = lengthAttr.getValue();

        totalLength = lengthCompCH + lengthAttrCH - 1;
        bytesToRead = lengthCompCH + lengthAttrCH + 2;

        return totalBytesRead;
    }

}
