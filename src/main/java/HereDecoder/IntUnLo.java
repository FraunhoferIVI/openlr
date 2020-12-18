package HereDecoder;

/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class IntUnLo {

    private static final int min_size = 4;
    public int value;
    public int size;
    public boolean isValid;

    public static byte[] encode(int value) {
        byte[] buf = new byte[min_size];
        for (int i = 0; i < min_size; i++) {
            int shift = (min_size - i - 1) * 8;
            byte b = (byte) (0xff & (value >> shift));
            buf[i] = b;
        }
        return buf;
    }

    public int decode(byte[] byteBuffer) {
        int buf_size = byteBuffer.length;
        if (buf_size < min_size) {
            // log
            isValid = false;
            return 0;
        }

        size = min_size;
        value = 0;

        for (int i = 0; i < size; i++) {
            //Add 7 bits to value
            value |= (byteBuffer[i]);
            //Make space for the next byte
            //For all except the rightmost byte
            if (i < size - 1)
                value <<= 8;

        }
        return size;

    }
}
