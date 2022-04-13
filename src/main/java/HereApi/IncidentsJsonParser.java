package HereApi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class IncidentsJsonParser {

    private Timestamp updated;
    List<IncidentItemV7> incidents = new ArrayList<>();

    public Timestamp getUpdated() { return updated; }

    public List<IncidentItemV7> getIncidentItems() { return incidents; }

    /**
     * Reads out the supplied json String for incident Information and stores it in a List of Incident-Items.
     *
     * @param json incident Information got from the Here-Traffic Api (v7)
     */
    public void parse(String json)
    {
        // cast Json String to Object
        JsonObject jsonObject = JsonParser.parseReader(new StringReader(json)).getAsJsonObject();

        updated = Timestamp.valueOf(jsonObject.get("sourceUpdated").getAsString().substring(0, 19)
                .replace('T', ' '));

        JsonArray results = jsonObject.get("results").getAsJsonArray();

        for (JsonElement incidentElement : results)
        {
            JsonObject incident = incidentElement.getAsJsonObject();
            JsonObject location = incident.get("location").getAsJsonObject();

            String olr = location.get("olr").getAsString();

            JsonObject details = incident.get("incidentDetails").getAsJsonObject();

            String id = details.get("id").getAsString();
            String originalId = details.get("originalId").getAsString();
            Timestamp startTime = Timestamp.valueOf(details.get("startTime").getAsString().substring(0, 19)
                    .replace('T', ' '));
            Timestamp endTime = Timestamp.valueOf(details.get("endTime").getAsString().substring(0, 19)
                    .replace('T', ' '));
            Boolean roadClosed = details.get("roadClosed").getAsBoolean();

            String juncTravStr;
            try { juncTravStr = details.get("junctionTraversability").getAsString(); }
            catch (Exception e) { juncTravStr = ""; }
            JunctionTraversability juncTrav = JunctionTraversability.get(juncTravStr);

            String description = details.get("description").getAsJsonObject().get("value").getAsString();
            String summary = details.get("summary").getAsJsonObject().get("value").getAsString();

            IncidentItemV7 item = new IncidentItemV7(id, originalId, olr, startTime, endTime, roadClosed,
                    description, summary, juncTrav);
            incidents.add(item);
        }
    }
}
