package io.kestra.plugin.hightouch.models;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Value
@Jacksonized
@SuperBuilder
public class RunDetails {
    Long id;
    ModifiedRows plannedRows;
    ModifiedRows successfulRows;
    ModifiedRows failedRows;
    Long querySize;
    RunStatus status;
    Instant createdAt;
    Instant startedAt;
    Instant finishedAt;
    Long completionRatio;
    String error;
}
