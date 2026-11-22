---
spike: 005
idea: phase2-runtime-dcb-foundations
name: paketo-web-and-boot-images
type: comparison
validates: "Given a prebuilt dist/ and a minimal Boot service, when Paketo builds both images on colima, then nginx serves the SPA with fallback and an SSE-safe /api proxy, and the Boot image runs with working BP_*/BPE_* defaults"
verdict: VALIDATED
related: [003]
tags: [paketo, buildpacks, nginx, sse, bootBuildImage, colima, docker29]
---

# Spike 005: Paketo — Static Web Image via nginx and a Boot Image

## What This Validates
Given a prebuilt `dist/` (no Node in the image) and a minimal WebFlux Boot service with an SSE endpoint, when both
images are built with Paketo on colima (aarch64), then: nginx serves the SPA with client-route fallback, hashed assets
with immutable caching, a static health endpoint, and an `/api` proxy that streams SSE; the Boot image starts with
working build-time `BP_*` and runtime-default `BPE_*` settings, a shell-less health check and documented labels.
Compared: **custom `nginx.conf`** vs **buildpack-generated config** (`BP_WEB_SERVER_*` + include file).

## Research
- Paketo nginx buildpack README: with an `nginx.conf` in the app root the buildpack uses it and renders `{{port}}`,
  `{{env "NAME"}}`, `{{module "…"}}` (and `{{ tempDir }}`, seen in its own generated config) at launch. Without one,
  `BP_WEB_SERVER=nginx`, `BP_WEB_SERVER_ROOT`, `BP_WEB_SERVER_ENABLE_PUSH_STATE` and
  `BP_WEB_SERVER_INCLUDE_FILE_PATH` generate the config. Sample `paketo-buildpacks/samples/web-servers/nginx-sample`.
- Builder architectures (Docker Hub, 2026-11-22): `ubuntu-noble-builder`, `builder-noble-java-tiny`, `web-servers`,
  `health-checker`, `image-labels` are amd64 + arm64; `builder-jammy-base` is amd64 only. Noble builders run natively
  on colima aarch64.
- Spring Boot 4.1 `bootBuildImage` default builder: `paketobuildpacks/builder-noble-java-tiny` (run image
  `ubuntu-noble-run-tiny`). Docker is found through the active docker context (`colima`) — no `docker {}` block needed.

| Approach | Pros | Cons | Verdict |
|----------|------|------|---------|
| Custom `nginx.conf` (+ vendored `mime.types`) | full control: 404 for missing assets, `Cache-Control` per location, env-templated upstream and buffering, dotfile deny | must vendor `mime.types` and set temp paths via `{{ tempDir }}` | **chosen** |
| Generated (`BP_WEB_SERVER_*` + `proxy.conf`) | zero nginx boilerplate, denies dotfiles | missing `/assets/x.js` returns `index.html` with 200 (masks broken deploys), no cache headers, `/healthz` served as `application/octet-stream`, include file is static (upstream hard-coded) | rejected |
| `pack` CLI | standard CNB tool | second toolchain; same export failure on Docker 29 containerd store | not needed |
| `BootBuildImage` task over a Gradle `Zip` of `dist/` + config | same Gradle/buildpack toolchain as services, `pack` not required | Spring Boot plugin applied only for the task type | **chosen** |

## How to Run
```bash
./gradlew -p .planning/spikes :005-api:bootBuildImage :005-web:customWebImage :005-web:generatedWebImage
cd .planning/spikes/005-paketo-web-and-boot-images
docker compose up -d --wait
curl -i localhost:18081/courses/123          # SPA fallback, Cache-Control: no-cache
curl -N localhost:18081/api/stream           # 5 SSE events, 0.5 s apart
PROXY_BUFFERING=on docker compose up -d --wait web-custom   # control run
docker compose down
```

## What to Expect
All three services healthy (thc). Custom variant: `/` and `/courses/123` → `index.html` (`no-cache`);
`/assets/app-3f9a1c.js` → 200 `application/javascript`, `public, max-age=31536000, immutable`; `/assets/missing.js`
→ 404; `/.hidden` → 404; `/healthz` → `ok`; `/api/hello` → JSON from Boot; SSE events at ≈0.5/1.0/1.5/2.0/2.5 s.

## Investigation Trail
1. **colima DNS (environment blocker).** First `bootBuildImage`: `unable to get dependency BellSoft Liberica JRE` /
   `lookup github.com: i/o timeout`. Busybox resolves; a Go program in a container takes **10.0 s per lookup**:
   colima's built-in forwarder `192.168.5.1` never answers AAAA, and Go waits for both A and AAAA. `network=host`, a
   user-defined network (8 s) and `GODEBUG` did not help; `--dns 100.64.0.1` / `8.8.4.4` → 130 ms. **Fix (applied
   with the author's approval): `colima start --dns 100.64.0.1 --dns 8.8.4.4`** (persisted as `network.dns` in
   `~/.colima/default/colima.yaml`); lookups then 105–121 ms. Not a CI issue. `doctor` check candidate: time an AAAA
   lookup in a container.
2. **Boot image** (3 m 52 s cold, ~20 s warm): arm64, user `1002:1001`, `created=1980-01-01` (reproducible), 351 MB,
   starts in 1.5 s, readiness UP.
3. **`BPE_*` and `Config.Env`**: `BPE_*` live in layer env files applied by the CNB launcher; **they never appear in
   `Config.Env`** — answers the INFRA-17 question: env defaults must be documented through labels.
   `BP_IMAGE_LABELS` + `BP_OCI_DESCRIPTION` (Image Labels buildpack, already in the java composite) put
   `io.github.aanbrn.courses.env.*` labels on the image.
4. **JVM memory**: defaults at `-m 512m` → `-Xmx122731K` because of `ReservedCodeCacheSize=240M` (+ 250 threads by
   default). `BPE_DEFAULT_BPL_JVM_THREAD_COUNT=50` works; `BPE_DEFAULT_JAVA_TOOL_OPTIONS` is **ignored** (Liberica sets
   `JAVA_TOOL_OPTIONS` itself); `BPE_PREPEND_JAVA_TOOL_OPTIONS=-XX:ReservedCodeCacheSize=64M` (+ `BPE_DELIM_…=" "`)
   works → `-Xmx302955K`, and a runtime `JAVA_TOOL_OPTIONS=-XX:ReservedCodeCacheSize=128M` still wins (appears later;
   calculator respects it → `-Xmx237419K`). `-XX:+ExitOnOutOfMemoryError` is already added by Liberica (my append
   duplicated it). NMT is on by default; `BPE_DEFAULT_BPL_JAVA_NMT_ENABLED=false` turns it off. Runtime
   `BPL_JVM_THREAD_COUNT=200` at 512 MB fails fast: `unable to calculate memory configuration … 555156K > 512M`.
5. **Health checker** (`paketobuildpacks/health-checker` 2.14.4, thc 0.39.0): no `health-check` process type, only
   `/workspace/health-check`. `docker exec` bypasses the launcher, so image `BPE_DEFAULT_THC_*` are invisible → 404 on
   `/`. Set `THC_PORT`/`THC_PATH` as **container env in compose**; then exit 0. If the health-checker buildpack is in
   the list without `BP_HEALTH_CHECKER_ENABLED=true`, detection fails for the whole group ("No buildpack groups passed").
6. **nginx on the tiny run image** builds but fails at start: `libcrypt.so.1: cannot open shared object file` → the
   web image needs the full `ubuntu-noble-run`.
7. **Docker 29 containerd image store (environment blocker).** Exporting onto `ubuntu-noble-run` fails for both `pack`
   0.40.11 and `BootBuildImage`: `no suitable export target found: image … does not provide the specified platform
   (linux/arm64)`. Reproduced with plain `docker save --platform linux/arm64 paketobuildpacks/ubuntu-noble-run:latest`
   (the tiny run image saves fine; re-pull, arm64-digest pin and `publish` to a local registry do not help). Known:
   moby#52193, docker/cli#6457; also hits CI runners using the containerd store (kubeflow/pipelines#15118,
   superfly/flyctl#5360). **Fix (applied with approval): `docker.features.containerd-snapshotter: false`** in
   colima.yaml (storage driver `overlay2`); all three images then build. Backup of the previous colima.yaml:
   scratchpad only (not kept).
8. **Custom nginx.conf fixes found by running it**: duplicate `pid` (the buildpack passes `-g 'pid /tmp/nginx.pid;'`);
   `mkdir() "/workspace/client_body_temp" failed (13)` → temp paths via `{{ tempDir }}`; `uwsgi_temp_path` /
   `scgi_temp_path` are unknown directives in this nginx build; sample `mime.types` is stale
   (`application/x-javascript`) → vendor nginx upstream `conf/mime.types`.
9. **SSE**: through nginx with `proxy_buffering off` events arrive at 0.52/1.02/1.52/2.03/2.53 s (same as direct).
   Control with `proxy_buffering on` arrived equally promptly for these small events (no `X-Accel-Buffering` header
   involved) — the control did not reproduce buffering, so `off` stays as the explicit guarantee rather than a
   demonstrated necessity. Idle-stream timeout (`proxy_read_timeout 1h`) not exercised.

## Results
**VALIDATED** (after two local environment fixes the author approved).
- Frontend image: Gradle `Zip` of `dist/` + `nginx.conf` + `mime.types` → `BootBuildImage` task with builder
  `paketobuildpacks/ubuntu-noble-builder`, buildpacks `paketo-buildpacks/web-servers` + health-checker; full
  `ubuntu-noble-run` (nginx needs libcrypt). No Node, no pnpm, no `pack`.
- Custom `nginx.conf` essentials: `listen {{port}}`, `{{ tempDir }}` temp paths, no `pid`, `root dist`,
  `try_files $uri $uri/ /index.html` with `no-cache`, `/assets/` immutable + `=404`, dotfile deny, `/healthz`,
  `/api/` → `{{env "API_UPSTREAM"}}` with HTTP/1.1, `Connection ""`, `proxy_buffering off`, `proxy_cache off`,
  `proxy_read_timeout 1h`. Image defaults via `BPE_DEFAULT_API_UPSTREAM`, `BPE_DEFAULT_PROXY_BUFFERING`.
- Boot image env defaults: `BP_JVM_VERSION=25`; `BPE_DEFAULT_BPL_JVM_THREAD_COUNT=50`;
  `BPE_DEFAULT_BPL_JAVA_NMT_ENABLED=false`; `BPE_DELIM_JAVA_TOOL_OPTIONS=" "` +
  `BPE_PREPEND_JAVA_TOOL_OPTIONS=-XX:ReservedCodeCacheSize=64M`; `BP_HEALTH_CHECKER_ENABLED=true`;
  `BP_IMAGE_LABELS`/`BP_OCI_*` for documentation; compose sets `THC_PORT`/`THC_PATH` and uses
  `/workspace/health-check`.
- `doctor` candidates: colima DNS (AAAA timing) and Docker image store (`docker info` driver-type
  `io.containerd.snapshotter.v1` → warn with the fix). CI: pin the runner image or disable the containerd snapshotter
  for the image job if the runner uses it.
- Pin builder/run/buildpack images by digest in the catalog (INFRA-09); `latest` was used here only for the spike.
