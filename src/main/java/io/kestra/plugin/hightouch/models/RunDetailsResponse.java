package io.kestra.plugin.hightouch.models;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import java.util.List;

@Value
@Jacksonized
@SuperBuilder
public class RunDetailsResponse {
    RunDetails data;
    Boolean hasMore;
}
