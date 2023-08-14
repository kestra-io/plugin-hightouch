import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.http.client.netty.NettyHttpClientFactory;
import io.micronaut.runtime.Micronaut;

import java.net.MalformedURLException;
import java.net.URI;

public class MicronautTrigger {

    public static void main(String[] args) {
        Micronaut.run(MicronautTrigger.class, args);

        triggerSync();
    }

    private static final NettyHttpClientFactory FACTORY = new NettyHttpClientFactory();

    public static void triggerSync() {
        String url = "api/v1/syncs/1127166/trigger";
        String requestBody = "{}";

        try {
            DefaultHttpClient httpClient = (DefaultHttpClient) FACTORY.createClient(URI.create("https://api.hightouch.com").toURL(), new DefaultHttpClientConfiguration());

            HttpRequest<String> request = HttpRequest
                    .POST(url, "{}")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.ACCEPT, MediaType.ALL)
                    .bearerAuth("WRONG_TOKEN")
                    .body(requestBody);

            String response = httpClient.toBlocking().retrieve(request);

            System.out.println("Response: " + response);
            httpClient.close();  // Close the HttpClient when you're done using it

        } catch (HttpClientResponseException e) {
        System.out.println("Request failed '" + e.getStatus().getCode() + "' and body '" + e.getResponse().getBody(String.class).orElse("null") + "'");
        throw new HttpClientResponseException(
                "Request failed '" + e.getStatus().getCode() + "' and body '" + e.getResponse().getBody(String.class).orElse("null") + "'",
                e,
                e.getResponse()
        );
        } catch ( MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
