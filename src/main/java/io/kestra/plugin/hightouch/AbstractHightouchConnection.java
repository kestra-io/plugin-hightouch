package io.kestra.plugin.hightouch;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
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

import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.JacksonMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
abstract class AbstractHightouchConnection extends Task {

    @Schema(
        title = "API Bearer token"
    )
    @NotNull
    Property<String> token;

    private final static String BASE_URL = "https://api.hightouch.com";
    private final static ObjectMapper OBJECT_MAPPER = JacksonMapper.ofJson();

    protected <REQ, RES> RES request(String method, String path, String body, Class<RES> responseType, RunContext runContext) throws IOException, InterruptedException {

        HttpClient httpClient = HttpClient.newBuilder().build();

        try {
            URI fullPath = URI.create(BASE_URL).resolve(path);

            HttpRequest request = HttpRequest.newBuilder(fullPath)
                    .method(method, HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + runContext.render(token).as(String.class).orElseThrow())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return OBJECT_MAPPER.readValue(response.body(), responseType);
            }
            else {
                throw new RuntimeException(
                        "Request to '" + fullPath.getPath() + "' failed with status '" + response.statusCode() + "' and body '" + response.body() + "'"
                );
            }
        } catch (ConnectException | MalformedURLException | IllegalVariableEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}
