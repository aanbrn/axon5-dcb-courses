# Container Images with Paketo (Frontend nginx and Boot Services)

## Requirements

- Local Docker (colima) runs with explicit `network.dns` and the classic image store (`containerd-snapshotter: false`).
- Spike code lives in `.planning/spikes/`, built by a standalone Gradle build whose versions come from the main catalog.

## How to Build It

1. **Boot service** — `bootBuildImage` (default builder `paketobuildpacks/builder-noble-java-tiny`, multi-arch; Docker
   found from the active docker context, no `docker {}` block):

   ```kotlin
   tasks.bootBuildImage {
       environment.set(mapOf(
           "BP_JVM_VERSION" to "25",
           "BPE_DEFAULT_BPL_JVM_THREAD_COUNT" to "50",
           "BPE_DEFAULT_BPL_JAVA_NMT_ENABLED" to "false",
           "BPE_DELIM_JAVA_TOOL_OPTIONS" to " ",
           "BPE_PREPEND_JAVA_TOOL_OPTIONS" to "-XX:ReservedCodeCacheSize=64M",
           "BP_HEALTH_CHECKER_ENABLED" to "true",
           "BP_OCI_DESCRIPTION" to "…",
           "BP_IMAGE_LABELS" to "io.github.aanbrn.courses.env.SERVER_PORT=8080 …",
       ))
       buildpacks.set(listOf("urn:cnb:builder:paketo-buildpacks/java", "docker.io/paketobuildpacks/health-checker:<pin>"))
   }
   ```

   Result: arm64-native, non-root `1002:1001`, `created=1980-01-01`, ~350 MB, starts in 1.5 s. At `-m 512m`:
   `-Xmx302955K` (vs `-Xmx122731K` with the 240M default code cache).
2. **Frontend** — Gradle builds `dist/` once; a `Zip` task packs `dist/` + `nginx.conf` + `mime.types`; a
   `BootBuildImage` task (Spring Boot plugin applied only for the task type) builds it:
   builder `paketobuildpacks/ubuntu-noble-builder`, buildpacks `paketo-buildpacks/web-servers` + health-checker,
   env `BPE_DEFAULT_API_UPSTREAM=http://bff:8080`, `BPE_DEFAULT_PROXY_BUFFERING=off`, `BP_HEALTH_CHECKER_ENABLED=true`.
   Run image: full `ubuntu-noble-run`. No Node, no pnpm, no `pack`.
3. **Custom `nginx.conf`** (rendered by the buildpack at launch; full file in
   `sources/005-…/web-custom/nginx.conf`):
   - `listen {{port}};` — no `pid` directive (the buildpack passes `-g 'pid /tmp/nginx.pid;'`);
   - `client_body_temp_path`, `proxy_temp_path`, `fastcgi_temp_path` under `{{ tempDir }}` (`/workspace` is read-only);
   - `include mime.types;` with a vendored nginx upstream `conf/mime.types` (the buildpack ships none);
   - `root dist;` `location / { try_files $uri $uri/ /index.html; add_header Cache-Control "no-cache"; }`;
   - `location /assets/ { add_header Cache-Control "public, max-age=31536000, immutable"; try_files $uri =404; }`;
   - `location ~ /\.(?!well-known) { deny all; return 404; }`, `location = /healthz` (static file);
   - `location /api/ { proxy_pass {{env "API_UPSTREAM"}}; proxy_http_version 1.1; proxy_set_header Connection "";
     proxy_buffering {{env "PROXY_BUFFERING"}}; proxy_cache off; proxy_read_timeout 1h; … }`.
4. **Compose health checks** (RES-07): `test: ["CMD", "/workspace/health-check"]` with **container env**
   `THC_PORT`/`THC_PATH` (`/actuator/health/readiness` for services, `/healthz` for the UI).
5. **Docs for env defaults** (INFRA-17): `BPE_*` never appear in `Config.Env`; document them with `BP_IMAGE_LABELS`
   labels (Image Labels buildpack, part of the java composite).
6. Pin builder, run and buildpack images by digest in the catalog (INFRA-09).

## What to Avoid

- **`BPE_DEFAULT_JAVA_TOOL_OPTIONS`**: ignored, Liberica sets `JAVA_TOOL_OPTIONS` itself. Use `PREPEND` (+ `DELIM`);
  a runtime `JAVA_TOOL_OPTIONS` still wins because it comes later.
- Appending `-XX:+ExitOnOutOfMemoryError`: Liberica already adds it.
- Relying on image `BPE_DEFAULT_THC_*` for `docker exec` health checks: `exec` bypasses the CNB launcher → thc
  probes `/` → 404.
- Listing the health-checker buildpack without `BP_HEALTH_CHECKER_ENABLED=true`: detection fails for the whole group
  ("No buildpack groups passed detection").
- **Generated nginx config** (`BP_WEB_SERVER_*` + include file): missing assets return `index.html` with 200, no cache
  headers, `/healthz` as `application/octet-stream`, static (non-templated) upstream.
- nginx on `ubuntu-noble-run-tiny`: `libcrypt.so.1: cannot open shared object file`.
- `uwsgi_temp_path`/`scgi_temp_path`: unknown directives in the buildpack's nginx build.
- The Paketo sample `mime.types`: stale (`application/x-javascript`).
- `builder-jammy-base`: amd64 only.
- The `pack` CLI: a second toolchain, and it hits the same Docker 29 export bug.

## Constraints

- **colima DNS**: the built-in forwarder `192.168.5.1` never answers AAAA; Go tools (CNB lifecycle, buildpacks) wait
  10 s per lookup and downloads fail (`lookup github.com: i/o timeout`). Fix: `colima start --dns <host resolvers>`
  (persists as `network.dns`). `doctor` check: time an AAAA lookup in a container.
- **Docker 29 containerd image store**: `docker save --platform linux/arm64 paketobuildpacks/ubuntu-noble-run` fails
  (`no suitable export target found … does not provide the specified platform`), which breaks the export of every
  image on the full noble run image (moby#52193, docker/cli#6457; also seen on CI runners). Fix: classic store
  (`docker.features.containerd-snapshotter: false` in colima). `doctor` check: `docker info` driver-type
  `io.containerd.snapshotter.v1` → warn. CI: pin the runner image or disable the snapshotter for the image job.
- Memory calculator fails fast when fixed regions exceed the limit (e.g. 200 threads at 512 MB), instead of OOMing.
- SSE: events arrived every 0.5 s through nginx with `proxy_buffering off`; the `on` control did not reproduce
  buffering for small events, so `off` is an explicit guarantee rather than a demonstrated necessity. Idle-stream
  timeout not exercised.
- Build times on colima: Boot image ~4 min cold / ~20 s warm; nginx image ~10 s warm.

## Origin

Synthesized from spikes: 005
Source files available in: sources/005-paketo-web-and-boot-images/
