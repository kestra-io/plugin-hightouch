package io.kestra.plugin.hightouch;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.collect.ImmutableMap;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import jakarta.inject.Inject;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@KestraTest
@WireMockTest(httpPort = 28181)
class SyncTest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void run() throws Exception {
        RunContext runContext = runContextFactory.of();

        stubHightouchApi();

        TestSync task = TestSync.builder()
            .base("http://localhost:28181")
            .token(Property.ofValue("token"))
            .syncId(Property.ofValue(0L))
            .wait(Property.ofValue(true))
            .build();

        Sync.Output runOutput = task.run(runContext);

        assertThat(runOutput, is(notNullValue()));
        assertThat(runOutput.getRunId(), is(notNullValue()));
    }

    @Test
    void shouldRunWithWait() throws Exception {
        RunContext runContext = runContextFactory.of();

        stubHightouchApi();

        TestSync task = TestSync.builder()
            .base("http://localhost:28181")
            .token(Property.ofValue("token"))
            .syncId(Property.ofValue(0L))
            .wait(Property.ofValue(true))
            .build();

        Sync.Output runOutput = task.run(runContext);

        assertThat(runOutput, is(notNullValue()));
        assertThat(runOutput.getRunId(), is(notNullValue()));
    }

    private void stubHightouchApi() {
        stubFor(get(urlEqualTo("/api/v1/syncs/0"))
            .willReturn(okJson("""
                {
                  "id": 0,
                  "slug": "test-sync"
                }
            """)));

        stubFor(post(urlEqualTo("/api/v1/syncs/0/trigger"))
            .willReturn(okJson("""
                {
                  "id": 123
                }
            """)));

        stubFor(get(urlPathEqualTo("/api/v1/syncs/0/runs"))
            .willReturn(okJson("""
                {
                  "data": [
                    {
                      "id": 123,
                      "status": "SUCCESS",
                      "completionRatio": 100,
                      "successfulRows": {
                        "addedCount": 10,
                        "removedCount": 0,
                        "changedCount": 0
                      },
                      "failedRows": {
                        "addedCount": 0,
                        "removedCount": 0,
                        "changedCount": 0
                      }
                    }
                  ]
                }
            """)));
    }

    @SuperBuilder
    static class TestSync extends Sync {
        private final String base;

        TestSync(String base) {
            this.base = base;
        }

        @Override
        protected String baseUrl() {
            return base;
        }
    }
}