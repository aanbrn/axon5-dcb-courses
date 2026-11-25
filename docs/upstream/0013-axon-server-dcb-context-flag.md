---
id: "UP-0013"
product: "image-axonserver"
product-version: "2026.1.8"
status: "open"
workaround: false
upstream: []
reported: null
updated: "2026-11-25"
---

# UP-0013: GET /v1/public/context reports dcbContext false for the DCB default context

## Symptom

An Axon Server 2026.1.8 that is started with a DCB `default` context (`AXONIQ_AXONSERVER_STANDALONE_DCB=true`) logs
`Creating DCB context: default` and serves DCB operations correctly, but the public REST endpoint
`GET /v1/public/context` reports `"dcbContext": false` for that context. The endpoint `GET /v2/contexts` reports
`"dcbContext": true` for the same context. A check or a dashboard that reads the v1 endpoint concludes that the
context is not a DCB context although it is.

Impact on this repository: none beyond a trap for tooling. No code reads the flag, so there is no workaround.

## Minimal reproduction

```sh
docker run -d --rm --name axonserver-dcb -p 8024:8024 -p 8124:8124 \
  -e AXONIQ_AXONSERVER_STANDALONE_DCB=true -e AXONIQ_AXONSERVER_DEVMODE_ENABLED=true \
  docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot

# wait until the log shows "Creating DCB context: default" (the list is empty until then)
curl -s http://localhost:8024/v1/public/context
curl -s http://localhost:8024/v2/contexts
docker stop axonserver-dcb
```

The first request returns, among other entries, `{"context":"default","dcbContext":false,...}`; the second returns
`[{"contextName":"default","dcbContext":true,...}]`. The same image and the same run were used for both requests.

## Root cause

Unknown. Axon Server is not open source, so the reason cannot be traced to a source line. The two endpoints disagree
about one context, and the server log and the behavior of DCB appends agree with the v2 endpoint. The v1 endpoint
looks like it was not updated for DCB contexts that the server creates itself at startup.

## Workaround

None, and none is needed: the repository does not read `dcbContext`. It proves DCB functionally instead, with a
tag-based conditional append that must be rejected when the criteria overlap an outdated consistency marker, in the
integration test `ConditionalAppendIntegrationTest`. The compose health check uses the actuator health endpoint and
not the context list.

## Removal criteria

Not applicable while there is no workaround. The record can move to `fixed-upstream` when an Axon Server release
reports `"dcbContext": true` for a DCB context on `GET /v1/public/context`, or documents that the endpoint is not
authoritative for DCB.

## How to verify removal

Run the commands of the reproduction against the new image: the v1 response for `default` carries `"dcbContext":true`.
Then bump the image in `gradle/libs.versions.toml` and run `mise x -- ./gradlew :command-service:integrationTest` for the
functional proof.

## Upstream issue draft

Target: the AxonIQ forum, category Axon Server, or AxonIQ support, because Axon Server has no public issue tracker. Not
published; publication needs the approval of the author (plan 02-22).

Publication status (2026-11-25): the forum refused a second topic while the UP-0006 post was pending, and the account is
on hold until staff review it. The author wrote to AxonIQ developer relations the same day and mentioned this report.
It is posted, under the forum posting rule of ADR-0013 ("Publication (UPS-03)"), once the hold is lifted; that step is a
follow-up outside Phase 2.

### Axon Server 2026.1.8: GET /v1/public/context reports dcbContext false for a DCB default context

#### Environment and versions

- Product and version: Axon Server 2026.1.8, image `docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot`
- Related products and versions: none
- JDK, operating system and build tool: the JDK of the image, Docker on macOS and Linux

#### Steps to reproduce

1. Start the image with `AXONIQ_AXONSERVER_STANDALONE_DCB=true` and `AXONIQ_AXONSERVER_DEVMODE_ENABLED=true`, publishing
   port 8024.
2. Wait for the log line `Creating DCB context: default`.
3. Request `GET /v1/public/context` and `GET /v2/contexts`.

#### Expected behavior

Both endpoints describe the `default` context as a DCB context, because the log says so and DCB operations work.

#### Actual behavior

`/v1/public/context` returns `{"context":"default","dcbContext":false,...}`, while `/v2/contexts` returns
`{"contextName":"default","dcbContext":true,...}`.

#### Minimal reproduction

```sh
docker run -d --rm --name axonserver-dcb -p 8024:8024 -p 8124:8124 \
  -e AXONIQ_AXONSERVER_STANDALONE_DCB=true -e AXONIQ_AXONSERVER_DEVMODE_ENABLED=true \
  docker.axoniq.io/axoniq/axonserver:2026.1.8-nonroot
curl -s http://localhost:8024/v1/public/context
curl -s http://localhost:8024/v2/contexts
```

#### Analysis

No source is available. The v2 endpoint and the server log agree with each other and with the behavior of a tag-based
conditional append, so the v1 field looks stale for contexts that the server creates at startup in DCB mode.

#### Proposed fix

Report `dcbContext` consistently on both endpoints, or document that `/v1/public/context` is not authoritative for DCB
and point to `/v2/contexts`.

_This report was prepared with an AI coding agent; I reviewed and approved the findings and the text before posting._
