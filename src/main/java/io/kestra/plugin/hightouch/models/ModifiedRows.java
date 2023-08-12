package io.kestra.plugin.hightouch.models;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@SuperBuilder
public class ModifiedRows {
    String addedCount;
    String changedCount;
    String removedCount;
}
