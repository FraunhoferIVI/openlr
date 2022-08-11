package HereApi;

import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

public class IncidentsXMLParserTest {

    @Test
    public void testParseXMLFromFile()
    {
        // Parse XML and get data
        List<TrafficItem> trafficItems;
        IncidentsXMLParser parser = new IncidentsXMLParser();
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = "example_incidents.xml";
        String path = classLoader.getResource(fileName).getPath();
        assertNotNull(path);
        parser.parseXMlFromFile(path);

        trafficItems = parser.getTrafficItems();
        TrafficItem trafficItem = trafficItems.get(0);

        // tests
        assertNotNull(trafficItems);
        assertEquals(248 ,trafficItems.size());

        assertEquals("236357998740183041", trafficItem.getId());
        assertEquals("Zwischen Großer Grasbrook und Shanghaibrücke - Bauarbeiten auf Abschnitt.",
                trafficItem.getShortDesc());
        assertEquals("Zwischen Großer Grasbrook und Shanghaibrücke - Bauarbeiten auf Abschnitt.",
                trafficItem.getLongDesc());
        assertEquals("11/05/2021 10:28:12", trafficItem.getStart());
        assertEquals("02/15/2022 22:59:00", trafficItem.getEnd());
        assertEquals("false", trafficItem.getClosure());
        assertEquals("minor", trafficItem.getCriticality());
        assertEquals("CCkBEAAlJAcbOyYTPwAJBQQDAjoACgUEA4QlAAMCALMACQUEAgKuADAAAA==", trafficItem.getOpenLR());
        assertEquals("ACTIVE", trafficItem.getStatus());
        assertEquals("CONSTRUCTION", trafficItem.getType());

    }
}
