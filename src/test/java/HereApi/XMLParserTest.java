package HereApi;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class XMLParserTest {

    @Test
    public void testParseFlowXML()
    {
        // Datei von Ressouren Ordner lesen
        // Parsen mit XMLParser
        // Liste von FlowItems zurückgeben
        List<FlowItem> flowItemList = new ArrayList<>();
        assertNotNull(flowItemList);
        assertEquals(20 ,flowItemList.size());

        FlowItem flowItem = flowItemList.get(0);
        assertEquals(0.99d, flowItem.getAccuracy());
    }

    // TODO analoger Test für Incidents (auch in separater Klasse)
}
