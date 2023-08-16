package io.kestra.plugin.hightouch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class SyncInvalidSyncIdTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Value("00000000000000000000000")
    private Long invalidSyncId;

    @Value("${hightouch.token}")
    private String token;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of(ImmutableMap.of());

        Sync task = Sync.builder()
                .token(this.token)
                .syncId(this.invalidSyncId)
                .build();

        Throwable exception = assertThrows(
                HttpClientResponseException.class,
                        () -> task.run(runContext),
                        "Expected Sync() to throw 404 error with HttpClientResponseException, but it didn't"
        );
        assertThat(exception.getMessage(), containsString("Request failed with status '404'"));
    }
}
