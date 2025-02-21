package io.kestra.plugin.hightouch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.core.http.HttpRequest;
import io.kestra.core.http.HttpResponse;
import io.kestra.core.http.client.HttpClient;
import io.kestra.core.http.client.HttpClientException;
import io.kestra.core.http.client.configurations.HttpConfiguration;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.IOException;
import java.net.URI;

import jakarta.validation.constraints.NotNull;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
public abstract class AbstractHightouchConnection extends Task {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new JavaTimeModule());

    private static final String BASE_URL = "https://api.hightouch.com";

    @Schema(title = "API Bearer token")
    @NotNull
    private Property<String> token;

    @Schema(title = "The HTTP client configuration.")
    protected HttpConfiguration options;

    /**
     * @param method        The HTTP method (GET, POST, PUT, DELETE).
     * @param path          The API endpoint path.
     * @param body          The request body (nullable).
     * @param responseType  The expected response type.
     * @param runContext    The Kestra run context.
     * @param <RES>         The response class.
     * @return HttpResponse of type RES.
     */
    protected <RES> HttpResponse<RES> request(String method, String path, Object body, Class<RES> responseType, RunContext runContext)
        throws HttpClientException, IllegalVariableEvaluationException {

        URI fullUri = URI.create(BASE_URL + path);

        HttpRequest.HttpRequestBuilder requestBuilder = HttpRequest.builder()
            .uri(fullUri)
            .method(method)
            .addHeader("Authorization", "Bearer " + runContext.render(this.token).as(String.class).orElseThrow())
            .addHeader("Content-Type", "application/json");

        if (body != null) {
            requestBuilder.body(HttpRequest.JsonRequestBody.builder().content(body).build());
        }

        try (HttpClient client = new HttpClient(runContext, options)) {
            HttpResponse<String> response = client.request(requestBuilder.build(), String.class);

            RES parsedResponse = MAPPER.readValue(response.getBody(), responseType);
            return HttpResponse.<RES>builder()
                .request(requestBuilder.build())
                .body(parsedResponse)
                .headers(response.getHeaders())
                .status(response.getStatus())
                .build();

        } catch (IOException e) {
            throw new RuntimeException("Error executing HTTP request", e);
        }
    }
}
