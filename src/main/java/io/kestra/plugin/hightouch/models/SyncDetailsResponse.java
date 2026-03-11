package io.kestra.plugin.hightouch.models;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

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
