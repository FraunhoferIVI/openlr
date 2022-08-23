package HereApi;

import Decoder.HereDecoder;
import com.google.gson.*;
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
 * Reads out information from the HERE traffic flow API version 7, creates FlowItemV7's from it and decodes their olr-codes.
 */
public class FlowJsonParser {

    private final HereDecoder hereDecoder;
    private Timestamp updated;
    private List<FlowItemV7> flowItems = new ArrayList<>();
    private final List<AffectedLine> affectedLines = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(FlowJsonParser.class);

    /**
     * @param hereDecoder The decoder for olr-code and stuff
     */
    public FlowJsonParser(@Nullable HereDecoder hereDecoder)
    {
        this.hereDecoder = hereDecoder;
    }

    public Timestamp getUpdated() { return updated; }

    public List<FlowItemV7> getFlowItems() { return flowItems; }

    public List<AffectedLine> getAffectedLines() { return affectedLines; }

    /**
     * Extracts affected lines from decoded location. Adds olr, line and positive / negative offset to List.
     *
     * @param location   Location decoded by the OpenLR code
     * @param olr        Olr-code of the FlowItem
     * @param posOff     From location extracted positive offset, defines the distance between the start of the
     *                   location reference path and the start of the location
     * @param negOff     From location extracted negative offset, defines the distance between the end of the
     *                   location and the end of the location reference path
     */
    private void getAffectedLines(Location location, String olr, int posOff, int negOff) {
        // decode location, extract list of affected lines
        List<Line> listLines = location.getLocationLines();
        AffectedLine affectedLine = null;

        if (listLines != null && !listLines.isEmpty()) {
            for (int i = 0; i < listLines.size(); i++) {
                if (i == 0) {
                    affectedLine = new AffectedLine(listLines.get(i).getID(), olr);
                    affectedLine.setPosOff(posOff);
                }
                if (i == listLines.size() - 1) {
                    affectedLine = new AffectedLine(listLines.get(i).getID(), olr);
                    affectedLine.setNegOff(negOff);
                }
                if (i != 0 && (i != listLines.size() - 1)) {
                    affectedLine = new AffectedLine(listLines.get(i).getID(), olr);
                }
                this.affectedLines.add(affectedLine);
            }
        }
    }

    /**
     * Reads out the supplied json String for flow Information and stores it in a List of Flow-Items.
     *
     * @param json flow Information got from the Here-Traffic Api (v7)
     */
    public void parse(String json)
    {
        // cast Json String to Object to work with it
        JsonObject jsonObject = JsonParser.parseReader(new StringReader(json)).getAsJsonObject();

        updated = Timestamp.valueOf(jsonObject.get("sourceUpdated").getAsString().substring(0, 19)
                .replace('T', ' '));

        JsonArray results = jsonObject.get("results").getAsJsonArray();
        int resultCount = results.size();

        for (int i = 0; i < resultCount; i++)
        {
            JsonObject flowObject = results.get(i).getAsJsonObject();
            JsonObject locationObj = flowObject.get("location").getAsJsonObject();

            String name;
            try { name = locationObj.get("description").getAsString(); }
            catch (Exception e) { name = null; }

            String olr = locationObj.get("olr").getAsString();

            JsonObject currentFlow = flowObject.get("currentFlow").getAsJsonObject();

            double freeFlow = currentFlow.get("freeFlow").getAsDouble();

            Double speed;
            try { speed = currentFlow.get("speed").getAsDouble(); }
            catch (Exception e) { speed = freeFlow; }

            Double speedUncapped;
            try { speedUncapped = currentFlow.get("speed").getAsDouble(); }
            catch (Exception e) { speedUncapped = freeFlow; }


            double jamFactor = currentFlow.get("jamFactor").getAsDouble();

            double confidence;
            try { confidence = currentFlow.get("confidence").getAsDouble(); }
            catch (Exception e) { confidence = 1.0; }

            String traversability = currentFlow.get("traversability").getAsString();

            String juncTravS;
            try { juncTravS = currentFlow.get("junctionTraversability").getAsString(); }
            catch (Exception e) { juncTravS = ""; }
            JunctionTraversability juncTrav = JunctionTraversability.get(juncTravS);

            // Reads out TPEG-OLR Locations
            Location location = null;
            try {
                location = hereDecoder.decodeHere(olr);
                Integer posOff = null;
                Integer negOff = null;
                if (location != null && location.isValid()) {
                    posOff = location.getPositiveOffset();
                    negOff = location.getNegativeOffset();

                    getAffectedLines(location, olr, posOff, negOff);
                }
                else
                {
                    logger.warn("Could not decode olr code: {}", olr);
                }

                FlowItemV7 item = new FlowItemV7(name, olr, speed, speedUncapped, freeFlow, jamFactor,
                        confidence, traversability, juncTrav, posOff, negOff);
                flowItems.add(item);

                // print progress
                System.out.println("Progress: " + (i + 1) + "/" + resultCount + " (" + (int) ((double) (i + 1) / resultCount * 100) + "%)");
            }
            catch (Exception e) { logger.error(e.getMessage()); }
        }
    }
}
