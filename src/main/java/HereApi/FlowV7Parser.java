package HereApi;

import com.google.gson.*;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FlowV7Parser {

    List<FlowItemV7> flowItems = new ArrayList<>();

    /**
     * Reads out the supplied json String for flow Information and stores it in a List of Flow-Items.
     * test run: 98 ms
     *
     * @param json flow Information got from the Here-Traffic Api (v7)
     */
    public void parse(String json)
    {
        System.out.println(System.currentTimeMillis());

        // cast Json String to Object to work with it
        JsonObject jsonObject = JsonParser.parseReader(new StringReader(json)).getAsJsonObject();

        LocalDateTime updated = LocalDateTime.parse(jsonObject.get("sourceUpdated").getAsString()
                .substring(0, 19));

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

            FlowItemV7 item = new FlowItemV7(name, olr, speed, speedUncapped, freeFlow, jamFactor,
                    confidence, traversability);
            flowItems.add(item);
        }
        System.out.println(System.currentTimeMillis());

        System.out.println(flowItems.get(10));
    }

    /**
     * test run: 495 ms (5 times slower)
     *
     * @param json the json resource as String
     */
    public void parseV2(String json)
    {
        System.out.println(System.currentTimeMillis());

        // cast Json String to Object
        JsonObject jsonObject = JsonParser.parseReader(new StringReader(json)).getAsJsonObject();

        String updated = jsonObject.get("sourceUpdated").getAsString();

        JsonArray results = jsonObject.get("results").getAsJsonArray();

        for (JsonElement flowElement : results)
        {
            JsonObject flowObject = flowElement.getAsJsonObject();
            JsonElement location = flowObject.get("location");
            JsonElement currentFlow = flowObject.get("currentFlow");

            FlowItemV7 flow = new Gson().fromJson(location, FlowItemV7.class);
            flow = new Gson().fromJson(currentFlow, FlowItemV7.class);

            flowItems.add(flow);
        }
        System.out.println(System.currentTimeMillis());

        System.out.println(flowItems.get(10));
    }

}
