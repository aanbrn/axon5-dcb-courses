---
id: "UP-0011"
product: "colima"
product-version: "0.10.3"
status: "open"
workaround: true
upstream: []
reported: null
updated: "2026-11-25"
---

# UP-0011: A colima profile without explicit resolvers once stalled Go DNS lookups for about 10 s, not reproduced on a clean profile

## Symptom

On a colima VM with the default DNS settings, a container that resolves a name with a Go program waits about 10 seconds
per lookup, because the Go resolver sends the A and the AAAA query together and waits for both. The first
`bootBuildImage` of this repository failed with Cloud Native Buildpacks (a Go tool) reporting
`unable to get dependency BellSoft Liberica JRE` and `lookup github.com: i/o timeout`, while `busybox` resolved the same
name at once (spike 005). A re-run on a throwaway clean profile on 2026-11-25 did not reproduce the stall (see Minimal
reproduction), so the symptom is recorded as observed once and not yet explained.

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
profile that has explicit resolvers (168 ms). The 10 s side was observed in the spike (spike 005).

Clean-profile result of 2026-11-25: not reproduced. On colima 0.10.3 (macOS 27.2.1, Apple Silicon, `vz` VM, Docker
runtime, 1 CPU, 2 GiB of memory, 10 GiB of disk), a throwaway profile was created with `--template=false` (the user
template of the author sets `network.address: true`, Kubernetes and a `dnsHosts` entry, so it is not a clean default)
and without `--dns`; its `colima.yaml` had `network.dns: null`, `network.address: false` and `dnsHosts: {}`, and the
resolver inside the containers was colima's forwarder `192.168.5.1`. The Go program printed three durations per
variant, the first one including a cold start of the container:

| Variant of the throwaway profile                         | Run 1     | Run 2   | Run 3   | Result         |
| -------------------------------------------------------- | --------- | ------- | ------- | -------------- |
| clean defaults (`network.address: false`, no `dnsHosts`) | 103.46 ms | 1.91 ms | 2.51 ms | not reproduced |
| `--network-address` (`network.address: true`)            | 132.25 ms | 2.51 ms | 2.37 ms | not reproduced |
| `--dns-host host.docker.internal=host.lima.internal`     | 103.31 ms | 2.68 ms | 2.32 ms | not reproduced |

No run printed an error, and the image pulls of the Docker daemon (also a Go program) resolved their registry without a
delay. A `busybox` `nslookup github.com` as a control took 0.14 s with `network.address: true` and 0.17 s with the
`dnsHosts` entry; the control of the clean-defaults variant took 7.3 s of wall time, which includes the first pull of
the `busybox` image and is therefore not a lookup time. The two extra variants separate the settings in which the user
template differs from a clean default and which `abiosoft/colima#1275` concerns (`network.address`) or which change the
resolver (`dnsHosts`); neither of them brings the stall back. The profile was deleted afterwards, and the author's own
profile was not touched.

## Root cause

Not established. The spike observed (spike 005) that the resolver address that colima hands to the VM and to its
containers (the built-in forwarder on the VM's gateway address, `192.168.5.1`) answers A queries but did not answer AAAA
queries in time, and that a resolver that waits for both answers, such as Go's, runs into its timeout. Setting
resolvers explicitly makes colima skip the built-in forwarder (the colima configuration notes that `dnsHosts` has no
effect once a resolver list is supplied), which is why the workaround helped. The clean-profile run of 2026-11-25 did
not reproduce the stall with the same forwarder, so the AAAA behaviour is not a property of the colima 0.10.3 defaults
by themselves. What differed in the spike is not known: the host's own upstream resolvers at that time, the rest of the
user template (Kubernetes, Rosetta, VirtioFS mounts) or the history of that VM are candidates, and none was tested.
Which component drops the AAAA query, the forwarder of Lima or colima's own configuration, was not investigated.

The author's assessment of 2026-11-25 is that the stall most likely came from a nuance of the local DNS settings of the
spike's machine, not from a colima default. That assessment is untested, so the record stays open, and the explicit
`network.dns` workaround and the `colima` check stay.

## Workaround

Explicit `network.dns` in the colima VM (`colima start --dns <resolver> --dns <resolver>`, persisted by colima in its
`colima.yaml`). This is host configuration and not part of the repository, so the repository only checks it.

- `scripts/doctor.sh`: the `colima` check warns when `network.dns` is empty and prints the `colima start --dns ...` fix,
  carrying `UPSTREAM: UP-0011`.

## Removal criteria

A colima release in which a default profile resolves AAAA queries (or answers them with an empty result) without
delay, confirmed by the verification below, and named in the colima release notes or the upstream report. Then the DNS
half of the `colima` check can go.

The verification already passes on colima 0.10.3 for a clean profile (2026-11-25, Minimal reproduction), so the record
cannot be closed by a release alone: the cause of the stall in the spike's profile has to be identified, or the author
has to decide that the warning of the `colima` check is no longer wanted. The explicit `network.dns` workaround and the
check stay until the author decides otherwise.

## How to verify removal

Start a throwaway profile on the candidate colima release without `--dns` and without the user template, so that a
template cannot mask the result (`colima start dns-check --template=false --runtime docker --cpus 1 --memory 2 --disk 10`),
check in `~/.colima/dns-check/colima.yaml` that `network.dns` is empty, run the Go lookup of the minimal reproduction
against it with `docker --context colima-dns-check ...`, and delete the profile (`colima delete --force dns-check`).
The defect is gone when the lookup prints a duration well under one second in several runs. To test the template of the
author instead, drop `--template=false`; the throwaway profile then also inherits the other settings of the template
(Kubernetes, Rosetta, a reachable address and custom DNS hosts).

## Upstream issue draft

Colima's tracker is `https://github.com/abiosoft/colima/issues`. A search on 2026-11-23 found related but different
reports: `abiosoft/colima#1275` (AAAA records not resolved in containers with `network.address=true`, which is about
IPv6 addresses on a dual-stack host) and several general DNS reports (`#711`, `#785`, `#1260`). None describes the
default forwarder dropping AAAA queries and the resulting 10 second Go lookups. The clean-profile reproduction of
2026-11-25 did not confirm the stall (Minimal reproduction), so this draft is not publishable as it stands: it needs a
condition under which the stall reproduces, which the draft has to name, before an issue is justified. The text below
has the environment and the measured numbers of the clean-profile run, and keeps the spike's observation as what was
seen once.

````markdown
# Go DNS lookups in containers took about 10 seconds on a colima profile without explicit resolvers (AAAA suspected)

## Environment and versions

- Product and version: colima 0.10.3
- Related products and versions: Docker as the runtime, Go 1.25 in the test container (`golang:1.25-alpine`)
- Operating system and build tool: macOS 27.2.1 on Apple Silicon, `vz` VM type

## Steps to reproduce

1. Start a profile without custom resolvers and without a user template:
   `colima start dns-check --template=false --cpus 1 --memory 2 --disk 10`.
2. Save the Go program of the minimal reproduction below as `lookup.go` and run it in a container:
   `docker create --name lookup golang:1.25-alpine go run /tmp/lookup.go`, `docker cp lookup.go lookup:/tmp/lookup.go`,
   `docker start -a lookup`.

## Expected behavior

The lookup takes about as long as an A lookup (around 100 ms).

## Actual behavior

[To be completed once a reproducing condition is known. Observed once, in a spike on a profile created from a user
template:] the lookup took about 10 seconds or failed with `i/o timeout`. Tools written in Go, such as the Cloud Native
Buildpacks lifecycle, failed to download dependencies with `lookup <host>: i/o timeout`. A `busybox` `nslookup` of the
same name was fast, and so was the same Go program once the profile was started with `--dns <resolver>`. On a clean
profile (colima 0.10.3, no template, no `--dns`) the Go program printed 103 ms, 1.9 ms and 2.5 ms in three runs, so the
stall did not reproduce.

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

The Go resolver sends the A and AAAA queries in parallel and waits for both. In the spike, the forwarder that the
profile used appeared not to answer the AAAA query in time; a clean profile did not show this. What differed was not
determined.

## Proposed fix

Answer AAAA queries (an empty answer is enough), or document the stall and recommend `--dns` for Go tooling.
````
