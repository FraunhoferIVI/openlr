package HereApi;

import DataBase.DatasourceConfig;
import Decoder.HereDecoder;
import Exceptions.InvalidBboxException;
import Exceptions.InvalidWGS84CoordinateException;
import com.here.account.auth.provider.FromDefaultHereCredentialsPropertiesFile;
import com.here.account.oauth2.HereAccessTokenProvider;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.sources.tables.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import Loader.RoutableOSMMapLoader;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Pattern;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.jooq.impl.DSL.*;
import static org.jooq.sources.tables.Kanten.KANTEN;

public class HereTrafficV7
{
    // token needed for requesting the data
    private String token;
    // bounding box (divided in smaller pieces, if to big for a single request)
    private List<Double[]> boundingBoxes = new ArrayList<>();
    // flow data
    private Timestamp flowUpdated;
    private List<FlowItemV7> flowItems = new ArrayList<>();
    private List<AffectedLine> flowAffectedLines = new ArrayList<>();
    // incident data
    private Timestamp incidentsUpdated;
    private List<IncidentItemV7> incidentItems = new ArrayList<>();
    private List<AffectedLine> incidentAffectedLines = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(ApiRequest.class);

    // needed for SQL queries
    static DSLContext ctx;
    static {
        try {
            ctx = DSL.using(DatasourceConfig.getConnection(), SQLDialect.POSTGRES);
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Enter the desired Bounding Box.
     */
    public void insertBbox()
    {
        Scanner scanner = new Scanner(System.in).useLocale(Locale.US);

        System.out.println("Enter the coordinates for the bounding box as follows: \n" +
                "SW Long,SW Lat,NE Long,NE Lat (counterclockwise)");
        String bbox = scanner.next();

        setBbox(bbox);

        logger.info("Bounding Box set to {}", bbox);
    }

    /**
     * @param bbox format: SW Long,SW Lat,NE Long,NE Lat (counterclockwise)
     *  Hamburg: 9.850,53.500,10.130,53.600
     *  Dresden: 13.600,50.900,13.800,51.100
     */
    public void setBbox(String bbox)
    {
        // get coordinates as double values
        Pattern pattern = Pattern.compile("[,]");
        double[] coordinates = pattern.splitAsStream(bbox)
                .mapToDouble(Double::parseDouble)
                .toArray();
        try { checkCoordinates(coordinates); }
        catch (Exception e) { logger.error(e.getMessage()); }
        // set boundingBoxes
        quadTreeDissect(coordinates, 1);
    }

    /**
     * Checks whether the given coordinates are in the correct format (SW Long, SW Lat, NE Long, NE Lat).
     *
     * @param coordinates Contains the WGS84 coordinates of the upper left and lower right corner of the bounding box
     * @throws InvalidBboxException            Invalid bounding box
     * @throws InvalidWGS84CoordinateException Coordinates out of WGS85 bounds
     */
    private void checkCoordinates(double[] coordinates) throws InvalidBboxException, InvalidWGS84CoordinateException {
        if (coordinates.length != 4)
            throw new InvalidBboxException();

        for (int i = 0; i < 4; i++) {

            double coordinate = coordinates[i];

            // check, that coordinates are in the right order
            if (i > 1 && coordinate <= coordinates[i - 2])
                    throw new InvalidWGS84CoordinateException();
            // ... and the numeric boundaries
            if (i == 0 || i == 2) {
                boolean validLat = (-180 <= coordinate) && (coordinate <= 180);
                if (!validLat)
                    throw new InvalidWGS84CoordinateException();
            }
            if (i == 1 || i == 3) {
                boolean validLon = (-90 <= coordinate) && (coordinate <= 90);
                if (!validLon)
                    throw new InvalidWGS84CoordinateException();
            }
        }
    }

    /**
     * As long as it's bigger than the given size, the Bounding Box-Rectangle get's recursively split
     * into four smaller ones of the same size (top left, top right, bottom left, bottom right)
     *
     * @param bbox bounding box
     * @param size maximum size of the Bounding box, for which a request at the HERE-API is possible
     */
    private void quadTreeDissect(double[] bbox, int size)
    {
        double width = Math.abs(bbox[2] - bbox[0]);
        double height = Math.abs(bbox[3] - bbox[1]);

        double halfHeight = height / 2;
        double halfWidth = width / 2;
        double leftLong = bbox[0];
        double bottomLat = bbox[1];

        if ((width > size) || (height > size))
        {
            // set variables
            double rightLong = bbox[2];
            double topLat = bbox[3];

            // calculate new, smaller Bounding Boxes
            double[] topLeft = {topLat, leftLong, (topLat - (halfHeight)), (leftLong + (halfWidth))};
            double[] topRight = {topLat, (leftLong + (halfWidth)), (topLat - (halfHeight)), rightLong};
            double[] bottomLeft = {(topLat - (halfHeight)), leftLong, bottomLat, (leftLong + (halfWidth))};
            double[] bottomRight = {(topLat - (halfHeight)), (leftLong + (halfWidth)), bottomLat, rightLong};

            // and check their size
            quadTreeDissect(topLeft, size);
            quadTreeDissect(topRight, size);
            quadTreeDissect(bottomLeft, size);
            quadTreeDissect(bottomRight, size);
        }
        else
        {
            Double[] bbox_ = new Double[4];
            bbox_[0] = bbox[0];
            bbox_[1] = bbox[1];
            bbox_[2] = bbox[2];
            bbox_[3] = bbox[3];

            // create midPoint
            boundingBoxes.add(bbox_);
        }
    }

    /**
     * Get the Token needed for the Api request
     */
    public void getToken()
    {
        // Get the current working directory
        String currentDirectory = System.getProperty("user.dir");

        // Specify the file name (adjust the file name as needed)
        String fileName = "credentials.properties";

        // Create the file path using the current directory and file name
        Path filePath = Paths.get(currentDirectory, fileName);
        File credentialsFile = filePath.toFile();
        FromDefaultHereCredentialsPropertiesFile fromDefaultHereCredentialsPropertiesFile = new FromDefaultHereCredentialsPropertiesFile(credentialsFile);

        // token request
        token = HereAccessTokenProvider.builder().setClientAuthorizationRequestProvider(fromDefaultHereCredentialsPropertiesFile).build().getAccessToken();

        logger.info("token: " + token);
    }


    /**
     * Build the Api URL.
     *
     * @param bbox the bounding box
     * @param resource "flow" or "incidents"
     * @return The traffic-Api URL
     */
    private String getApiURL(String bbox, String resource)
    {
        // invariable parts
        String front = "https://data.traffic.hereapi.com/v7/";
        String area = "?locationReferencing=olr&in=bbox:";

        // piecing together
        String url = front + resource + area + bbox;

        logger.info("request-URL: {}", url);

        return url;
    }

    /**
     * Get the traffic information.
     *
     * @param bbox the bounding box
     * @param resource "incidents" or "flow"
     */
    private String request(String bbox, String resource) throws IOException
    {
        // setup
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(getApiURL(bbox, resource)))
                // add token and no Cache-Control headers
                .header("Authorization", "Bearer " + token)
                .header("Cache-Control", "no-cache")
                .build();

        try {
            // request
            HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (httpResponse.statusCode() == 200)
            {
                // cast response to String
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpResponse.body()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) { response.append(line); }
                return response.toString();

            } else { logger.error("GET Request failed"); }
        }
        catch (InterruptedException e) { logger.error(e.getMessage()); }

        return null;
    }

    /**
     * Update both incident and flow information.
     *
     */
    public void update()
    {
        getToken();
        String flowJson = null;
        String incidentJson = null;
        // has to be done separately for each Bounding Box
        for (int i = 0; i < boundingBoxes.size(); i++)
        {
            // create bbox-String
            Double[] bbox = boundingBoxes.get(i);
            String box = String.format("%3.3f;%3.3f;%3.3f;%3.3f",
                            bbox[0], bbox[1], bbox[2], bbox[3])
                    .replace(',', '.')
                    .replace(';', ',');

            try {
                flowJson = request(box, "flow");
                incidentJson = request(box, "incidents");
            } catch (IOException e) {
                logger.error("API request failed: {}",e.getMessage());
            }

            // compute midPoint
            double radius = (bbox[3] - bbox[1]) / 2;
            double leftLong = bbox[0];
            double bottomLat = bbox[1];

            GeometryFactory factory = new GeometryFactory();
            Point midPoint = factory.createPoint(new Coordinate(leftLong + radius, bottomLat + radius));
            RoutableOSMMapLoader mapLoader = null;

            try {
                // Initialize OSM Database Loader and close connection
                mapLoader = new RoutableOSMMapLoader(midPoint, radius);
                // mapLoader.close();
            } catch (SQLException e) {
                logger.error("Couldn't initialize MapLoader. Message: {}", e.getMessage());
                System.exit(0);
            }

            // Initialize Decoder for HERE OpenLR Codes.
            HereDecoder hereDecoder = new HereDecoder(mapLoader);
            FlowJsonParser flowJsonParser = new FlowJsonParser(hereDecoder);

            flowJsonParser.parse(flowJson);
            // get update-time, flow info and affected lines
            flowUpdated = flowJsonParser.getUpdated();
            flowItems.addAll(flowJsonParser.getFlowItems());
            flowAffectedLines.addAll(flowJsonParser.getAffectedLines());

            // update Database Tables
            updateFlowTables();
            IncidentsJsonParser incidentsJsonParser = new IncidentsJsonParser(hereDecoder);

            incidentsJsonParser.parse(incidentJson);
            // get update-time, incident info and affected lines
            incidentsUpdated = incidentsJsonParser.getUpdated();
            incidentItems.addAll(incidentsJsonParser.getIncidentItems());
            incidentAffectedLines.addAll(incidentsJsonParser.getAffectedLines());

            // update Database Tables
            updateIncidentTables();
        }
    }

    /**
     * Query to check whether table exists in the database. Returns null if not available, otherwise schema.name.
     *
     * @param schema Name of the schema, in which the Table should be.
     * @param table  Name of the table to be checked.
     * @return Field to use in select query.
     */
    private static Field<?> table(String schema, String table) {

        String query = "to_regclass('" + schema + "." + table + "')";
        return DSL.field(query);
    }

    private void updateIncidentTables()
    {
        // Needed to set schema for creating tables
        Name temp_incidents_v7 = DSL.name("openlr", "temp_incidents_v7");
        Name temp_kanten_incidents_v7 = DSL.name("openlr", "temp_kanten_incidents_v7");
        Name incidents_v7 = DSL.name("openlr", "incidents_v7");
        Name kanten_incidents_v7 = DSL.name("openlr", "kanten_incidents_v7");
        Name incident_affected_v7 = DSL.name("openlr", "incident_affected_v7");
        Name kanten = DSL.name("openlr", "kanten");

        // Checks if "incidents_v7" table already exists
        String incidentsV7TableExists = String.valueOf(ctx.select(table("openlr", "incidents_v7"))
                .fetchOne().value1());

        // Checks if "temp_incidents_v7" table already exists
        String tempIncidentsV7TableExists = String.valueOf(ctx.select(table("openlr", "temp_incidents_v7"))
                .fetchOne().value1());

        // Begin First Transaction - Fills temporary tables
        ctx.transaction(configuration1 -> {

            // Deleting temp_ tables if they exist, prevents program from running into "already exists"-Error
            if (tempIncidentsV7TableExists.equals("openlr.temp_incidents_v7")) {
                ctx.dropTable(DSL.table(temp_kanten_incidents_v7)).cascade().execute();
                ctx.dropTable(DSL.table(temp_incidents_v7)).cascade().execute();

                logger.info("Dropped old temporary tables.");
            }

            // Create temporary incident table
            DSL.using(configuration1).createTable(temp_incidents_v7)
                    .column("incident_id", SQLDataType.CHAR(64).nullable(false))
                    .column("original_id", SQLDataType.CHAR(64))
                    .column("olr", SQLDataType.CHAR(255))
                    .column("start_time", SQLDataType.TIMESTAMP)
                    .column("end_time", SQLDataType.TIMESTAMP.nullable(true))
                    .column("road_closed", SQLDataType.BOOLEAN)
                    .column("description", SQLDataType.CLOB)
                    .column("summary", SQLDataType.CLOB)
                    .column("junction_traversability", SQLDataType.CHAR(32).nullable(true))
                    .column("posoff", SQLDataType.INTEGER)
                    .column("negoff", SQLDataType.INTEGER)
                    .column("last_updated", SQLDataType.TIMESTAMP.defaultValue(incidentsUpdated))
                    .constraints(
                            primaryKey("incident_id"))
                    .execute();

            // Create temporary foreign key table
            DSL.using(configuration1).createTable(temp_kanten_incidents_v7)
                    .column("incident_id", SQLDataType.CHAR(64).nullable(false))
                    .column("line_id", SQLDataType.INTEGER.nullable(false))
                    .column("posoff", SQLDataType.INTEGER.defaultValue(0))
                    .column("negoff", SQLDataType.INTEGER.defaultValue(0))
                    .execute();

            // Fill temp_incidents_v7 table
            for (IncidentItemV7 incident : this.incidentItems) {

                try(DSLContext dslContext = DSL.using(configuration1))
                {
                    dslContext.insertInto(DSL.table(temp_incidents_v7),
                        field(name("incident_id")), field(name("original_id")),
                        field(name("olr")), field(name("start_time")),
                        field(name("end_time")), field(name("road_closed")),
                        field(name("description")), field(name("summary")),
                        field(name("junction_traversability")), field(name("posoff")),
                        field(name("negoff")))
                        .values(incident.getId(), incident.getOriginalId(), incident.getOlr(),
                                incident.getStartTime(), incident.getEndTime(), incident.isRoadClosed(),
                                incident.getDescription(), incident.getSummary(),
                                incident.getJunctionTraversability().toString(),
                                incident.getPosOff() == null ? 0 : incident.getPosOff(),
                                incident.getNegOff() == null ? 0 : incident.getNegOff())
                        .execute();
                } catch (Exception e) {
                    logger.error("Error inserting incident data. IncidentItem: {}. Message: {}.", incident, e.getMessage());
                }


            }

            // Fill temporary foreign key table
            for (AffectedLine affectedLine : this.incidentAffectedLines) {
                DSL.using(configuration1)
                        .insertInto(DSL.table(temp_kanten_incidents_v7),
                                field(name("line_id")), field(name("incident_id")),
                                field(name("posoff")), field(name("negoff")))
                        .values(affectedLine.getLineId(), affectedLine.getIncidentId(),
                                affectedLine.getPosOff(), affectedLine.getNegOff())
                        .execute();
            }

            logger.info("Created temporary tables.");

        }); // End first transaction

        // Begin Second Transaction
        ctx.transaction(configuration2 -> {

            // Drop tables with old data if they exist
            if (incidentsV7TableExists.equals("openlr.incidents_v7"))
            {
                ctx.dropTable(DSL.table(kanten_incidents_v7)).cascade().execute();
                ctx.dropTable(DSL.table(incidents_v7)).cascade().execute();

                logger.info("Dropped old tables.");
            }

            // Rename temp tables and add foreign keys
            ctx.alterTable(temp_incidents_v7).renameTo(incidents_v7).execute();
            ctx.alterTable(temp_kanten_incidents_v7).renameTo(kanten_incidents_v7).execute();

            ctx.alterTable(kanten_incidents_v7).add(constraint("fk_kanten")
                    .foreignKey("line_id").references(KANTEN)).execute();
            ctx.alterTable(kanten_incidents_v7).add(constraint("fk_incidents")
                    .foreignKey("incident_id").references(incidents_v7)).execute();

        }); // End second transaction

        // Delete old incident affected table, if it exists
        String affectedExists = String.valueOf(ctx.select(table("openlr", "incident_affected_v7"))
                .fetchOne().value1());

        if (affectedExists.equals("openlr.incident_affected_v7")) {
            ctx.dropTable(DSL.table(incident_affected_v7)).cascade().execute();
        }

        // Create incident_affected_v7 table for display in QGIS
        ctx.transaction(configuration3 -> {
            DSL.using(configuration3).createTable(incident_affected_v7).as(
                    select(KANTEN.LINE_ID, KANTEN.NAME, IncidentsV7.INCIDENTS_V7.INCIDENT_ID, IncidentsV7.INCIDENTS_V7.ORIGINAL_ID,
                            IncidentsV7.INCIDENTS_V7.START_TIME, IncidentsV7.INCIDENTS_V7.END_TIME,
                            IncidentsV7.INCIDENTS_V7.ROAD_CLOSED, IncidentsV7.INCIDENTS_V7.DESCRIPTION,
                            IncidentsV7.INCIDENTS_V7.SUMMARY, IncidentsV7.INCIDENTS_V7.JUNCTION_TRAVERSABILITY,
                            KANTEN.GEOM).from(DSL.table(kanten_incidents_v7))
                    .join(incidents_v7).on(KantenIncidentsV7.KANTEN_INCIDENTS_V7.INCIDENT_ID.eq(IncidentsV7.INCIDENTS_V7.INCIDENT_ID))
                    .join(kanten).on(KantenIncidentsV7.KANTEN_INCIDENTS_V7.LINE_ID.eq(Kanten.KANTEN.LINE_ID))
                    ).execute();
        });
        logger.info("Updated incident data.");
    }

    private void updateFlowTables()
    {
        // Needed to set schema for creating tables
        Name temp_flow_v7 = DSL.name("openlr", "temp_flow_v7");
        Name temp_kanten_flow_v7 = DSL.name("openlr", "temp_kanten_flow_v7");
        Name flow_v7 = DSL.name("openlr", "flow_v7");
        Name kanten_flow_v7 = DSL.name("openlr", "kanten_flow_v7");
        Name flow_affected_v7 = DSL.name("openlr", "flow_affected_v7");
        Name kanten = DSL.name("openlr","kanten");

        // Checks if "flow_v7" table already exists
        String flowTableExists = String.valueOf(ctx.select(table("openlr", "flow_v7"))
                .fetchOne().value1());

        // Checks if "temp_flow_v7" table already exists
        String tempflowTableExists = String.valueOf(ctx.select(table("openlr", "temp_flow_v7"))
                .fetchOne().value1());

        // Begin First Transaction - Fills temporary tables
        ctx.transaction(configuration1 -> {

            // Deleting temp_ table if it exists, prevents program from running into "already exists"-Error
            if (tempflowTableExists.equals("openlr.temp_flow_v7")) {
                ctx.dropTable(DSL.table(temp_kanten_flow_v7)).cascade().execute();
                ctx.dropTable(DSL.table(temp_flow_v7)).cascade().execute();

                logger.info("Dropped old temporary table.");
            }

            // Create temporary flow table
            DSL.using(configuration1).createTable(temp_flow_v7)
                    .column("name", SQLDataType.CHAR(50))
                    .column("olr", SQLDataType.CHAR(255))
                    .column("speed", SQLDataType.DOUBLE.nullable(true))
                    .column("speed_uncapped", SQLDataType.DOUBLE.nullable(true))
                    .column("free_flow_speed", SQLDataType.DOUBLE.nullable(true))
                    .column("jam_factor", SQLDataType.DOUBLE.nullable(true))
                    .column("confidence", SQLDataType.DOUBLE.nullable(true))
                    .column("traversability", SQLDataType.CHAR(50).nullable(true))
                    .column("junction_traversability", SQLDataType.CHAR(50).nullable(true))
                    .column("posoff", SQLDataType.INTEGER)
                    .column("negoff", SQLDataType.INTEGER)
                    .column("last_updated", SQLDataType.TIMESTAMP.defaultValue(flowUpdated))
                    .constraints(
                            primaryKey("olr"))
                    .execute();

            // Create temporary foreign key table
            DSL.using(configuration1)
                    .createTable(temp_kanten_flow_v7)
                    .column("olr", SQLDataType.CHAR(255).nullable(false))
                    .column("line_id", SQLDataType.INTEGER.nullable(false))
                    .column("posoff", SQLDataType.INTEGER.defaultValue(0))
                    .column("negoff", SQLDataType.INTEGER.defaultValue(0))
                    .execute();

            // Fill temp flow_v7 table
            for (FlowItemV7 flowItem : flowItems) {

                if (flowItem.isInvalid())
                {
                    logger.debug("Incomplete flowItem: {}", flowItem);
                }


                Result<Record> records = DSL.using(configuration1).select(DSL.asterisk()).from(DSL.table(temp_flow_v7)).where(field("olr").equal(flowItem.getOlr())).fetch();
                boolean olrExists = records.size() > 0;
                if (olrExists)
                {
                    Record record = records.get(0);
                    FlowItemV7 existingItem = new FlowItemV7(
                            record.get("name").toString(),
                            record.get("olr").toString(),
                            Double.parseDouble(record.get("speed").toString()),
                            Double.parseDouble(record.get("speed_uncapped").toString()),
                            Double.parseDouble(record.get("free_flow_speed").toString()),
                            Double.parseDouble(record.get("jam_factor").toString()),
                            Double.parseDouble(record.get("confidence").toString()),
                            record.get("traversability").toString(),
                            JunctionTraversability.get(record.get("junction_traversability").toString()),
                            Integer.parseInt(record.get("posoff").toString()),
                            Integer.parseInt(record.get("negoff").toString()));

                    logger.info("existing flow item: {}", existingItem);
                    logger.info("new flow item: {}", flowItem);
                }


                try(DSLContext dslContext = DSL.using(configuration1))
                {
                        dslContext.insertInto(DSL.table(temp_flow_v7),
                            field(name("name")),
                            field(name("olr")), field(name("speed")),
                            field(name("speed_uncapped")),
                            field(name("free_flow_speed")), field(name("jam_factor")),
                            field(name("confidence")), field(name("traversability")),
                            field(name("junction_traversability")), field(name("posoff")),
                            field(name("negoff")))
                            .values(flowItem.getName(),
                                    flowItem.getOlr(),
                                    flowItem.getSpeed() == null ? "NaN" : flowItem.getSpeed(),
                                    flowItem.getSpeedUncapped() == null ? "NaN" : flowItem.getSpeedUncapped(),
                                    flowItem.getFreeFlow() == null ? "NaN" : flowItem.getFreeFlow(),
                                    flowItem.getJamFactor() == null ? "NaN" : flowItem.getJamFactor(),
                                    flowItem.getConfidence() == null ? "NaN" : flowItem.getConfidence(),
                                    flowItem.getTraversability(),
                                    flowItem.getJunctionTraversability().toString(),
                                    flowItem.getPosoff() == null ? 0 : flowItem.getPosoff(),
                                    flowItem.getNegoff() == null ? 0 : flowItem.getNegoff())
                            // ignore duplicate OLR keys
                            .onConflictDoNothing()
                            .execute();
                } catch (Exception e) {
                    logger.error("Error inserting flow data. FlowItem: {}. Message: {}.", flowItem, e.getMessage());
                }


            }

            // Fill temp foreign key table
            for (AffectedLine affectedLine : this.flowAffectedLines) {
                DSL.using(configuration1)
                        .insertInto(DSL.table(temp_kanten_flow_v7),
                                field(name("line_id")), field(name("olr")),
                                field(name("posoff")), field(name("negoff")))
                        .values(affectedLine.getLineId(), affectedLine.getIncidentId(),
                                affectedLine.getPosOff(), affectedLine.getNegOff())
                        .execute();
            }

            logger.info("Created temporary tables.");

        }); // End first transaction

        // Begin Second Transaction
        ctx.transaction(configuration2 -> {

            // Drop tables with old data if exists
            if (flowTableExists.equals("openlr.flow_v7"))
            {
                ctx.dropTable(DSL.table(kanten_flow_v7)).cascade().execute();
                ctx.dropTable(DSL.table(flow_v7)).cascade().execute();

                logger.info("Dropped old tables.");
            }
            // Rename temp tables and add foreign keys
            ctx.alterTable(temp_flow_v7).renameTo(flow_v7).execute();
            ctx.alterTable(temp_kanten_flow_v7).renameTo(kanten_flow_v7).execute();

            ctx.alterTable(kanten_flow_v7).add(constraint("fk_kanten")
                    .foreignKey("line_id").references(KANTEN)).execute();
            ctx.alterTable(kanten_flow_v7).add(constraint("fk_flow")
                    .foreignKey("olr").references(flow_v7)).execute();

        }); // End second transaction

        // Delete old incident affected table, if it exists
        String affectedExists = String.valueOf(ctx.select(table("openlr", "flow_affected_v7"))
                .fetchOne().value1());

        if (affectedExists.equals("openlr.flow_affected_v7")) {
            ctx.dropTable(DSL.table(flow_affected_v7)).cascade().execute();
        }

        // Create flow_affected_v7 table for display in QGIS
        ctx.transaction(configuration3 -> {
            DSL.using(configuration3).createTable(flow_affected_v7).as(
                    select(KANTEN.LINE_ID, KANTEN.NAME, FlowV7.FLOW_V7.OLR, FlowV7.FLOW_V7.SPEED,
                            FlowV7.FLOW_V7.SPEED_UNCAPPED, FlowV7.FLOW_V7.FREE_FLOW_SPEED,
                            FlowV7.FLOW_V7.JAM_FACTOR, FlowV7.FLOW_V7.CONFIDENCE,
                            FlowV7.FLOW_V7.TRAVERSABILITY, FlowV7.FLOW_V7.JUNCTION_TRAVERSABILITY,
                            KANTEN.GEOM).from(DSL.table(kanten_flow_v7))
                            .join(flow_v7).on(KantenFlowV7.KANTEN_FLOW_V7.OLR.eq(FlowV7.FLOW_V7.OLR))
                            .join(kanten).on(KantenFlowV7.KANTEN_FLOW_V7.LINE_ID.eq(Kanten.KANTEN.LINE_ID))
            ).execute();
        });

        logger.info("Updated flow data.");
    }
}


