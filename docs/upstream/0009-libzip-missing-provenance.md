---
id: "UP-0009"
product: "npm:renovate"
product-version: "44.252.8"
status: "reported"
workaround: true
upstream: ["https://github.com/yarnpkg/berry/issues/7182"]
reported: "2026-06-08"
updated: "2026-11-23"
---

# UP-0009: `@yarnpkg/libzip@3.2.2`, pulled by Renovate, was published without provenance

## Symptom

mise installs the `npm:renovate` tool with npm's `trustPolicy=no-downgrade`, which refuses a package whose release
lost the provenance attestation that an earlier release had. Renovate depends transitively on `@yarnpkg/libzip`, whose
releases 3.2.0 and 3.2.1 carry npm provenance attestations while 3.2.2 does not, so the install of Renovate fails the
trust policy with a downgrade error for `@yarnpkg/libzip@3.2.2`.

## Minimal reproduction

No repository is needed, only the public npm registry:

```sh
for version in 3.2.1 3.2.2; do
  printf '%s: ' "$version"
  curl -fsS "https://registry.npmjs.org/@yarnpkg%2flibzip/$version" | jq -c '.dist.attestations'
done
```

Version 3.2.1 prints an attestations object, 3.2.2 prints `null` (checked on 2026-11-23: the registry also shows 3.2.2
as the latest release, published by a person and not by the release bot that published 3.2.0 and 3.2.1). Installing a
package that depends on 3.2.2 with a trust policy that rejects provenance downgrades then fails: a commenter on the
upstream issue reports `ERR_PNPM_TRUST_DOWNGRADE` for `@yarnpkg/libzip@3.2.2` from `pnpm dlx renovate` with the trust
policy `no-downgrade`.

## Root cause

Described in the upstream issue: the 3.2.2 release commit came from a cherry-pick branch whose `.yarnrc.yml` lacked
`npmPublishProvenance: true`, a setting that exists on the main branch, so the Yarn binary built from that branch
published without provenance. The release is the newest one, so every Renovate version that depends on `libzip` with
a compatible range resolves it.

## Workaround

An exact-version exception in the mise trust policy, approved by the author:

- `mise.toml`: `trust_policy_excludes = ["@yarnpkg/libzip@3.2.2"]` on the `npm:renovate` entry, carrying
  `UPSTREAM: UP-0009`. The exception names one version, so a later `libzip` release without provenance is rejected
  again.

## Removal criteria

Either of these makes the exception unnecessary:

- `@yarnpkg/libzip` publishes a release newer than 3.2.2 with provenance, and Renovate's dependency range resolves it
  (the upstream issue suggests exactly such a patch release), or
- a Renovate release stops depending on `@yarnpkg/libzip@3.2.2`.

## How to verify removal

Remove `trust_policy_excludes` from the `npm:renovate` entry in `mise.toml` and run
`mise install npm:renovate`. The exception is no longer needed when the install succeeds with the trust policy active,
and `curl -fsS https://registry.npmjs.org/@yarnpkg%2flibzip | jq -r '."dist-tags".latest'` names a version whose
`.dist.attestations` is not `null`.

## Upstream issue draft

Not needed: `https://github.com/yarnpkg/berry/issues/7182` (opened 2026-06-08, open) is this defect, with the root
cause, the verification command and a suggested fix. Add a reaction or a comment only after the author approves it
(plan 02-22).
