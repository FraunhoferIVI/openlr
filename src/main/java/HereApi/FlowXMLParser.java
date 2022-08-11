package HereApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FlowXMLParser {

    private List<FlowItem> flowItems = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(FlowXMLParser.class);

    public List<FlowItem> getFlowItems() { return flowItems; }

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

            } catch (SAXException | IOException e) { logger.error(e.getMessage()); }
        } catch (ParserConfigurationException e) { logger.error(e.getMessage()); }
    }

    /**
     * Method to parse flowXML from HereApi.
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

            } catch (SAXException | IOException e) { logger.error(e.getMessage()); }
        } catch (ParserConfigurationException e) { logger.error(e.getMessage()); }
    }

    /**
     * Method to parse the given XML Document for traffic flow information.
     * Creates a FlowItem object and adds it to the list of traffic items.
     *
     * @param document XML Document
     */
    private void parseXML(Document document) {

        document.getDocumentElement().normalize();

        NodeList roadWays = document.getElementsByTagName("RW");

        for (int i = 0; i < roadWays.getLength(); i++)
        {
            Node roadWay = roadWays.item(i);
            NodeList childsRW = roadWay.getChildNodes();

            for (int j = 0; j < childsRW.getLength(); j++) {

                Node fIS = childsRW.item(j);
                if (!fIS.getNodeName().equals("FIS")) { continue; }

                String li = roadWay.getAttributes().getNamedItem("LI").getTextContent();

                NodeList flowItems = fIS.getChildNodes();

                for (int k = 0; k < flowItems.getLength(); k++) {
                    Node flowItem = flowItems.item(k);

                    if (!(flowItem.getNodeName().equals("FI"))) { continue; }

                    String id = null;
                    String pc = null;
                    String name = null;
                    double accuracy = 0;
                    double freeFlowSpeed = 0;
                    double jamFactor = 0;
                    double speedLimited = 0;
                    double speed = 0;

                    NodeList flowItemChildNodes = flowItem.getChildNodes();

                    // Get information from relevant child Nodes
                    for (int l = 0; l < flowItemChildNodes.getLength(); l++) {

                        Node flowItemChildNode = flowItemChildNodes.item(l);

                        if (flowItemChildNode.getNodeName().equals("TMC")) {

                            NamedNodeMap TMCAttributes = flowItemChildNode.getAttributes();

                            pc = TMCAttributes.getNamedItem("PC").getTextContent();
                            name = TMCAttributes.getNamedItem("DE").getTextContent();

                        }
                        if (flowItemChildNode.getNodeName().equals("CF")) {

                            NamedNodeMap CFAttributes = flowItemChildNode.getAttributes();

                            accuracy = Double.parseDouble(CFAttributes.getNamedItem("CN").getTextContent());
                            freeFlowSpeed = Double.parseDouble(CFAttributes.getNamedItem("FF").getTextContent());
                            jamFactor = Double.parseDouble(CFAttributes.getNamedItem("JF").getTextContent());
                            speedLimited = Double.parseDouble(CFAttributes.getNamedItem("SP").getTextContent());
                            speed = (speedLimited == -1) ? speedLimited :
                                    Double.parseDouble(CFAttributes.getNamedItem("SU").getTextContent());
                        }
                    }
                    if (pc != null) {
                        id = "LI_" + li + "_PC_" + pc;
                        flowItemToList(id, name, accuracy,
                                freeFlowSpeed, jamFactor, speedLimited, speed);
                    }
                }
            }
        }
    }

    /**
     * Generates flow item object and adds it to the list of flow items.
     */
    private void flowItemToList(String id, String name, double accuracy, double freeFlowSpeed,
                                double jamFactor, double speedLimited, double speed)
    {
        FlowItem flowItem = new FlowItem(id, name, accuracy,
                freeFlowSpeed, jamFactor, speedLimited, speed);

        this.flowItems.add(flowItem);
    }
}
