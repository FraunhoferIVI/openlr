package HereApi;

import Decoder.HereDecoder;
import com.google.gson.*;
import openlr.location.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class FlowJsonParser {

    private Timestamp updated;
    private List<FlowItemV7> flowItems = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(FlowJsonParser.class);

    public Timestamp getUpdated() { return updated; }

    public List<FlowItemV7> getFlowItems() { return flowItems; }

    /**
     * Reads out the supplied json String for flow Information and stores it in a List of Flow-Items.
     *
     * @param json flow Information got from the Here-Traffic Api (v7)
     */
    public void parse(String json)
    {
        // cast Json String to Object to work with it
        JsonObject jsonObject = JsonParser.parseReader(new StringReader(json)).getAsJsonObject();

        String updatedStr = jsonObject.get("sourceUpdated").getAsString().substring(0, 19).replace('T', ' ');
        updated = Timestamp.valueOf(updatedStr);

        JsonArray results = jsonObject.get("results").getAsJsonArray();

        for (JsonElement flowElement : results)
        {
            JsonObject flowObject = flowElement.getAsJsonObject();
            JsonObject location = flowObject.get("location").getAsJsonObject();

            String name;
            try { name = location.get("description").getAsString(); }
            catch (Exception e) { name = null; }

            String olr = location.get("olr").getAsString();

            JsonObject currentFlow = flowObject.get("currentFlow").getAsJsonObject();

            Double speed;
            try { speed = currentFlow.get("speed").getAsDouble(); }
            catch (Exception e) { speed = null; }

            Double speedUncapped;
            try { speedUncapped = currentFlow.get("speed").getAsDouble(); }
            catch (Exception e) { speedUncapped = null; }

            double freeFlow = currentFlow.get("freeFlow").getAsDouble();
            double jamFactor = currentFlow.get("jamFactor").getAsDouble();

            double confidence;
            try { confidence = currentFlow.get("confidence").getAsDouble(); }
            catch (Exception e) { confidence = 1.0; }

            String traversability = currentFlow.get("traversability").getAsString();

            String juncTravS;
            try { juncTravS = currentFlow.get("junctionTraversability").getAsString(); }
            catch (Exception e) { juncTravS = ""; }
            JunctionTraversability juncTrav = JunctionTraversability.get(juncTravS);

            FlowItemV7 item = new FlowItemV7(name, olr, speed, speedUncapped, freeFlow, jamFactor,
                    confidence, traversability, juncTrav);
            flowItems.add(item);
        }
    }
}
