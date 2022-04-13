package HereApi;

import DataBase.DatasourceConfig;
import com.here.account.auth.provider.FromDefaultHereCredentialsPropertiesFile;
import com.here.account.oauth2.HereAccessTokenProvider;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.jooq.impl.DSL.*;
import static org.jooq.sources.tables.Kanten.KANTEN;

public class HereTrafficV7
{
    private String token;
    private String bbox;
    // the bounding box devided in smaller pieces, when to big
    private List<String> boundingBoxes = new ArrayList<>();
    // traffic data
    private Timestamp flowUpdated;
    private List<FlowItemV7> flowItems = new ArrayList<>();
    private Timestamp incidentsUpdated;
    private List<IncidentItemV7> incidentItems = new ArrayList<>();

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
     * @param bbox format: SW Long,SW Lat,NE Long,NE Lat (counterclockwise)
     *  Hamburg: 9.850,53.500,10.130,53.600
     *  Dresden: 13.600,50.900,13,800,51.100
     */
    public HereTrafficV7(String bbox)
    {
        this.bbox = bbox;
        // get coordinates as double values
        Pattern pattern = Pattern.compile("[,]");
        double[] coordinates = pattern.splitAsStream(bbox)
                .mapToDouble(Double::parseDouble)
                .toArray();
        // set boundingBoxes
        quadTreeDissect(coordinates, 1);
    }

    /**
     * Quad-Tree-dissection of the Bounding box, if bigger than size.
     *
     * @param bbox bounding box
     * @param size maximum piece-size
     */
    private void quadTreeDissect(double[] bbox, int size)
    {
        double width = Math.abs(bbox[2] - bbox[0]);
        double height = Math.abs(bbox[3] - bbox[1]);

        if ((width > size) || (height > size))
        {
            double leftLong = bbox[0];
            double bottomLat = bbox[1];
            double rightLong = bbox[2];
            double topLat = bbox[3];
            double halfHeight = height / 2;
            double halfWidth = width / 2;

            double[] topLeft = {topLat, leftLong, (topLat - (halfHeight)), (leftLong + (halfWidth))};
            double[] topRight = {topLat, (leftLong + (halfWidth)), (topLat - (halfHeight)), rightLong};
            double[] bottomLeft = {(topLat - (halfHeight)), leftLong, bottomLat, (leftLong + (halfWidth))};
            double[] bottomRight = {(topLat - (halfHeight)), (leftLong + (halfWidth)), bottomLat, rightLong};

            quadTreeDissect(topLeft, size);
            quadTreeDissect(topRight, size);
            quadTreeDissect(bottomLeft, size);
            quadTreeDissect(bottomRight, size);
        }
        else
            {
                String box = String.format("%3.3f;%3.3f;%3.3f;%3.3f",
                        bbox[0], bbox[1], bbox[2], bbox[3])
                        .replace(',', '.')
                        .replace(';', ',');

                boundingBoxes.add(box);
            }
    }

    /**
     * get the Token needed for the Api request
     */
    public void getToken()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = "hereCredentials_new.properties";
        URL url = classLoader.getResource(fileName);
        File credentialsFile = new File(url.getPath());

        FromDefaultHereCredentialsPropertiesFile fromDefaultHereCredentialsPropertiesFile = new FromDefaultHereCredentialsPropertiesFile(credentialsFile);
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
        String front = "https://data.traffic.hereapi.com/v7/";
        String area = "?locationReferencing=olr&in=bbox:";

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
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(getApiURL(bbox, resource)))
                .header("Authorization", "Bearer " + token)
                .header("Cache-Control", "no-cache")
                .build();

        try {
            HttpResponse<InputStream> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

            if (httpResponse.statusCode() == 200)
            {
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
     * Update the traffic information
     *
     * @param resource "incidents"/"flow"
     */
    public void update(String resource)
    {
        getToken();
        String json = null;
        for (String bbox : boundingBoxes)
        {
            try {
                json = request(bbox, resource);
            } catch (IOException e) {
                logger.error(e.getMessage());
            }

            if (resource.equals("flow")) {
                FlowJsonParser parser = new FlowJsonParser();

                parser.parse(json);
                flowUpdated = parser.getUpdated();
                flowItems.addAll(parser.getFlowItems());
            }
            else if (resource.equals("incidents")) {
                IncidentsJsonParser parser = new IncidentsJsonParser();

                parser.parse(json);
                incidentsUpdated = parser.getUpdated();
                incidentItems.addAll(parser.getIncidentItems());
            }
        }
        if (resource.equals("flow")) { updateFlowTables(); }
        else { updateIncidentTables(); }
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

        // Checks if "incidents_v7" table already exists
        String incidentsV7TableExists = String.valueOf(ctx.select(table("openlr", "incidents_v7"))
                .fetchOne().value1());

        // Checks if "temp_incidents_v7" table already exists
        String tempIncidentsV7TableExists = String.valueOf(ctx.select(table("openlr", "temp_incidents_v7"))
                .fetchOne().value1());

        // Timestamp is only created when table "incidents_v7" exists
        Timestamp youngestEntry = incidentsV7TableExists.equals("null") ? null :
                (Timestamp) ctx.select(min(field("generationdate"))).from(DSL.table(incidents_v7)).fetchOne().value1();

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
                    .column("generationdate", SQLDataType.TIMESTAMP.defaultValue(field("now()", SQLDataType.TIMESTAMP)))
                    .constraints(primaryKey("incident_id"))
                    .execute();

            // Create temporary foreign key table
            DSL.using(configuration1)
                    .createTable(temp_kanten_incidents_v7)
                    .column("incident_id", SQLDataType.CHAR(64).nullable(false))
                    .column("line_id", SQLDataType.INTEGER.nullable(false))
                    .column("posoff", SQLDataType.INTEGER.defaultValue(0))
                    .column("negoff", SQLDataType.INTEGER.defaultValue(0))
                    .execute();

            // Fill temp incidents_v7 table
            for (IncidentItemV7 incident : this.incidentItems) {

                DSL.using(configuration1)
                        .insertInto(DSL.table(temp_incidents_v7),
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
                                posoff, negoff)
                        .execute();
            }

            // Fill temp foreign key table
            for (AffectedLine affectedLine : this.affectedLinesList) {
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

            // Drop tables with old data if exists
            if (incidentsV7TableExists.equals("openlr.incidents_v7")) {

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

        // Checks if incident_affected_v7 table already exists
        String affectedExists = String.valueOf(ctx.select(table("openlr", "incident_affected_v7"))
                .fetchOne().value1());

        if (affectedExists.equals("openlr.incident_affected_v7")) {
            ctx.dropTable(DSL.table(incident_affected_v7)).cascade().execute();
        }

        // Create QGIS view containing incident_affected_v7 lines
        ctx.execute("CREATE TABLE openlr.incident_affected_v7 AS select k.line_id , k.name, " +
                "i.incident_id , i.original_id , i.start_time , i.end_time , i.road_closed ," +
                "i.description , i.summary , i.junction_traversability" +
                "k.geom from openlr.kanten_incidents_v7 ki " +
                "join openlr.incidents_v7 i on (ki.incident_id = i.incident_id)" +
                "join openlr.kanten k on (ki.line_id = k.line_id);");

        logger.info("Updated incident data.");
    }

    private void updateFlowTables()
    {

    }
}


