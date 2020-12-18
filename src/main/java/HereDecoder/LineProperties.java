package HereDecoder;

import java.util.Arrays;

/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class LineProperties {

    //functional road class
    public int frc;
    public TpegEnums.FormOfWay fow;
    public int fow_id;
    public float bearing;
    public String frcStr = "FRC" + frc + 1;
    public int bearingLeft;
    public int bearingRight;

    public String getFrcStr() {
        return frcStr;
    }

    public int getFOW_id() {
        return fow_id;
    }

    public int getFrc() {
        return frc;
    }

    public int decode(int[] buf) {
        int numberBytesRead = 0;

        //Byte 0
        int functionalRoadClass = buf[0];
        numberBytesRead++;
        frc = functionalRoadClass;

        //Byte 1
        int form_of_way = buf[1];
        fow_id = form_of_way;
        numberBytesRead++;

        fow = (TpegEnums.FormOfWay.values()[form_of_way]);

        //Byte 2
        int getBearing = buf[2];
        numberBytesRead++;
        bearing = (float) ((360.0 / 256) * getBearing);
        /* 360 / 256 == 1 because of integer division
         * so this line becomes
         *      Bearing = (float) bearing;
         * Is this right? I wouldn't second-guess it, but there are unit tests
         * that are written that rely on this fact.
         */

        //Byte 3
        FixedBitArray selector = new FixedBitArray();
        numberBytesRead += selector.decode(Arrays.copyOfRange(buf, numberBytesRead, buf.length));

        return numberBytesRead;
    }

    public byte[] encode() {
        byte[] buf = new byte[4];
        byte selector = 0x00;
        buf[0] = (byte) frc;
        buf[1] = (byte) fow.ordinal();
        buf[2] = (byte) (bearing * 2560.0 / 360);
        buf[3] = selector;
        return buf;
    }

}
