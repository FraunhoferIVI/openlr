package HereApi;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class FlowJsonParserTest {

    @Test
    public void testParse()
    {
        // todo gesamtzahl Items testen
        // Parse XML and get data
        FlowJsonParser parser = new FlowJsonParser();
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = "example_flow_v7.json";
        String pth = classLoader.getResource(fileName).getPath();
        String pathStr = pth.substring(1).replaceAll("/", "\\\\");
        Path path = Path.of(pathStr);
        String json = null;
        try { json = Files.readString(path); }
        catch (IOException e) { fail(); }
        parser.parse(json);

        List<FlowItemV7> flowItems = parser.getFlowItems();
        assertNotNull(flowItems);
        FlowItemV7 flowItem = flowItems.get(0);

        assertEquals("Wandsbeker Marktstraße", flowItem.getName());
        assertEquals("CCgBEAAkIwcoSSYYYQAJBQQBA0YACgQDAWYAAJb/9QAJBQQBA7oAMAAX", flowItem.getOlr());
        assertEquals(5.277778, flowItem.getSpeed(), 0);
        assertEquals(5.277778, flowItem.getSpeedUncapped(), 0);
        assertEquals(9.444445, flowItem.getFreeFlow(), 0);
        assertEquals(3.1, flowItem.getJamFactor(), 0);
        assertEquals(0.93, flowItem.getConfidence(), 0);
        assertEquals("open", flowItem.getTraversability());
        assertEquals(JunctionTraversability.ALL_OPEN, flowItem.getJunctionTraversability());
    }
}
