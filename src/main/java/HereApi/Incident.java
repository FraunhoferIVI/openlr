package HereApi;

import HereApi.TrafficItem;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * This class describes the incident object.
 *
 * @author Emily Kast
 *
 */

public class Incident {

    private String incidentId;
    private String type;
    private String status;
    private Timestamp start;
    private Timestamp end;
    private String criticality;
    private String openLRCode;
    private String shortDesc;
    private String longDesc;
    private boolean roadClosure;
    private int posOff;
    private int negOff;

}
