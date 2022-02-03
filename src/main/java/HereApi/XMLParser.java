package HereApi;

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

/**
 *
 */

public class XMLParser {

    // List contains the traffic elements in the XML file
    private List<TrafficItem> listIncidentTrafficItems;
    private List<FlowItem> listFlowItems;

    public XMLParser() {
        this.listIncidentTrafficItems = new ArrayList<>();
        this.listFlowItems = new ArrayList<>();
    }

    public List<TrafficItem> getListIncidentTrafficItems() { return listIncidentTrafficItems; }

    public List<FlowItem> getListFlowItems() {
        return listFlowItems;
    }

    /**
     * Method to parse XML form given file path.
     * @param path Filepath of the XML file as String
     * @param resource "incidents"/"flow"
     */
    public void parseXMlFromFile(String path, String resource) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            try {
                Document document = builder.parse(new File(path));
                if (resource.equals("incidents"))
                    parseIncidentXML(document);
                else
                    parseFlowXML(document);

            } catch (SAXException | IOException e) {
                e.printStackTrace();
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to parse answer from HereApi request.
     * @param requestAnswer Request answer as String
     * @param resource "incidents"/"flow"
     */
    public void parseXMLFromApi(String requestAnswer, String resource) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            try {
                Document document = builder.parse(new InputSource(new StringReader(requestAnswer)));

                if (resource.equals("incidents"))
                    parseIncidentXML(document);
                else
                    parseFlowXML(document);

            } catch (SAXException | IOException e) { e.printStackTrace(); }
        } catch (ParserConfigurationException e) { e.printStackTrace(); }
    }

    /**
     * Method to parse the given XML Document.
     * Runs through each traffic item node and checks for OpenLR code, if available relevant information are
     * extracted. Creates a traffic item object and adds it to the list of traffic items.
     */
    private void parseIncidentXML(Document document) {
        // Normalize xml document
        document.getDocumentElement().normalize();

        // List containing TRAFFIC_ITEM nodes
        NodeList trfItemList = document.getElementsByTagName("TRAFFIC_ITEM");

        // Run through traffic item node
        for (int node = 0; node < trfItemList.getLength(); node++) {

            boolean hasOpenLRCode = false;
            String tIId = null;
            String tIStatus = null;
            String tIType = null;
            String tIStart = null;
            String tIEnd = null;
            String tIOpenLR = null;
            String tIClosure = null;
            String tIShortDesc = null;
            String tILongDesc = null;
            String tICriticality = null;

            Node trfItem = trfItemList.item(node);

            // Get children from specific traffic item (given by trfItemList.item(node)
            NodeList trfItemChildNodesList = trfItem.getChildNodes();

            //Find child node "LOCATION" to check for OpenLR Code
            for (int i = 0; i < trfItemChildNodesList.getLength(); i++) {

                if (trfItemChildNodesList.item(i).getNodeName().equals("CRITICALITY")) {
                    // Create Node criticality
                    Node criticalityNode = trfItemChildNodesList.item(i);
                    // Get criticality
                    if (criticalityNode.getNodeType() == Node.ELEMENT_NODE) {
                        // Cast TRAFFIC_ITEM_DETAIL node to element
                        Element criticalityElement = (Element) criticalityNode;
                        //Get information if road is closed
                        tICriticality = criticalityElement.getElementsByTagName("DESCRIPTION").item(0).getTextContent();
                    }
                }
                if (trfItemChildNodesList.item(i).getNodeName().equals("LOCATION")) {
                    // Create node location
                    Node locationNode = trfItemChildNodesList.item(i);
                    // Create list containing all child nodes of location
                    NodeList locationChildNodesList = locationNode.getChildNodes();
                    // Find child node "TPEGOpenLRBase64"
                    for (int j = 0; j < locationChildNodesList.getLength(); j++) {
                        //check for OpenLR code tag, if not available, no traffic item object will be created
                        if (locationChildNodesList.item(j).getNodeName().equals("TPEGOpenLRBase64") && locationChildNodesList.item(j).getTextContent() != null) {
                            hasOpenLRCode = true;
                            // Get OpenLR Code
                            tIOpenLR = locationChildNodesList.item(j).getTextContent().replaceAll("[\n ]", "");
                            // get information from different nodes
                            if (trfItem.getNodeType() == Node.ELEMENT_NODE) {
                                // Cast Node to Element to get elements by tag name
                                Element trfItemElement = (Element) trfItem;
                                //Reading out information from trafficItem Node
                                tIId = trfItemElement.getElementsByTagName("TRAFFIC_ITEM_ID").item(0).getTextContent();
                                tIStatus = trfItemElement.getElementsByTagName("TRAFFIC_ITEM_STATUS_SHORT_DESC").item(0).getTextContent();
                                tIType = trfItemElement.getElementsByTagName("TRAFFIC_ITEM_TYPE_DESC").item(0).getTextContent();
                                tIStart = trfItemElement.getElementsByTagName("START_TIME").item(0).getTextContent();
                                tIEnd = trfItemElement.getElementsByTagName("END_TIME").item(0).getTextContent();
                                // get different descriptions, same node name, different types
                                NodeList trfItemDescList = trfItemElement.getElementsByTagName("TRAFFIC_ITEM_DESCRIPTION");
                                tIShortDesc = trfItemDescList.item(0).getTextContent().replaceAll("\n", "");
                                tILongDesc = trfItemDescList.item(1).getTextContent().replaceAll("\n", "");
                            }
                        }
                    }
                }
                // If OpenLR Code is available get node TRAFFIC_ITEM_DETAIL
                if (hasOpenLRCode) {
                    if (trfItemChildNodesList.item(i).getNodeName().equals("TRAFFIC_ITEM_DETAIL")) {
                        Node trfItemDetailNode = trfItemChildNodesList.item(i);
                        if (trfItemDetailNode.getNodeType() == Node.ELEMENT_NODE) {
                            // Cast TRAFFIC_ITEM_DETAIL node to element
                            Element trfItemDetailElement = (Element) trfItemDetailNode;
                            //Get information if road is closed
                            tIClosure = trfItemDetailElement.getElementsByTagName("ROAD_CLOSED").item(0).getTextContent();

                        }
                    }
                }
            }

            if (tIId != null)
                // Generate traffic item object and add to list of traffic items
                IncidentTrafficItemToList(tIId, tIStatus, tIType, tIStart, tIEnd, tICriticality,
                        tIOpenLR, tIClosure, tIShortDesc, tILongDesc);
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
        this.listIncidentTrafficItems.add(trafficItem);
    }

    /**
     * Method to parse the given XML Document for traffic flow information.
     * Creates a FlowItem object and adds it to the list of traffic items.
     *
     * @param document XML Document
     */
    private void parseFlowXML(Document document) {
        // Normalize xml document
        document.getDocumentElement().normalize();

        NodeList flowItems = document.getElementsByTagName("FIS");

        // Go through flow item Nodes
        for (int node = 0; node < flowItems.getLength(); node++) {

            String fiName = null;
            double fiAccuracy = 0;
            double fiFreeFlowSpeed = 0;
            double fiJamFactor = 0;
            double fiSpeedLimited = 0;
            double fiSpeed = 0;

            Node flowItem = flowItems.item(node);

            NodeList fiChildNodes = flowItem.getChildNodes();

            // Get info from relevant child Nodes
            for (int i = 0; i < fiChildNodes.getLength(); i++) {

                Node fiChildNode = fiChildNodes.item(i);

                if (fiChildNode.getNodeName().equals("TMC")) {

                    NamedNodeMap TMCAttributes = fiChildNode.getAttributes();

                    fiName = TMCAttributes.getNamedItem("DE").getTextContent();

                }
                if (fiChildNode.getNodeName().equals("CF")) {

                    NamedNodeMap CFAttributes = fiChildNode.getAttributes();

                    fiAccuracy = Double.parseDouble(CFAttributes.getNamedItem("CN").getTextContent());
                    fiFreeFlowSpeed = Double.parseDouble(CFAttributes.getNamedItem("FF").getTextContent());
                    fiJamFactor = Double.parseDouble(CFAttributes.getNamedItem("JF").getTextContent());
                    fiSpeedLimited = Double.parseDouble(CFAttributes.getNamedItem("SP").getTextContent());
                    fiSpeed = Double.parseDouble(CFAttributes.getNamedItem("SU").getTextContent());

                }
            }
            if (fiName != null) {
                flowItemToList(fiName, fiAccuracy,
                        fiFreeFlowSpeed, fiJamFactor, fiSpeedLimited, fiSpeed);
            }
        }
    }

    /**
     * Generates flow item object and adds it to the list of flow items.
     */
    private void flowItemToList(String name, double accuracy, double freeFlowSpeed,
                                double jamFactor, double speedLimited, double speed)
    {
        FlowItem flowItem = new FlowItem(name, accuracy,
                freeFlowSpeed, jamFactor, speedLimited, speed);

        this.listFlowItems.add(flowItem);
    }
}
