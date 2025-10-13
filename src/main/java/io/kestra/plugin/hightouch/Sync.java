package io.kestra.plugin.hightouch;

import io.kestra.core.http.HttpResponse;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.utils.Await;
import io.kestra.plugin.hightouch.models.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;

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
    title = "Trigger a Hightouch sync and wait for its completion."
)
@Plugin(
    examples = {
        @Example(
            full = true,
            title = "Run a Hightouch sync",
            code = """
                id: hightouch_sync
                namespace: company.team

                tasks:
                  - id: sync
                    type: io.kestra.plugin.hightouch.Sync
                    token: "{{ secret('HIGHTOUCH_API_TOKEN') }}"
                    syncId: 1127166
                """
        )
    }
)
public class Sync extends AbstractHightouchConnection implements RunnableTask<Sync.Output> {
    private static final List<RunStatus> ENDED_STATUS = List.of(
        RunStatus.FAILED,
        RunStatus.CANCELLED,
        RunStatus.SUCCESS,
        RunStatus.COMPLETED_WITH_ERRORS,
        RunStatus.WARNING,
        RunStatus.INTERRUPTED
    );

    private static final Duration STATUS_REFRESH_RATE = Duration.ofSeconds(1);

    @Schema(
        title = "The sync id to trigger run"
    )
    @NotNull
    private Property<Long> syncId;

    @Schema(
        title = "Whether to do a full resynchronization"
    )
    @Builder.Default
    private Property<Boolean> fullResynchronization = Property.ofValue(false);

    @Schema(
        title = "Whether to wait for the end of the run.",
        description = "Allowing to capture run status and logs"
    )
    @Builder.Default
    private Property<Boolean> wait = Property.ofValue(true);

    @Schema(
        title = "The max total wait duration"
    )
    @Builder.Default
    private Property<Duration> maxDuration = Property.ofValue(Duration.ofMinutes(5));

    @Builder.Default
    @Getter(AccessLevel.NONE)
    private transient Map<Integer, Integer> loggedLine = new HashMap<>();

    @Override
    public Sync.Output run(RunContext runContext) throws Exception {
        Logger logger = runContext.logger();

        final String syncId = runContext.render(this.syncId).as(Long.class).orElseThrow().toString();

        // Get details of sync to display slug
        HttpResponse<SyncDetailsResponse> syncDetails = this.request(
            "GET",
            String.format("/api/v1/syncs/%s", syncId),
            "{}",
            SyncDetailsResponse.class,
            runContext
        );

        // Trigger sync run
        HttpResponse<Run> jobInfoRead = this.request(
            "POST",
            String.format("/api/v1/syncs/%s/trigger", syncId),
            String.format(
                "{\"fullResync\": %s}",
                runContext.render(this.fullResynchronization).as(Boolean.class).orElse(false)
            ),
            Run.class,
            runContext
        );

        Long runId = jobInfoRead.getBody().getId();
        logger.info("[syncId={}] {}: Job triggered with runId {}", syncDetails.getBody().getId(), syncDetails.getBody().getSlug(), runId);

        if (!runContext.render(wait).as(Boolean.class).orElseThrow()) {
            return Output.builder()
                .runId(runId)
                .build();
        }

        RunDetails finalJobStatus = Await.until(
            throwSupplier(() -> {
                HttpResponse<RunDetailsResponse> runDetailsResponse = this.request(
                    "GET",
                    String.format("/api/v1/syncs/%s/runs?runId=%s", syncId, runId),
                    "{}",
                    RunDetailsResponse.class,
                    runContext
                );

                List<RunDetails> runs = runDetailsResponse.getBody().getData();

                if (runs.isEmpty()) {
                    logger.debug("[Hightouch] No runs found yet for syncId={} runId={} — retrying",
                        syncId, runId);
                    return null;
                }

                RunDetails runDetails = runs.stream()
                    .filter(r -> r.getId().equals(runId))
                    .findFirst()
                    .orElse(null);

                if (runDetails == null) {
                    logger.debug("[Hightouch] runId={} not yet visible in /runs response ({} total runs) — retrying",
                        runId, runs.size());
                    return null;
                }

                logger.info("[Hightouch] syncId={} runId={} current status='{}'",
                    syncId, runId, runDetails.getStatus());

                sendLog(logger, syncDetails.getBody(), runDetails);

                if (ENDED_STATUS.contains(runDetails.getStatus())) {
                    return runDetails;
                }

                return null;
            }),
            STATUS_REFRESH_RATE,
            runContext.render(this.maxDuration).as(Duration.class).orElseThrow()
        );

        if (!List.of(RunStatus.SUCCESS, RunStatus.COMPLETED_WITH_ERRORS, RunStatus.WARNING).contains(finalJobStatus.getStatus())) {
            var createdAt = finalJobStatus.getCreatedAt();
            var finishedAt = finalJobStatus.getFinishedAt();

            String durationHumanized = finishedAt != null
                ? DurationFormatUtils.formatDurationHMS(Duration.between(createdAt, finishedAt).toMillis())
                : "N/A";

            if (finishedAt == null) {
                logger.warn("Run {} has null finishedAt - duration set to N/A", finalJobStatus.getId());
            }

            throw new RuntimeException("Failed run with status '" + finalJobStatus.getStatus() +
                "' after " + durationHumanized + ": " + finalJobStatus.getStatus()
            );
        }

        if (finalJobStatus.getStatus() == RunStatus.COMPLETED_WITH_ERRORS
            || finalJobStatus.getStatus() == RunStatus.WARNING) {
            logger.warn("Run completed with errors (runId={}): {} failed rows", runId,
                finalJobStatus.getFailedRows());
        }

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
