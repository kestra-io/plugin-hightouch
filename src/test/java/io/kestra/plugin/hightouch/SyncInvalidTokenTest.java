package io.kestra.plugin.hightouch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.kestra.core.junit.annotations.KestraTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KestraTest
@Disabled
class SyncInvalidTokenTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Value("${hightouch.sync-id}")
    private Long syncId;

    @Value("INVALID_TOKEN")
    private String token;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of(ImmutableMap.of());

        Sync task = Sync.builder()
                .token(Property.of(this.token))
                .syncId(Property.of(this.syncId))
                .build();

        Throwable exception = assertThrows(
                HttpClientResponseException.class,
                        () -> task.run(runContext),
                        "Expected Sync() to throw 401 error with HttpClientResponseException, but it didn't"
        );
        assertThat(exception.getMessage(), containsString("Request failed with status '401'"));
        assertThat(exception.getMessage(), containsString("Token is invalid"));
    }
}
