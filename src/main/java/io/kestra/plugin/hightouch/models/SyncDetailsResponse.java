package io.kestra.plugin.hightouch.models;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;
import java.util.List;

@Value
@Jacksonized
@SuperBuilder
public class SyncDetailsResponse {
    Long id;
    String slug;
    Long workspaceId;
    Instant createdAt;
    Instant updatedAt;
    Long destinationId;
    Long modelId;
    Map<String, Object> configuration;
    Map<String, Object> schedule;
    RunStatus status;
    Boolean disabled;
    Instant lastRunAt;
    List<String> referencedColumns;
    String primaryKey;
    Map<String, Object> externalSegment;
}
