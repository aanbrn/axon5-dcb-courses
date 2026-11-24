---
status: "accepted"
date: "2026-11-23"
decision-makers: ["repository author"]
---

# ADR-0008: Container images are built with Cloud Native Buildpacks, and the frontend image serves a prebuilt `dist/`

## Context and Problem Statement

The system ships container images for the Spring Boot services and for the frontend (CICD-02, CICD-10). The project
decided to build them with Cloud Native Buildpacks (Paketo) and not with Jib or Dockerfiles. Spike 005 checked, on an
Apple Silicon machine with colima, what that choice needs in practice: how a static single-page application is served
with a client-route fallback and a proxy that streams server-sent events, how a Boot image gets sensible JVM defaults, and
what the local container engine must provide.

The images themselves are built in Phase 6. This ADR records the constraints that the spike found, so Phase 6 starts from
working settings (decision D-35).

## Decision Drivers

- No Node.js and no pnpm inside the frontend image, and one build toolchain (Gradle) for every image.
- A proxy for `/api` that never buffers server-sent events.
- A missing asset must return 404, not the index page: a masked 404 hides a broken deployment.
- Build-time and run-time defaults must be documented, because the platform hides them from the image configuration.
- The same image build must work on developer machines and on CI runners.

## Considered Options

- Frontend: a Gradle-built `dist/` plus a custom, templated `nginx.conf`, served by the Paketo Web Servers buildpack
  (chosen).
- Frontend: the same `dist/` with the nginx configuration generated from `BP_WEB_SERVER_*` and an include file.
- Frontend: a build of the frontend inside the buildpack, with Node and pnpm in the build image.
- The `pack` CLI as the build tool.
- Boot images through the Spring Boot plugin's `bootBuildImage` (chosen) with the Paketo defaults described below.

## Decision Outcome

Chosen option: "a prebuilt `dist/` with a custom `nginx.conf` on the Paketo Web Servers buildpack, built through Gradle",
and "`bootBuildImage` for the Boot services", because they use one toolchain, put no Node in an image and keep every
nginx and JVM setting visible in the repository. The settings that the spike proved:

### Frontend image

- A Gradle `Zip` task packs `dist/`, `nginx.conf` and the vendored nginx upstream `conf/mime.types`, and a
  `BootBuildImage` task builds the image from the archive with the builder `paketobuildpacks/ubuntu-noble-builder`, the
  `web-servers` buildpack and the health checker. The Spring Boot plugin is applied for the task type only. No `pack`
  CLI is needed.
- The run image is the full `ubuntu-noble-run`. The tiny run image builds an nginx image that fails at start with
  `libcrypt.so.1: cannot open shared object file`.
- The custom `nginx.conf` contains: `listen {{port}}`, temp paths through `{{ tempDir }}` (the working directory is not
  writable), no `pid` directive (the buildpack passes its own), `root dist`,
  `try_files $uri $uri/ /index.html` with `Cache-Control: no-cache` for the fallback, `/assets/` with
  `public, max-age=31536000, immutable` and `=404` for a missing file, a deny rule for dotfiles, a static `/healthz`, and
  `/api/` proxied to `{{env "API_UPSTREAM"}}` with HTTP/1.1, an empty `Connection` header, `proxy_buffering off`,
  `proxy_cache off` and `proxy_read_timeout 1h`. The defaults `BPE_DEFAULT_API_UPSTREAM` and
  `BPE_DEFAULT_PROXY_BUFFERING` are set on the image.
- `proxy_buffering off` is kept as an explicit guarantee, not as a demonstrated necessity: the spike's control run with
  buffering on delivered the small events just as promptly. An idle stream and the one-hour read timeout were not
  exercised.

### Boot images

- `BP_JVM_VERSION=25`.
- The JVM memory calculator reserves the code cache and thread stacks first: at a 512 MB limit the defaults give a heap of
  about 120 MB. `BPE_DEFAULT_BPL_JVM_THREAD_COUNT=50` and `BPE_DEFAULT_BPL_JAVA_NMT_ENABLED=false` set better defaults,
  and `BPE_DELIM_JAVA_TOOL_OPTIONS=" "` with `BPE_PREPEND_JAVA_TOOL_OPTIONS=-XX:ReservedCodeCacheSize=64M` shrinks the
  code cache. The prepend form is the working one: **`BPE_DEFAULT_JAVA_TOOL_OPTIONS` is ignored**, because the
  Liberica buildpack sets `JAVA_TOOL_OPTIONS` itself. A `JAVA_TOOL_OPTIONS` given at run time still wins and the
  calculator respects it. A thread count that does not fit the limit fails fast at start with a clear message.
- `-XX:+ExitOnOutOfMemoryError` is already added by the buildpack, so it is not repeated.
- `BP_HEALTH_CHECKER_ENABLED=true` adds the health checker. `docker exec` bypasses the launcher, so image defaults of the
  form `BPE_DEFAULT_THC_*` are invisible to it. `THC_PORT` and `THC_PATH` are set as container environment in compose, and
  the check runs `/workspace/health-check`. A health-checker buildpack in the list without `BP_HEALTH_CHECKER_ENABLED`
  fails the detection of the whole group.
- `BPE_*` defaults never appear in the image's `Config.Env`, because they live in layer files applied by the CNB launcher.
  They are documented through image labels: `BP_IMAGE_LABELS` and `BP_OCI_DESCRIPTION` put
  `io.github.aanbrn.courses.env.*` labels on the image (INFRA-17).
- The builder, run image and buildpacks are pinned by digest in the version catalog in Phase 6 (INFRA-09). The spike used
  `latest` only to learn the settings.

### Local prerequisites

Two defects of the local engine broke the spike's first builds. Neither is part of the repository, so `doctor` checks the
DNS setting and prints the fix, and reports the image store:

- **colima DNS.** The built-in resolver of colima never answers AAAA queries, and Go-based tools wait for both A and AAAA,
  so every lookup took 10 seconds and the buildpacks timed out downloading the JRE. The fix is explicit resolvers,
  `colima start --dns <resolver> --dns <resolver>`. Tracked as UP-0011.
- **Docker 29 containerd image store.** Exporting an image onto `ubuntu-noble-run` fails with `no suitable export target
found` on both `pack` and `BootBuildImage`. The fix is the classic image store (`overlay2`): in colima
  `docker.features.containerd-snapshotter: false`. Tracked as UP-0012, which is fixed upstream in Docker 29.9.0. The
  workaround was removed on 2026-11-24 after the export of the noble run image succeeded on a Docker 29.10.0 engine
  with the containerd store: both image stores are accepted, and the containerd store needs Docker 29.9.0 or newer
  (`docs/tooling.md`). The CI image job (CICD-02) needs such an engine when its runner uses the containerd store,
  because the defect can hit CI runners too.

### Consequences

- Good, because one Gradle toolchain builds every image, reproducibly, and no image contains Node, pnpm or a shell
  that the application does not need.
- Good, because the nginx behaviour is explicit and testable: a missing asset is a 404, the SPA fallback is not cached
  and the API proxy streams.
- Bad, because the project owns an `nginx.conf` and a vendored `mime.types`, and must keep them current with the
  buildpack's nginx.
- Bad, because the environment defaults are invisible in `Config.Env`, so the labels are the only documentation on the
  image, and the health check needs the same variables again in compose.
- Bad, because a first image build takes about four minutes cold (about 20 seconds warm) and needs a Docker daemon, which
  is why the images are built by a separate task and CI job, not by `check` (decision for Phase 6).
- Bad, because the colima DNS defect (UP-0011) must be fixed by hand until upstream moves.

### Confirmation

- Phase 6 builds the images in CI and is to check the settings above against the spike's observations: the nginx cases
  (fallback, immutable assets, 404 for a missing asset, `/healthz`, the streaming proxy), the health check and the
  labels. Until then the spike is the evidence, and no test in this repository builds an image.
- `scripts/doctor.sh` has the `colima` check (DNS, carrying the `UPSTREAM` marker of UP-0011) and the `image-store`
  check, which reports the active image store.
- A Konsist rule keeps image names out of source literals, so every image comes from the version catalog.

## Pros and Cons of the Options

### A prebuilt `dist/` with a custom `nginx.conf`

- Good, because it controls the 404 for missing assets, cache headers per location, the templated upstream and buffering.
- Bad, because the `mime.types` of the buildpack sample is stale (`application/x-javascript`) and must be vendored, and the
  temp paths must be set through `{{ tempDir }}`.

### Generated configuration (`BP_WEB_SERVER_*` and an include file)

- Good, because it needs no nginx boilerplate and it denies dotfiles.
- Bad, because a missing `/assets/x.js` returns `index.html` with status 200 and masks a broken deployment, there are no
  cache headers, `/healthz` is served as `application/octet-stream`, and the include file is static, so the upstream is
  hard-coded.

### Building the frontend inside the buildpack

- Good, because there is one step from sources to image.
- Bad, because it needs Node and pnpm support in the buildpack and a second place that builds the frontend. The Gradle
  Node plugin already builds `dist/` once.

### The `pack` CLI

- Good, because it is the standard tool.
- Bad, because it is a second toolchain next to Gradle and hits the same export failure on the Docker 29 containerd store.

## More Information

- Spike: `.planning/spikes/005-paketo-web-and-boot-images/README.md`, verdict VALIDATED after the two environment fixes.
- Decisions: D-35 (prebuilt `dist/`, Web Servers buildpack, no pnpm in the image); the deferred image builds belong to
  Phase 6 (CICD-02, CICD-10).
- Upstream records: [UP-0011](../upstream/0011-colima-aaaa-dns.md) (colima DNS, not yet reported) and
  [UP-0012](../upstream/0012-docker-containerd-platform-export.md) (containerd image store, workaround removed).
- Revisit the settings when Paketo changes the Liberica or health-checker buildpacks, when a Spring Boot or Paketo release
  makes `BPE_DEFAULT_JAVA_TOOL_OPTIONS` work, and when UP-0011 lets the `colima` check of `doctor` go.
