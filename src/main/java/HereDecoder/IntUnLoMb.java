package HereDecoder;
import lombok.Getter;
import lombok.Setter;
/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class IntUnLoMb {

    public static int max_size = 5;
    public static int min_size = 1;

    @Getter @Setter
    private int size;
    @Getter @Setter
    private int value;
    @Getter @Setter
    private boolean isValid;

    public static byte[] encode(int value) {
        int size;

        if ((long) value < 1 << 7)
            size = 1; // 7 bits available
        else if ((long) value < 1 << 14)
            size = 2; // 14 bits available;
        else if ((long) value < 1 << 21)
            size = 3; // 21 bits available;
        else if ((long) value < 1 << 28)
            size = 4; // 28 bits available;
        else
            size = 5;

        byte[] buf = new byte[size];
        for (int i = 0; i < size; i++) {

            int shift = 7 * (size - i - 1);
            byte curByte = (byte) (0x7f & ((long) value >> shift));
            curByte |= 0x80; //Set continue bit to true;
            buf[i] = curByte;
        }
        buf[size - 1] &= 0x7f; //Set the continue bit on the last byte to 0
        return buf;

    }

    public int decode(int[] byteBuffer) {
        int buf_size = byteBuffer.length;
        if (buf_size < min_size) {
            isValid = false;
            return 0;
        }
        size = min_size;
        //Start with MSB
        value = byteBuffer[0] & 0x7F;

        // Processing 1---5 input bytes
        for (int i = 0; i < max_size; i++) {
            int buf = byteBuffer[i];
            //No continuation?
            if ((buf & 0x80) == 0x0) {
                isValid = true;
                // Return number of bytes read
                return size;
            }
            size++;

            //Make space for the next 7 bits
            value <<= 7;

            // add 7 bite to value
            value |= byteBuffer[i + 1] & 0x7f;
        }

        // If we reach here, we've read 5 bytes
        // all bytes have the continuation bit set to 1  < encoding is wrong

        //nothing res
        return 0;
    }
}
