import Exceptions.InvalidBboxException;
import Exceptions.InvalidWGS84CoordinateException;
import HereApi.ApiRequest;
import HereApi.HereTrafficV7;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Time;
import java.sql.Timestamp;

public class Here2Osm {

    private static final Logger logger = LoggerFactory.getLogger(Here2Osm.class);

    // mainMethode
    public static void main(String[] args) {

        long start = System.currentTimeMillis();

        HereTrafficV7 request = new HereTrafficV7();
        request.insertBbox();
        // Use request.setBbox("<bbox>") instead to set the bounding box in Code.
        request.update("incidents");
        request.update("flow");

        long end = System.currentTimeMillis();
        Time duration = new Time(end - start - 3_600_000);

        logger.info("Program ended. Duration: " + duration);
    }
}
