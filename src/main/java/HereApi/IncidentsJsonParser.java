package HereApi;

import Decoder.HereDecoder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import openlr.location.Location;
import openlr.map.Line;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads out information from the HERE traffic incident API version 7, creates IncidentItemV7's from it and decodes their olr-codes.
 */
public class IncidentsJsonParser {

    private final HereDecoder hereDecoder;
    private Timestamp updated;
    private final List<IncidentItemV7> incidents = new ArrayList<>();
    private final List<AffectedLine> affectedLines = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(IncidentsJsonParser.class);

    public IncidentsJsonParser(@Nullable HereDecoder hereDecoder)
    {
        this.hereDecoder = hereDecoder;
    }

    public Timestamp getUpdated() {
        return updated;
    }

    public List<IncidentItemV7> getIncidentItems() {
        return incidents;
    }

    public List<AffectedLine> getAffectedLines() {
        return affectedLines;
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
     * Reads out the supplied json String for incident Information and stores it in a List of Incident-Items.
     *
     * @param json incident Information got from the Here-Traffic Api (v7)
     */
    public void parse(String json) {
        // cast Json String to Object
        JsonObject jsonObject = JsonParser.parseReader(new StringReader(json)).getAsJsonObject();

        updated = Timestamp.valueOf(jsonObject.get("sourceUpdated").getAsString().substring(0, 19)
                .replace('T', ' '));

        JsonArray results = jsonObject.get("results").getAsJsonArray();
        int resultCount = results.size();

        for (int i = 0; i < resultCount; i++) {
            JsonObject incident = results.get(i).getAsJsonObject();
            JsonObject locationObj = incident.get("location").getAsJsonObject();

            String olr = locationObj.get("olr").getAsString();

            JsonObject details = incident.get("incidentDetails").getAsJsonObject();

            String id = details.get("id").getAsString();
            String originalId = details.get("originalId").getAsString();
            Timestamp startTime = Timestamp.valueOf(details.get("startTime").getAsString().substring(0, 19)
                    .replace('T', ' '));
            Timestamp endTime = Timestamp.valueOf(details.get("endTime").getAsString().substring(0, 19)
                    .replace('T', ' '));
            Boolean roadClosed = details.get("roadClosed").getAsBoolean();

            String juncTravStr;
            try {
                juncTravStr = details.get("junctionTraversability").getAsString();
            } catch (Exception e) {
                juncTravStr = "";
            }
            JunctionTraversability juncTrav = JunctionTraversability.get(juncTravStr);

            String description = details.get("description").getAsJsonObject().get("value").getAsString();
            String summary = details.get("summary").getAsJsonObject().get("value").getAsString();

            // Reads out TPEG-OLR Locations
            Location location = null;
            try {
                location = hereDecoder.decodeHere(olr);
                Integer posOff = null;
                Integer negOff = null;
                if (location != null && location.isValid()) {
                    posOff = location.getPositiveOffset();
                    negOff = location.getNegativeOffset();

                    getAffectedLines(location, id, posOff, negOff);
                }
                else
                {
                    logger.warn("Could not decode olr code: {}", olr);
                }

                IncidentItemV7 item = new IncidentItemV7(id, originalId, olr, startTime, endTime, roadClosed,
                        description, summary, juncTrav, posOff, negOff);
                incidents.add(item);
            }
            catch (Exception e) { logger.error(e.getMessage()); }

            // print progress
            System.out.println("Progress: " + (i + 1) + "/" + resultCount + " (" + (int) ((double) (i + 1) / resultCount * 100) + "%)");
        }
    }
}
