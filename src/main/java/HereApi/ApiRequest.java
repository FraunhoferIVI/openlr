package HereApi;

import DataBase.DatasourceConfig;
import Exceptions.InvalidBboxException;
import Exceptions.InvalidWGS84CoordinateException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

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
    private String hereApikey = "yourApi";
    private String answer;

    // Contains incident information for all requested bounding boxes
    private List<Incident> incidentList;
    // Contains affected lines for all requested bounding boxes
    private List<AffectedLine> affectedLinesList;

    public ApiRequest() {
        this.incidentList = new ArrayList<>();
        this.affectedLinesList = new ArrayList<>();
    }

    // needed for SQL queries
    static DSLContext ctx;
    static {
        try {
            ctx = DSL.using(DatasourceConfig.getConnection(), SQLDialect.POSTGRES);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Query to check whether table exists in the database. Returns null if not available, otherwise schema.name.
     *
     * @param schema Name of the schema where table should be available.
     * @param table  Name of the table to be checked.
     * @return Field to use in select query.
     */
    public static Field<?> to_regclass(String schema, String table) {

        String query = "to_regclass('" + schema + "." + table + "')";
        return DSL.field(query);
    }

    /**
     * Sets URL with given bbox.
     * @throws MalformedURLException URL is in the wrong format or an unknown transmission protocol is specified.
     */
    private URL setUrl(String bbox) throws MalformedURLException {
        String baseUrl = "https://traffic.ls.hereapi.com";
        String incidents = "/traffic/6.3/";
        String flow = "/traffic/6.2/";
        String resource = "incidents";
        String format = ".xml";
        String apiKey = "?apiKey=" + hereApikey;
        //String criticality = "&criticality=minor";
        return new URL(baseUrl + incidents + resource + format + apiKey + bbox);
    }

    /**
     * Sends request to HERE API.
     * API returns xml, xml is converted to String.
     * @param bboxString Coordinates for bbox given as String to use in Api Request URL.
     * @return HERE Api answer as String
     * @throws IOException Signals a general input / output error
     */
    private String sendRequest(String bboxString) throws IOException {

        URL request = setUrl(bboxString);
        System.out.println(request);
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
            System.out.println("GET Request failed.");
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
        return new Timestamp(System.currentTimeMillis());
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
                "\nExample: 51.057,13.744;51.053,13.751 ");
        String bboxString = scanner.next();

        //BBox to request incidents for Dresden
        //String bboxString = "51.1809,13.5766;50.9766,13.9812";

        //get coordinates as double values
        Pattern pattern = Pattern.compile("[,;]");

        double[] coordinates = pattern.splitAsStream(bboxString)
                .mapToDouble(Double::parseDouble)
                .toArray();

        checkWGS84validity(coordinates);

        return new BoundingBox(coordinates[0], coordinates[1], coordinates[2], coordinates[3]);
    }

    /**
     * Checks the bounding box. For the Api Request, the request boundin box is limited to a
     * maximum of 2 degrees (https://developer.here.com/documentation/traffic/dev_guide/topics/limitations.html).
     * If the specified bounding box is too large, it is broken down into sufficiently small boxes.
     * For each bounding box an API request is made, the XML file is parsed, the OpenLR code is decoded
     * and the incident information and the affected lines are collected.
     *
     * @param bbox Bounding box
     */
    private void getRecursiveBbox(@NotNull BoundingBox bbox) {

        // Recursive bounding box query
        if ((bbox.width > 10) || (bbox.height > 10)) {

            // Box upper left
            getRecursiveBbox(new BoundingBox(bbox.getUpperLeftLat(), bbox.getUpperLeftLon(),
                    (bbox.getUpperLeftLat() - (bbox.getHeight() / 2)), (bbox.getUpperLeftLon() + (bbox.getWidth() / 2))));
            // Box upper right
            getRecursiveBbox(new BoundingBox(bbox.getUpperLeftLat(), (bbox.getUpperLeftLon() + (bbox.getWidth() / 2)),
                    (bbox.getUpperLeftLat() - (bbox.getHeight() / 2)), bbox.getBottomRightLon()));
            // Box lower left
            getRecursiveBbox(new BoundingBox((bbox.getUpperLeftLat() - (bbox.getHeight() / 2)),
                    bbox.getUpperLeftLon(), bbox.getBottomRightLat(), (bbox.getUpperLeftLon() - (bbox.getWidth() / 2))));
            // Box lower right
            getRecursiveBbox(new BoundingBox((bbox.getUpperLeftLat() - (bbox.getHeight() / 2)),
                    (bbox.getUpperLeftLon() + (bbox.getWidth() / 2)), bbox.getUpperLeftLat(), bbox.getBottomRightLon()));
        } else {

            //Gets Here Api request answer
            try {
                sendRequest(bbox.getBboxRequestString());
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Parse answer or file
            XMLParser parser = new XMLParser();
            parser.parseXMLFromApi(answer);
            // If you wanne test out a file instead of the API
            //parser.parseXMlFromFile("");


            // Collect relevant data per incident and decoding location
            DataCollector collector = new DataCollector();
            try {
                collector.collectInformation(parser.getListTrafficItems());
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Collects incident data and affected lines for all requested bounding boxes
            this.incidentList.addAll(collector.getListIncidents());
            this.affectedLinesList.addAll(collector.getListAffectedLines());

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

        // Get current timestamp
        Timestamp currentTimestamp = getTimeStamp();

        // Get recursive bounding boxes if bbox is bigger than 2 degrees
        getRecursiveBbox(setBoundingBox());

        // Needed to set schema for creating tables
        Name temp_incidents = DSL.name("openlr", "temp_incidents");
        Name temp_kanten_incidents = DSL.name("openlr", "temp_kanten_incidents");
        Name incidents = DSL.name("openlr", "incidents");
        Name kanten_incidents = DSL.name("openlr", "kanten_incidents");
        Name affected = DSL.name("openlr", "affected");

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
            }

            // If the most recent entry in the incident table is younger than the time stamp when the program
            // was started, this message is printed.
            if (youngestEntry != null && currentTimestamp.before(youngestEntry)) {
                System.out.println("The incident data is up to date, the data has not been updated.");

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
            }

            // Rename temp tables and add foreign keys
            ctx.alterTable(temp_incidents).renameTo(incidents).execute();
            ctx.alterTable(temp_kanten_incidents).renameTo(kanten_incidents).execute();

            ctx.alterTable(kanten_incidents).add(constraint("fk_kanten")
                    .foreignKey("line_id").references(KANTEN)).execute();
            ctx.alterTable(kanten_incidents).add(constraint("fk_incidents")
                    .foreignKey("incident_id").references(incidents)).execute();

        }); // End second transaction

        // Checks if affected table already exists
        String affectedExists = String.valueOf(ctx.select(to_regclass("openlr", "affected"))
                .fetchOne().value1());

        if (affectedExists.equals("openlr.affected")) {
            ctx.dropTable(table(affected)).cascade().execute();
        }

        // Create QGIS view containing affected lines
        ctx.execute("CREATE TABLE openlr.affected AS select k.line_id , k.name, i.incident_id , i.incident_type , i.criticality ," +
                "i.roadclosure , i.start_date , i.end_date , i.shortdesc , i.longdesc ," +
                "k.geom from openlr.kanten_incidents ki " +
                "join openlr.incidents i on (ki.incident_id = i.incident_id)" +
                "join openlr.kanten k on (ki.line_id = k.line_id);");


        System.out.println("Program ended.");
    }

}
