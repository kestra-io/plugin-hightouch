package io.kestra.plugin.hightouch;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.Await;
import io.kestra.plugin.hightouch.models.Run;
import io.kestra.plugin.hightouch.models.RunDetails;
import io.kestra.plugin.hightouch.models.RunDetailsResponse;
import io.kestra.plugin.hightouch.models.RunStatus;
import io.kestra.plugin.hightouch.models.SyncDetailsResponse;

import io.micronaut.http.uri.UriTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.kestra.core.utils.Rethrow.throwSupplier;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Trigger a Hightouch sync and optionally wait for its completion"
)
@Plugin(
    examples = {
        @Example(
            code = {
                "token: YOUR_API_TOKEN",
                "syncId: 1127166",
            }
        )
    }
)
public class Sync extends AbstractHightouchConnection implements RunnableTask<Sync.Output> {
    private static final List<RunStatus> ENDED_STATUS = List.of(
        RunStatus.FAILED,
        RunStatus.CANCELLED,
        RunStatus.SUCCESS
    );

    private static final Duration STATUS_REFRESH_RATE = Duration.ofSeconds(1);

    @Schema(
        title = "The sync id to trigger run"
    )
    @NotNull
    @PluginProperty(dynamic = true)
    private Long syncId;

    @Schema(
            title = "Whether to do a full resynchronization"
    )
    @PluginProperty(dynamic = true)
    @Builder.Default
    private Boolean fullResynchronization = false;

    @Schema(
        title = "Whether to wait for the end of the run.",
        description = "Allowing to capture run status and logs"
    )
    @PluginProperty
    @Builder.Default
    private Boolean wait = true;

    @Schema(
        title = "The max total wait duration"
    )
    @PluginProperty
    @Builder.Default
    private Duration maxDuration = Duration.ofMinutes(5);

    @Builder.Default
    @Getter(AccessLevel.NONE)
    private transient Map<Integer, Integer> loggedLine = new HashMap<>();

    @Override
    public Sync.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // Get details of sync to display slug
        SyncDetailsResponse syncDetails = this.request(
                "GET",
                UriTemplate
                        .of("/api/v1/syncs/{syncId}")
                        .expand(Map.of(
                                "syncId", runContext.render(this.syncId.toString())
                        )),
                "{}",
                SyncDetailsResponse.class
        );

        // Trigger sync run
        Run jobInfoRead = this.request(
                "POST",
                UriTemplate
                        .of("/api/v1/syncs/{syncId}/trigger")
                        .expand(Map.of(
                                "syncId", runContext.render(this.syncId.toString())
                        )),
                String.format(
                        "{\"fullResync\": %s}",
                        runContext.render(this.fullResynchronization.toString())
                ),
                Run.class
        );

        Long runId = jobInfoRead.getId();
        logger.info("[syncId={}] {}: Job triggered with runId {}", syncDetails.getId(), syncDetails.getSlug(), runId);

        if (!this.wait) {
            return Output.builder()
                .runId(runId)
                .build();
        }

        // wait for end
        RunDetails finalJobStatus = Await.until(
            throwSupplier(() -> {
                        RunDetailsResponse runDetailsResponse = this.request(
                                "GET",
                                UriTemplate
                                        .of("/api/v1/syncs/{syncId}/runs?runId={runId}")
                                        .expand(Map.of(
                                                "syncId", runContext.render(this.syncId.toString()),
                                                "runId", runId
                                        )),
                                "{}",
                                RunDetailsResponse.class
                        );

                // Check we correctly get one run
                if (runDetailsResponse.getData().isEmpty()) {
                    logger.warn("Could not find the triggered runId {}", runId);
                    throw new Exception("Failed : could not find the triggered runId : " + runId);
                }

                // Illegal state where we have more than 1 item
                if (runDetailsResponse.getData().size() > 1) {
                    logger.warn("Found several entries for runId {}", runId);
                    throw new Exception("Failed: found several runs with runId : " + runId);
                }

                RunDetails runDetails = runDetailsResponse.getData().get(0);
                sendLog(logger, syncDetails, runDetails);

                // ended
                if (ENDED_STATUS.contains(runDetails.getStatus())) {
                    return runDetails;
                }
                return null;
            }),
            STATUS_REFRESH_RATE,
            this.maxDuration
        );

        // handle failure
        if (!finalJobStatus.getStatus().equals(RunStatus.SUCCESS)) {
            String durationHumanized = DurationFormatUtils.formatDurationHMS(Duration.between(
                finalJobStatus.getFinishedAt(),
                finalJobStatus.getCreatedAt()
            ).toMillis());

            throw new RuntimeException("Failed run with status '" + finalJobStatus.getStatus() +
                "' after " +  durationHumanized + ": " + finalJobStatus.getStatus()
            );
        }

        // metrics
        runContext.metric(Counter.of("completionRatio", finalJobStatus.getCompletionRatio()));

        runContext.metric(Counter.of("rows.successfullyAdded", finalJobStatus.getSuccessfulRows().getAddedCount()));
        runContext.metric(Counter.of("rows.successfullyRemoved", finalJobStatus.getSuccessfulRows().getRemovedCount()));
        runContext.metric(Counter.of("rows.successfullyChanged", finalJobStatus.getSuccessfulRows().getChangedCount()));

        runContext.metric(Counter.of("rows.failedAdded", finalJobStatus.getFailedRows().getAddedCount()));
        runContext.metric(Counter.of("rows.failedRemoved", finalJobStatus.getFailedRows().getRemovedCount()));
        runContext.metric(Counter.of("rows.failedChanged", finalJobStatus.getFailedRows().getChangedCount()));

        return Output.builder()
            .runId(runId)
            .build();
    }

    private void sendLog(Logger logger, SyncDetailsResponse syncDetails, RunDetails run) {
        logger.info("[syncId={}] {}: [runId={}] is now {}", syncDetails.getId(), syncDetails.getSlug(), run.getId(), run.getStatus());
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "The runId of the sync created"
        )
        private final Long runId;
    }
}
