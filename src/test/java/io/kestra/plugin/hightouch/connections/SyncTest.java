package io.kestra.plugin.hightouch.connections;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.hightouch.connections.Sync;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@MicronautTest
class SyncTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of(ImmutableMap.of());

        Sync task = Sync.builder()
                .token("YOUR_TOKEN")
                .syncId(90)
                .build();

        Sync.Output runOutput = task.run(runContext);

        System.out.println(runOutput.getRunId());

        assertThat(runOutput, is(notNullValue()));
        assertThat(runOutput.getRunId(), is(notNullValue()));

    }
}
