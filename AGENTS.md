# Kestra Hightouch Plugin

## What

- Provides plugin components under `io.kestra.plugin.hightouch`.
- Includes classes such as `Sync`, `RunStatus`, `RunDetailsResponse`, `Run`.

## Why

- This plugin integrates Kestra with Hightouch.
- It provides tasks that trigger and monitor Hightouch syncs.

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
