package HereApi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class IncidentsV7Parser {

    List<IncidentItemV7> incidents = new ArrayList<>();

    public void parse(String json)
    {
        // cast Json String to Object
        JsonObject jsonObject = JsonParser.parseReader(new StringReader(json)).getAsJsonObject();

        LocalDateTime updated = LocalDateTime.parse(jsonObject.get("sourceUpdated").getAsString()
                .substring(0, 19));

        JsonArray results = jsonObject.get("results").getAsJsonArray();

        for (JsonElement incidentElement : results)
        {
            JsonObject incident = incidentElement.getAsJsonObject();
            JsonObject location = incident.get("location").getAsJsonObject();

            String olr = location.get("olr").getAsString();

            JsonObject details = incident.get("incidentDetails").getAsJsonObject();

            String id = details.get("id").getAsString();
            LocalDateTime startTime = LocalDateTime.parse(details.get("startTime").getAsString()
                    .substring(0, 19));
            LocalDateTime endTime = LocalDateTime.parse(details.get("endTime").getAsString()
                    .substring(0, 19));
            Boolean roadClosed = details.get("roadClosed").getAsBoolean();

            String description = details.get("description").getAsJsonObject().get("value").getAsString();
            String summary = details.get("summary").getAsJsonObject().get("value").getAsString();

            IncidentItemV7 incidentItemV7 = new IncidentItemV7(id, olr, startTime, endTime, roadClosed,
                    description, summary);
            incidents.add(incidentItemV7);
        }
        System.out.println(incidents.get(11));
    }
}
