package io.kestra.plugin.hightouch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.http.client.HttpClientResponseException;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.micronaut.context.annotation.Value;
import io.kestra.core.junit.annotations.KestraTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
class SyncInvalidTokenTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Value("00000000000000000000000")
    private Long invalidSyncId;

    @Value("INVALID_TOKEN")
    private String token;

    @Test
    void runInvalidToken() throws Exception {
        RunContext runContext = runContextFactory.of(ImmutableMap.of());

        Sync task = Sync.builder()
            .token(Property.ofValue(this.token))
            .syncId(Property.ofValue(this.invalidSyncId))
            .build();

        io.kestra.core.http.client.HttpClientResponseException exception = assertThrows(
            HttpClientResponseException.class,
            () -> task.run(runContext),
            "Expected Sync() to throw 404 error with HttpClientResponseException, but it didn't"
        );
        assertEquals(401, Objects.requireNonNull(exception.getResponse()).getStatus().getCode());
    }
}
