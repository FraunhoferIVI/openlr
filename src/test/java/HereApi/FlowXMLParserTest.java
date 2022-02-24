package HereApi;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.*;

public class FlowXMLParserTest {

    @Test
    public void testParseXMLFromFile()
    {
        // Parse XML and get data
        List<FlowItem> flowItems;
        FlowXMLParser parser = new FlowXMLParser();
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = "example_flow.xml";
        String path = classLoader.getResource(fileName).getPath();
        assertNotNull(path);
        parser.parseXMlFromFile(path);

        flowItems = parser.getFlowItems();
        FlowItem flowItem = flowItems.get(0);
        // tests
        assertNotNull(flowItems);
        assertEquals(2054 ,flowItems.size());

        assertEquals("LI_D01-06516_PC_6520", flowItem.getId());
        assertEquals(0.97d, flowItem.getAccuracy(),0.01);
        assertEquals(30.0d, flowItem.getFreeFlowSpeed(),0.01);
        assertEquals(2.49685d, flowItem.getJamFactor(),0.01);
        assertEquals(18.95d, flowItem.getSpeedLimited(),0.01);
        assertEquals(18.95d, flowItem.getSpeed(),0.01);
    }
}
