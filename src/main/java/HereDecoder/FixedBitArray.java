
package HereDecoder;

/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class FixedBitArray {

    public static final int max_size = 5;
    public static final int min_size = 1;
    public final int maxBitsAvailable = max_size * 7;
    public int value;
    public int size;
    public boolean isValid;
    private int lon;
    private int lat;

    public FixedBitArray() {
        size = 1;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public boolean isBitSet(int indexFromLeftMostBit) {
        int totalSizeOfBitArray = (max_size * 7) - 1;
        int newIndex = totalSizeOfBitArray - indexFromLeftMostBit;
        return isBitSetFromRight(newIndex);
    }

    public boolean isBitSetFromRight(int indexFromRightMostBit) {
        boolean retVal = false;
        int baseIndexOn = 0x1;
        if ((indexFromRightMostBit >= 0) && (indexFromRightMostBit <= maxBitsAvailable)) {
            baseIndexOn = baseIndexOn << indexFromRightMostBit;
            if ((value & baseIndexOn) == baseIndexOn) {
                retVal = true;
            }
        }
        return retVal;
    }

    @Override
    public String toString() {
        return "(0)" + org.apache.commons.lang.StringUtils.leftPad(Integer.toBinaryString(value), 8 * size, "0");
    }

    public int decode(int[] byteBuffer) {
        size = 0;
        value = 0x0;
        if (isBifferValid(byteBuffer)) {
            //Proessing 1..5 input bytes
            for (int i = 0; i < max_size; i++) {
                value = (value | (byteBuffer[i] & 0x7F));
                size++;

                // is the continuation bit set?
                if ((byteBuffer[i] & 0x80) != 0x0) {
                    //Shift 7 bits of value
                    value = (value << 7);
                } else {
                    isValid = true;
                    //break from the loop
                    break;
                }
            }
        }

        if (!isValid) {
            //If we reach here, either the buffer is  invalide or
            // we've read 5 bytes and all bytes have the continuation bit set to 1
            // -> Encoding is wrong
            size = 0;
            value = 0x0;

        }
        return size;
    }

    private boolean isBifferValid(int[] byteBuffer) {
        boolean retVal = true;
        int buf_size = byteBuffer.length;
        if (buf_size < min_size) {
            isValid = false;
            retVal = false;
        }
        return retVal;
    }

    public byte getValue() {
        return (byte) 0;
    }
}
