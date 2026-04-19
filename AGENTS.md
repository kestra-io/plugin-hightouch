# Kestra Hightouch Plugin

## What

- Provides plugin components under `io.kestra.plugin.hightouch`.
- Includes classes such as `Sync`, `RunStatus`, `RunDetailsResponse`, `Run`.

## Why

- What user problem does this solve? Teams need to trigger and monitor Hightouch syncs from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Hightouch steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Hightouch.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `hightouch`

### Key Plugin Classes

- `io.kestra.plugin.hightouch.Sync`

### Project Structure

```
plugin-hightouch/
├── src/main/java/io/kestra/plugin/hightouch/models/
├── src/test/java/io/kestra/plugin/hightouch/models/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
