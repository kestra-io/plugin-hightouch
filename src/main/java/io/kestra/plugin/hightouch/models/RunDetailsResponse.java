package io.kestra.plugin.hightouch.models;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import java.util.List;

/**
 * see <a href="https://hightouch.com/docs/api-reference#operation/ListSyncRuns">ListSyncRuns</a>
 */
@Value
@Jacksonized
@SuperBuilder
public class RunDetailsResponse {
    List<RunDetails> data;
    Boolean hasMore;
}
