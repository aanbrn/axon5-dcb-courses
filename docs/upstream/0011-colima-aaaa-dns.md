---
id: "UP-0011"
product: "colima"
product-version: "0.10.3"
status: "open"
workaround: true
upstream: []
reported: null
updated: "2026-11-23"
---

# UP-0011: The default colima DNS forwarder never answers AAAA queries and stalls Go tools

## Symptom

On a colima VM with the default DNS settings, a container that resolves a name with a Go program waits about 10 seconds
per lookup, because the Go resolver sends the A and the AAAA query together and waits for both. The first
`bootBuildImage` of this repository failed with Cloud Native Buildpacks (a Go tool) reporting
`unable to get dependency BellSoft Liberica JRE` and `lookup github.com: i/o timeout`, while `busybox` resolved the same
name at once (spike 005).

The product key `colima` is a host tool without a pinned key in this repository, so the dependency-update advisory
never matches this record.

## Minimal reproduction

On a colima profile started without `--dns` (the default `network.dns: []`), with a Docker runtime. Save the Go program
of the issue draft below as `lookup.go`, then copy it into a container (a bind mount would fail for paths outside
colima's mounts):

```sh
docker create --name lookup golang:1.25-alpine go run /tmp/lookup.go
docker cp lookup.go lookup:/tmp/lookup.go
docker start -a lookup
docker rm lookup
```

Expected with the defect: a duration of about 10 s (or an `i/o timeout` error). The same program prints about 100 ms
when the profile has explicit upstream resolvers, as in the spike (`--dns 8.8.4.4`: 130 ms; `network=host`, a
user-defined network and `GODEBUG` settings did not help). The program and the commands were run on 2026-11-23 on a
profile that has explicit resolvers (168 ms). The 10 s side was observed in the spike (spike 005) and
has not been re-run on a clean profile: a throwaway profile needs a multi-hundred-megabyte VM image download, which was
abandoned for this plan. Re-running it is the first step of plan 02-22 before the issue is published.

## Root cause

Observed, not traced to source: the resolver address that colima hands to the VM and to its containers (the built-in
forwarder on the VM's gateway address, `192.168.5.1` in the spike) answers A queries but never answers AAAA queries. A
resolver that waits for both answers, such as Go's, runs into its timeout. Setting resolvers explicitly makes colima
skip the built-in forwarder (the colima configuration notes that `dnsHosts` has no effect once a resolver list is
supplied), which is why the workaround helps. Which component drops the AAAA query, the forwarder of Lima or colima's
own configuration, was not investigated.

## Workaround

Explicit `network.dns` in the colima VM (`colima start --dns <resolver> --dns <resolver>`, persisted by colima in its
`colima.yaml`). This is host configuration and not part of the repository, so the repository only checks it.

- `scripts/doctor.sh`: the `colima` check warns when `network.dns` is empty and prints the `colima start --dns ...` fix,
  carrying `UPSTREAM: UP-0011`.

## Removal criteria

A colima release in which a default profile resolves AAAA queries (or answers them with an empty result) without
delay, confirmed by the verification below, and named in the colima release notes or the upstream report. Then the DNS
half of the `colima` check can go.

## How to verify removal

Start a throwaway profile on the candidate colima release without `--dns`
(`colima start dns-check --cpu 1 --memory 1 --disk 10`), run the Go lookup of the minimal reproduction against it with
`docker --context colima-dns-check run ...`, and delete the profile (`colima delete --force dns-check`). The defect is
gone when the lookup prints a duration well under one second in several runs.

## Upstream issue draft

Colima's tracker is `https://github.com/abiosoft/colima/issues`. A search on 2026-11-23 found related but different
reports: `abiosoft/colima#1275` (AAAA records not resolved in containers with `network.address=true`, which is about
IPv6 addresses on a dual-stack host) and several general DNS reports (`#711`, `#785`, `#1260`). None describes the
default forwarder dropping AAAA queries and the resulting 10 second Go lookups, so a new issue is justified once the
reproduction below is confirmed on a clean profile (plan 02-22).

````markdown
# Default DNS forwarder never answers AAAA queries; Go programs wait about 10 seconds per lookup

## Environment and versions

- Product and version: colima 0.10.3
- Related products and versions: Docker 29.x as the runtime, Go 1.25 in the test container
- Operating system and build tool: macOS on Apple Silicon

## Steps to reproduce

1. Start a profile without custom resolvers: `colima start --cpu 1 --memory 1 --disk 10`.
2. Save the Go program of the minimal reproduction below as `lookup.go` and run it in a container:
   `docker create --name lookup golang:1.25-alpine go run /tmp/lookup.go`, `docker cp lookup.go lookup:/tmp/lookup.go`,
   `docker start -a lookup`.

## Expected behavior

The lookup takes about as long as an A lookup (around 100 ms).

## Actual behavior

The lookup takes about 10 seconds or fails with `i/o timeout`. Tools written in Go, such as the Cloud Native Buildpacks
lifecycle, fail to download dependencies with `lookup <host>: i/o timeout`. A `busybox` `nslookup` of the same name is
fast, and so is the same Go program once the profile is started with `--dns <resolver>`.

## Minimal reproduction

```go
package main

import (
	"fmt"
	"net"
	"time"
)

func main() {
	start := time.Now()
	_, err := net.LookupHost("github.com")
	fmt.Println(time.Since(start), err)
}
```

## Analysis

The Go resolver sends the A and AAAA queries in parallel and waits for both. The forwarder that the profile uses by
default appears never to answer the AAAA query. Which component drops it was not determined.

## Proposed fix

Answer AAAA queries (an empty answer is enough), or document the stall and recommend `--dns` for Go tooling.
````
