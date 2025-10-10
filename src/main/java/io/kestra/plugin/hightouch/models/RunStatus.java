package io.kestra.plugin.hightouch.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    COMPLETED_WITH_ERRORS("completed_with_errors");

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
        if (text == null) return null;

        String n = normalize(text);

        RunStatus match = ALIASES.get(n);
        if (match != null) {
            return match;
        }

        for (RunStatus b : RunStatus.values()) {
            if (normalize(b.value).equals(n)) {
                return b;
            }
        }

        return null;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim()
            .toLowerCase()
            .replace('-', '_')
            .replace(' ', '_');
    }

    private static final Map<String, RunStatus> ALIASES = Collections.unmodifiableMap(buildAliases());

    private static Map<String, RunStatus> buildAliases() {
        Map<String, RunStatus> m = new HashMap<>();

        put(m, "processing", PROCESSING);
        put(m, "queued", QUEUED);
        put(m, "failed", FAILED);
        put(m, "cancelled", CANCELLED);
        put(m, "canceled", CANCELLED);
        put(m, "success", SUCCESS);
        put(m, "querying", QUERYING);
        put(m, "warning", WARNING);
        put(m, "reporting", REPORTING);
        put(m, "interrupted", INTERRUPTED);
        put(m, "completed_with_errors", COMPLETED_WITH_ERRORS);

        put(m, "completed", SUCCESS);
        put(m, "completed with errors", COMPLETED_WITH_ERRORS);
        put(m, "aborted due to fatal error", FAILED);
        put(m, "aborted_due_to_fatal_error", FAILED);
        put(m, "cancelled by user", CANCELLED);
        put(m, "canceled by user", CANCELLED);

        put(m, "complete", SUCCESS);
        put(m, "completed_successfully", SUCCESS);
        put(m, "completed_success", SUCCESS);
        put(m, "completed_errors", COMPLETED_WITH_ERRORS);
        put(m, "completed_with_error", COMPLETED_WITH_ERRORS);

        return m;
    }

    private static void put(Map<String, RunStatus> m, String key, RunStatus v) {
        m.put(normalize(key), v);
    }
}
