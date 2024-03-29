package HereApi;

import Decoder.HereDecoder;
import openlr.location.Location;
import openlr.map.Line;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataCollector {

    // List containing incident information
    private List<Incident> incidents = new ArrayList<>();
    // List containing affected line info (just for incidents right now)
    private List<AffectedLine> affectedLines = new ArrayList<>();
    // List containing traffic flow info
    private List<FlowItem> flowItems = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(DataCollector.class);

    public List<Incident> getIncidents() { return incidents; }

    public List<AffectedLine> getAffectedLines() { return affectedLines; }

    public List<FlowItem> getFlowItems() { return flowItems; }

    /**
     * Converts String to Timestamp
     *
     * @param dateString Date as String, Format: MM/dd/yyyy hh:mm:ss
     *
     * @return Timestamp
     *
     * @throws ParseException Signals that an error has been reached unexpectedly while parsing.
     */
    private Timestamp convertString2Timestamp(String dateString) throws ParseException {
        // Date formatter, needed to convert String to Timestamp
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        Date date = formatter.parse(dateString);
        return new Timestamp(date.getTime());
    }

    /**
     * Collects incident information and affected lines for all traffic items read from the XML.
     * Uses OpenLR decoder (TomTom, https://github.com/tomtom-international/openlr ) to determine affected lines.
     *
     * @param trafficItemList List containing extracted traffic items
     *
     * @throws Exception Exception
     */
    public void collectIncidentInformation(@NotNull List<TrafficItem> trafficItemList) throws Exception {

        for (TrafficItem trafficItemObject : trafficItemList) {

            String incidentId = trafficItemObject.getId();
            String type = trafficItemObject.getType();
            String status = trafficItemObject.getStatus();

            // Converts start and end date from String to Timestamp
            Timestamp start = convertString2Timestamp(trafficItemObject.getStart());
            Timestamp end = convertString2Timestamp(trafficItemObject.getEnd());
            String criticality = trafficItemObject.getCriticality();
            String openLRCode = trafficItemObject.getOpenLR();
            String shortDesc = trafficItemObject.getShortDesc();
            String longDesc = trafficItemObject.getLongDesc();
            // Parses road closure information from string to boolean
            boolean roadClosure = Boolean.parseBoolean(trafficItemObject.getClosure());

            // Reads out TPEG-OLR Locations
            // Location location = decoderHere.decodeHere(openLRCode);

            int posOff;
            int negOff;
/*
            // If location is invalid positive and negative offsets get the value -100
            if (location == null) {
                posOff = -100;
                negOff = -100;
            } else {
                // Gets positive and negative offset
                posOff = location.getPositiveOffset();
                negOff = location.getNegativeOffset();

                // Extract affected lines from location and add to list
                getAffectedLines(location, incidentId, posOff, negOff);
            }

 */

            // Create incident and add to list
            // incident2list(incidentId, type, status, start, end, criticality, openLRCode, shortDesc, longDesc, roadClosure, posOff, negOff);
        }
        logger.info("Collected incident data.");
    }

    /**
     * Collects flow information for all flow items read from the XML.
     *
     * @param flowItemList List containing extracted flow items
     */
    public void collectFlowInformation(@NotNull List<FlowItem> flowItemList) {
        this.flowItems = flowItemList;

        logger.info("Collected flow data.");
    }

    /**
     * Extracts affected lines from decoded location. Adds incident id, line and positive / negative offset to List.
     *
     * @param location   Location decoded by the OpenLR code
     * @param incidentId Incident id
     * @param posOff     From location extracted positive offset, defines the distance between the start of the
     *                   location reference path and the start of the location
     * @param negOff     From location extracted negative offset, defines the distance between the end of the
     *                   location and the end of the location reference path
     */
    private void getAffectedLines(Location location, String incidentId, int posOff, int negOff) {
        // decode location, extract list of affected lines
        List<Line> listLines = location.getLocationLines();
        AffectedLine affectedLine = null;

        if (listLines != null && !listLines.isEmpty()) {
            for (int i = 0; i < listLines.size(); i++) {
                if (i == 0) {
                    affectedLine = new AffectedLine(listLines.get(i).getID(), incidentId);
                    affectedLine.setPosOff(posOff);
                }
                if (i == listLines.size() - 1) {
                    affectedLine = new AffectedLine(listLines.get(i).getID(), incidentId);
                    affectedLine.setNegOff(negOff);
                }
                if (i != 0 && (i != listLines.size() - 1)) {
                    affectedLine = new AffectedLine(listLines.get(i).getID(), incidentId);
                }
                this.affectedLines.add(affectedLine);
            }
        }
    }

    /**
     * Creates a new incident and adds it to the list
     *
     * @param incidentId  Incident ID
     * @param type        Type of incident
     * @param status      Incident status
     * @param start       Assumed start date of the incident
     * @param end         Assumed end date of the incident
     * @param criticality Severity of the accidents
     * @param openLRCode  OpenLR Code as Base64 String
     * @param shortDesc   Abbreviated incident description
     * @param longDesc    Details incident description
     * @param roadClosure Information if road is closed due to the incident
     * @param posOff      From location extracted positive offset, defines the distance between the start of the
     *                    location reference path and the start of the location
     * @param negOff      From location extracted negative offset, defines the distance between the end of the
     *                    location and the end of the location reference path
     */
    private void incident2list(String incidentId, String type, String status, Timestamp start, Timestamp end,
                               String criticality, String openLRCode, String shortDesc, String longDesc,
                               boolean roadClosure, int posOff, int negOff) {

        Incident incident = new Incident(incidentId, type, status, start, end, criticality, openLRCode, shortDesc,
                longDesc, roadClosure, posOff, negOff);

        incidents.add(incident);
    }


}
