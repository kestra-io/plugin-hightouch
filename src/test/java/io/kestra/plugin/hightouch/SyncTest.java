package io.kestra.plugin.hightouch;

import com.google.common.collect.ImmutableMap;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.micronaut.context.annotation.Value;
import io.kestra.core.junit.annotations.KestraTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
class SyncTest {
    @Inject
    private RunContextFactory runContextFactory;

    @Value("${hightouch.sync-id}")
    private Long syncId;

    @Value("${hightouch.token}")
    private String token;

    @Test
    @Disabled
    void run() throws Exception {
        RunContext runContext = runContextFactory.of(ImmutableMap.of());

        Sync task = Sync.builder()
                .token(Property.ofValue(this.token))
                .syncId(Property.ofValue(this.syncId))
                .build();

        Sync.Output runOutput = task.run(runContext);

        assertThat(runOutput, is(notNullValue()));
        assertThat(runOutput.getRunId(), is(notNullValue()));

    }
}
