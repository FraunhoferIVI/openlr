package HereApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class IncidentsXMLParser {

    private List<TrafficItem> trafficItems;

    private static final Logger logger = LoggerFactory.getLogger(IncidentsXMLParser.class);

    public IncidentsXMLParser() {
        this.trafficItems = new ArrayList<>();
    }

    public List<TrafficItem> getTrafficItems() {
        return trafficItems;
    }

    /**
     * Method to parse incidents XML form given file path.
     *
     * @param path Filepath of the XML file as String
     */
    public void parseXMlFromFile(String path) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            try {
                Document document = builder.parse(new File(path));

                parseXML(document);

            } catch (SAXException | IOException e) { logger.warn(e.getMessage()); }
        } catch (ParserConfigurationException e) { logger.warn(e.getMessage()); }
    }

    /**
     * Method to parse incidentsXML from HereApi.
     *
     * @param requestAnswer Request answer as String
     */
    public void parseXMLFromApi(String requestAnswer) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            try {
                Document document = builder.parse(new InputSource(new StringReader(requestAnswer)));
                parseXML(document);

            } catch (SAXException | IOException e) { logger.warn(e.getMessage()); }
        } catch (ParserConfigurationException e) { logger.warn(e.getMessage()); }
    }

    /**
     * Method to parse the given XML Document for incident information.
     * Runs through each traffic item node and checks for OpenLR code,
     * if available relevant information is extracted.
     * Creates a traffic item object and adds it to the list of traffic items.
     */
    private void parseXML(Document document) {

        document.getDocumentElement().normalize();

        NodeList trafficItems = document.getElementsByTagName("TRAFFIC_ITEM");

        for (int node = 0; node < trafficItems.getLength(); node++) {

            boolean hasOpenLRCode = false;
            String id = null;
            String status = null;
            String type = null;
            String start = null;
            String end = null;
            String openLR = null;
            String closure = null;
            String shortDesc = null;
            String longDesc = null;
            String criticality = null;

            Node trafficItem = trafficItems.item(node);

            NodeList trafficItemChildNodes = trafficItem.getChildNodes();

            // Get Information from relevant child Nodes
            for (int i = 0; i < trafficItemChildNodes.getLength(); i++) {

                Node trafficItemChildNode = trafficItemChildNodes.item(i);

                if (trafficItemChildNode.getNodeName().equals("CRITICALITY")) {

                    if (trafficItemChildNode.getNodeType() == Node.ELEMENT_NODE) {
                        // Cast TRAFFIC_ITEM_DETAIL node to element
                        Element criticalityElement = (Element) trafficItemChildNode;
                        //Get information if road is closed
                        criticality = criticalityElement.getElementsByTagName("DESCRIPTION").item(0).getTextContent();
                    }
                }
                if (trafficItemChildNode.getNodeName().equals("LOCATION")) {

                    NodeList locationChildNodes = trafficItemChildNode.getChildNodes();

                    for (int j = 0; j < locationChildNodes.getLength(); j++) {

                        Node locationChildNode = locationChildNodes.item(j);

                        //check for OpenLR code tag, if not available, no traffic item object will be created
                        if (locationChildNode.getNodeName().equals("TPEGOpenLRBase64") &&
                                locationChildNode.getTextContent() != null) {
                            hasOpenLRCode = true;

                            openLR = locationChildNode.getTextContent().replaceAll("[\n ]", "");

                            if (trafficItem.getNodeType() == Node.ELEMENT_NODE) {
                                // Cast Node to Element to get elements by tag name
                                Element trafficItemElement = (Element) trafficItem;
                                //Reading out information from trafficItem Node
                                id = trafficItemElement.getElementsByTagName("TRAFFIC_ITEM_ID").item(0).getTextContent();
                                status = trafficItemElement.getElementsByTagName("TRAFFIC_ITEM_STATUS_SHORT_DESC").item(0).getTextContent();
                                type = trafficItemElement.getElementsByTagName("TRAFFIC_ITEM_TYPE_DESC").item(0).getTextContent();
                                start = trafficItemElement.getElementsByTagName("START_TIME").item(0).getTextContent();
                                end = trafficItemElement.getElementsByTagName("END_TIME").item(0).getTextContent();
                                // get different descriptions, same node name, different types
                                NodeList trafficItemDescriptions = trafficItemElement.getElementsByTagName("TRAFFIC_ITEM_DESCRIPTION");
                                shortDesc = trafficItemDescriptions.item(0).getTextContent().replaceAll("\n", "");
                                longDesc = trafficItemDescriptions.item(1).getTextContent().replaceAll("\n", "");
                            }
                        }
                    }
                }
                // If OpenLR Code is available get node TRAFFIC_ITEM_DETAIL
                if (hasOpenLRCode) {
                    if (trafficItemChildNode.getNodeName().equals("TRAFFIC_ITEM_DETAIL")) {

                        if (trafficItemChildNode.getNodeType() == Node.ELEMENT_NODE) {
                            // Cast TRAFFIC_ITEM_DETAIL node to element
                            Element trafficItemDetailElement = (Element) trafficItemChildNode;
                            //Get information if road is closed
                            closure = trafficItemDetailElement.getElementsByTagName("ROAD_CLOSED").item(0).getTextContent();

                        }
                    }
                }
            }

            if (id != null)
                // Generate traffic item object and add to list of traffic items
                IncidentTrafficItemToList(id, status, type, start, end, criticality,
                        openLR, closure, shortDesc, longDesc);
        }
    }

    /**
     * Generates traffic item object and adds it to the list of traffic items.
     *
     * @param id          Traffic item id
     * @param status      Status of the traffic item
     * @param type        Type of traffic item
     * @param start       Start time of the traffic item
     * @param end         End time of the traffic item
     * @param criticality Severity of the accidents
     * @param openLR      OpenLR Code of the traffic item
     * @param closure     Information whether the street is closed
     * @param shortDesc   Brief description of the traffic item
     * @param longDesc    Detailed description of the traffic item
     */
    private void IncidentTrafficItemToList(String id, String status, String type, String start, String end, String criticality,
                                           String openLR, String closure, String shortDesc, String longDesc) {
        // generate traffic Item
        TrafficItem trafficItem = new TrafficItem(id, status, type, start, end, criticality, openLR, closure, shortDesc, longDesc);

        // add TrafficItem to list of traffic items
        this.trafficItems.add(trafficItem);
    }
}
