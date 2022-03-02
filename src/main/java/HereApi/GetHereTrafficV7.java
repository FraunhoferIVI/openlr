package HereApi;

import com.here.account.auth.provider.FromDefaultHereCredentialsPropertiesFile;
import com.here.account.oauth2.HereAccessTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GetHereTrafficV7
{
    String token;
    String bbox = "9.850,53.500,10.130,53.600";
    // SW,NE
    // Hamburg: 53.500,9.850,53.600,10.130
    // Dresden: 50.900,13.600,51.100,13,800
    String flowData;
    String incidentData;

    private static final Logger logger = LoggerFactory.getLogger(ApiRequest.class);

    /*public GetHereTrafficV7(String bbox)
    {
        this.bbox = bbox;
    }*/

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
     * @param resource "flow" or "incidents"
     * @return The traffic-Api URL
     */
    private String getApiURL(String resource)
    {
        String front = "https://data.traffic.hereapi.com/v7/";
        String area = "?locationReferencing=olr&in=bbox:";

        return front + resource + area + bbox;
    }

    /**
     * Get the traffic information.
     *
     * @param resource "incidents" or "flow"
     */
    public void request(String resource) throws IOException
    {
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .uri(URI.create(getApiURL(resource)))
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
                String json = response.toString();

                System.out.println(resource + "JSON: " + json);

            } else { logger.error("GET Request failed"); }
        }
        catch (InterruptedException e) { logger.error(e.getMessage()); }
    }
}


