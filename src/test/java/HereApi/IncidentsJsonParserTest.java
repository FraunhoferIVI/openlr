package HereApi;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

public class IncidentsJsonParserTest {

    @Test
    public void testParse()
    {
        // todo gesamtzahl Items testen
        // Parse XML and get data
        IncidentsJsonParser parser = new IncidentsJsonParser();
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = "example_incidents_v7.json";
        String pth = classLoader.getResource(fileName).getPath();
        String pathStr = pth.substring(1).replaceAll("/", "\\\\");
        Path path = Path.of(pathStr);
        String json = null;
        try { json = Files.readString(path); }
        catch (IOException e) { fail(); }
        parser.parse(json);

        List<IncidentItemV7> incidentItems = parser.getIncidentItems();
        assertNotNull(incidentItems);
        IncidentItemV7 incidentItem = incidentItems.get(0);

        assertEquals("3712282026996998623", incidentItem.getId());
        assertEquals("4454517718886483395", incidentItem.getOriginalId());
        assertEquals("CCgBEAAkIwcPAyYUTAAJBQQEAzUACgQDBFcAAH4AFgAJBQQEA7UAMAAA", incidentItem.getOlr());
        assertEquals(LocalDateTime.parse("2022-02-15T18:16:18"), incidentItem.getStartTime());
        assertEquals(LocalDateTime.parse("2022-03-03T06:16:18"), incidentItem.getEndTime());
        assertEquals(true, incidentItem.isRoadClosed());
        assertEquals("Geschlossen zwischen Boninstraße und Rothestraße - Gesperrt.", incidentItem.getDescription());
        assertEquals("Geschlossen zwischen Boninstraße und Rothestraße - Gesperrt.", incidentItem.getSummary());
        assertEquals(JunctionTraversability.INTERMEDIATE_CLOSED_EDGE_OPEN, incidentItem.getJunctionTraversability());
    }
}
