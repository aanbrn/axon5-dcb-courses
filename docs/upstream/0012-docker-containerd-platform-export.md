---
id: "UP-0012"
product: "docker"
product-version: "29.5.2"
status: "fixed-upstream"
workaround: true
upstream: ["https://github.com/moby/moby/issues/52193", "https://github.com/docker/cli/issues/6457"]
reported: "2026-03-19"
updated: "2026-11-23"
---

# UP-0012: Docker 29's containerd image store cannot `docker save --platform` the Paketo noble run image

## Symptom

With the containerd image store enabled, exporting a single platform of an image that was pulled for that platform
fails:

```text
Error response from daemon: no suitable export target found: image with reference <image> was found but does not
provide the specified platform (linux/arm64)
```

On this repository the image is `paketobuildpacks/ubuntu-noble-run`, the run image of the web (nginx) image. Both `pack`
0.40.11 and Spring Boot's `bootBuildImage`, which export onto the run image, fail the same way, so no container image
of the project could be built (spike 005). The tiny run image of the Boot service image saved fine. Other
projects hit the same failure on CI runners that use the containerd store (`kubeflow/pipelines#15118`,
`superfly/flyctl#5360`), so the CI image jobs need the engine version checked as well.

The defect is fixed upstream: `moby/moby#52193` was closed as completed on 2026-11-16 with the explanation that
`moby/moby#53615` ("Always fetch missing layer content", released in Docker 29.9.0) fixes its root cause
`moby/moby#49784`. This repository's engine (Docker 29.5.2 on colima) is older, so the workaround stays until the
engines are upgraded and the removal check below passes.

## Minimal reproduction

On a machine whose Docker engine uses the containerd image store (Docker Desktop with "Use containerd for pulling and
storing images" turned on, or colima with `docker.features.containerd-snapshotter: true`) and an engine older than 29.9.0:

```sh
docker pull --platform linux/arm64 paketobuildpacks/ubuntu-noble-run:latest
docker save --platform linux/arm64 paketobuildpacks/ubuntu-noble-run:latest | tar t
```

Expected with the defect: `no suitable export target found ... does not provide the specified platform
(linux/arm64)`. Re-pulling, pinning the arm64 digest and publishing to a local registry did not help in the spike. The
upstream report reproduces it with `heroku/heroku:24` instead of a Paketo image, and describes that the failure appears
when layers of the pulled image had already been unpacked for another image.

## Root cause

Described in the upstream discussion: with the containerd image store, a layer whose snapshot already exists (because
another image shares it) was not pulled again as content, so the image was missing blobs and a platform-specific
export found nothing to write. That the tiny run image saved fine while the full noble run image failed fits shared layers, which is plausible but
was not checked. `moby/moby#53615` makes the daemon always fetch missing layer content. That
the fix covers the Paketo noble run image is likely, by the matching symptom and cause, but is not verified on this
repository's images.

## Workaround

This repository uses the classic image store and warns when the containerd store is active.

- `scripts/doctor.sh`: the `image-store` check warns when the engine reports the `overlayfs` driver or the
  `io.containerd.snapshotter` entry, with the fix for colima and for Docker Desktop, and carries `UPSTREAM: UP-0012`.
- `scripts/doctor.sh`: the `colima` check reports `docker.features.containerd-snapshotter` when it is not `false`, and
  carries `UPSTREAM: UP-0012`.

## Removal criteria

Every engine that builds this project's images runs Docker 29.9.0 or newer (the latest release at the time of writing is
29.10.0): the developer machines and the GitHub-hosted runner images used by the CI image jobs. The status moves to
`workaround-removed` when the verification below passes with the containerd image store enabled on such an engine.

## How to verify removal

On an engine of 29.9.0 or newer with the containerd image store enabled (`docker info --format '{{.Driver}}'` prints
`overlayfs`), run the two commands of the minimal reproduction. The defect is gone when `docker save` lists the blobs of
the image, and the project's image build (`./gradlew bootBuildImage` and the web image task) succeeds. Then relax the
`image-store` and `colima` checks of `scripts/doctor.sh` to accept both stores, drop both markers, and update the
quick start.

## Upstream issue draft

Not needed: `https://github.com/moby/moby/issues/52193` (opened 2026-03-19, closed as completed on 2026-11-16) and
`https://github.com/docker/cli/issues/6457` (opened 2025-09-18, closed as not planned) are this defect, and the fix
`moby/moby#53615` is released. No upstream action remains except, if the author wants, a comment on `moby/moby#52193`
that the Paketo noble run image was affected too, after the verification above and with the author's approval (plan
02-22).
