package HereDecoder;

import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
/**
 * HERE implementation of the TPEG-OLR standard (ISO/TS 21219-22)
 * Original C# program translated to Java.
 */

public class BaseLocationReference {
    @Getter @Setter
    public boolean valid;
    @Getter @Setter
    public double offset;
    @Getter @Setter
    public Geoposition origin;
    public OpenLocationReference.OLRType type;

    public void baseLocationReference() {

    }

    public OpenLocationReference.OLRType getType() {
        return OpenLocationReference.OLRType.valueOf("Unknown");
    }


    public BaseLocationReference fromBinary(int[] bytes, int bytesRead) {
        //Check if Buffer is valid
        if (!isBufferValid(bytes)) {
            bytesRead = 0;
            return this;
        }

        BaseLocationReference retVal = new BaseLocationReference();
        int totalBytesRead = 0;

        ComponentHeader olrHeader = new ComponentHeader();

        totalBytesRead += olrHeader.decode(bytes);

        int take = olrHeader.getTotalLength();

        ComponentHeader locationTypeHeader = new ComponentHeader();
        locationTypeHeader.decode(Arrays.copyOfRange(bytes, totalBytesRead, bytes.length));

        if (locationTypeHeader.getGcId() == 0x08) {
            OpenLocationReference olr = new OpenLocationReference();
            int bytesReadFrom = olr.decode(Arrays.copyOfRange(bytes, totalBytesRead, take));
            totalBytesRead += olr.getBytesRead();

            if (olr.isValid()) {
                retVal = olr;
            }
        } else {
            //Undecodable Data
            totalBytesRead += take;
        }

        bytesRead = totalBytesRead;
        return retVal;
    }

    private boolean isBufferValid(int[] byteBuffer) {
        final int min_size = 3;
        boolean retVal = true;
        int buf_size = byteBuffer.length;
        if (buf_size < min_size) {
            isValid = false;
            retVal = false;
        }
        return retVal;
    }

}
