import Exceptions.InvalidBboxException;
import Exceptions.InvalidWGS84CoordinateException;
import HereApi.ApiRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Here2Osm {

    private static final Logger logger = LoggerFactory.getLogger(Here2Osm.class);

    // mainMethode
    public static void main(String[] args) {

        ApiRequest request = new ApiRequest();
        try {
            request.updateIncidentData();
            request.updateFlowData();
        } catch (InvalidBboxException | InvalidWGS84CoordinateException e) {
            logger.error("Failed to read incident and flow data due to a invalid bounding box. Message: {}", e.getMessage(), e);
        }
    }
}
