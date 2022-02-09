package HereApi;

import DataBase.DatasourceConfig;
import Exceptions.InvalidBboxException;
import Exceptions.InvalidWGS84CoordinateException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

import static org.jooq.impl.DSL.*;
import static org.jooq.sources.tables.Kanten.KANTEN;

/**
 *
 *
 * @author Emily Kast
 *
 */

public class ApiRequest {

    //your API key
    private final String hereApikey = "OOnP0DKtdmJcVaRywCD3T-QR5QUVbwGRTaJRLgItXjs";
    private String answer;

    // Contains incident information for all requested bounding boxes
    private List<Incident> incidentList;
    // Contains affected lines for all requested bounding boxes
    private List<AffectedLine> affectedLinesList;
    private List<FlowItem> flowItems;

    private static final Logger logger = LoggerFactory.getLogger(ApiRequest.class);

    public ApiRequest() {
        this.incidentList = new ArrayList<>();
        this.affectedLinesList = new ArrayList<>();
        this.flowItems = new ArrayList<>();
    }

    // needed for SQL queries
    static DSLContext ctx;
    static {
        try {
            ctx = DSL.using(DatasourceConfig.getConnection(), SQLDialect.POSTGRES);
        } catch (SQLException e) {
            logger.error("{}", e.getMessage());
        }
    }

    /**
     * Query to check whether table exists in the database. Returns null if not available, otherwise schema.name.
     *
     * @param schema Name of the schema, in which the Table should be.
     * @param table  Name of the table to be checked.
     * @return Field to use in select query.
     */
    public static Field<?> to_regclass(String schema, String table) {

        String query = "to_regclass('" + schema + "." + table + "')";
        return DSL.field(query);
    }

    /**
     * Sets URL with given bbox.
     *
     * @param resource "incidents"/"flow"
     *
     * @throws MalformedURLException URL is in the wrong format or an unknown transmission protocol is specified.
     */
    private URL generateURL(String bbox, String resource) throws MalformedURLException {
        String baseUrl = "https://traffic.ls.hereapi.com";
        String version = "/traffic/6.3/";
        String format = ".xml";
        String apiKey = "?apiKey=" + hereApikey;
        URL url = new URL(baseUrl + version + resource + format + apiKey + bbox);

        logger.info("URL set to: {}", url);

        return url;
    }

    /**
     * Sends request to HERE API.
     * API returns xml, xml is converted to String.
     *
     * @param bboxString Coordinates for bbox given as String to use in Api Request URL.
     * @param resource "incidents"/"flow"
     *
     * @return HERE Api answer as String
     *
     * @throws IOException Signals a general input / output error
     */
    private String sendRequest(String bboxString, String resource) throws IOException {

        URL request = generateURL(bboxString, resource);
        HttpURLConnection con = (HttpURLConnection) request.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/xml");
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String readLine;
            while ((readLine = in .readLine()) != null) {
                response.append(readLine);
            } in .close();
            answer = response.toString();

        } else {
            logger.error("GET Request failed");
        }
        return answer;
    }

    /**
     * Generates current timestamp
     *
     * @return timestamp
     */
    @NotNull
    @Contract(" -> new")
    private Timestamp getTimeStamp() {

        Timestamp now = new Timestamp(System.currentTimeMillis());

        logger.info("Timestamp: {}", now);

        return now;
    }

    /**
     * Checks whether the specified bounding box is valid and the coordinates correspond to the wgs84 format.
     *
     * @param coordinatesArray Contains the WGS84 coordinates of the upper left and lower right corner of the bounding box
     * @throws InvalidBboxException            Invalid bounding box
     * @throws InvalidWGS84CoordinateException Coordinates out of WGS85 bounds
     */
    private void checkWGS84validity(double[] coordinatesArray) throws InvalidBboxException, InvalidWGS84CoordinateException {
        if (coordinatesArray.length != 4)
            throw new InvalidBboxException();

        for (int i = 0; i < 4; i++) {
            if (i == 0 || i == 2) {
                boolean validLat = (-180 <= coordinatesArray[i]) && (coordinatesArray[i] <= 180);
                if (!validLat)
                    throw new InvalidWGS84CoordinateException();
            }
            if (i == 1 || i == 3) {
                boolean validLon = (-90 <= coordinatesArray[i]) && (coordinatesArray[i] <= 90);
                if (!validLon)
                    throw new InvalidWGS84CoordinateException();
            }
        }
    }

    /**
     * Method to set bounding box size in terminal window.
     * Bounding box needs to be given in WGS84.
     * Example: 51.057,13.744;51.053,13.751
     */
    private BoundingBox setBoundingBox() throws InvalidBboxException, InvalidWGS84CoordinateException {
        Scanner scanner = new Scanner(System.in).useLocale(Locale.US);

        System.out.println("Enter the coordinates for the bounding box as follows (format WGS84):" +
                "\nUpper Left Lat,Upper Left Lon;Bottom Right Lat,Bottom Right Lon" +
                "\nExample: 53.60,9.85;53.50,10.13 (Hamburg)");
        String bboxString = scanner.next();

        logger.info("Bounding Box set to {}", bboxString);

        //get coordinates as double values
        Pattern pattern = Pattern.compile("[,;]");

        double[] coordinates = pattern.splitAsStream(bboxString)
                .mapToDouble(Double::parseDouble)
                .toArray();

        checkWGS84validity(coordinates);

        return new BoundingBox(coordinates[0], coordinates[1], coordinates[2], coordinates[3]);
    }

    /**
     * Checks the bounding box. For the Api Request, the request bounding box is limited to a
     * maximum of 2 degrees (https://developer.here.com/documentation/traffic/dev_guide/topics/limitations.html).
     * If the specified bounding box is too large, it is broken down into sufficiently small boxes.
     * For each bounding box an API request is made, the XML file is parsed, the OpenLR code is decoded (for incidents)
     * and the relevant information and affected lines are collected.
     *
     * @param bbox Bounding box
     * @param resource "incidents"/"flow"
     */
    private void gatherTrafficInfo(@NotNull BoundingBox bbox, String resource) {

        // Recursive bounding box query
        if ((bbox.width > 10) || (bbox.height > 10)) {

            double upperLeftLat = bbox.getUpperLeftLat();
            double upperLeftLon = bbox.getUpperLeftLon();
            double bottomRightLat = bbox.getBottomRightLat();
            double bottomRightLon = bbox.getBottomRightLon();
            double halfHeight = bbox.getHeight() / 2;
            double halfWidth = bbox.getWidth() / 2;

            // top left
            gatherTrafficInfo(new BoundingBox(upperLeftLat, upperLeftLon,
                    (upperLeftLat - (halfHeight)), (upperLeftLon + (halfWidth))), resource);
            // top right
            gatherTrafficInfo(new BoundingBox(upperLeftLat, (upperLeftLon + (halfWidth)),
                    (upperLeftLat - (halfHeight)), bottomRightLon), resource);
            // bottom left
            gatherTrafficInfo(new BoundingBox((upperLeftLat - (halfHeight)),
                    upperLeftLon, bottomRightLat, (upperLeftLon - (halfWidth))), resource);
            // bottom right
            gatherTrafficInfo(new BoundingBox((upperLeftLat - (halfHeight)),
                    (upperLeftLon + (halfWidth)), upperLeftLat, bottomRightLon), resource);
        } else {

            if (resource.equals("incidents")) {
                // Gets Here Api request answer
                try {
                    sendRequest(bbox.getBboxRequestString(), "incidents");
                } catch (IOException e) {
                    logger.error("{}", e.getMessage());
                }

                // Parse answer or file
                IncidentsXMLParser parser = new IncidentsXMLParser();
                parser.parseXMLFromApi(answer);
                // If you want to test out a file instead of the API
                // parser.parseXMlFromFile("");

                // Collect relevant data per incident and decoding location
                DataCollector collector = new DataCollector();
                try {
                    collector.collectIncidentInformation(parser.getTrafficItems());
                } catch (Exception e) {
                    logger.error("{}", e.getMessage());
                }

                // Collects incident data and affected lines for all requested bounding boxes
                this.incidentList.addAll(collector.getIncidents());
                this.affectedLinesList.addAll(collector.getAffectedLines());

            } else if (resource.equals("flow")) {
                // Gets Here Api request answer
                try {
                    sendRequest(bbox.getBboxRequestString(), "flow");
                } catch (IOException e) {
                    logger.error("{}", e.getMessage());
                }

                // Parse answer or file
                FlowXMLParser parser = new FlowXMLParser();
                parser.parseXMLFromApi(answer);
                // If you want to test out a file instead of the API
                // parser.parseXMlFromFile("");

                // Collect relevant flow data
                DataCollector collector = new DataCollector();

                collector.collectFlowInformation(parser.getFlowItems());

                // Collects incident data and affected lines for all requested bounding boxes
                flowItems.addAll(collector.getFlowItems());
            }
        }
    }

    /**
     * Method for updating the incident information contained in the database for a specified bounding box.
     * If the database does not yet contain an incident table with the corresponding foreign key table, this is
     * created and then filled. Generating temp tables containing the incident and foreign key data (first
     * transaction). Deleting the tables containing the old date and altering the created temp tables
     * (second transaction).
     *
     * @throws InvalidBboxException            Invalid bounding box
     * @throws InvalidWGS84CoordinateException Coordinates out of WGS85 bounds
     */
    public void updateIncidentData() throws InvalidBboxException, InvalidWGS84CoordinateException {

        logger.info("INCIDENTS:");

        // Get current timestamp
        Timestamp currentTimestamp = getTimeStamp();

        // Get recursive bounding boxes if bbox is bigger than 2 degrees
        gatherTrafficInfo(setBoundingBox(), "incidents");

        // Needed to set schema for creating tables
        Name temp_incidents = DSL.name("openlr", "temp_incidents");
        Name temp_kanten_incidents = DSL.name("openlr", "temp_kanten_incidents");
        Name incidents = DSL.name("openlr", "incidents");
        Name kanten_incidents = DSL.name("openlr", "kanten_incidents");
        Name incident_affected = DSL.name("openlr", "incident_affected");

        // Checks if "incidents" table already exists
        String incidentsTableExists = String.valueOf(ctx.select(to_regclass("openlr", "incidents"))
                .fetchOne().value1());

        // Checks if "temp_incidents" table already exists
        String tempIncidentsTableExists = String.valueOf(ctx.select(to_regclass("openlr", "temp_incidents"))
                .fetchOne().value1());

        // Timestamp is only created when table "incidents" exists
        Timestamp youngestEntry = "null".equals(incidentsTableExists) ? null :
                (Timestamp) ctx.select(min(field("generationdate"))).from(table(incidents)).fetchOne().value1();

        // Begin First Transaction - Fills temporary tables
        ctx.transaction(configuration1 -> {

            // Deleting temp_ tables if they exist, prevents program from running into "already exists"-Error
            if (tempIncidentsTableExists.equals("openlr.temp_incidents")) {
                ctx.dropTable(table(temp_kanten_incidents)).cascade().execute();
                ctx.dropTable(table(temp_incidents)).cascade().execute();

                logger.info("Dropped old temporary tables.");
            }

            // If the most recent entry in the incident table is younger than the time stamp when the program
            // was started, this message is printed.
            if (youngestEntry != null && currentTimestamp.before(youngestEntry)) {

                logger.info("The incident data is up to date, the data has not been updated.");

            } else {
                // Create temporary incident table
                DSL.using(configuration1).createTable(temp_incidents)
                        .column("incident_id", SQLDataType.CHAR(64).nullable(false))
                        .column("incident_type", SQLDataType.CHAR(50))
                        .column("status", SQLDataType.CHAR(50))
                        .column("start_date", SQLDataType.TIMESTAMP)
                        .column("end_date", SQLDataType.TIMESTAMP)
                        .column("criticality", SQLDataType.CHAR(10).defaultValue("lowImpact"))
                        .column("openlrcode", SQLDataType.CHAR(255))
                        .column("shortdesc", SQLDataType.CLOB)
                        .column("longdesc", SQLDataType.CLOB)
                        .column("roadclosure", SQLDataType.BOOLEAN)
                        .column("posoff", SQLDataType.INTEGER)
                        .column("negoff", SQLDataType.INTEGER)
                        .column("generationdate", SQLDataType.TIMESTAMP.defaultValue(field("now()", SQLDataType.TIMESTAMP)))
                        .constraints(
                                primaryKey("incident_id"))
                        .execute();

                // Create temporary foreign key table
                DSL.using(configuration1)
                        .createTable(temp_kanten_incidents)
                        .column("incident_id", SQLDataType.CHAR(64).nullable(false))
                        .column("line_id", SQLDataType.INTEGER.nullable(false))
                        .column("posoff", SQLDataType.INTEGER.defaultValue(0))
                        .column("negoff", SQLDataType.INTEGER.defaultValue(0))
                        .execute();

                // Fill temp incidents table
                for (Incident incident : this.incidentList) {

                    DSL.using(configuration1)
                            .insertInto(table(temp_incidents),
                                    field(name("incident_id")), field(name("incident_type")),
                                    field(name("status")), field(name("start_date")),
                                    field(name("end_date")), field(name("criticality")),
                                    field(name("openlrcode")), field(name("shortdesc")),
                                    field(name("longdesc")), field(name("roadclosure")),
                                    field(name("posoff")), field(name("negoff")))
                            .values(incident.getIncidentId(), incident.getType(), incident.getStatus(), incident.getStart(),
                                    incident.getEnd(), incident.getCriticality(), incident.getOpenLRCode(),
                                    incident.getShortDesc(), incident.getLongDesc(), incident.getRoadClosure(),
                                    incident.getPosOff(), incident.getNegOff())
                            .execute();
                }

                // Fill temp foreign key table
                for (AffectedLine affectedLine : this.affectedLinesList) {
                    DSL.using(configuration1)
                            .insertInto(table(temp_kanten_incidents),
                                    field(name("line_id")), field(name("incident_id")),
                                    field(name("posoff")), field(name("negoff")))
                            .values(affectedLine.getLineId(), affectedLine.getIncidentId(),
                                    affectedLine.getPosOff(), affectedLine.getNegOff())
                            .execute();
                }
            }
            logger.info("Created temporary tables.");

        }); // End first transaction

        //If the most recent entry in the incident table is younger than the time stamp when the program was started,
        // the data will not be updated.
        if (youngestEntry != null && currentTimestamp.before(youngestEntry)) {
            return;
        }

        // Begin Second Transaction
        ctx.transaction(configuration2 -> {

            // Drop tables with old data if exists
            if (incidentsTableExists.equals("openlr.incidents")) {

                ctx.dropTable(table(kanten_incidents)).cascade().execute();
                ctx.dropTable(table(incidents)).cascade().execute();

                logger.info("Dropped old tables.");
            }

            // Rename temp tables and add foreign keys
            ctx.alterTable(temp_incidents).renameTo(incidents).execute();
            ctx.alterTable(temp_kanten_incidents).renameTo(kanten_incidents).execute();

            ctx.alterTable(kanten_incidents).add(constraint("fk_kanten")
                    .foreignKey("line_id").references(KANTEN)).execute();
            ctx.alterTable(kanten_incidents).add(constraint("fk_incidents")
                    .foreignKey("incident_id").references(incidents)).execute();

        }); // End second transaction

        // Checks if incident_affected table already exists
        String affectedExists = String.valueOf(ctx.select(to_regclass("openlr", "incident_affected"))
                .fetchOne().value1());

        if (affectedExists.equals("openlr.incident_affected")) {
            ctx.dropTable(table(incident_affected)).cascade().execute();
        }

        // Create QGIS view containing incident_affected lines
        ctx.execute("CREATE TABLE openlr.incident_affected AS select k.line_id , k.name, i.incident_id , i.incident_type , i.criticality ," +
                "i.roadclosure , i.start_date , i.end_date , i.shortdesc , i.longdesc ," +
                "k.geom from openlr.kanten_incidents ki " +
                "join openlr.incidents i on (ki.incident_id = i.incident_id)" +
                "join openlr.kanten k on (ki.line_id = k.line_id);");

        logger.info("Updated incident data.");
    }

    /**
     * Method for updating the flow information contained in the database for a specified bounding box.
     * If the database does not yet contain an flow table with the corresponding foreign key table, this is
     * created and then filled. Generating temp tables containing the flow and foreign key data (first
     * transaction). Deleting the tables containing the old date and altering the created temp tables
     * (second transaction).
     *
     * @throws InvalidBboxException            Invalid bounding box
     * @throws InvalidWGS84CoordinateException Coordinates out of WGS85 bounds
     */
    public void updateFlowData() throws InvalidBboxException, InvalidWGS84CoordinateException {

        logger.info("FLOW:");

        // Get current timestamp
        Timestamp currentTimestamp = getTimeStamp();

        // Get recursive bounding boxes if bbox is bigger than 2 degrees
        gatherTrafficInfo(setBoundingBox(), "flow");

        // Needed to set schema for creating tables
        Name temp_flow = DSL.name("openlr", "temp_flow");
        Name flow = DSL.name("openlr", "flow");

        // Checks if "flow" table already exists
        String flowTableExists = String.valueOf(ctx.select(to_regclass("openlr", "flow"))
                .fetchOne().value1());

        // Checks if "temp_flow" table already exists
        String tempflowTableExists = String.valueOf(ctx.select(to_regclass("openlr", "temp_flow"))
                .fetchOne().value1());

        // Timestamp is only created when table "flow" exists
        Timestamp youngestEntry = "null".equals(flowTableExists) ? null :
                (Timestamp) ctx.select(min(field("generationdate"))).from(table(flow)).fetchOne().value1();

        // Begin First Transaction - Fills temporary tables
        ctx.transaction(configuration1 -> {

            // Deleting temp_ table if it exists, prevents program from running into "already exists"-Error
            if (tempflowTableExists.equals("openlr.temp_flow")) {
                ctx.dropTable(table(temp_flow)).cascade().execute();

                logger.info("Dropped old temporary table.");
            }

            // If the most recent entry in the flow table is younger than the time stamp when the program
            // was started, this message is printed.
            if (youngestEntry != null && currentTimestamp.before(youngestEntry)) {

                logger.info("The flow data is up to date, the data has not been updated.");

            } else {
                // Create temporary flow table
                DSL.using(configuration1).createTable(temp_flow)
                        .column("id", SQLDataType.CHAR(50))
                        .column("road_name", SQLDataType.CHAR(50))
                        .column("accuracy", SQLDataType.DOUBLE)
                        .column("free_flow_speed", SQLDataType.DOUBLE)
                        .column("jam_factor", SQLDataType.DOUBLE)
                        .column("speed_limited", SQLDataType.DOUBLE)
                        .column("speed", SQLDataType.DOUBLE)
                        .column("generationdate", SQLDataType.TIMESTAMP.defaultValue(field("now()", SQLDataType.TIMESTAMP)))
                        .constraints(
                                primaryKey("id"))
                        .execute();

                // Fill temp flow table
                for (FlowItem flowItem : flowItems) {

                    DSL.using(configuration1)
                            .insertInto(table(temp_flow),
                                    field(name("id")),
                                    field(name("road_name")), field(name("accuracy")),
                                    field(name("free_flow_speed")), field(name("jam_factor")),
                                    field(name("speed_limited")), field(name("speed")))
                            .values(flowItem.getId(),flowItem.getName(), flowItem.getAccuracy(), flowItem.getFreeFlowSpeed(),
                                    flowItem.getJamFactor(), flowItem.getSpeedLimited(), flowItem.getSpeed())
                            .execute();
                }
            }
            logger.info("Created temporary table.");

        }); // End first transaction

        //If the most recent entry in the incident table is younger than the time stamp when the program was started,
        // the data will not be updated.
        if (youngestEntry != null && currentTimestamp.before(youngestEntry)) {
            return;
        }

        // Begin Second Transaction
        ctx.transaction(configuration2 -> {

            // Drop table with old data if exists
            if (flowTableExists.equals("openlr.flow"))
            {
                ctx.dropTable(table(flow)).cascade().execute();

                logger.info("Dropped old table.");
            }
            // Rename temp table
            ctx.alterTable(temp_flow).renameTo(flow).execute();

        }); // End second transaction
    }
}
