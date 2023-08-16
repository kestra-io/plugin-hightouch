package io.kestra.plugin.hightouch;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.Task;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.net.MalformedURLException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractHightouchConnection extends Task {

    @Schema(
        title = "API Bearer token"
    )
    @NotNull
    @PluginProperty(dynamic = true)
    String token;

    protected <REQ, RES> io.micronaut.http.HttpResponse <RES> request(String method, String path, String body, Class<RES> responseType) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newBuilder().build();
        String baseUrl = "https://api.hightouch.com";
        ObjectMapper objectMapper = new ObjectMapper() // Jackson ObjectMapper for JSON parsing
                .registerModule(new JavaTimeModule());

        try {
            URI fullPath = URI.create(baseUrl).resolve(path);

            HttpRequest request = HttpRequest.newBuilder(fullPath)
                    .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                RES parsedBody =  objectMapper.readValue(response.body(), responseType);
                return io.micronaut.http.HttpResponse.created(parsedBody, fullPath)
                        .status(response.statusCode());
            }
            else {
                System.out.println("Request failed with status '" + response.statusCode() + "' and body '" + response.body() + "'");
                throw new HttpClientResponseException(
                        "Request failed with status '" + response.statusCode() + "' and body '" + response.body() + "'",
                        io.micronaut.http.HttpResponse.created(null, fullPath)
                        .status(response.statusCode()));
            }
        } catch (ConnectException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
