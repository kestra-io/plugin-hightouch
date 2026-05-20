# How to use the Hightouch plugin

Trigger Hightouch sync runs from Kestra flows and optionally wait for completion.

## Authentication

Set `token` to your Hightouch API bearer token (required). Store it in a [secret](https://kestra.io/docs/concepts/secret) and apply it globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

`Sync` triggers a sync run by `syncId` (required, numeric) and waits for completion by default (`wait: true`). Set `fullResynchronization: true` to force a full reload rather than an incremental sync. Cap wait time with `maxDuration` (default 5 minutes). The output includes `runId` and `metadata` with row-level success/failure counts and final status.
