package io.kestra.plugin.hightouch.connections;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.Await;
import io.kestra.plugin.hightouch.AbstractHightouchConnection;
import io.kestra.plugin.hightouch.models.Run;
import io.kestra.plugin.hightouch.models.RunDetails;
import io.kestra.plugin.hightouch.models.RunDetailsResponse;
import io.kestra.plugin.hightouch.models.RunStatus;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpMethod;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.uri.UriTemplate;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.kestra.core.utils.Rethrow.throwSupplier;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Trigger HighTouch sync"
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

    @Schema(
        title = "The sync id to trigger run"
    )
    @PluginProperty(dynamic = true)
    private String syncId;

    @Schema(
            title = "Full Resynchronization"
    )
    @PluginProperty(dynamic = true)
    @Builder.Default
    private Boolean fullResynchronization = false;

    @Schema(
        title = "Wait for the end of the run.",
        description = "Allowing to capture run status & logs"
    )
    @PluginProperty
    @Builder.Default
    Boolean wait = true;

    @Schema(
        title = "The max total wait duration"
    )
    @PluginProperty
    @Builder.Default
    Duration maxDuration = Duration.ofMinutes(60);

    @Builder.Default
    @Getter(AccessLevel.NONE)
    private transient Map<Integer, Integer> loggedLine = new HashMap<>();

    @Override
    public Sync.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        // create sync
        HttpResponse<Run> syncResponse = this.request(
            runContext,
            HttpRequest
                .create(
                    HttpMethod.POST,
                    UriTemplate
                        .of("/api/v1/syncs/trigger/")
                        .toString()
                )
                .body(Map.of(
                        "syncId", runContext.render(this.syncId),
                        "fullResync", this.fullResynchronization)),
            Argument.of(Run.class)
        );

        Run jobInfoRead = syncResponse.getBody().orElseThrow(() -> new IllegalStateException("Missing body on trigger"));

        logger.info("Job status {} with response: {}", syncResponse.getStatus(), jobInfoRead);
        Long runId = jobInfoRead.getId();

        if (!this.wait) {
            return Output.builder()
                .runId(runId)
                .build();
        }

        // wait for end
        RunDetails finalJobStatus = Await.until(
            throwSupplier(() -> {
                HttpResponse<RunDetailsResponse> detailsResponse = this.request(
                    runContext,
                    HttpRequest
                        .create(
                            HttpMethod.POST,
                            UriTemplate
                                .of("/api/v1/syncs/{syncId}/?runId={runId}")
                                .expand(Map.of(
                                        "syncId", runContext.render(this.syncId),
                                        "runId", runId
                                ))
                        ),
                    Argument.of(RunDetailsResponse.class)
                );

                RunDetailsResponse runDetailsResponse = detailsResponse.getBody().orElseThrow(() -> new IllegalStateException("Missing body on trigger"));

                // Check we correctly get one Run
                if (runDetailsResponse.getData().size() != 1) {
                    logger.warn("Could not find the triggered runId {}", runId);
                }

                RunDetails runDetails = runDetailsResponse.getData().get(0);
                sendLog(logger, runDetails);

                // ended
                if (ENDED_STATUS.contains(runDetails.getStatus())) {
                    return runDetails;
                }
                return null;
            }),
            Duration.ofSeconds(1),
            this.maxDuration
        );

        // failure message
        /*
        finalJobStatus.getAttempts()
            .stream()
            .map(AttemptInfo::getAttempt)
            .map(Attempt::getFailureSummary)
            .filter(Objects::nonNull)
            .forEach(attemptFailureSummary -> logger.warn("Failure with reason {}", attemptFailureSummary));
         */

        // handle failure
        if (!finalJobStatus.getStatus().equals(RunStatus.SUCCESS)) {
            String durationHumanized = DurationFormatUtils.formatDurationHMS(Duration.between(
                finalJobStatus.getFinishedAt(),
                finalJobStatus.getCreatedAt()
            ).toMillis());

            throw new Exception("Failed run with status '" + finalJobStatus.getStatus() +
                "' after " +  durationHumanized + ": " + finalJobStatus.getStatus()
            );
        }

        // metrics
        // runContext.metric(Counter.of("attempts.count", finalJobStatus.getAttempts().size()));
        runContext.metric(Counter.of("rows.successfullyAdded", finalJobStatus.getSuccessfulRows().getAddedCount()));
        runContext.metric(Counter.of("rows.successfullyRemoved", finalJobStatus.getSuccessfulRows().getRemovedCount()));

        return Output.builder()
            .runId(runId)
            .build();
    }

    private void sendLog(Logger logger, RunDetails run) {
        logger.info("[Run {}]: {}", run.getId(), run.getCompletionRatio());
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
