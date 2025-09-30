package io.kestra.plugin.hightouch.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RunStatus {
    PROCESSING("processing"),
    QUEUED("queued"),
    FAILED("failed"),
    CANCELLED("cancelled"),
    SUCCESS("success"),
    QUERYING("querying"),
    WARNING("warning"),
    REPORTING("reporting"),
    INTERRUPTED("interrupted"),
    COMPLETED_WITH_ERRORS("Completed with errors");

    private final String value;

    RunStatus(String value) {
        this.value = value;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static RunStatus fromValue(String text) {
        for (RunStatus b : RunStatus.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        return null;
    }
}
